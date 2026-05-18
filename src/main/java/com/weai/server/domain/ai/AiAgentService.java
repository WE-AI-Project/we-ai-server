package com.weai.server.domain.ai;

import dev.langchain4j.model.ollama.OllamaChatModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.List;

@Service
public class AiAgentService {

    private final OllamaChatModel oracleModel;
    private final OllamaChatModel architectModel;
    private final OllamaChatModel syncModel;

    public AiAgentService(@Value("${ollama.base-url}") String baseUrl) {
        // 각 페르소나별 모델 연결
        this.oracleModel = OllamaChatModel.builder().baseUrl(baseUrl).modelName("SYNAIPSE_Oracle").build();
        this.architectModel = OllamaChatModel.builder().baseUrl(baseUrl).modelName("SYNAIPSE_Architect").build();
        this.syncModel = OllamaChatModel.builder().baseUrl(baseUrl).modelName("SYNAIPSE_Sync").build();
    }

    public AiDebateResponse getBestSolution(String userQuery) {
        List<String> debateLogs = new ArrayList<>();

        // 1. Architect AI의 기술적 검토
        String architectOpinion = architectModel.generate("사용자 요청: " + userQuery + "\n위 요청에 대해 백엔드 설계 관점에서 기술적 의견을 짧고 굵게 말해줘.");
        debateLogs.add("[Architect]: " + architectOpinion);

        // 2. Sync AI의 일정 및 기획 검토 (Architect의 의견을 참고해서 반박하거나 보완)
        String syncOpinion = syncModel.generate("사용자 요청: " + userQuery + "\nArchitect의 의견: " + architectOpinion + "\n위 의견이 프로젝트 일정(WBS)에 무리가 없을지 검토해줘.");
        debateLogs.add("[Sync]: " + syncOpinion);

        // 3. Oracle AI가 두 의견을 듣고 최종 결론 도출
        String finalDecision = oracleModel.generate("사용자 요청: " + userQuery + "\nArchitect 의견: " + architectOpinion + "\nSync 의견: " + syncOpinion + "\n팀원들의 토론을 종합해서 병권 팀장님께 드릴 최적의 최종 방안을 결정해줘.");
        
        return new AiDebateResponse(finalDecision, debateLogs);
    }
}