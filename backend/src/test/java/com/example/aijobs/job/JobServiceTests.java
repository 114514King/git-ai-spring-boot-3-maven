package com.example.aijobs.job;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.example.aijobs.common.BusinessException;
import com.example.aijobs.job.dto.JobRequest;
import com.example.aijobs.job.dto.JobResponse;
import com.example.aijobs.job.entity.JobPosting;
import com.example.aijobs.job.mapper.JobPostingMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JobServiceTests {
    @Mock private JobPostingMapper jobMapper;
    private JobService jobService;

    @BeforeEach
    void setUp() {
        jobService = new JobService(jobMapper);
    }

    @Test
    void createStoresOwnedDraft() {
        doAnswer(invocation -> {
            JobPosting job = invocation.getArgument(0);
            job.setId(7L);
            return 1;
        }).when(jobMapper).insert(any(JobPosting.class));

        JobResponse response = jobService.create(42L, request("10000", "15000"));

        assertEquals(7L, response.id());
        assertEquals(42L, response.hrId());
        assertEquals("DRAFT", response.status());
        verify(jobMapper).insert(any(JobPosting.class));
    }

    @Test
    void createRejectsReversedSalaryRange() {
        BusinessException exception = assertThrows(BusinessException.class,
                () -> jobService.create(42L, request("16000", "15000")));

        assertEquals(400, exception.getStatus().value());
        verifyNoInteractions(jobMapper);
    }

    @Test
    void publishedListMapsResults() {
        JobPosting job = ownedJob(42L);
        job.setStatus("PUBLISHED");
        when(jobMapper.selectList(any(Wrapper.class))).thenReturn(List.of(job));

        List<JobResponse> response = jobService.listPublished("Java", "上海", "FULL_TIME");

        assertEquals(1, response.size());
        assertEquals("Java 开发工程师", response.getFirst().title());
    }

    @Test
    void anotherHrCannotUpdateJob() {
        when(jobMapper.selectById(7L)).thenReturn(ownedJob(42L));

        BusinessException exception = assertThrows(BusinessException.class,
                () -> jobService.update(99L, 7L, request("10000", "15000")));

        assertEquals(403, exception.getStatus().value());
        verify(jobMapper, never()).updateById(any(JobPosting.class));
    }

    private JobRequest request(String min, String max) {
        return new JobRequest("Java 开发工程师", "示例科技", "上海", "FULL_TIME",
                new BigDecimal(min), new BigDecimal(max), "负责后端开发", "熟悉 Spring Boot");
    }

    private JobPosting ownedJob(Long hrId) {
        JobPosting job = new JobPosting();
        job.setId(7L);
        job.setHrId(hrId);
        job.setTitle("Java 开发工程师");
        job.setCompanyName("示例科技");
        job.setCity("上海");
        job.setEmploymentType("FULL_TIME");
        job.setDescription("负责后端开发");
        job.setRequirements("熟悉 Spring Boot");
        job.setStatus("DRAFT");
        return job;
    }
}
