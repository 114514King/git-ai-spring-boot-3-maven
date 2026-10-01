package com.example.aijobs.match;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.example.aijobs.application.entity.JobApplication;
import com.example.aijobs.application.mapper.JobApplicationMapper;
import com.example.aijobs.common.BusinessException;
import com.example.aijobs.job.entity.JobPosting;
import com.example.aijobs.job.mapper.JobPostingMapper;
import com.example.aijobs.match.dto.MatchRequest;
import com.example.aijobs.match.dto.MatchResponse;
import com.example.aijobs.match.entity.AiMatchResult;
import com.example.aijobs.match.mapper.AiMatchResultMapper;
import com.example.aijobs.resume.entity.Resume;
import com.example.aijobs.resume.mapper.ResumeMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
public class AiMatchService {
    private static final String MODEL_NAME = "local-skill-coverage-v1";

    private final AiMatchResultMapper matchMapper;
    private final ResumeMapper resumeMapper;
    private final JobPostingMapper jobMapper;
    private final JobApplicationMapper applicationMapper;

    public AiMatchService(AiMatchResultMapper matchMapper,
                          ResumeMapper resumeMapper,
                          JobPostingMapper jobMapper,
                          JobApplicationMapper applicationMapper) {
        this.matchMapper = matchMapper;
        this.resumeMapper = resumeMapper;
        this.jobMapper = jobMapper;
        this.applicationMapper = applicationMapper;
    }

    public List<MatchResponse> listStudentMatches(Long studentId, Long resumeId) {
        List<Long> resumeIds = ownedResumeIds(studentId, resumeId);
        if (resumeIds.isEmpty()) return List.of();
        return matchMapper.selectList(Wrappers.<AiMatchResult>lambdaQuery()
                        .in(AiMatchResult::getResumeId, resumeIds)
                        .orderByDesc(AiMatchResult::getScore)
                        .orderByDesc(AiMatchResult::getUpdatedAt)).stream()
                .map(MatchResponse::from).toList();
    }

    public List<MatchResponse> listHrMatches(Long hrId, Long jobId) {
        List<Long> jobIds = ownedJobIds(hrId, jobId);
        if (jobIds.isEmpty()) return List.of();
        return matchMapper.selectList(Wrappers.<AiMatchResult>lambdaQuery()
                        .in(AiMatchResult::getJobId, jobIds)
                        .orderByDesc(AiMatchResult::getScore)
                        .orderByDesc(AiMatchResult::getUpdatedAt)).stream()
                .map(MatchResponse::from).toList();
    }

    @Transactional
    public MatchResponse matchForStudent(Long studentId, MatchRequest request) {
        Resume resume = requirePublishedOwnedResume(studentId, request.resumeId());
        JobPosting job = requirePublishedJob(request.jobId());
        return MatchResponse.from(saveResult(resume, job));
    }

