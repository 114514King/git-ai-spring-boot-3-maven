package com.example.aijobs.resume;

import com.example.aijobs.common.ApiResponse;
import com.example.aijobs.resume.dto.ResumeImportPreviewResponse;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/student/resumes")
public class ResumeImportController {
    private final ResumeImportService importService;

    public ResumeImportController(ResumeImportService importService) {
        this.importService = importService;
    }

    @PostMapping(value = "/import-preview", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<ResumeImportPreviewResponse> preview(@RequestPart("file") MultipartFile file) {
        return ApiResponse.success("提取成功，请核对文本后确认创建草稿", importService.preview(file));
    }
}
