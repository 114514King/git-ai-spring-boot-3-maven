package com.example.aijobs.match;

import com.example.aijobs.common.BusinessException;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class SkillCoverageServiceTests {
    private final SkillCoverageService service = new SkillCoverageService();

    @ParameterizedTest
    @MethodSource("coverageCases")
    void returnsOrderedEvidenceAndExpectedCoverage(String resume, String requirements, String score,
                                                   List<String> matched, List<String> missing) {
        SkillCoverageService.Evidence evidence = service.calculate(resume, requirements);

        assertEquals(new BigDecimal(score), evidence.score());
        assertEquals(matched, evidence.matched());
        assertEquals(missing, evidence.missing());
        assertEquals(matched.size() + missing.size(), evidence.required().size());
        assertTrue(evidence.required().containsAll(matched));
        assertTrue(evidence.required().containsAll(missing));
    }

    private static Stream<Arguments> coverageCases() {
        return Stream.of(
                Arguments.of("英语 教学设计 备课", "English, lesson planning, classroom management", "66.67",
                        List.of("Lesson planning", "English"), List.of("Classroom management")),
                Arguments.of("Java SpringBoot", "JAVA Java Spring Boot springboot Redis Redis QuantumWidget", "66.67",
                        List.of("Java", "Spring Boot"), List.of("Redis")),
                Arguments.of(null, "Java Spring Boot Redis", "0.00", List.of(),
                        List.of("Java", "Spring Boot", "Redis")),
                Arguments.of("Java Script My SQL", "Java JavaScript SQL MySQL", "50.00",
                        List.of("JavaScript", "MySQL"), List.of("Java", "SQL")),
                Arguments.of("英语 English", "English English 英语", "100.00", List.of("English"), List.of()));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = { " \t ", "认真负责 QuantumWidget" })
    void unsupportedRequirementsDoNotProduceAMisleadingZeroScore(String requirements) {
        BusinessException failure = assertThrows(BusinessException.class,
                () -> service.calculate("Java Spring Boot", requirements));
        assertEquals(422, failure.getStatus().value());
    }
}
