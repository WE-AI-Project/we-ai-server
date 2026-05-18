package com.weai.server.domain.ai;

import dev.langchain4j.model.ollama.OllamaChatModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.time.Duration;
import java.util.*;

@Service
public class AiAgentService {

    private final OllamaChatModel oracle;
    private final OllamaChatModel architect;
    private final OllamaChatModel sync;

    public AiAgentService(@Value("${ollama.base-url}") String baseUrl) {
        // 타임아웃을 2분(120초)으로 넉넉하게 잡아야 토론하다 안 끊겨!
        this.oracle = OllamaChatModel.builder().baseUrl(baseUrl).modelName("SYNAIPSE_Oracle").timeout(Duration.ofSeconds(120)).build();
        this.architect = OllamaChatModel.builder().baseUrl(baseUrl).modelName("SYNAIPSE_Architect").timeout(Duration.ofSeconds(120)).build();
        this.sync = OllamaChatModel.builder().baseUrl(baseUrl).modelName("SYNAIPSE_Sync").timeout(Duration.ofSeconds(120)).build();
    }

    public Map<String, Object> runAiDebate(String userQuery) {
        List<Map<String, String>> debateLogs = new ArrayList<>();

        // 1. Architect의 기술 검토
        String architectOpinion = architect.generate("의뢰인 요청: " + userQuery + "\n이 요청에 대해 백엔드 설계 및 DB 관점에서 기술적 의견을 말해줘.");
        debateLogs.add(Map.of("agent", "Architect", "message", architectOpinion));

        // 2. Sync의 일정/기획 검토 (Architect 의견 참고)
        String syncOpinion = sync.generate("의뢰인 요청: " + userQuery + "\n아키텍트 의견: " + architectOpinion + "\n위 의견이 프로젝트 일정 내에 가능한지 검토해줘.");
        debateLogs.add(Map.of("agent", "Sync", "message", syncOpinion));

        // 3. Oracle의 최종 결론 (종합)
        String finalDecision = oracle.generate("의뢰인 요청: " + userQuery + "\n[설계 의견]: " + architectOpinion + "\n[기획 의견]: " + syncOpinion + "\n위 토론을 종합해서 의뢰인에게 최적의 최종 방안을 결정해서 보고해줘.");

        Map<String, Object> response = new HashMap<>();
        response.put("finalDecision", finalDecision);
        response.put("debateLogs", debateLogs);
        
        return response;
    }
}