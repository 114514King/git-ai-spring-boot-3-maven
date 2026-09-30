package com.example.aijobs.match;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/** Versioned, literal skill recognition; mentions are evidence to review, not proof of proficiency. */
public final class SkillNormalizer {
    public static final String VERSION = "skill-dictionary-v1";
    private static final List<Skill> SKILLS = List.of(
            skill("Java", "java"),
            skill("JavaScript", "javascript", "java script", "js"),
            skill("TypeScript", "typescript", "type script", "ts"),
            skill("Python", "python"),
            skill("C++", "c++", "cpp"),
            skill("C#", "c#", "csharp", "c sharp"),
            skill("Spring Boot", "spring boot", "springboot", "spring-boot"),
            skill("MySQL", "mysql", "my sql"),
            skill("SQL", "sql"),
            skill("Redis", "redis"),
            skill("Git", "git"),
            skill("Linux", "linux"),
            skill("Vue", "vue", "vue.js", "vuejs", "vue3", "vue 3"),
            skill("React", "react", "react.js", "reactjs"),
            skill("Node.js", "node.js", "nodejs", "node js"),
            skill("HTML", "html", "html5"),
            skill("CSS", "css", "css3"),
            skill("Excel", "excel", "microsoft excel"),
            skill("PowerPoint", "powerpoint", "power point", "ppt"),
            skill("Lesson planning", "lesson planning", "lesson preparation", "备课", "教学设计"),
            skill("Classroom management", "classroom management", "课堂管理", "班级管理"),
            skill("Assessment", "assessment", "student assessment", "学习评价", "学情评估"),
            skill("Tutoring", "tutoring", "课后辅导", "学业辅导"),
            skill("Communication", "communication", "沟通能力", "沟通技巧"),
            skill("Teamwork", "teamwork", "team work", "团队合作", "团队协作"),
            skill("English", "english", "英语", "英文"),
            skill("Mandarin", "mandarin", "普通话"),
            skill("Safeguarding", "safeguarding", "儿童保护"),
            skill("First aid", "first aid", "急救"));

    private SkillNormalizer() { }

    public static Set<String> normalize(String text) {
        String normalized = normalizeText(text == null ? "" : text);
        List<Hit> hits = new ArrayList<>();
        for (Skill skill : SKILLS) {
            var matcher = skill.pattern().matcher(normalized);
            while (matcher.find()) {
                hits.add(new Hit(skill.name(), matcher.start(), matcher.end()));
            }
        }
        // Prefer a complete alias: "Java Script" must not also count as Java,
        // and "My SQL" must not also count as SQL.
        hits.sort(Comparator.comparingInt((Hit hit) -> hit.end() - hit.start()).reversed());
        boolean[] occupied = new boolean[normalized.length()];
        Set<String> found = new LinkedHashSet<>();
        for (Hit hit : hits) {
            boolean overlaps = false;
            for (int i = hit.start(); i < hit.end(); i++) {
                if (occupied[i]) { overlaps = true; break; }
            }
            if (!overlaps) {
                found.add(hit.name());
                for (int i = hit.start(); i < hit.end(); i++) occupied[i] = true;
            }
        }
        Set<String> ordered = new LinkedHashSet<>();
        for (Skill skill : SKILLS) {
            if (found.contains(skill.name())) ordered.add(skill.name());
        }
        return ordered;
    }

    private static Skill skill(String name, String... aliases) {
        List<String> alternatives = new ArrayList<>();
        for (String alias : aliases) {
            alternatives.add(Pattern.quote(normalizeText(alias)));
        }
        // Han text may touch an English skill; Latin words and identifiers may not.
        return new Skill(name, Pattern.compile("(?<![\\p{L}\\p{N}_+#&&[^\\p{IsHan}]])(?:"
                + String.join("|", alternatives) + ")(?![\\p{L}\\p{N}_+#&&[^\\p{IsHan}]])"));
    }

    private static String normalizeText(String text) {
        return Normalizer.normalize(text, Normalizer.Form.NFKC).toLowerCase(Locale.ROOT)
                .replaceAll("(?U)\\s+", " ");
    }

    private record Skill(String name, Pattern pattern) { }
    private record Hit(String name, int start, int end) { }
}
