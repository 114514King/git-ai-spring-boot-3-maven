package com.example.aijobs.match.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@TableName("ai_match_result")
public class AiMatchResult {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long resumeId;
    private Long jobId;
    private BigDecimal score;
    private String analysis;
    private String strengthSummary;
    private String gapSummary;
    private String actionSuggestions;
    private String modelName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getResumeId() { return resumeId; }
    public void setResumeId(Long resumeId) { this.resumeId = resumeId; }
    public Long getJobId() { return jobId; }
    public void setJobId(Long jobId) { this.jobId = jobId; }
    public BigDecimal getScore() { return score; }
    public void setScore(BigDecimal score) { this.score = score; }
    public String getAnalysis() { return analysis; }
    public void setAnalysis(String analysis) { this.analysis = analysis; }
    public String getStrengthSummary() { return strengthSummary; }
    public void setStrengthSummary(String strengthSummary) { this.strengthSummary = strengthSummary; }
    public String getGapSummary() { return gapSummary; }
    public void setGapSummary(String gapSummary) { this.gapSummary = gapSummary; }
    public String getActionSuggestions() { return actionSuggestions; }
    public void setActionSuggestions(String actionSuggestions) { this.actionSuggestions = actionSuggestions; }
    public String getModelName() { return modelName; }
    public void setModelName(String modelName) { this.modelName = modelName; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
