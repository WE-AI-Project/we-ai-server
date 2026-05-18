package com.weai.server.domain.ai;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/ai")
@RequiredArgsConstructor
public class AiController {

    private final AiAgentService aiAgentService;

    @PostMapping("/debate")
    public AiDebateResponse chatWithAgents(@RequestBody AiRequest request) {
        // 인공지능끼리 대화해서 결론을 가져옴
        return aiAgentService.getBestSolution(request.getQuery());
    }
}