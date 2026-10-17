package com.example.aijobs.job.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.aijobs.job.entity.JobPosting;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface JobPostingMapper extends BaseMapper<JobPosting> {
}
