package com.example.aijobs.application;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.example.aijobs.application.dto.ApplicationRequest;
import com.example.aijobs.application.dto.ApplicationResponse;
import com.example.aijobs.application.entity.JobApplication;
import com.example.aijobs.application.mapper.JobApplicationMapper;
import com.example.aijobs.common.BusinessException;
import com.example.aijobs.job.entity.JobPosting;
import com.example.aijobs.job.mapper.JobPostingMapper;
import com.example.aijobs.resume.entity.Resume;
import com.example.aijobs.resume.mapper.ResumeMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JobApplicationServiceTests {
    @Mock private JobApplicationMapper applicationMapper;
    @Mock private JobPostingMapper jobMapper;
    @Mock private ResumeMapper resumeMapper;
    private JobApplicationService applicationService;

    @BeforeEach
    void setUp() {
        applicationService = new JobApplicationService(applicationMapper, jobMapper, resumeMapper);
    }

    @Test
    void studentCanSubmitWithPublishedOwnedResume() {
        when(jobMapper.selectById(9L)).thenReturn(publishedJob(7L));
        when(resumeMapper.selectById(5L)).thenReturn(publishedResume(42L));
        when(applicationMapper.selectCount(any(Wrapper.class))).thenReturn(0L);
        doAnswer(invocation -> {
            JobApplication application = invocation.getArgument(0);
            application.setId(11L);
            return 1;
        }).when(applicationMapper).insert(any(JobApplication.class));

        ApplicationResponse response = applicationService.submit(42L, new ApplicationRequest(9L, 5L));

        assertEquals(11L, response.id());
        assertEquals(9L, response.jobId());
        assertEquals(42L, response.studentId());
        assertEquals("SUBMITTED", response.status());
    }

    @Test
    void cannotSubmitDraftResume() {
        when(jobMapper.selectById(9L)).thenReturn(publishedJob(7L));
        Resume resume = publishedResume(42L);
        resume.setStatus("DRAFT");
        when(resumeMapper.selectById(5L)).thenReturn(resume);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> applicationService.submit(42L, new ApplicationRequest(9L, 5L)));

        assertEquals(400, exception.getStatus().value());
        verify(applicationMapper, never()).insert(any(JobApplication.class));
    }

    @Test
    void cannotSubmitSameJobTwice() {
        when(jobMapper.selectById(9L)).thenReturn(publishedJob(7L));
        when(resumeMapper.selectById(5L)).thenReturn(publishedResume(42L));
        when(applicationMapper.selectCount(any(Wrapper.class))).thenReturn(1L);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> applicationService.submit(42L, new ApplicationRequest(9L, 5L)));

        assertEquals(409, exception.getStatus().value());
    }

    @Test
    void hrCanListOnlyOwnedJobApplications() {
        JobPosting ownedJob = publishedJob(7L);
        ownedJob.setId(9L);
        when(jobMapper.selectList(any(Wrapper.class))).thenReturn(List.of(ownedJob));
        when(applicationMapper.selectList(any(Wrapper.class))).thenReturn(List.of(application(42L, 9L)));

        List<ApplicationResponse> response = applicationService.listHrApplications(7L, 9L);

        assertEquals(1, response.size());
        assertEquals(9L, response.getFirst().jobId());
    }

    @Test
    void anotherHrCannotUpdateApplicationStatus() {
        when(applicationMapper.selectById(11L)).thenReturn(application(42L, 9L));
        when(jobMapper.selectById(9L)).thenReturn(publishedJob(7L));

        BusinessException exception = assertThrows(BusinessException.class,
                () -> applicationService.updateHrStatus(99L, 11L, "REVIEWING"));

        assertEquals(403, exception.getStatus().value());
    }

    @Test
    void studentCanWithdrawOwnApplication() {
        when(applicationMapper.selectById(11L)).thenReturn(application(42L, 9L));

        ApplicationResponse response = applicationService.withdraw(42L, 11L);

        assertEquals("WITHDRAWN", response.status());
        verify(applicationMapper).updateById(any(JobApplication.class));
    }

    private JobPosting publishedJob(Long hrId) {
        JobPosting job = new JobPosting();
        job.setId(9L);
        job.setHrId(hrId);
        job.setStatus("PUBLISHED");
        return job;
    }

    private Resume publishedResume(Long studentId) {
        Resume resume = new Resume();
        resume.setId(5L);
        resume.setStudentId(studentId);
        resume.setStatus("PUBLISHED");
        return resume;
    }

    private JobApplication application(Long studentId, Long jobId) {
        JobApplication application = new JobApplication();
        application.setId(11L);
        application.setJobId(jobId);
        application.setStudentId(studentId);
        application.setResumeId(5L);
        application.setStatus("SUBMITTED");
        return application;
    }
}
