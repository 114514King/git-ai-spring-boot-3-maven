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
import java.util.Locale;
import java.util.Set;

@Service
public class AiMatchService {
    private static final String MODEL_NAME = "local-keyword-match-v1";

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
        result.setModelName(MODEL_NAME);
        if (existing == null) {
            matchMapper.insert(result);
        } else {
            matchMapper.updateById(result);
        }
        return result;
    }

    private MatchScore calculateScore(Resume resume, JobPosting job) {
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
        int denominator = Math.max(jobTokens.size(), 1);
        BigDecimal score = BigDecimal.valueOf(matched.size() * 100.0 / denominator)
                .setScale(2, RoundingMode.HALF_UP);
        String keywords = matched.isEmpty() ? "暂无明显关键词重合" : String.join("、", matched.stream().limit(8).toList());
        String analysis = "基于简历技能、经历与岗位要求的关键词重合度生成，匹配关键词：" + keywords + "。";
        return new MatchScore(score, analysis);
    }

    private Set<String> tokenize(String text) {
        Set<String> tokens = new LinkedHashSet<>();
        for (String token : text.toLowerCase(Locale.ROOT).split("[^\\p{IsHan}\\p{Alnum}]+")) {
            if (token.length() >= 2) tokens.add(token);
        }
        return tokens;
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

    private record MatchScore(BigDecimal value, String analysis) {
    }
}