    @Transactional
    public MatchResponse matchForHr(Long hrId, MatchRequest request) {
        JobPosting job = requireOwnedPublishedJob(hrId, request.jobId());
        Resume resume = resumeMapper.selectById(request.resumeId());
        if (resume == null || !"PUBLISHED".equals(resume.getStatus())) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "简历不存在或尚未发布");
        }

        Long applicationCount = applicationMapper.selectCount(Wrappers.<JobApplication>lambdaQuery()
                .eq(JobApplication::getJobId, request.jobId())
                .eq(JobApplication::getResumeId, request.resumeId()));
        if (applicationCount == 0) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "只能匹配已投递到自己岗位的简历");
        }
        return MatchResponse.from(saveResult(resume, job));
    }

    private AiMatchResult saveResult(Resume resume, JobPosting job) {
        MatchScore score = calculateScore(resume, job);
        AiMatchResult existing = matchMapper.selectOne(Wrappers.<AiMatchResult>lambdaQuery()
                .eq(AiMatchResult::getResumeId, resume.getId())
                .eq(AiMatchResult::getJobId, job.getId()));

        AiMatchResult result = existing == null ? new AiMatchResult() : existing;
        result.setResumeId(resume.getId());
        result.setJobId(job.getId());
        result.setScore(score.value());
        result.setAnalysis(score.analysis());
        result.setStrengthSummary(score.strengthSummary());
        result.setGapSummary(score.gapSummary());
        result.setActionSuggestions(score.actionSuggestions());
        result.setModelName(MODEL_NAME);
        if (existing == null) {
            matchMapper.insert(result);
        } else {
            matchMapper.updateById(result);
        }
        return result;
    }

    private MatchScore calculateScore(Resume resume, JobPosting job) {
        Set<String> resumeSkills = SkillNormalizer.normalize(String.join(" ; ",
                safe(resume.getEducation()),
                safe(resume.getWorkExperience()),
                safe(resume.getProjectExperience()),
                safe(resume.getSkills()),
                safe(resume.getSourceText()),
                safe(resume.getSelfEvaluation())));
        Set<String> requiredSkills = SkillNormalizer.normalize(job.getRequirements());
        if (requiredSkills.isEmpty()) {
            throw new BusinessException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "岗位要求中未识别到词典支持的技能，无法计算覆盖率；请 HR 补充明确技能后重试。");
        }
        Set<String> matched = new LinkedHashSet<>(requiredSkills);
        matched.retainAll(resumeSkills);
        Set<String> gaps = new LinkedHashSet<>(requiredSkills);
        gaps.removeAll(matched);
        BigDecimal score = BigDecimal.valueOf(matched.size()).multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(requiredSkills.size()), 2, RoundingMode.HALF_UP);
        String analysis = "技能覆盖率：" + matched.size() + "/" + requiredSkills.size()
                + " × 100 = " + score.toPlainString() + "%；岗位识别技能："
                + String.join("、", requiredSkills) + "。词典：" + SkillNormalizer.VERSION
                + "。仅衡量文本提及，不代表熟练度或录用概率；未收录技能不参与计算，最终决定由人工复核。";
        String strengthSummary = buildStrengthSummary(matched);
        String gapSummary = buildGapSummary(gaps);
        String actionSuggestions = buildActionSuggestions(score, matched, gaps);
        return new MatchScore(score, analysis, strengthSummary, gapSummary, actionSuggestions);
    }

    private String buildStrengthSummary(Set<String> matched) {
        if (matched.isEmpty()) {
            return "简历文本中暂未识别到岗位所需技能。";
        }
        return "简历文本已识别的岗位技能：" + String.join("、", matched) + "。";
    }

    private String buildGapSummary(Set<String> gaps) {
        if (gaps.isEmpty()) {
            return "已覆盖词典识别到的全部岗位技能；仍需人工核验熟练度、证据与未收录要求。";
        }
        return "简历文本尚未识别的岗位技能：" + String.join("、", gaps) + "；未提及不等于不具备。";
    }

    private String buildActionSuggestions(BigDecimal score, Set<String> matched, Set<String> gaps) {
        if (matched.isEmpty()) {
            return "请核对缺口技能；仅补充真实具备的技能和经历，由人工评估岗位适配度。";
        }
        if (score.compareTo(BigDecimal.valueOf(70)) >= 0) {
            return "请围绕已匹配技能准备真实案例，并核对剩余缺口；覆盖率不能替代人工招聘评估。";
        }
        if (!gaps.isEmpty()) {
            return "请核对缺口技能，仅补充真实项目或教学经历；尚不具备的技能可纳入学习计划。";
        }
        return "建议保留当前优势表达，并补充更具体的业务场景、个人贡献和结果指标。";
    }

    private Resume requirePublishedOwnedResume(Long studentId, Long resumeId) {
        Resume resume = resumeMapper.selectById(resumeId);
        if (resume == null || !"PUBLISHED".equals(resume.getStatus())) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "简历不存在或尚未发布");
        }
        if (!studentId.equals(resume.getStudentId())) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "只能匹配自己的简历");
        }
        return resume;
    }

    private JobPosting requirePublishedJob(Long jobId) {
        JobPosting job = jobMapper.selectById(jobId);
        if (job == null || !"PUBLISHED".equals(job.getStatus())) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "岗位不存在或尚未发布");
        }
        return job;
    }

    private JobPosting requireOwnedPublishedJob(Long hrId, Long jobId) {
        JobPosting job = requirePublishedJob(jobId);
        if (!hrId.equals(job.getHrId())) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "只能匹配自己发布的岗位");
        }
        return job;
    }

    private List<Long> ownedResumeIds(Long studentId, Long resumeId) {
        if (resumeId != null) {
            Resume resume = resumeMapper.selectById(resumeId);
            if (resume == null) throw new BusinessException(HttpStatus.NOT_FOUND, "简历不存在");
            if (!studentId.equals(resume.getStudentId())) {
                throw new BusinessException(HttpStatus.FORBIDDEN, "只能查看自己简历的匹配结果");
            }
            return List.of(resumeId);
        }
        return resumeMapper.selectList(Wrappers.<Resume>lambdaQuery().eq(Resume::getStudentId, studentId)).stream()
                .map(Resume::getId).toList();
    }

    private List<Long> ownedJobIds(Long hrId, Long jobId) {
        if (jobId != null) {
            JobPosting job = jobMapper.selectById(jobId);
            if (job == null) throw new BusinessException(HttpStatus.NOT_FOUND, "岗位不存在");
            if (!hrId.equals(job.getHrId())) {
                throw new BusinessException(HttpStatus.FORBIDDEN, "只能查看自己岗位的匹配结果");
            }
            return List.of(jobId);
        }
        return jobMapper.selectList(Wrappers.<JobPosting>lambdaQuery().eq(JobPosting::getHrId, hrId)).stream()
                .map(JobPosting::getId).toList();
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private record MatchScore(BigDecimal value, String analysis, String strengthSummary, String gapSummary,
                              String actionSuggestions) {
    }
}
