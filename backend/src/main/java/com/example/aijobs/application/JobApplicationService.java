package com.example.aijobs.application;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.example.aijobs.application.dto.ApplicationFollowUpAdviceResponse;
import com.example.aijobs.application.dto.ApplicationRequest;
import com.example.aijobs.application.dto.ApplicationResponse;
import com.example.aijobs.application.dto.CandidateRecommendationResponse;
import com.example.aijobs.application.dto.CandidateCommunicationDraftResponse;
import com.example.aijobs.application.dto.InterviewKitResponse;
import com.example.aijobs.application.dto.StudentApplicationActionPlanResponse;
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
    private static final String LOCAL_INTERVIEW_MODEL = "local-interview-kit-v1";
    private static final String LOCAL_FOLLOW_UP_MODEL = "local-application-follow-up-v1";
    private static final String LOCAL_STUDENT_ACTION_PLAN_MODEL = "local-student-action-plan-v1";
    private static final String LOCAL_COMMUNICATION_DRAFT_MODEL = "local-candidate-communication-draft-v1";
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

    public InterviewKitResponse generateInterviewKit(Long hrId, Long applicationId) {
        JobApplication application = applicationMapper.selectById(applicationId);
        if (application == null) throw new BusinessException(HttpStatus.NOT_FOUND, "投递不存在");
        if ("WITHDRAWN".equals(application.getStatus())) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "已撤回投递不能生成面试题");
        }

        JobPosting job = jobMapper.selectById(application.getJobId());
        if (job == null) throw new BusinessException(HttpStatus.NOT_FOUND, "岗位不存在");
        if (!hrId.equals(job.getHrId())) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "只能为本人岗位的投递生成面试题");
        }

        Resume resume = resumeMapper.selectById(application.getResumeId());
        if (resume == null) throw new BusinessException(HttpStatus.NOT_FOUND, "简历不存在");

        KeywordSignal signal = keywordSignal(resume, job);
        List<String> matched = signal.matchedKeywords();
        List<String> missing = signal.missingKeywords();
        String primarySkill = firstOrDefault(matched, firstOrDefault(missing, safe(job.getTitle())));
        String gapSkill = firstOrDefault(missing, "岗位核心要求");
        String projectTopic = safe(resume.getProjectExperience()).isBlank() ? "候选人最近一个项目" : "简历中的项目经历";
        String summary = "基于岗位“" + safe(job.getTitle()) + "”与简历“" + safe(resume.getTitle())
                + "”生成本地规则面试题，重点验证已匹配能力、岗位缺口和项目证据。";

        List<InterviewKitResponse.InterviewQuestion> questions = List.of(
                new InterviewKitResponse.InterviewQuestion("技能验证",
                        "请结合实际经历说明你如何使用 " + primarySkill + " 解决过一个具体问题？",
                        "验证候选人是否真正掌握岗位核心技能，而不是只在简历中罗列关键词。",
                        "能说清业务背景、个人职责、技术选择、结果指标和复盘结论。"),
                new InterviewKitResponse.InterviewQuestion("项目深挖",
                        "请展开介绍" + projectTopic + "中最复杂的技术或协作挑战，以及你负责的部分。",
                        "核验项目经历的真实性、复杂度和候选人的实际贡献。",
                        "能区分团队成果和个人贡献，并给出可验证的交付结果。"),
                new InterviewKitResponse.InterviewQuestion("缺口确认",
                        "岗位需要 " + gapSkill + "，你过往有哪些相关经验？如果经验不足，会如何快速补齐？",
                        "确认待核验能力缺口是否可接受，以及候选人的学习路径是否清晰。",
                        "能给出相关迁移经验、学习计划或试用期内可交付动作。"),
                new InterviewKitResponse.InterviewQuestion("场景判断",
                        "如果入职后需要在两周内交付一个与该岗位相关的核心任务，你会如何拆解计划？",
                        "观察候选人的任务拆解、优先级判断和风险意识。",
                        "能拆分里程碑、识别依赖和风险，并说明沟通节奏。")
        );

        List<InterviewKitResponse.ScoringDimension> dimensions = List.of(
                new InterviewKitResponse.ScoringDimension("岗位技能匹配", 35,
                        "回答能覆盖岗位关键词，并给出可验证的实践细节。",
                        "只能泛泛描述概念，无法说明实际使用场景。"),
                new InterviewKitResponse.ScoringDimension("项目证据质量", 25,
                        "项目背景、个人职责、技术方案和结果指标完整。",
                        "项目描述停留在团队层面，个人贡献不清晰。"),
                new InterviewKitResponse.ScoringDimension("缺口补齐能力", 20,
                        "能正面回应缺口，并给出可执行的补齐计划。",
                        "回避关键缺口，或学习计划不可验证。"),
                new InterviewKitResponse.ScoringDimension("沟通与复盘", 20,
                        "表达结构清晰，能说明取舍和复盘改进。",
                        "回答跳跃，缺少对问题和结果的反思。")
        );

        return new InterviewKitResponse(application.getId(), application.getJobId(), application.getResumeId(),
                application.getStudentId(), LOCAL_INTERVIEW_MODEL, summary, questions, dimensions,
                buildInterviewRisks(application, resume, missing), buildInterviewFollowUps(matched, missing));
    }

    public ApplicationFollowUpAdviceResponse generateFollowUpAdvice(Long hrId, Long applicationId) {
        JobApplication application = applicationMapper.selectById(applicationId);
        if (application == null) throw new BusinessException(HttpStatus.NOT_FOUND, "投递不存在");

        JobPosting job = jobMapper.selectById(application.getJobId());
        if (job == null) throw new BusinessException(HttpStatus.NOT_FOUND, "岗位不存在");
        if (!hrId.equals(job.getHrId())) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "只能为本人岗位的投递生成跟进建议");
        }

        Resume resume = resumeMapper.selectById(application.getResumeId());
        if (resume == null) throw new BusinessException(HttpStatus.NOT_FOUND, "简历不存在");

        KeywordSignal signal = keywordSignal(resume, job);
        AiMatchResult match = matchMapper.selectOne(Wrappers.<AiMatchResult>lambdaQuery()
                .eq(AiMatchResult::getJobId, application.getJobId())
                .eq(AiMatchResult::getResumeId, application.getResumeId()));
        BigDecimal score = match == null ? signal.score() : match.getScore();
        String scoreSource = match == null ? LOCAL_FOLLOW_UP_MODEL : MATCH_SCORE_SOURCE;

        return new ApplicationFollowUpAdviceResponse(application.getId(), application.getJobId(),
                application.getStudentId(), application.getResumeId(), application.getStatus(),
                LOCAL_FOLLOW_UP_MODEL, score, scoreSource, buildFollowUpPriority(application, score),
                buildFollowUpSummary(application, score, signal.matchedKeywords(), signal.missingKeywords()),
                buildNextStatusSuggestion(application, score), signal.matchedKeywords(), signal.missingKeywords(),
                buildFollowUpRisks(application, resume, signal.missingKeywords(), score),
                buildFollowUpActions(application, score, signal.missingKeywords()),
                buildCommunicationTips(application, job, signal.matchedKeywords(), signal.missingKeywords()));
    }

    public CandidateCommunicationDraftResponse generateCommunicationDraft(Long hrId, Long applicationId) {
        JobApplication application = applicationMapper.selectById(applicationId);
        if (application == null) throw new BusinessException(HttpStatus.NOT_FOUND, "投递不存在");

        JobPosting job = jobMapper.selectById(application.getJobId());
        if (job == null) throw new BusinessException(HttpStatus.NOT_FOUND, "岗位不存在");
        if (!hrId.equals(job.getHrId())) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "只能为本人岗位的投递生成沟通话术");
        }

        Resume resume = resumeMapper.selectById(application.getResumeId());
        if (resume == null) throw new BusinessException(HttpStatus.NOT_FOUND, "简历不存在");

        KeywordSignal signal = keywordSignal(resume, job);
        AiMatchResult match = matchMapper.selectOne(Wrappers.<AiMatchResult>lambdaQuery()
                .eq(AiMatchResult::getJobId, application.getJobId())
                .eq(AiMatchResult::getResumeId, application.getResumeId()));
        BigDecimal score = match == null ? signal.score() : match.getScore();
        String scoreSource = match == null ? LOCAL_COMMUNICATION_DRAFT_MODEL : MATCH_SCORE_SOURCE;

        return new CandidateCommunicationDraftResponse(application.getId(), application.getJobId(),
                application.getStudentId(), application.getResumeId(), application.getStatus(),
                LOCAL_COMMUNICATION_DRAFT_MODEL, score, scoreSource,
                buildCommunicationScenario(application, score),
                buildCommunicationSubject(application, job),
                buildOpeningMessage(application, job, score, signal.matchedKeywords(), signal.missingKeywords()),
                buildKeyQuestions(signal.matchedKeywords(), signal.missingKeywords()),
                buildCommunicationRiskNotes(application, resume, score, signal.missingKeywords()),
                buildCommunicationNextActions(application, score));
    }

    public StudentApplicationActionPlanResponse generateStudentActionPlan(Long studentId, Long applicationId) {
        JobApplication application = ownedApplication(studentId, applicationId);

        JobPosting job = jobMapper.selectById(application.getJobId());
        if (job == null) throw new BusinessException(HttpStatus.NOT_FOUND, "岗位不存在");

        Resume resume = resumeMapper.selectById(application.getResumeId());
        if (resume == null) throw new BusinessException(HttpStatus.NOT_FOUND, "简历不存在");

        KeywordSignal signal = keywordSignal(resume, job);
        AiMatchResult match = matchMapper.selectOne(Wrappers.<AiMatchResult>lambdaQuery()
                .eq(AiMatchResult::getJobId, application.getJobId())
                .eq(AiMatchResult::getResumeId, application.getResumeId()));
        BigDecimal score = match == null ? signal.score() : match.getScore();
        String scoreSource = match == null ? LOCAL_STUDENT_ACTION_PLAN_MODEL : MATCH_SCORE_SOURCE;

        return new StudentApplicationActionPlanResponse(application.getId(), application.getJobId(),
                application.getResumeId(), application.getStatus(), LOCAL_STUDENT_ACTION_PLAN_MODEL, score, scoreSource,
                buildStudentActionPriority(application, score),
                buildStudentActionSummary(application, job, score, signal.matchedKeywords(), signal.missingKeywords()),
                buildStudentPreparationChecklist(application, job, signal.matchedKeywords(), signal.missingKeywords()),
                buildStudentRiskReminders(application, resume, signal.missingKeywords(), score),
                buildStudentNextActions(application, score, signal.missingKeywords()));
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

    private List<String> buildInterviewRisks(JobApplication application, Resume resume, List<String> missing) {
        if ("REJECTED".equals(application.getStatus())) {
            return List.of("该投递已标记未通过，生成题目仅适合复盘参考。");
        }
        if (!"PUBLISHED".equals(resume.getStatus())) {
            return List.of("简历当前不是已发布状态，面试前应确认候选资料是否仍可使用。");
        }
        if (!missing.isEmpty()) {
            return List.of("需要重点核验缺口能力：" + String.join("、", missing.stream().limit(5).toList()) + "。");
        }
        return List.of("未识别明显能力缺口，建议重点确认项目真实性和结果指标。");
    }

    private List<String> buildInterviewFollowUps(List<String> matched, List<String> missing) {
        String matchedText = matched.isEmpty() ? "简历中的核心技能" : String.join("、", matched.stream().limit(3).toList());
        String missingText = missing.isEmpty() ? "岗位长期成长要求" : String.join("、", missing.stream().limit(3).toList());
        return List.of(
                "围绕 " + matchedText + " 继续追问实际交付结果和个人贡献。",
                "围绕 " + missingText + " 追问候选人的补齐计划和可接受风险。",
                "要求候选人用一个失败或返工案例说明复盘能力。"
        );
    }

    private String buildFollowUpPriority(JobApplication application, BigDecimal score) {
        if ("WITHDRAWN".equals(application.getStatus()) || "REJECTED".equals(application.getStatus())) {
            return "LOW";
        }
        if (score.compareTo(BigDecimal.valueOf(75)) >= 0) {
            return "HIGH";
        }
        if (score.compareTo(BigDecimal.valueOf(50)) >= 0) {
            return "MEDIUM";
        }
        return "LOW";
    }

    private String buildFollowUpSummary(JobApplication application,
                                        BigDecimal score,
                                        List<String> matched,
                                        List<String> missing) {
        if ("WITHDRAWN".equals(application.getStatus())) {
            return "该投递已撤回，仅建议保留记录并停止主动推进。";
        }
        if ("REJECTED".equals(application.getStatus())) {
            return "该投递已标记未通过，建议仅用于复盘筛选标准。";
        }
        if (score.compareTo(BigDecimal.valueOf(75)) >= 0) {
            return "匹配分较高，建议尽快推进下一轮并验证关键项目证据。";
        }
        if (!matched.isEmpty()) {
            return "候选人覆盖部分岗位要求，可先复核缺口后再决定是否推进。";
        }
        if (!missing.isEmpty()) {
            return "暂未识别出稳定匹配优势，建议人工确认岗位关键要求。";
        }
        return "岗位或简历关键词较少，建议补充人工判断后再推进。";
    }

    private String buildNextStatusSuggestion(JobApplication application, BigDecimal score) {
        if ("WITHDRAWN".equals(application.getStatus())) return "WITHDRAWN";
        if ("REJECTED".equals(application.getStatus())) return "REJECTED";
        if ("OFFERED".equals(application.getStatus())) return "OFFERED";
        if (score.compareTo(BigDecimal.valueOf(75)) >= 0) return "INTERVIEW";
        if (score.compareTo(BigDecimal.valueOf(50)) >= 0) return "REVIEWING";
        return "REJECTED";
    }

    private List<String> buildFollowUpRisks(JobApplication application,
                                            Resume resume,
                                            List<String> missing,
                                            BigDecimal score) {
        if ("WITHDRAWN".equals(application.getStatus())) {
            return List.of("候选人已撤回投递，不建议继续主动邀约。");
        }
        if (!"PUBLISHED".equals(resume.getStatus())) {
            return List.of("简历不是已发布状态，推进前需确认候选资料是否仍有效。");
        }
        if ("REJECTED".equals(application.getStatus())) {
            return List.of("投递已标记未通过，重新推进前需记录明确复议原因。");
        }
        if (score.compareTo(BigDecimal.valueOf(40)) < 0) {
            return List.of("匹配分低于 40，建议避免直接进入面试。");
        }
        if (!missing.isEmpty()) {
            return List.of("待核验能力缺口：" + String.join("、", missing.stream().limit(5).toList()) + "。");
        }
        return List.of("暂未发现明显跟进风险，重点确认项目真实性和到岗意向。");
    }

    private List<String> buildFollowUpActions(JobApplication application, BigDecimal score, List<String> missing) {
        if ("WITHDRAWN".equals(application.getStatus())) {
            return List.of("停止推进该投递。", "记录撤回原因，后续用于优化岗位描述。");
        }
        if ("REJECTED".equals(application.getStatus())) {
            return List.of("保留未通过结论。", "复盘是否存在岗位要求表达不清或误筛问题。");
        }
        if (score.compareTo(BigDecimal.valueOf(75)) >= 0) {
            return List.of("将投递状态推进到面试。", "生成 AI 面试题并围绕匹配关键词准备追问。");
        }
        if (score.compareTo(BigDecimal.valueOf(50)) >= 0) {
            return List.of("保持筛选中状态。", "先核验 " + firstOrDefault(missing, "关键岗位要求") + " 后再决定面试。");
        }
        return List.of("暂缓推进该候选人。", "仅在候选池不足时进行人工复核。");
    }

    private List<String> buildCommunicationTips(JobApplication application,
                                                JobPosting job,
                                                List<String> matched,
                                                List<String> missing) {
        if ("WITHDRAWN".equals(application.getStatus())) {
            return List.of("如需联系候选人，先确认其是否仍有求职意向。");
        }
        String matchedText = matched.isEmpty() ? "已投递经历" : String.join("、", matched.stream().limit(3).toList());
        String missingText = missing.isEmpty() ? "岗位期望" : String.join("、", missing.stream().limit(3).toList());
        return List.of(
                "沟通开场说明岗位“" + safe(job.getTitle()) + "”当前流程和预计反馈时间。",
                "围绕 " + matchedText + " 请候选人补充最近一次实际交付案例。",
                "围绕 " + missingText + " 直接确认经验深度、学习计划或可接受风险。"
        );
    }

    private String buildCommunicationScenario(JobApplication application, BigDecimal score) {
        if ("WITHDRAWN".equals(application.getStatus())) return "WITHDRAWN_CONFIRMATION";
        if ("REJECTED".equals(application.getStatus())) return "REJECTION_FEEDBACK";
        if ("OFFERED".equals(application.getStatus())) return "OFFER_CONFIRMATION";
        if ("INTERVIEW".equals(application.getStatus())) return "INTERVIEW_COORDINATION";
        if (score.compareTo(BigDecimal.valueOf(75)) >= 0) return "FAST_TRACK_INVITATION";
        if (score.compareTo(BigDecimal.valueOf(50)) >= 0) return "SCREENING_CONFIRMATION";
        return "LOW_MATCH_CLARIFICATION";
    }

    private String buildCommunicationSubject(JobApplication application, JobPosting job) {
        return switch (application.getStatus()) {
            case "INTERVIEW" -> "关于" + safe(job.getTitle()) + "面试安排的进一步确认";
            case "OFFERED" -> "关于" + safe(job.getTitle()) + "录用信息的确认";
            case "REJECTED" -> "关于" + safe(job.getTitle()) + "投递结果的反馈";
            case "WITHDRAWN" -> "关于" + safe(job.getTitle()) + "投递撤回后的意向确认";
            default -> "关于" + safe(job.getTitle()) + "投递进展的沟通";
        };
    }

    private String buildOpeningMessage(JobApplication application,
                                       JobPosting job,
                                       BigDecimal score,
                                       List<String> matched,
                                       List<String> missing) {
        String title = safe(job.getTitle()).isBlank() ? "当前岗位" : safe(job.getTitle());
        if ("WITHDRAWN".equals(application.getStatus())) {
            return "你好，我们看到你已撤回「" + title + "」投递。如仍对该方向感兴趣，可以回复说明新的求职意向，我们会据此判断是否继续沟通。";
        }
        if ("REJECTED".equals(application.getStatus())) {
            return "你好，感谢你投递「" + title + "」。本次评估暂未进入下一轮，主要建议围绕 "
                    + firstOrDefault(missing, "岗位核心要求") + " 继续补充项目证据。";
        }
        if ("OFFERED".equals(application.getStatus())) {
            return "你好，关于「" + title + "」录用流程，我们希望进一步确认到岗时间、薪资预期和材料提交安排。";
        }
        if ("INTERVIEW".equals(application.getStatus()) || score.compareTo(BigDecimal.valueOf(75)) >= 0) {
            return "你好，你投递的「" + title + "」与岗位要求匹配度较高，尤其是 "
                    + firstOrDefault(matched, "相关项目经验") + "。想进一步约时间确认面试安排。";
        }
        return "你好，感谢你投递「" + title + "」。我们正在复核简历信息，想先确认 "
                + firstOrDefault(missing, "岗位关键经验") + " 相关经历，方便判断下一步流程。";
    }

    private List<String> buildKeyQuestions(List<String> matched, List<String> missing) {
        return List.of(
                "请补充一个最能证明 " + firstOrDefault(matched, "岗位相关能力") + " 的实际项目或实习案例。",
                "请说明你对 " + firstOrDefault(missing, "岗位关键要求") + " 的经验深度、学习计划或可迁移经历。",
                "请确认当前求职意向、可面试时间和预期到岗时间。"
        );
    }

    private List<String> buildCommunicationRiskNotes(JobApplication application,
                                                     Resume resume,
                                                     BigDecimal score,
                                                     List<String> missing) {
        if ("WITHDRAWN".equals(application.getStatus())) {
            return List.of("候选人已撤回投递，继续沟通前需先确认真实意向。");
        }
        if (!"PUBLISHED".equals(resume.getStatus())) {
            return List.of("候选人简历当前不是已发布状态，沟通前需确认 HR 看到的信息是否仍有效。");
        }
        if ("REJECTED".equals(application.getStatus())) {
            return List.of("投递已标记未通过，发送反馈时避免承诺后续流程。");
        }
        if (score.compareTo(BigDecimal.valueOf(40)) < 0) {
            return List.of("匹配分低于 40，话术应以补充信息和澄清为主，避免直接邀约面试。");
        }
        if (!missing.isEmpty()) {
            return List.of("需重点确认缺口：" + String.join("、", missing.stream().limit(5).toList()) + "。");
        }
        return List.of("暂未识别明显沟通风险，重点确认候选人意向和项目证据。");
    }

    private List<String> buildCommunicationNextActions(JobApplication application, BigDecimal score) {
        if ("WITHDRAWN".equals(application.getStatus())) {
            return List.of("仅在候选人明确回复仍感兴趣后再恢复流程。", "记录撤回原因用于岗位描述复盘。");
        }
        if ("REJECTED".equals(application.getStatus())) {
            return List.of("发送礼貌反馈并保留候选人标签。", "如候选人补充强相关材料，再人工复核是否重启流程。");
        }
        if ("OFFERED".equals(application.getStatus())) {
            return List.of("确认 offer 条件、到岗时间和材料清单。", "同步下一步签约或入职联系人。");
        }
        if ("INTERVIEW".equals(application.getStatus()) || score.compareTo(BigDecimal.valueOf(75)) >= 0) {
            return List.of("发送面试邀约并提供 2 到 3 个可选时间。", "附上面试准备方向和需要补充的作品材料。");
        }
        if (score.compareTo(BigDecimal.valueOf(50)) >= 0) {
            return List.of("先收集缺口信息，再决定是否进入面试。", "将回复内容同步到筛选备注中。");
        }
        return List.of("先请求补充材料，不直接承诺面试。", "若 3 个工作日无回复，保持当前筛选状态。");
    }

    private String buildStudentActionPriority(JobApplication application, BigDecimal score) {
        if ("WITHDRAWN".equals(application.getStatus()) || "REJECTED".equals(application.getStatus())) {
            return "LOW";
        }
        if ("INTERVIEW".equals(application.getStatus()) || score.compareTo(BigDecimal.valueOf(75)) >= 0) {
            return "HIGH";
        }
        if ("REVIEWING".equals(application.getStatus()) || score.compareTo(BigDecimal.valueOf(50)) >= 0) {
            return "MEDIUM";
        }
        return "LOW";
    }

    private String buildStudentActionSummary(JobApplication application,
                                             JobPosting job,
                                             BigDecimal score,
                                             List<String> matched,
                                             List<String> missing) {
        if ("WITHDRAWN".equals(application.getStatus())) {
            return "该投递已撤回，建议先确认是否重新关注岗位“" + safe(job.getTitle()) + "”。";
        }
        if ("REJECTED".equals(application.getStatus())) {
            return "该投递已未通过，建议复盘缺口并沉淀下一次投递素材。";
        }
        if ("OFFERED".equals(application.getStatus())) {
            return "该投递已进入录用阶段，建议重点准备入职确认和薪资沟通材料。";
        }
        if ("INTERVIEW".equals(application.getStatus())) {
            return "该投递已进入面试阶段，建议优先准备岗位相关项目证据和追问材料。";
        }
        if (score.compareTo(BigDecimal.valueOf(75)) >= 0) {
            return "当前匹配度较高，可主动准备面试材料并跟踪 HR 反馈节奏。";
        }
        if (!matched.isEmpty()) {
            return "当前已覆盖部分岗位要求，建议围绕缺口补充简历证据和沟通话术。";
        }
        if (!missing.isEmpty()) {
            return "当前岗位要求与简历重合度有限，建议先补齐核心关键词证据再推进。";
        }
        return "岗位或简历关键词较少，建议补充项目、技能和量化结果后再跟进。";
    }

    private List<String> buildStudentPreparationChecklist(JobApplication application,
                                                          JobPosting job,
                                                          List<String> matched,
                                                          List<String> missing) {
        String matchedText = matched.isEmpty() ? "已有项目经历" : String.join("、", matched.stream().limit(3).toList());
        String missingText = missing.isEmpty() ? "岗位长期能力要求" : String.join("、", missing.stream().limit(3).toList());
        if ("OFFERED".equals(application.getStatus())) {
            return List.of("整理薪资期望、到岗时间和证明材料。", "确认岗位“" + safe(job.getTitle()) + "”的工作内容和试用期目标。");
        }
        return List.of(
                "准备 2 个能证明 " + matchedText + " 的项目或实习案例。",
                "补充 " + missingText + " 的学习记录、迁移经验或短期补齐计划。",
                "整理一段 60 秒自我介绍，突出与岗位“" + safe(job.getTitle()) + "”最相关的经历。"
        );
    }

    private List<String> buildStudentRiskReminders(JobApplication application,
                                                   Resume resume,
                                                   List<String> missing,
                                                   BigDecimal score) {
        if ("WITHDRAWN".equals(application.getStatus())) {
            return List.of("投递已撤回，继续沟通前先确认自己的求职意向。");
        }
        if (!"PUBLISHED".equals(resume.getStatus())) {
            return List.of("当前简历不是已发布状态，建议先确认 HR 看到的是最新版本。");
        }
        if ("REJECTED".equals(application.getStatus())) {
            return List.of("投递已未通过，不建议重复催促同一岗位，可用于复盘下一次投递。");
        }
        if (score.compareTo(BigDecimal.valueOf(40)) < 0) {
            return List.of("匹配分低于 40，直接推进成功率较低，建议先补强简历证据。");
        }
        if (!missing.isEmpty()) {
            return List.of("待补强能力：" + String.join("、", missing.stream().limit(5).toList()) + "。");
        }
        return List.of("暂未识别明显风险，重点保持反馈节奏和材料完整。");
    }

    private List<String> buildStudentNextActions(JobApplication application, BigDecimal score, List<String> missing) {
        if ("WITHDRAWN".equals(application.getStatus())) {
            return List.of("停止跟进该投递。", "如仍感兴趣，重新筛选岗位并更新简历后再投递。");
        }
        if ("REJECTED".equals(application.getStatus())) {
            return List.of("记录未通过原因。", "围绕 " + firstOrDefault(missing, "岗位核心要求") + " 补充简历案例。");
        }
        if ("OFFERED".equals(application.getStatus())) {
            return List.of("确认 offer 条件和入职时间。", "准备薪资、试用期目标和材料提交清单。");
        }
        if ("INTERVIEW".equals(application.getStatus()) || score.compareTo(BigDecimal.valueOf(75)) >= 0) {
            return List.of("准备面试项目复盘。", "主动整理可发送给 HR 的作品、证书或项目链接。");
        }
        if (score.compareTo(BigDecimal.valueOf(50)) >= 0) {
            return List.of("补充缺口关键词对应的经历。", "等待 2 到 3 个工作日后礼貌跟进投递进展。");
        }
        return List.of("先优化简历再扩大投递范围。", "优先选择要求与现有技能更接近的岗位。");
    }

    private String firstOrDefault(List<String> values, String fallback) {
        return values.isEmpty() ? fallback : values.getFirst();
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private record KeywordSignal(BigDecimal score, List<String> matchedKeywords, List<String> missingKeywords) {
    }
}
