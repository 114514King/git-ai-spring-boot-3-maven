package com.example.aijobs.externaljobs;

import com.example.aijobs.common.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.http.ResponseEntity;

@RestController
public class ExternalJobsController {
    private final ExternalJobsService service;

    public ExternalJobsController(ExternalJobsService service) {
        this.service = service;
    }

    @GetMapping("/api/external-jobs")
    public ApiResponse<ExternalJobsPage> list(@RequestParam(required = false) String keyword,
                                             @RequestParam(required = false) String location,
                                             @RequestParam(defaultValue = "1") int page,
                                             @RequestParam(defaultValue = "20") int pageSize) {
        return ApiResponse.success("查询成功", service.list(keyword, location, page, pageSize));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<Void>> invalidParameter() {
        return ResponseEntity.badRequest().body(ApiResponse.failure("页码和每页数量须为整数"));
    }
}
