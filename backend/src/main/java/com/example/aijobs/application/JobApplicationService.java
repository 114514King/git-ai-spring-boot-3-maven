package com.example.aijobs.application;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.example.aijobs.application.dto.ApplicationRequest;
import com.example.aijobs.application.dto.ApplicationResponse;
import com.example.aijobs.application.entity.JobApplication;
import com.example.aijobs.application.mapper.JobApplicationMapper;
import com.example.aijobs.common.BusinessException;
import com.example.aijobs.job.entity.JobPosting;
import com.example.aijobs.job.mapper.JobPostingMapper;
import com.example.aijobs.resume.entity.Resume;
import com.example.aijobs.resume.mapper.ResumeMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class JobApplicationService {
    private final JobApplicationMapper applicationMapper;
    private final JobPostingMapper jobMapper;
    private final ResumeMapper resumeMapper;

    public JobApplicationService(JobApplicationMapper applicationMapper,
                                 JobPostingMapper jobMapper,
                                 ResumeMapper resumeMapper) {
        this.applicationMapper = applicationMapper;
        this.jobMapper = jobMapper;
        this.resumeMapper = resumeMapper;
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
}
