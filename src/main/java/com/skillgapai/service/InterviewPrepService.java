package com.skillgapai.service;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

@Service
public class InterviewPrepService {
    private static final Map<String, List<Question>> ROLE_QUESTIONS = Map.of(
            "Backend Developer", List.of(
                    new Question("technical", "Intermediate", "How would you design a REST endpoint that validates input and reports errors consistently?", "Java"),
                    new Question("conceptual", "Intermediate", "What is the difference between a database index and a unique constraint, and when would you use each?", "SQL"),
                    new Question("scenario", "Advanced", "A production API becomes slow after a release. Describe how you would investigate and reduce the impact.", "Problem solving")),
            "Data Analyst", List.of(
                    new Question("technical", "Intermediate", "How would you use SQL to identify duplicate records while preserving the original data?", "SQL"),
                    new Question("conceptual", "Intermediate", "How do you choose a useful metric and check whether it represents the business question?", "Data analysis"),
                    new Question("scenario", "Advanced", "A stakeholder disagrees with a report's conclusion. How would you validate the analysis and communicate uncertainty?", "Communication")),
            "Cloud Engineer", List.of(
                    new Question("technical", "Intermediate", "What factors would you consider when designing a resilient service across availability zones?", "Cloud fundamentals"),
                    new Question("conceptual", "Intermediate", "Explain the difference between scaling vertically and horizontally, including a trade-off for each.", "Cloud fundamentals"),
                    new Question("scenario", "Advanced", "A service is intermittently unavailable after a deployment. How would you isolate the cause and restore service?", "Problem solving")));

    public List<Question> questions(String role, List<String> skillGaps) {
        List<Question> templates = ROLE_QUESTIONS.getOrDefault(role, ROLE_QUESTIONS.get("Backend Developer"));
        if (skillGaps == null || skillGaps.isEmpty()) {
            return templates;
        }
        return templates.stream()
            .sorted(java.util.Comparator.comparingInt(question -> {
                int gapPosition = skillGaps.indexOf(question.skill());
                return gapPosition < 0 ? Integer.MAX_VALUE : gapPosition;
            }))
                .toList();
    }

    public Feedback evaluate(String answer) {
        String normalized = answer == null ? "" : answer.trim();
        int wordCount = normalized.isBlank() ? 0 : normalized.split("\\s+").length;
        int score = Math.min(100, wordCount * 3);
        boolean hasExample = normalized.toLowerCase().matches("(?s).*(for example|for instance|i would|i did|we would|we did).*?");
        if (hasExample) {
            score = Math.min(100, score + 15);
        }

        String feedback;
        if (wordCount < 15) {
            feedback = "Add more detail: explain your reasoning and include a concrete example or trade-off.";
        } else if (!hasExample) {
            feedback = "Your answer has a useful starting point. Strengthen it with a specific example and the result you would measure.";
        } else {
            feedback = "Your answer includes a concrete approach. Make the result or trade-off explicit to help the interviewer follow your reasoning.";
        }

        return new Feedback(score, feedback, "RULE_BASED_PRACTICE_FEEDBACK");
    }

    public record Question(String category, String difficulty, String prompt, String skill) {}
    public record Feedback(int practiceScore, String feedback, String evaluationMethod) {}
}