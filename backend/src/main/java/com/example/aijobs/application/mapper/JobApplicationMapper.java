package com.example.aijobs.application.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.aijobs.application.entity.JobApplication;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface JobApplicationMapper extends BaseMapper<JobApplication> {
}
