package com.example.aijobs.admin;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.aijobs.admin.dto.AdminAiOperationInsight;
import com.example.aijobs.admin.dto.AdminDashboardResponse;
import com.example.aijobs.admin.dto.AdminStatusCount;
import com.example.aijobs.application.entity.JobApplication;
import com.example.aijobs.application.mapper.JobApplicationMapper;
import com.example.aijobs.auth.entity.PlatformUser;
import com.example.aijobs.auth.entity.Role;
import com.example.aijobs.auth.entity.UserRole;
import com.example.aijobs.auth.mapper.PlatformUserMapper;
import com.example.aijobs.auth.mapper.RoleMapper;
import com.example.aijobs.auth.mapper.UserRoleMapper;
import com.example.aijobs.job.entity.JobPosting;
import com.example.aijobs.job.mapper.JobPostingMapper;
import com.example.aijobs.match.entity.AiMatchResult;
import com.example.aijobs.match.mapper.AiMatchResultMapper;
import com.example.aijobs.resume.entity.Resume;
import com.example.aijobs.resume.mapper.ResumeMapper;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class AdminDashboardService {
    private static final List<String> ROLE_CODES = List.of("STUDENT", "HR", "ADMIN");
    private static final String AI_OPERATION_MODEL = "local-admin-ai-ops-v1";

    private final PlatformUserMapper userMapper;
    private final RoleMapper roleMapper;
    private final UserRoleMapper userRoleMapper;
    private final JobPostingMapper jobMapper;
    private final ResumeMapper resumeMapper;
    private final JobApplicationMapper applicationMapper;
    private final AiMatchResultMapper matchMapper;

    public AdminDashboardService(PlatformUserMapper userMapper,
                                 RoleMapper roleMapper,
                                 UserRoleMapper userRoleMapper,
                                 JobPostingMapper jobMapper,
                                 ResumeMapper resumeMapper,
                                 JobApplicationMapper applicationMapper,
                                 AiMatchResultMapper matchMapper) {
        this.userMapper = userMapper;
        this.roleMapper = roleMapper;
        this.userRoleMapper = userRoleMapper;
        this.jobMapper = jobMapper;
        this.resumeMapper = resumeMapper;
        this.applicationMapper = applicationMapper;
        this.matchMapper = matchMapper;
    }

    public AdminDashboardResponse dashboard() {
        List<PlatformUser> users = userMapper.selectList(null);
        List<JobPosting> jobs = jobMapper.selectList(null);
        List<Resume> resumes = resumeMapper.selectList(null);
        List<JobApplication> applications = applicationMapper.selectList(null);
        List<AiMatchResult> matches = matchMapper.selectList(null);
        Map<String, Long> roleCounts = countUsersByRole();

        long activeUsers = users.stream().filter(user -> "ACTIVE".equals(user.getStatus())).count();
        long publishedJobs = jobs.stream().filter(job -> "PUBLISHED".equals(job.getStatus())).count();
        long publishedResumes = resumes.stream().filter(resume -> "PUBLISHED".equals(resume.getStatus())).count();
        long activeApplications = applications.stream()
                .filter(application -> !"WITHDRAWN".equals(application.getStatus()))
                .count();
        BigDecimal averageMatchScore = averageScore(matches);

        return new AdminDashboardResponse(
                users.size(),
                activeUsers,
                roleCounts.getOrDefault("STUDENT", 0L),
                roleCounts.getOrDefault("HR", 0L),
                roleCounts.getOrDefault("ADMIN", 0L),
                jobs.size(),
                publishedJobs,
                resumes.size(),
                publishedResumes,
                applications.size(),
                activeApplications,
                matches.size(),
                averageMatchScore,
                ROLE_CODES.stream()
                        .map(code -> new AdminStatusCount(code, roleCounts.getOrDefault(code, 0L)))
                        .toList(),
                countStatus(jobs, JobPosting::getStatus),
                countStatus(resumes, Resume::getStatus),
                countStatus(applications, JobApplication::getStatus),
                buildAiOperationInsight(publishedJobs, publishedResumes, activeApplications, applications, matches,
                        averageMatchScore));
    }

    private Map<String, Long> countUsersByRole() {
        return roleMapper.selectList(null).stream()
                .collect(Collectors.toMap(Role::getCode, role -> userRoleMapper.selectCount(
                        new LambdaQueryWrapper<UserRole>().eq(UserRole::getRoleId, role.getId()))));
    }

    private <T> List<AdminStatusCount> countStatus(List<T> rows, Function<T, String> statusGetter) {
        Map<String, Long> counts = rows.stream()
                .collect(Collectors.groupingBy(statusGetter, Collectors.counting()));
        return counts.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> new AdminStatusCount(entry.getKey(), entry.getValue()))
                .toList();
    }

    private BigDecimal averageScore(List<AiMatchResult> matches) {
        if (matches.isEmpty()) {
            return BigDecimal.ZERO.setScale(2);
        }

        BigDecimal total = matches.stream()
                .map(AiMatchResult::getScore)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return total.divide(BigDecimal.valueOf(matches.size()), 2, RoundingMode.HALF_UP);
    }

    private AdminAiOperationInsight buildAiOperationInsight(long publishedJobs,
                                                            long publishedResumes,
                                                            long activeApplications,
                                                            List<JobApplication> applications,
                                                            List<AiMatchResult> matches,
                                                            BigDecimal averageMatchScore) {
        BigDecimal coverageRate = percentage(matches.size(), activeApplications);
        long lowScoreMatches = matches.stream()
                .filter(match -> match.getScore() != null && match.getScore().compareTo(BigDecimal.valueOf(60)) < 0)
                .count();
        long withdrawnApplications = applications.stream()
                .filter(application -> "WITHDRAWN".equals(application.getStatus()))
                .count();

        List<String> focusAreas = new ArrayList<>();
        focusAreas.add("AI 匹配覆盖率 " + coverageRate + "%，当前已生成 " + matches.size() + " 条匹配结果。");
        focusAreas.add("AI 平均匹配分 " + averageMatchScore + "，低于 60 分的结果 " + lowScoreMatches + " 条。");
        focusAreas.add("有效投递 " + activeApplications + " 条，已发布岗位 " + publishedJobs + " 个，已发布简历 " + publishedResumes + " 份。");

        List<String> riskAlerts = new ArrayList<>();
        if (publishedJobs == 0) {
            riskAlerts.add("暂无已发布岗位，学生端可投递供给不足。");
        }
        if (publishedResumes == 0) {
            riskAlerts.add("暂无已发布简历，AI 匹配和候选人推荐数据不足。");
        }
        if (activeApplications > 0 && coverageRate.compareTo(BigDecimal.valueOf(60)) < 0) {
            riskAlerts.add("AI 匹配覆盖率低于 60%，HR 侧可能缺少筛选参考。");
        }
        if (!matches.isEmpty() && averageMatchScore.compareTo(BigDecimal.valueOf(65)) < 0) {
            riskAlerts.add("AI 平均匹配分低于 65，需要关注岗位要求和简历质量。");
        }
        if (applications.size() > 0
                && percentage(withdrawnApplications, applications.size()).compareTo(BigDecimal.valueOf(30)) >= 0) {
            riskAlerts.add("撤回投递占比较高，需要检查岗位描述清晰度和候选人预期匹配。");
        }
        if (riskAlerts.isEmpty()) {
            riskAlerts.add("当前暂无明显运营风险，建议持续观察新增投递和 AI 匹配质量。");
        }

        List<String> suggestedActions = new ArrayList<>();
        if (activeApplications > matches.size()) {
            suggestedActions.add("优先引导 HR 为未覆盖投递生成 AI 匹配结果。");
        }
        if (lowScoreMatches > 0) {
            suggestedActions.add("抽查低分匹配样本，优化岗位要求表达和简历技能填写。");
        }
        if (publishedJobs == 0 || publishedResumes == 0) {
            suggestedActions.add("补充岗位和简历发布数据后再评估 AI 运营趋势。");
        }
        if (suggestedActions.isEmpty()) {
            suggestedActions.add("保持每日复盘 AI 覆盖率、平均分和投递流转状态。");
        }

        String healthSummary = "本地规则根据匹配覆盖、平均分和投递流转生成运营洞察，未调用外部 AI 服务。";
        return new AdminAiOperationInsight(AI_OPERATION_MODEL, coverageRate, lowScoreMatches, healthSummary,
                focusAreas, riskAlerts, suggestedActions);
    }

    private BigDecimal percentage(long numerator, long denominator) {
        if (denominator <= 0) {
            return BigDecimal.ZERO.setScale(2);
        }
        return BigDecimal.valueOf(Math.min(numerator, denominator))
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(denominator), 2, RoundingMode.HALF_UP);
    }
}
