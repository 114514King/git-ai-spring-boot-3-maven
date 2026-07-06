package com.example.aijobs.application;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.example.aijobs.application.dto.ApplicationRequest;
import com.example.aijobs.application.dto.ApplicationResponse;
import com.example.aijobs.application.dto.CandidateRecommendationResponse;
import com.example.aijobs.application.entity.JobApplication;
import com.example.aijobs.application.mapper.JobApplicationMapper;
import com.example.aijobs.common.BusinessException;
import com.example.aijobs.job.entity.JobPosting;
import com.example.aijobs.job.mapper.JobPostingMapper;
import com.example.aijobs.match.entity.AiMatchResult;
import com.example.aijobs.match.mapper.AiMatchResultMapper;
import com.example.aijobs.resume.entity.Resume;
import com.example.aijobs.resume.mapper.ResumeMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class JobApplicationService {
    private static final String LOCAL_RECOMMENDATION_MODEL = "local-candidate-ranker-v1";
    private static final String MATCH_SCORE_SOURCE = "ai-match-result";
    private static final Set<String> RECOMMENDATION_STOP_WORDS = Set.of(
            "full", "time", "part", "internship", "负责", "经验", "岗位", "要求", "工作");

    private final JobApplicationMapper applicationMapper;
    private final JobPostingMapper jobMapper;
    private final ResumeMapper resumeMapper;
    private final AiMatchResultMapper matchMapper;

    public JobApplicationService(JobApplicationMapper applicationMapper,
                                 JobPostingMapper jobMapper,
                                 ResumeMapper resumeMapper,
                                 AiMatchResultMapper matchMapper) {
        this.applicationMapper = applicationMapper;
        this.jobMapper = jobMapper;
        this.resumeMapper = resumeMapper;
        this.matchMapper = matchMapper;
    }

    public List<ApplicationResponse> listStudentApplications(Long studentId) {
        return applicationMapper.selectList(Wrappers.<JobApplication>lambdaQuery()
                        .eq(JobApplication::getStudentId, studentId)
                        .orderByDesc(JobApplication::getAppliedAt)).stream()
                .map(ApplicationResponse::from).toList();
    }

    public List<ApplicationResponse> listHrApplications(Long hrId, Long jobId) {
        List<Long> ownedJobIds = jobMapper.selectList(Wrappers.<JobPosting>lambdaQuery()
                        .eq(JobPosting::getHrId, hrId)).stream()
                .map(JobPosting::getId).toList();
        if (ownedJobIds.isEmpty()) return List.of();
        if (jobId != null && !ownedJobIds.contains(jobId)) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "只能查看自己岗位的投递");
        }

        var query = Wrappers.<JobApplication>lambdaQuery()
                .in(JobApplication::getJobId, ownedJobIds)
                .orderByDesc(JobApplication::getAppliedAt);
        if (jobId != null) query.eq(JobApplication::getJobId, jobId);
        return applicationMapper.selectList(query).stream().map(ApplicationResponse::from).toList();
    }

    public List<CandidateRecommendationResponse> recommendHrCandidates(Long hrId, Long jobId) {
        List<Long> ownedJobIds = jobMapper.selectList(Wrappers.<JobPosting>lambdaQuery()
                        .eq(JobPosting::getHrId, hrId)).stream()
                .map(JobPosting::getId).toList();
        if (ownedJobIds.isEmpty()) return List.of();
        if (jobId != null && !ownedJobIds.contains(jobId)) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "只能推荐自己岗位的候选人");
        }

        var query = Wrappers.<JobApplication>lambdaQuery()
                .in(JobApplication::getJobId, ownedJobIds)
                .ne(JobApplication::getStatus, "WITHDRAWN")
                .orderByDesc(JobApplication::getAppliedAt);
        if (jobId != null) query.eq(JobApplication::getJobId, jobId);

        return applicationMapper.selectList(query).stream()
                .map(this::buildCandidateRecommendation)
                .sorted(Comparator.comparing(CandidateRecommendationResponse::recommendationScore).reversed()
                        .thenComparing(CandidateRecommendationResponse::appliedAt,
                                Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
    }

    @Transactional
    public ApplicationResponse submit(Long studentId, ApplicationRequest request) {
        JobPosting job = jobMapper.selectById(request.jobId());
        if (job == null || !"PUBLISHED".equals(job.getStatus())) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "岗位不存在或尚未发布");
        }

        Resume resume = resumeMapper.selectById(request.resumeId());
        if (resume == null) throw new BusinessException(HttpStatus.NOT_FOUND, "简历不存在");
        if (!studentId.equals(resume.getStudentId())) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "只能使用自己的简历投递");
        }
        if (!"PUBLISHED".equals(resume.getStatus())) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "只能使用已发布简历投递");
        }

        Long duplicateCount = applicationMapper.selectCount(Wrappers.<JobApplication>lambdaQuery()
                .eq(JobApplication::getJobId, request.jobId())
                .eq(JobApplication::getStudentId, studentId));
        if (duplicateCount > 0) throw new BusinessException(HttpStatus.CONFLICT, "不能重复投递同一岗位");

        JobApplication application = new JobApplication();
        application.setJobId(request.jobId());
        application.setStudentId(studentId);
        application.setResumeId(request.resumeId());
        application.setStatus("SUBMITTED");
        applicationMapper.insert(application);
        return ApplicationResponse.from(application);
    }

    @Transactional
    public ApplicationResponse withdraw(Long studentId, Long id) {
        JobApplication application = ownedApplication(studentId, id);
        if ("WITHDRAWN".equals(application.getStatus())) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "投递已撤回");
        }
        application.setStatus("WITHDRAWN");
        applicationMapper.updateById(application);
        return ApplicationResponse.from(application);
    }

    @Transactional
    public ApplicationResponse updateHrStatus(Long hrId, Long id, String status) {
        JobApplication application = applicationMapper.selectById(id);
        if (application == null) throw new BusinessException(HttpStatus.NOT_FOUND, "投递不存在");
        JobPosting job = jobMapper.selectById(application.getJobId());
        if (job == null) throw new BusinessException(HttpStatus.NOT_FOUND, "岗位不存在");
        if (!hrId.equals(job.getHrId())) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "只能管理自己岗位的投递");
        }
        if ("WITHDRAWN".equals(application.getStatus())) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "已撤回投递不能更新状态");
        }
        application.setStatus(status);
        applicationMapper.updateById(application);
        return ApplicationResponse.from(application);
    }

    private JobApplication ownedApplication(Long studentId, Long id) {
        JobApplication application = applicationMapper.selectById(id);
        if (application == null) throw new BusinessException(HttpStatus.NOT_FOUND, "投递不存在");
        if (!studentId.equals(application.getStudentId())) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "只能管理自己的投递");
        }
        return application;
    }

    private CandidateRecommendationResponse buildCandidateRecommendation(JobApplication application) {
        JobPosting job = jobMapper.selectById(application.getJobId());
        Resume resume = resumeMapper.selectById(application.getResumeId());
        if (job == null || resume == null) {
            return CandidateRecommendationResponse.from(application, BigDecimal.ZERO, LOCAL_RECOMMENDATION_MODEL,
                    List.of(), List.of(), "岗位或简历数据不完整，暂不建议推进。",
                    "关键数据缺失，需先核对投递记录。", "先核对岗位和简历是否仍然存在。");
        }

        KeywordSignal signal = keywordSignal(resume, job);
        AiMatchResult match = matchMapper.selectOne(Wrappers.<AiMatchResult>lambdaQuery()
                .eq(AiMatchResult::getJobId, application.getJobId())
                .eq(AiMatchResult::getResumeId, application.getResumeId()));
        BigDecimal score = match == null ? signal.score() : match.getScore();
        String scoreSource = match == null ? LOCAL_RECOMMENDATION_MODEL : MATCH_SCORE_SOURCE;
        String reason = buildRecommendationReason(score, signal.matchedKeywords(), signal.missingKeywords());
        String risk = buildRiskSummary(score, application, resume, signal.missingKeywords());
        String action = buildSuggestedAction(score, risk);
        return CandidateRecommendationResponse.from(application, score, scoreSource, signal.matchedKeywords(),
                signal.missingKeywords(), reason, risk, action);
    }

    private KeywordSignal keywordSignal(Resume resume, JobPosting job) {
        Set<String> resumeTokens = tokenize(String.join(" ",
                safe(resume.getTitle()),
                safe(resume.getEducation()),
                safe(resume.getWorkExperience()),
                safe(resume.getProjectExperience()),
                safe(resume.getSkills()),
                safe(resume.getSelfEvaluation())));
        Set<String> jobTokens = tokenize(String.join(" ",
                safe(job.getTitle()),
                safe(job.getDescription()),
                safe(job.getRequirements()),
                safe(job.getEmploymentType())));
        Set<String> matched = new LinkedHashSet<>(resumeTokens);
        matched.retainAll(jobTokens);
        Set<String> missing = new LinkedHashSet<>(jobTokens);
        missing.removeAll(resumeTokens);
        missing.removeAll(RECOMMENDATION_STOP_WORDS);
        BigDecimal score = BigDecimal.valueOf(matched.size() * 100.0 / Math.max(jobTokens.size(), 1))
                .setScale(2, RoundingMode.HALF_UP);
        return new KeywordSignal(score, matched.stream().limit(8).toList(), missing.stream().limit(8).toList());
    }

    private String buildRecommendationReason(BigDecimal score, List<String> matched, List<String> missing) {
        if (score.compareTo(BigDecimal.valueOf(75)) >= 0) {
            return "候选人与岗位要求重合度高，建议优先进入面试评估。";
        }
        if (!matched.isEmpty()) {
            return "候选人已覆盖部分关键要求：" + String.join("、", matched.stream().limit(5).toList()) + "。";
        }
        if (!missing.isEmpty()) {
            return "暂未识别到明显匹配优势，需重点核验岗位关键要求。";
        }
        return "岗位要求信息较少，建议结合简历详情进行人工判断。";
    }

    private String buildRiskSummary(BigDecimal score, JobApplication application, Resume resume, List<String> missing) {
        if (!"PUBLISHED".equals(resume.getStatus())) {
            return "简历当前不是已发布状态，建议确认候选人资料是否仍可用于招聘流程。";
        }
        if ("REJECTED".equals(application.getStatus())) {
            return "该投递已标记未通过，仅建议作为复盘参考，不建议继续推进。";
        }
        if (score.compareTo(BigDecimal.valueOf(40)) < 0) {
            return missing.isEmpty()
                    ? "推荐分偏低，需人工核验简历信息是否充分。"
                    : "推荐分偏低，主要缺口：" + String.join("、", missing.stream().limit(4).toList()) + "。";
        }
        if (!missing.isEmpty()) {
            return "存在待核验缺口：" + String.join("、", missing.stream().limit(4).toList()) + "。";
        }
        return "未发现明显风险，可按正常招聘流程推进。";
    }

    private String buildSuggestedAction(BigDecimal score, String riskSummary) {
        if (riskSummary.contains("未通过")) {
            return "保留记录，不进入新的筛选动作。";
        }
        if (score.compareTo(BigDecimal.valueOf(75)) >= 0) {
            return "优先安排面试，并围绕匹配关键词准备追问。";
        }
        if (score.compareTo(BigDecimal.valueOf(50)) >= 0) {
            return "进入简历复核，补充确认缺口技能和项目证据。";
        }
        return "暂缓推进，除非业务侧需要补充候选池。";
    }

    private Set<String> tokenize(String text) {
        Set<String> tokens = new LinkedHashSet<>();
        for (String token : text.toLowerCase(Locale.ROOT).split("[^\\p{IsHan}\\p{Alnum}]+")) {
            if (token.length() >= 2) tokens.add(token);
        }
        return tokens;
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private record KeywordSignal(BigDecimal score, List<String> matchedKeywords, List<String> missingKeywords) {
    }
}
