package com.weai.server.domain.ai;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/ai")
@RequiredArgsConstructor
public class AiController {

    private final AiAgentService aiAgentService;

    @PostMapping("/debate")
    public Map<String, Object> debate(@RequestBody Map<String, String> body) {
        return aiAgentService.runAiDebate(body.get("query"));
    }
}