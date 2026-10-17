package com.example.aijobs.resume;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.example.aijobs.common.BusinessException;
import com.example.aijobs.resume.dto.ResumeRequest;
import com.example.aijobs.resume.dto.ResumeResponse;
import com.example.aijobs.resume.entity.Resume;
import com.example.aijobs.resume.mapper.ResumeMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ResumeService {
    private final ResumeMapper resumeMapper;

    public ResumeService(ResumeMapper resumeMapper) {
        this.resumeMapper = resumeMapper;
    }

    public List<ResumeResponse> listOwned(Long studentId) {
        return resumeMapper.selectList(Wrappers.<Resume>lambdaQuery()
                        .eq(Resume::getStudentId, studentId).orderByDesc(Resume::getUpdatedAt)).stream()
                .map(ResumeResponse::from).toList();
    }

    public ResumeResponse getOwned(Long studentId, Long id) {
        return ResumeResponse.from(ownedResume(studentId, id));
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

    private void apply(Resume resume, ResumeRequest request) {
        resume.setTitle(request.title());
        resume.setEducation(request.education());
        resume.setWorkExperience(request.workExperience());
        resume.setProjectExperience(request.projectExperience());
        resume.setSkills(request.skills());
        resume.setSelfEvaluation(request.selfEvaluation());
    }
}
