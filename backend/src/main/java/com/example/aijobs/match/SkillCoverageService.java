package com.example.aijobs.match;

import com.example.aijobs.common.BusinessException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
public class SkillCoverageService {
    public static final String MODEL_NAME = "local-skill-coverage-v1";

    public Evidence calculate(String resumeText, String requirements) {
        Set<String> required = SkillNormalizer.normalize(requirements);
        if (required.isEmpty()) {
            throw new BusinessException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "岗位要求中未识别到词典支持的技能，无法计算覆盖率；请 HR 补充明确技能后重试。");
        }
        Set<String> matched = new LinkedHashSet<>(required);
        matched.retainAll(SkillNormalizer.normalize(resumeText));
        Set<String> missing = new LinkedHashSet<>(required);
        missing.removeAll(matched);
        BigDecimal score = BigDecimal.valueOf(matched.size()).multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(required.size()), 2, RoundingMode.HALF_UP);
        return new Evidence(List.copyOf(required), List.copyOf(matched), List.copyOf(missing), score);
    }

    public record Evidence(List<String> required, List<String> matched, List<String> missing,
                           BigDecimal score) {
        public Evidence {
            required = List.copyOf(required);
            matched = List.copyOf(matched);
            missing = List.copyOf(missing);
        }
    }
}
