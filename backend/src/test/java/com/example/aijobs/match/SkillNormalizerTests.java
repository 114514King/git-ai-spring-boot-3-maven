package com.example.aijobs.match;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class SkillNormalizerTests {
    @ParameterizedTest
    @CsvSource({"ＪＡＶＡ, Java", "springboot, Spring Boot", "Spring-Boot, Spring Boot",
            "java script, JavaScript", "My SQL, MySQL", "熟悉Java开发, Java",
            "熟悉Ｃ＋＋开发, C++", "C Sharp, C#", "node.js, Node.js", "教学设计, Lesson planning",
            "classroom management, Classroom management", "英语, English"})
    void recognizesAliasesWithoutAddingOverlappingSkills(String text, String expected) {
        assertEquals(Set.of(expected), SkillNormalizer.normalize(text));
    }

    @Test
    void normalizesWhitespaceAndDeduplicatesAliasesInStableDictionaryOrder() {
        assertEquals(List.of("Java", "Spring Boot", "Lesson planning"),
                List.copyOf(SkillNormalizer.normalize("备课 教学设计 Spring\t\nBoot springboot JAVA java")));
    }

    @Test
    void preservesSymbolSkillsAndDoesNotConfuseRelatedNames() {
        assertEquals(Set.of("C++", "C#", "JavaScript", "MySQL"),
                SkillNormalizer.normalize("C++ C# JavaScript MySQL"));
    }

    @Test
    void doesNotRecognizeSubstringsInWordsOrIdentifiers() {
        assertTrue(SkillNormalizer.normalize("JavaBeans NoSQL PostgreSQL GitHub reactivate mysql8 java_dev caféJava").isEmpty());
    }

    @Test
    void handlesEmptyAndUnsupportedTextWithoutInventingSkills() {
        assertTrue(SkillNormalizer.normalize(null).isEmpty());
        assertTrue(SkillNormalizer.normalize(" \t ").isEmpty());
        assertTrue(SkillNormalizer.normalize("负责相关工作，认真负责；QuantumWidget").isEmpty());
    }
}
