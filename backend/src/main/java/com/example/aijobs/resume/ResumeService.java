package com.example.aijobs.resume;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.example.aijobs.common.BusinessException;
import com.example.aijobs.job.entity.JobPosting;
import com.example.aijobs.job.mapper.JobPostingMapper;
import com.example.aijobs.resume.dto.ResumeOptimizationRequest;
import com.example.aijobs.resume.dto.ResumeOptimizationResponse;
import com.example.aijobs.resume.dto.ResumeRequest;
import com.example.aijobs.resume.dto.ResumeResponse;
import com.example.aijobs.resume.entity.Resume;
import com.example.aijobs.resume.mapper.ResumeMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class ResumeService {
    private static final String OPTIMIZATION_MODEL_NAME = "local-resume-optimizer-v1";
    private static final Set<String> OPTIMIZATION_STOP_WORDS = Set.of(
            "full", "time", "part", "internship", "负责", "经验", "岗位", "要求", "工作");

    private final ResumeMapper resumeMapper;
    private final JobPostingMapper jobMapper;

    public ResumeService(ResumeMapper resumeMapper, JobPostingMapper jobMapper) {
        this.resumeMapper = resumeMapper;
        this.jobMapper = jobMapper;
    }

    public List<ResumeResponse> listOwned(Long studentId) {
        return resumeMapper.selectList(Wrappers.<Resume>lambdaQuery()
                        .eq(Resume::getStudentId, studentId).orderByDesc(Resume::getUpdatedAt)).stream()
                .map(ResumeResponse::from).toList();
    }

    public ResumeResponse getOwned(Long studentId, Long id) {
        return ResumeResponse.from(ownedResume(studentId, id));
    }

    public ResumeOptimizationResponse optimizeForJob(Long studentId, Long id, ResumeOptimizationRequest request) {
        Resume resume = ownedResume(studentId, id);
        JobPosting job = requirePublishedJob(request.jobId());
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
        missing.removeAll(OPTIMIZATION_STOP_WORDS);

        return new ResumeOptimizationResponse(
                resume.getId(),
                job.getId(),
                OPTIMIZATION_MODEL_NAME,
                buildOverallSummary(resume, job, matched, missing),
                matched.stream().limit(8).toList(),
                missing.stream().limit(8).toList(),
                buildContentSuggestions(resume, job, matched, missing),
                buildActionPlan(missing));
    }

    @Transactional
    public ResumeResponse create(Long studentId, ResumeRequest request) {
        Resume resume = new Resume();
        resume.setStudentId(studentId);
        apply(resume, request);
        resume.setStatus("DRAFT");
        resumeMapper.insert(resume);
        return ResumeResponse.from(resume);
    }

    @Transactional
    public ResumeResponse update(Long studentId, Long id, ResumeRequest request) {
        Resume resume = ownedResume(studentId, id);
        apply(resume, request);
        resumeMapper.updateById(resume);
        return ResumeResponse.from(resume);
    }

    @Transactional
    public ResumeResponse updateStatus(Long studentId, Long id, String status) {
        Resume resume = ownedResume(studentId, id);
        resume.setStatus(status);
        resumeMapper.updateById(resume);
        return ResumeResponse.from(resume);
    }

    private Resume ownedResume(Long studentId, Long id) {
        Resume resume = resumeMapper.selectById(id);
        if (resume == null) throw new BusinessException(HttpStatus.NOT_FOUND, "简历不存在");
        if (!studentId.equals(resume.getStudentId())) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "只能管理自己的简历");
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

    private void apply(Resume resume, ResumeRequest request) {
        resume.setTitle(request.title());
        resume.setEducation(request.education());
        resume.setWorkExperience(request.workExperience());
        resume.setProjectExperience(request.projectExperience());
        resume.setSkills(request.skills());
        resume.setSelfEvaluation(request.selfEvaluation());
    }

    private String buildOverallSummary(Resume resume, JobPosting job, Set<String> matched, Set<String> missing) {
        if (matched.isEmpty()) {
            return "当前简历与目标岗位“" + job.getTitle() + "”的关键词重合较少，建议先补齐岗位核心技能和项目证据。";
        }
        if (missing.isEmpty()) {
            return "当前简历已覆盖目标岗位“" + job.getTitle() + "”的主要关键词，建议继续强化成果表达和可量化指标。";
        }
        return "当前简历“" + resume.getTitle() + "”已覆盖 " + matched.size()
                + " 个目标岗位关键词，仍有 " + missing.size() + " 个关键词缺少明确证据。";
    }

    private List<String> buildContentSuggestions(Resume resume, JobPosting job, Set<String> matched, Set<String> missing) {
        String missingText = missing.isEmpty() ? "岗位核心要求" : String.join("、", missing.stream().limit(4).toList());
        String matchedText = matched.isEmpty() ? "已有项目经历" : String.join("、", matched.stream().limit(4).toList());
        return List.of(
                "技能关键词：在技能栏补充与“" + job.getTitle() + "”相关的 " + missingText + "，避免只写宽泛能力。",
                "项目经历：围绕 " + matchedText + " 写清个人职责、技术选型、交付结果和业务影响。",
                "工作经历：优先补充可验证的动作和数据，例如性能提升、接口数量、协作角色或上线结果。",
                "自我评价：减少笼统表述，改为突出目标岗位最看重的能力证据。");
    }

    private List<String> buildActionPlan(Set<String> missing) {
        if (missing.isEmpty()) {
            return List.of(
                    "保留现有关键词覆盖，并为每段项目增加 1 个量化结果。",
                    "检查简历标题和技能栏是否直接呼应目标岗位名称。",
                    "投递前用 AI 匹配模块复核分数和解释结果。");
        }
        return List.of(
                "优先补充前 3 个缺口关键词：" + String.join("、", missing.stream().limit(3).toList()) + "。",
                "为每个新增关键词提供对应项目场景，避免只堆砌术语。",
                "完善后重新发布简历，并再次生成岗位匹配评分。");
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
}
