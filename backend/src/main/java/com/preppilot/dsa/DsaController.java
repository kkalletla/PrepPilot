package com.preppilot.dsa;

import com.preppilot.auth.AuthenticatedUser;
import com.preppilot.dsa.DsaDtos.*;
import java.util.List;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/dsa")
public class DsaController {

    private final DsaService dsa;

    public DsaController(DsaService dsa) {
        this.dsa = dsa;
    }

    @GetMapping("/problems")
    public List<ProblemSummary> list(@AuthenticationPrincipal AuthenticatedUser user,
                                     @RequestParam(required = false) ProblemCategory category,
                                     @RequestParam(required = false) DifficultyTier difficulty) {
        return dsa.listProblems(user.id(), category, difficulty);
    }

    @GetMapping("/problems/{id}")
    public ProblemDetail get(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable Long id) {
        return dsa.getProblem(user.id(), id);
    }

    @PostMapping("/problems/{id}/attempts")
    public ProgressView attempt(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable Long id,
                                @RequestBody(required = false) AttemptRequest body) {
        return dsa.recordAttempt(user.id(), id);
    }

    @PostMapping("/problems/{id}/hints")
    public HintView hint(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable Long id,
                         @RequestBody(required = false) HintRequestBody body) {
        return dsa.requestHint(user.id(), id, body == null ? "" : body.attempt());
    }

    @PostMapping("/problems/{id}/solve")
    public SolveResult solve(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable Long id) {
        return dsa.markSolved(user.id(), id);
    }

    @GetMapping("/progress")
    public DashboardView progress(@AuthenticationPrincipal AuthenticatedUser user) {
        return dsa.dashboard(user.id());
    }
}
