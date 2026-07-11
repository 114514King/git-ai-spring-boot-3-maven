package com.example.aijobs.job.dto;

import java.util.List;

public record JobJdAnalysisResponse(Long jobId,
                                    String modelName,
                                    String summary,
                                    List<String> keySkills,
                                    List<String> highlights,
                                    List<String> gaps,
                                    List<String> suggestions) {
}
