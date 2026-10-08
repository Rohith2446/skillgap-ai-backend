package com.skillgapai.service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class SkillGapAnalysisService {

    private static final List<String> SKILLS = List.of(
            "Java", "SQL", "Communication", "Problem solving", "Data analysis", "Cloud fundamentals");

    private static final List<Map<String, Double>> ROLE_WEIGHTS = List.of(
            Map.of("Java", 0.30, "SQL", 0.20, "Communication", 0.10, "Problem solving", 0.20, "Data analysis", 0.05, "Cloud fundamentals", 0.15),
            Map.of("Java", 0.05, "SQL", 0.25, "Communication", 0.15, "Problem solving", 0.15, "Data analysis", 0.30, "Cloud fundamentals", 0.10),
            Map.of("Java", 0.15, "SQL", 0.10, "Communication", 0.10, "Problem solving", 0.20, "Data analysis", 0.05, "Cloud fundamentals", 0.40)
    );

    private static final List<String> ROLE_NAMES = List.of("Backend Developer", "Data Analyst", "Cloud Engineer");

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${skillgap.gemini.enabled:false}")
    private boolean geminiEnabled;

    @Value("${skillgap.gemini.api-key:}")
    private String geminiApiKey;

    public AnalysisResponse generateAnalysis(Map<String, Integer> scores, String requestedRole) {
        Map<String, Integer> normalizedScores = normalizeScores(scores);
        AnalysisResponse fallback = buildFallbackAnalysis(normalizedScores, requestedRole);

        if (!geminiEnabled || geminiApiKey == null || geminiApiKey.isBlank()) {
            return fallback;
        }

        try {
            String text = callGemini(normalizedScores, requestedRole);
            if (text != null && !text.isBlank()) {
                return fallback.withSummary(text, "GEMINI");
            }
        } catch (Exception exception) {
            // Fall back to locally generated analysis when Gemini is unreachable or misconfigured.
        }

        return fallback;
    }

    private String callGemini(Map<String, Integer> scores, String requestedRole) throws IOException, InterruptedException {
        String prompt = "You are a career coach. Evaluate the following self-assessment and provide a brief actionable summary in plain English with no markdown. " +
                "Role context: " + (requestedRole == null || requestedRole.isBlank() ? "Student" : requestedRole) + ". " +
                "Scores: " + scores.entrySet().stream()
                        .map(entry -> entry.getKey() + "=" + entry.getValue())
                        .collect(Collectors.joining(", ")) + ". " +
                "Reply with 2 sentences that explain the strongest fit, biggest gaps, and the next 3 learning priorities.";

        String requestBody = objectMapper.writeValueAsString(Map.of(
                "contents", List.of(Map.of(
                        "parts", List.of(Map.of("text", prompt))
                ))
        ));

        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create("https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent"))
                .timeout(Duration.ofSeconds(25))
                .header("Content-Type", "application/json")
            .header("x-goog-api-key", geminiApiKey)
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

        HttpResponse<String> response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() >= 300) {
            throw new IOException("Gemini API responded with HTTP " + response.statusCode() + ": " + response.body());
        }

        JsonNode root = objectMapper.readTree(response.body());
        return root.path("candidates").path(0).path("content").path("parts").path(0).path("text").asText(null);
    }

    private AnalysisResponse buildFallbackAnalysis(Map<String, Integer> scores, String requestedRole) {
        List<String> sortedSkills = SKILLS.stream()
                .sorted(Comparator.comparingInt(score -> scores.getOrDefault(score, 0)))
                .toList();
        List<String> focusAreas = sortedSkills.stream().limit(3).toList();

        List<RoleMatch> roleMatches = new ArrayList<>();
        for (int i = 0; i < ROLE_NAMES.size(); i++) {
            String roleName = ROLE_NAMES.get(i);
            double score = ROLE_WEIGHTS.get(i).entrySet().stream()
                    .mapToDouble(entry -> (scores.getOrDefault(entry.getKey(), 0) / 5.0) * entry.getValue())
                    .sum() * 100;
            roleMatches.add(new RoleMatch(roleName, (int) Math.round(score)));
        }

        roleMatches.sort(Comparator.comparingInt(RoleMatch::readinessPercent).reversed());
        List<String> topRoles = roleMatches.stream()
                .limit(3)
            .map(RoleMatch::title)
                .toList();

        List<String> nextSteps = new ArrayList<>();
        for (String skill : focusAreas) {
            nextSteps.add("Practice " + skill + " with a focused project or mini lab session.");
        }
        if (nextSteps.size() < 3) {
            nextSteps.add("Add a short reflection journal to track confidence and wins after each learning sprint.");
        }

        double overallReadiness = normalizedAverage(scores);
        String strongest = SKILLS.stream()
                .max(Comparator.comparingInt(score -> scores.getOrDefault(score, 0)))
                .orElse("Career readiness");
        String primaryGap = focusAreas.getFirst();
        String friendlyRole = requestedRole == null || requestedRole.isBlank() ? "your target role" : requestedRole;

        String summary = "Your strongest area is " + strongest + ". Overall self-rated readiness across the supported skills is " + Math.round(overallReadiness) + "%. " +
            "For " + friendlyRole + ", the biggest gap is " + primaryGap + "; focus your next learning sprint there and reinforce the skills that already match the role.";

        String selectedRole = ROLE_NAMES.contains(requestedRole) ? requestedRole : ROLE_NAMES.getFirst();
        int selectedRoleIndex = ROLE_NAMES.indexOf(selectedRole);
        double readiness = ROLE_WEIGHTS.get(selectedRoleIndex).entrySet().stream()
            .mapToDouble(entry -> scores.getOrDefault(entry.getKey(), 0) / 5.0 * entry.getValue())
            .sum() * 100;
        List<String> gaps = SKILLS.stream().filter(skill -> scores.getOrDefault(skill, 0) <= 3).toList();
        List<String> proficient = SKILLS.stream().filter(skill -> scores.getOrDefault(skill, 0) >= 4).toList();
        List<String> critical = SKILLS.stream().filter(skill -> scores.getOrDefault(skill, 0) <= 1).toList();

        return new AnalysisResponse(summary, selectedRole, (int) Math.round(readiness), proficient, gaps, critical,
            focusAreas, topRoles, List.copyOf(roleMatches), nextSteps, "DETERMINISTIC_FALLBACK");
    }

    private Map<String, Integer> normalizeScores(Map<String, Integer> scores) {
        Map<String, Integer> normalized = new LinkedHashMap<>();
        if (scores == null) {
            return normalized;
        }
        for (String skill : SKILLS) {
            Integer rating = scores.get(skill);
            normalized.put(skill, rating == null ? 0 : Math.max(0, Math.min(5, rating)));
        }
        return normalized;
    }

    private double normalizedAverage(Map<String, Integer> scores) {
        if (scores == null || scores.isEmpty()) {
            return 0;
        }
        double total = scores.values().stream().mapToDouble(Integer::doubleValue).sum();
        return (total / (scores.size() * 5.0)) * 100;
    }

    public record AnalysisResponse(String summary, String targetRole, int readinessPercent, List<String> proficientSkills,
            List<String> skillGaps, List<String> criticalSkills, List<String> focusAreas, List<String> topRoles,
            List<RoleMatch> roleMatches, List<String> nextSteps, String analysisMethod) {
        AnalysisResponse withSummary(String nextSummary, String method) {
            return new AnalysisResponse(nextSummary, targetRole, readinessPercent, proficientSkills, skillGaps,
                    criticalSkills, focusAreas, topRoles, roleMatches, nextSteps, method);
        }
    }

    public record RoleMatch(String title, int readinessPercent) {}
}
