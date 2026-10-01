package com.example.aijobs.resume;

import com.example.aijobs.common.BusinessException;
import com.example.aijobs.resume.dto.ResumeImportPreviewResponse;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.encryption.InvalidPasswordException;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.Writer;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;

@Service
public class ResumeImportService {
    public static final int MAX_BYTES = 2 * 1024 * 1024;
    public static final int MAX_TEXT_LENGTH = 20000;
    public static final int MAX_PAGES = 20;

    public ResumeImportPreviewResponse preview(MultipartFile file) {
        if (file.isEmpty()) throw error(HttpStatus.BAD_REQUEST, "请选择非空的简历文件");
        if (file.getSize() > MAX_BYTES) throw error(HttpStatus.PAYLOAD_TOO_LARGE, "简历文件不能超过 2 MiB");
        String filename = file.getOriginalFilename() == null ? "" : file.getOriginalFilename();
        filename = filename.replace('\\', '/');
        filename = filename.substring(filename.lastIndexOf('/') + 1);
        String extension = filename.toLowerCase(Locale.ROOT);
        if (!extension.endsWith(".pdf") && !extension.endsWith(".txt")) {
            throw error(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "仅支持文本型 PDF 或 UTF-8 TXT 文件");
        }
        try (var input = file.getInputStream()) {
            byte[] bytes = input.readNBytes(MAX_BYTES + 1);
            if (bytes.length > MAX_BYTES) throw error(HttpStatus.PAYLOAD_TOO_LARGE, "简历文件不能超过 2 MiB");
            if (extension.endsWith(".pdf")) return pdf(filename, bytes);
            String text = StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT).onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(bytes)).toString();
            if (text.startsWith("%PDF-") || text.codePoints().anyMatch(c ->
                    Character.isISOControl(c) && c != '\n' && c != '\r' && c != '\t' && c != '\f')) {
                throw error(HttpStatus.UNPROCESSABLE_ENTITY, "文件内容不是有效的纯文本，请检查文件格式");
            }
            return new ResumeImportPreviewResponse(filename, "TXT", 0, checkedText(text), List.of());
        } catch (CharacterCodingException exception) {
            throw error(HttpStatus.UNPROCESSABLE_ENTITY, "TXT 文件须采用 UTF-8 编码");
        } catch (InvalidPasswordException exception) {
            throw error(HttpStatus.UNPROCESSABLE_ENTITY, "不支持加密 PDF，请提供未加密的文本型 PDF");
        } catch (TextLimitException exception) {
            throw error(HttpStatus.PAYLOAD_TOO_LARGE, "提取文本不能超过 20,000 字符，请缩短文件后重试");
        } catch (IOException exception) {
            throw error(HttpStatus.UNPROCESSABLE_ENTITY, "无法读取简历文件，请检查文件是否损坏");
        }
    }

    private ResumeImportPreviewResponse pdf(String filename, byte[] bytes) throws IOException {
        if (bytes.length < 5 || !new String(bytes, 0, 5, StandardCharsets.US_ASCII).equals("%PDF-")) {
            throw error(HttpStatus.UNPROCESSABLE_ENTITY, "PDF 文件内容与格式不符");
        }
        try (PDDocument document = Loader.loadPDF(bytes)) {
            if (document.isEncrypted() || !document.getCurrentAccessPermission().canExtractContent()) {
                throw error(HttpStatus.UNPROCESSABLE_ENTITY, "不支持加密或限制文本提取的 PDF");
            }
            if (document.getNumberOfPages() > MAX_PAGES) {
                throw error(HttpStatus.PAYLOAD_TOO_LARGE, "PDF 不能超过 20 页");
            }
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);
            LimitedTextWriter writer = new LimitedTextWriter();
            stripper.writeText(document, writer);
            String text = checkedText(writer.text.toString());
            return new ResumeImportPreviewResponse(filename, "PDF", document.getNumberOfPages(), text,
                    List.of("PDF 分栏、表格的阅读顺序可能变化，请核对提取文本；本功能不含 OCR。"));
        }
    }

    private String checkedText(String text) {
        if (text.length() > MAX_TEXT_LENGTH) {
            throw error(HttpStatus.PAYLOAD_TOO_LARGE, "提取文本不能超过 20,000 字符，请缩短文件后重试");
        }
        String cleaned = text.replace("\uFEFF", "").replace("\r\n", "\n").replace('\r', '\n').strip();
        if (cleaned.isBlank()) {
            throw error(HttpStatus.UNPROCESSABLE_ENTITY, "未提取到文字；扫描件请先进行 OCR 或提供 UTF-8 TXT");
        }
        return cleaned;
    }

    private static BusinessException error(HttpStatus status, String message) {
        return new BusinessException(status, message);
    }

    private static class TextLimitException extends IOException { }

    private static class LimitedTextWriter extends Writer {
        private final StringBuilder text = new StringBuilder();
        @Override public void write(char[] buffer, int offset, int length) throws IOException {
            if ((long) text.length() + length > MAX_TEXT_LENGTH) throw new TextLimitException();
            text.append(buffer, offset, length);
        }
        @Override public void flush() { }
        @Override public void close() { }
    }
}
