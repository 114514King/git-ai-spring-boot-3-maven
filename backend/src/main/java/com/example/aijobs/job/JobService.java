package com.example.aijobs.job;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.example.aijobs.common.BusinessException;
import com.example.aijobs.job.dto.JobJdAnalysisResponse;
import com.example.aijobs.job.dto.JobRequest;
import com.example.aijobs.job.dto.JobResponse;
import com.example.aijobs.job.entity.JobPosting;
import com.example.aijobs.job.mapper.JobPostingMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class JobService {
    private static final String JD_ANALYSIS_MODEL_NAME = "local-jd-analyzer-v1";
    private static final Set<String> JD_STOP_WORDS = Set.of(
            "full", "time", "part", "internship", "负责", "岗位", "职位", "要求", "工作", "相关", "优先", "熟悉", "具备");

    private final JobPostingMapper jobMapper;
    private final JobCacheService jobCacheService;

    public JobService(JobPostingMapper jobMapper, JobCacheService jobCacheService) {
        this.jobMapper = jobMapper;
        this.jobCacheService = jobCacheService;
    }

    public List<JobResponse> listPublished(String keyword, String city, String employmentType) {
        var cached = jobCacheService.getPublishedList(keyword, city, employmentType);
        if (cached.isPresent()) return cached.get();

        var query = Wrappers.<JobPosting>lambdaQuery().eq(JobPosting::getStatus, "PUBLISHED");
        if (StringUtils.hasText(keyword)) {
            query.and(wrapper -> wrapper.like(JobPosting::getTitle, keyword)
                    .or().like(JobPosting::getCompanyName, keyword));
        }
        if (StringUtils.hasText(city)) query.eq(JobPosting::getCity, city);
        if (StringUtils.hasText(employmentType)) query.eq(JobPosting::getEmploymentType, employmentType);
        List<JobResponse> jobs = jobMapper.selectList(query.orderByDesc(JobPosting::getPublishedAt)).stream()
                .map(JobResponse::from).toList();
        jobCacheService.putPublishedList(keyword, city, employmentType, jobs);
        return jobs;
    }

    public JobResponse getPublished(Long id) {
        var cached = jobCacheService.getPublishedDetail(id);
        if (cached.isPresent()) return cached.get();

        JobPosting job = jobMapper.selectOne(Wrappers.<JobPosting>lambdaQuery()
                .eq(JobPosting::getId, id).eq(JobPosting::getStatus, "PUBLISHED"));
        if (job == null) throw new BusinessException(HttpStatus.NOT_FOUND, "岗位不存在或尚未发布");
        JobResponse response = JobResponse.from(job);
        jobCacheService.putPublishedDetail(id, response);
        return response;
    }

    public List<JobResponse> listOwned(Long hrId) {
        return jobMapper.selectList(Wrappers.<JobPosting>lambdaQuery()
                        .eq(JobPosting::getHrId, hrId).orderByDesc(JobPosting::getCreatedAt)).stream()
                .map(JobResponse::from).toList();
    }

    public JobJdAnalysisResponse analyzeJd(Long hrId, Long id) {
        JobPosting job = ownedJob(hrId, id);
        Set<String> titleTokens = tokenize(job.getTitle());
        Set<String> descriptionTokens = tokenize(job.getDescription());
        Set<String> requirementTokens = tokenize(job.getRequirements());
        Set<String> allTokens = new LinkedHashSet<>();
        allTokens.addAll(titleTokens);
        allTokens.addAll(descriptionTokens);
        allTokens.addAll(requirementTokens);
        allTokens.removeAll(JD_STOP_WORDS);

        Set<String> overlap = new LinkedHashSet<>(descriptionTokens);
        overlap.retainAll(requirementTokens);
        overlap.removeAll(JD_STOP_WORDS);

        return new JobJdAnalysisResponse(
                job.getId(),
                JD_ANALYSIS_MODEL_NAME,
                buildJdSummary(job, allTokens, overlap),
                allTokens.stream().limit(10).toList(),
                buildJdHighlights(job, titleTokens, overlap),
                buildJdGaps(job, descriptionTokens, requirementTokens),
                buildJdSuggestions(job, allTokens, overlap));
    }

    @Transactional
    public JobResponse create(Long hrId, JobRequest request) {
        validateSalary(request);
        JobPosting job = new JobPosting();
        job.setHrId(hrId);
        apply(job, request);
        job.setStatus("DRAFT");
        jobMapper.insert(job);
        jobCacheService.evictPublished(job.getId());
        return JobResponse.from(job);
    }

    @Transactional
    public JobResponse update(Long hrId, Long id, JobRequest request) {
        validateSalary(request);
        JobPosting job = ownedJob(hrId, id);
        apply(job, request);
        jobMapper.updateById(job);
        jobCacheService.evictPublished(id);
        return JobResponse.from(job);
    }

    @Transactional
    public JobResponse updateStatus(Long hrId, Long id, String status) {
        JobPosting job = ownedJob(hrId, id);
        job.setStatus(status);
        if ("PUBLISHED".equals(status) && job.getPublishedAt() == null) job.setPublishedAt(LocalDateTime.now());
        jobMapper.updateById(job);
        jobCacheService.evictPublished(id);
        return JobResponse.from(job);
    }

    private JobPosting ownedJob(Long hrId, Long id) {
        JobPosting job = jobMapper.selectById(id);
        if (job == null) throw new BusinessException(HttpStatus.NOT_FOUND, "岗位不存在");
        if (!hrId.equals(job.getHrId())) throw new BusinessException(HttpStatus.FORBIDDEN, "只能管理自己发布的岗位");
        return job;
    }

    private void validateSalary(JobRequest request) {
        if (request.salaryMin() != null && request.salaryMax() != null
                && request.salaryMin().compareTo(request.salaryMax()) > 0) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "最低薪资不能高于最高薪资");
        }
    }

    private void apply(JobPosting job, JobRequest request) {
        job.setTitle(request.title());
        job.setCompanyName(request.companyName());
        job.setCity(request.city());
        job.setEmploymentType(request.employmentType());
        job.setSalaryMin(request.salaryMin());
        job.setSalaryMax(request.salaryMax());
        job.setDescription(request.description());
        job.setRequirements(request.requirements());
    }

    private String buildJdSummary(JobPosting job, Set<String> allTokens, Set<String> overlap) {
        if (allTokens.isEmpty()) {
            return "岗位“" + job.getTitle() + "”的 JD 信息较少，建议先补充职责、技能要求和交付目标。";
        }
        if (overlap.size() >= 3) {
            return "岗位“" + job.getTitle() + "”的职责与要求呼应较清晰，已识别 "
                    + allTokens.size() + " 个关键表达，可继续强化成果指标和候选人画像。";
        }
        return "岗位“" + job.getTitle() + "”已具备基础 JD 信息，但职责描述与任职要求的关键词呼应不足。";
    }

    private List<String> buildJdHighlights(JobPosting job, Set<String> titleTokens, Set<String> overlap) {
        String titleText = titleTokens.isEmpty() ? job.getTitle() : String.join("、", titleTokens.stream().limit(3).toList());
        String overlapText = overlap.isEmpty() ? "职责和技能要求" : String.join("、", overlap.stream().limit(4).toList());
        return List.of(
                "岗位标题聚焦在“" + titleText + "”，便于候选人快速判断方向。",
                "岗位城市和用工类型明确：" + job.getCity() + " / " + job.getEmploymentType() + "。",
                "JD 中已出现 " + overlapText + " 等可用于匹配的核心表达。");
    }

    private List<String> buildJdGaps(JobPosting job, Set<String> descriptionTokens, Set<String> requirementTokens) {
        boolean shortDescription = safe(job.getDescription()).length() < 40;
        boolean shortRequirements = safe(job.getRequirements()).length() < 30;
        boolean missingSalary = job.getSalaryMin() == null && job.getSalaryMax() == null;
        Set<String> requirementOnly = new LinkedHashSet<>(requirementTokens);
        requirementOnly.removeAll(descriptionTokens);
        requirementOnly.removeAll(JD_STOP_WORDS);

        List<String> gaps = new java.util.ArrayList<>();
        if (shortDescription) gaps.add("岗位描述偏短，缺少具体工作场景、交付物或协作对象。");
        if (shortRequirements) gaps.add("任职要求偏短，候选人难以判断必须能力和加分能力。");
        if (missingSalary) gaps.add("薪资范围未填写，可能降低候选人投递决策效率。");
        if (!requirementOnly.isEmpty()) {
            gaps.add("任职要求中的 " + String.join("、", requirementOnly.stream().limit(4).toList()) + " 尚未在职责中形成呼应。");
        }
        if (gaps.isEmpty()) gaps.add("暂未发现明显结构缺口，建议继续补充量化指标提升吸引力。");
        return gaps;
    }

    private List<String> buildJdSuggestions(JobPosting job, Set<String> allTokens, Set<String> overlap) {
        String keywordText = allTokens.isEmpty() ? "核心技能" : String.join("、", allTokens.stream().limit(4).toList());
        String overlapText = overlap.isEmpty() ? "职责目标与能力要求" : String.join("、", overlap.stream().limit(3).toList());
        return List.of(
                "职责表达：按“业务场景 + 关键任务 + 交付结果”重写描述，突出 " + overlapText + "。",
                "要求分层：将 " + keywordText + " 拆分为必备条件和加分项，减少宽泛表述。",
                "吸引力补充：增加团队规模、技术栈、成长空间或薪资范围等候选人决策信息。",
                "匹配复核：发布后结合 AI 匹配结果观察缺口关键词是否过多。");
    }

    private Set<String> tokenize(String text) {
        Set<String> tokens = new LinkedHashSet<>();
        for (String token : safe(text).toLowerCase(Locale.ROOT).split("[^\\p{IsHan}\\p{Alnum}]+")) {
            if (token.length() >= 2) tokens.add(token);
        }
        return tokens;
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}
