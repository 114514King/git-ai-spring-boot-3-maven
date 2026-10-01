package com.example.aijobs.resume.dto;

import java.util.List;

public record ResumeImportPreviewResponse(String filename, String format, int pageCount,
                                         String text, List<String> warnings) { }
