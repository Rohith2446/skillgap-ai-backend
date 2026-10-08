package com.skillgapai.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.Map;

import org.junit.jupiter.api.Test;

class SkillGapAnalysisServiceTest {

    @Test
    void shouldGenerateRoleFocusedAnalysisFromScores() {
        SkillGapAnalysisService service = new SkillGapAnalysisService();

        var analysis = service.generateAnalysis(Map.of(
                "Java", 4,
                "SQL", 2,
                "Communication", 3,
                "Problem solving", 4,
                "Data analysis", 2,
                "Cloud fundamentals", 1), "Backend Developer");

        assertNotNull(analysis);
        assertFalse(analysis.summary().isBlank());
        assertFalse(analysis.focusAreas().isEmpty());
        assertFalse(analysis.topRoles().isEmpty());
    }
}
