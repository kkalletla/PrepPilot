package com.preppilot.coaching;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.preppilot.coaching.CoachingTemplates.Concept;
import com.preppilot.coaching.CoachingTemplates.DesignTemplate;
import com.preppilot.coaching.CoachingTemplates.HintTemplate;
import com.preppilot.coaching.CoachingTemplates.StageTemplate;
import com.preppilot.design.DesignStage;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * MVP {@link CoachingEngine}: curated, static hints and rubric feedback keyed by problem/question + step.
 * No live model calls, zero runtime AI cost, fully deterministic and testable offline.
 */
public class TemplateCoachingEngine implements CoachingEngine {

    private static final Pattern SENTENCE_SPLIT = Pattern.compile("[.!?\\n]+");
    private static final Set<String> JUSTIFICATION_WORDS = Set.of(
            "because", "trade-off", "tradeoff", "trade off", "instead", "alternatively", "however",
            "downside", "cost", "versus", " vs ", "at the expense", "prefer", "rather than");

    private final Map<String, HintTemplate> hints;
    private final Map<String, DesignTemplate> designs;

    public TemplateCoachingEngine(Map<String, HintTemplate> hints, Map<String, DesignTemplate> designs) {
        this.hints = Map.copyOf(hints);
        this.designs = Map.copyOf(designs);
    }

    /** Loads the bundled templates from {@code classpath:coaching/hints.json} and {@code coaching/design.json}. */
    public static TemplateCoachingEngine fromClasspath(ObjectMapper mapper) {
        return new TemplateCoachingEngine(
                read(mapper, "coaching/hints.json", new TypeReference<Map<String, HintTemplate>>() {}),
                read(mapper, "coaching/design.json", new TypeReference<Map<String, DesignTemplate>>() {}));
    }

    private static <T> T read(ObjectMapper mapper, String path, TypeReference<T> type) {
        try (InputStream in = TemplateCoachingEngine.class.getClassLoader().getResourceAsStream(path)) {
            if (in == null) throw new IllegalStateException("missing template resource " + path);
            return mapper.readValue(in, type);
        } catch (IOException e) {
            throw new UncheckedIOException("failed to read " + path, e);
        }
    }

    public Set<String> problemSlugs() { return hints.keySet(); }
    public Set<String> questionSlugs() { return designs.keySet(); }

    /** The intro prompt that opens a design session for the given question. */
    public String designIntro(String questionSlug) {
        return template(questionSlug).intro();
    }

    // ---------------------------------------------------------------- hints

    @Override
    public HintResponse generateHint(HintRequest request) {
        HintTemplate t = hints.get(request.problemSlug());
        if (t == null) throw new IllegalArgumentException("no hint template for problem " + request.problemSlug());
        int max = t.hints().size();
        int depth = Math.min(request.hintDepthUsed() + 1, max);   // graduated: never skips, never exceeds the outline
        String hint = t.hints().get(depth - 1);
        boolean exhausted = depth == max;
        String followUp = exhausted
                ? "That's the deepest hint available — the rest is yours to write. " + t.followUp()
                : t.followUp();
        return new HintResponse(hint, depth, max, exhausted, followUp);
    }

    // -------------------------------------------------------------- grading

    @Override
    public DesignFeedback gradeDesignAnswer(DesignAnswerRequest request) {
        DesignTemplate dt = template(request.questionSlug());
        StageTemplate st = dt.stages().get(request.stage().name());
        if (st == null) throw new IllegalArgumentException("no template for stage " + request.stage() + " of " + request.questionSlug());

        String answer = request.answer() == null ? "" : request.answer().toLowerCase(Locale.ROOT);
        List<String> strengths = new ArrayList<>();
        List<String> gaps = new ArrayList<>();
        Map<RubricDimension, int[]> tally = new EnumMap<>(RubricDimension.class);   // [covered, total]

        for (Concept c : st.concepts()) {
            boolean hit = c.aliases().stream().anyMatch(a -> answer.contains(a.toLowerCase(Locale.ROOT)));
            (hit ? strengths : gaps).add(c.label());
            int[] counts = tally.computeIfAbsent(c.dimension(), k -> new int[2]);
            counts[1]++;
            if (hit) counts[0]++;
        }

        Map<RubricDimension, Integer> dims = new EnumMap<>(RubricDimension.class);
        tally.forEach((dim, counts) -> dims.put(dim, scaled(counts[0], counts[1])));
        dims.merge(RubricDimension.TRADE_OFF_REASONING, justificationBonus(answer), (base, bonus) -> Math.min(10, base + bonus));
        dims.put(RubricDimension.COMMUNICATION_CLARITY, clarityScore(request.answer()));

        int stageScore = scaled(strengths.size(), st.concepts().size());
        String followUp = gaps.isEmpty()
                ? st.followUp()
                : st.challenge().replace("{gap}", gaps.get(0));
        String narrative = "Covered %d of %d key points for the %s stage.%s".formatted(
                strengths.size(), st.concepts().size(), request.stage().name().toLowerCase(Locale.ROOT).replace('_', ' '),
                gaps.isEmpty() ? " Strong coverage — now defend the trade-offs." : " Missing: " + String.join(", ", gaps) + ".");

        return new DesignFeedback(request.stage(), stageScore, List.copyOf(strengths), List.copyOf(gaps), narrative, dims, followUp);
    }

    private DesignTemplate template(String slug) {
        DesignTemplate dt = designs.get(slug);
        if (dt == null) throw new IllegalArgumentException("no design template for question " + slug);
        return dt;
    }

    private static int scaled(int covered, int total) {
        return total == 0 ? 0 : (int) Math.round(10.0 * covered / total);
    }

    /** Small bonus when the candidate justifies choices rather than just listing components. */
    private static int justificationBonus(String lowerAnswer) {
        long n = JUSTIFICATION_WORDS.stream().filter(lowerAnswer::contains).count();
        return (int) Math.min(3, n);
    }

    /** 0-10: length (enough substance), structure (several sentences / bullets), and justification words. */
    static int clarityScore(String answer) {
        if (answer == null || answer.isBlank()) return 0;
        String lower = answer.toLowerCase(Locale.ROOT);
        int words = answer.trim().split("\\s+").length;
        int sentences = (int) SENTENCE_SPLIT.splitAsStream(answer).filter(s -> !s.isBlank()).count();
        int score = 0;
        if (words >= 15) score += 2;
        if (words >= 40) score += 2;
        if (sentences >= 3) score += 3;
        if (JUSTIFICATION_WORDS.stream().anyMatch(lower::contains)) score += 3;
        return Math.min(10, score);
    }

    /** Stages a session walks through, in order (excludes COMPLETE). */
    public static List<DesignStage> gradedStages() {
        return List.of(DesignStage.REQUIREMENTS, DesignStage.COMPONENTS, DesignStage.DATA_MODEL, DesignStage.SCALING);
    }
}
