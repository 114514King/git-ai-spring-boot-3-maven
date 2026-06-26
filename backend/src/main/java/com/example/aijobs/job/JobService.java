package com.example.aijobs.job;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.example.aijobs.common.BusinessException;
import com.example.aijobs.job.dto.JobRequest;
import com.example.aijobs.job.dto.JobResponse;
import com.example.aijobs.job.entity.JobPosting;
import com.example.aijobs.job.mapper.JobPostingMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class JobService {
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
}
