package com.example.aijobs.evaluation;

import com.example.aijobs.common.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/evaluations")
public class ControlledComparisonController {
    private final ControlledComparisonService comparisonService;

    public ControlledComparisonController(ControlledComparisonService comparisonService) {
        this.comparisonService = comparisonService;
    }

    @GetMapping("/day30-controlled-comparison")
    public ApiResponse<ControlledComparisonReport> day30ControlledComparison() {
        return ApiResponse.success("查询成功", comparisonService.runDay30Comparison());
    }
}
