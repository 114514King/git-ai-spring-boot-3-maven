package com.example.aijobs.assistant;

import com.example.aijobs.deepseek.DeepSeekChatResult;

import java.math.BigDecimal;
import java.util.List;

public record RecruitmentChatResponse(String conversationId,
                                      String role,
                                      String assistantMessage,
                                      boolean generated,
                                      DeepSeekChatResult.Status modelStatus,
                                      String modelName,
                                      String contextSummary,
                                      Integer historyMessageCount,
                                      Integer maxHistoryMessages,
                                      BigDecimal readOnlyMatchScore,
                                      String matchScoreSource,
                                      boolean humanReviewRequired,
                                      List<String> warnings) {
}
