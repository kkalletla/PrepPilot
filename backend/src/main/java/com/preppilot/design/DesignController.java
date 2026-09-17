package com.preppilot.design;

import com.preppilot.auth.AuthenticatedUser;
import com.preppilot.design.DesignDtos.*;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/design")
public class DesignController {

    private final DesignService design;

    public DesignController(DesignService design) {
        this.design = design;
    }

    @GetMapping("/questions")
    public List<QuestionSummary> questions(@AuthenticationPrincipal AuthenticatedUser user,
                                           @RequestParam(required = false) SeniorityLevel seniority) {
        return design.listQuestions(user.id(), seniority);
    }

    @PostMapping("/sessions")
    @ResponseStatus(HttpStatus.CREATED)
    public SessionView start(@AuthenticationPrincipal AuthenticatedUser user, @Valid @RequestBody StartSessionRequest req) {
        return design.startSession(user.id(), req.questionId());
    }

    @GetMapping("/sessions")
    public List<SessionView> list(@AuthenticationPrincipal AuthenticatedUser user) {
        return design.listSessions(user.id());
    }

    @GetMapping("/sessions/{id}")
    public SessionView get(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable Long id) {
        return design.getSession(user.id(), id);
    }

    @PostMapping("/sessions/{id}/answers")
    public AnswerResult answer(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable Long id,
                               @Valid @RequestBody AnswerRequest req) {
        return design.answer(user.id(), id, req.answer());
    }
}
