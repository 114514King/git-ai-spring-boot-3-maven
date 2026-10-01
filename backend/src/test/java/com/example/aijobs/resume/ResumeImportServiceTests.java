package com.example.aijobs.resume;

import com.example.aijobs.common.BusinessException;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.encryption.AccessPermission;
import org.apache.pdfbox.pdmodel.encryption.StandardProtectionPolicy;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class ResumeImportServiceTests {
    private final ResumeImportService service = new ResumeImportService();

    @Test
    void extractsUtf8BomChineseAndNormalizesLineEndings() {
        var result = service.preview(file("C:\\fakepath\\简历.TXT", "\uFEFF英语\r\n教学设计\rJava"));
        assertEquals("简历.TXT", result.filename());
        assertEquals("TXT", result.format());
        assertEquals("英语\n教学设计\nJava", result.text());
        assertEquals(0, result.pageCount());
    }

    @Test
    void extractsRealPdfAcrossPagesAndProvidesReviewWarning() throws Exception {
        var result = service.preview(pdf("cv.pdf", pdfBytes("English lesson planning", 2, false)));
        assertEquals(2, result.pageCount());
        assertEquals("PDF", result.format());
        assertTrue(result.text().contains("English lesson planning"));
        assertTrue(result.text().contains("Page 2"));
        assertFalse(result.warnings().isEmpty());
    }

    @Test
    void rejectsEmptyAndWhitespaceFiles() {
        assertStatus(400, file("empty.txt", ""));
        assertStatus(422, file("blank.txt", " \n\t"));
    }

    @Test
    void rejectsUnsupportedExtensionEvenWithTextMime() {
        assertStatus(415, file("cv.docx", "English"));
        assertStatus(415, file("cv.pdf.exe", "English"));
    }

    @Test
    void rejectsBinaryAndInvalidUtf8MasqueradingAsText() {
        assertStatus(422, new MockMultipartFile("file", "cv.txt", "text/plain", new byte[]{(byte) 0xff}));
        assertStatus(422, file("cv.txt", "abc\u0000xyz"));
        assertStatus(422, file("cv.txt", "%PDF-1.7\n"));
    }

    @Test
    void rejectsOversizedUploadAndExcessiveTextWithoutTruncation() {
        assertStatus(413, new MockMultipartFile("file", "cv.txt", "text/plain",
                new byte[ResumeImportService.MAX_BYTES + 1]));
        assertStatus(413, file("cv.txt", "a".repeat(20001)));
        assertEquals(20000, service.preview(file("cv.txt", "a".repeat(20000))).text().length());
    }

    @Test
    void rejectsInvalidPdfHeaderAndCorruptPdf() {
        assertStatus(422, file("cv.pdf", "not a PDF"));
        assertStatus(422, file("cv.pdf", "%PDF-1.7\ncorrupt"));
    }

    @Test
    void rejectsScannedOrBlankPdfWithoutClaimingExtraction() throws Exception {
        assertStatus(422, pdf("scan.pdf", pdfBytes("", 1, false)));
    }

    @Test
    void rejectsPasswordProtectedPdf() throws Exception {
        assertStatus(422, pdf("encrypted.pdf", pdfBytes("English", 1, true)));
    }

    @Test
    void enforcesPdfPageAndExtractedTextLimits() throws Exception {
        assertStatus(413, pdf("long.pdf", pdfBytes("English", 21, false)));
        assertStatus(413, pdf("long-text.pdf", pdfBytes("a".repeat(20001), 1, false)));
        assertEquals(20, service.preview(pdf("limit.pdf", pdfBytes("English", 20, false))).pageCount());
    }

    private void assertStatus(int status, MockMultipartFile file) {
        BusinessException exception = assertThrows(BusinessException.class, () -> service.preview(file));
        assertEquals(status, exception.getStatus().value());
    }

    private MockMultipartFile file(String filename, String text) {
        return new MockMultipartFile("file", filename, "text/plain", text.getBytes(StandardCharsets.UTF_8));
    }

    private MockMultipartFile pdf(String filename, byte[] bytes) {
        // Deliberately inaccurate MIME: actual bytes and filename are validated by the parser.
        return new MockMultipartFile("file", filename, "application/octet-stream", bytes);
    }

    static byte[] pdfBytes(String text, int pages, boolean encrypted) throws Exception {
        try (PDDocument document = new PDDocument(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            for (int i = 1; i <= pages; i++) {
                PDPage page = new PDPage();
                document.addPage(page);
                if (!text.isEmpty()) {
                    try (PDPageContentStream stream = new PDPageContentStream(document, page)) {
                        stream.beginText();
                        stream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                        stream.newLineAtOffset(30, 700);
                        stream.showText(text + " Page " + i);
                        stream.endText();
                    }
                }
            }
            if (encrypted) {
                StandardProtectionPolicy policy = new StandardProtectionPolicy("owner", "reader", new AccessPermission());
                policy.setEncryptionKeyLength(128);
                document.protect(policy);
            }
            document.save(output);
            return output.toByteArray();
        }
    }
}
