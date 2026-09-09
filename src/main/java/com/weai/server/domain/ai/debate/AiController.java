package com.weai.server.domain.ai.debate;

import com.weai.server.domain.project.service.ProjectService;
import com.weai.server.domain.user.domain.User;
import com.weai.server.domain.user.service.UserService;
import com.weai.server.global.dto.ApiResponse;
import com.weai.server.global.error.ErrorCode;
import com.weai.server.global.exception.ApiException;
import com.weai.server.global.swagger.SwaggerErrorResponses;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.Arrays;
import java.util.List;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "AI Debate", description = "Dynamic four-agent debate API powered by Ollama qwen2.5-coder and llama3.1.")
@SecurityRequirement(name = "bearerAuth")
@CrossOrigin
@RestController
@RequestMapping("/api/v1/ai")
public class AiController {

	private final AiDebateService aiDebateService;
	private final AgentMetricsService agentMetricsService;
	private final UserService userService;
	private final ProjectService projectService;

	public AiController(
		@Lazy AiDebateService aiDebateService,
		AgentMetricsService agentMetricsService,
		UserService userService,
		ProjectService projectService
	) {
		this.aiDebateService = aiDebateService;
		this.agentMetricsService = agentMetricsService;
		this.userService = userService;
		this.projectService = projectService;
	}

	@Operation(
		summary = "List selectable AI agents",
		description = "Returns the agent keys that can be used by custom debate and single-agent question APIs."
	)
	@GetMapping("/agents")
	public ApiResponse<List<AiAgentResponse>> agents() {
		return ApiResponse.success(
			"AI_AGENTS_SUCCESS",
			"Selectable AI agents loaded successfully.",
			Arrays.stream(AiAgentType.values())
				.map(AiAgentResponse::from)
				.toList()
		);
	}

	@Operation(
		summary = "Get real agent invocation metrics",
		description = "Returns each agent's actual invocation count, success/failure count, average latency, and last-invoked time, "
			+ "recorded from real debate/single-agent calls. Agents are LLM personas, not OS processes, so CPU/memory/uptime are not applicable."
	)
	@GetMapping("/agents/metrics")
	public ApiResponse<List<AgentMetricsResponse>> agentMetrics() {
		return ApiResponse.success(
			"AI_AGENT_METRICS_SUCCESS",
			"Agent invocation metrics loaded successfully.",
			agentMetricsService.getMetrics()
		);
	}

	@Operation(
		summary = "Get an agent's recent invocation history",
		description = "Returns the most recent real invocation records (project, success, latency, error) for one agent."
	)
	@GetMapping("/agents/{agent}/invocations")
	public ApiResponse<List<AgentInvocationResponse>> agentInvocations(
		@Parameter(description = "Agent key: ORACLE, BACKEND, FRONTEND, or INSPECTOR")
		@PathVariable AiAgentType agent,
		@Parameter(description = "Maximum number of records to return (1-100)")
		@RequestParam(defaultValue = "20") int limit
	) {
		return ApiResponse.success(
			"AI_AGENT_INVOCATIONS_SUCCESS",
			"Agent invocation history loaded successfully.",
			agentMetricsService.getRecentInvocations(agent, limit)
		);
	}

	@Operation(
		summary = "Run dynamic turn-based four-agent debate",
		description = "Oracle, Backend, Frontend, and Inspector debate over project-isolated RAG context using qwen2.5-coder and llama3.1. Inspector can end the loop with [토론 종료]."
	)
	@SwaggerErrorResponses({ErrorCode.INVALID_INPUT, ErrorCode.UNAUTHORIZED, ErrorCode.PROJECT_ACCESS_DENIED, ErrorCode.INTERNAL_SERVER_ERROR})
	@PostMapping("/debate")
	public ApiResponse<DebateResponse> debate(
		Authentication authentication,
		@Valid @RequestBody EditorContextDto request
	) {
		User user = authenticatedUser(authentication);
		projectService.validateProjectAccess(request.projectId(), user.getId());

		return ApiResponse.success(
			"AI_DEBATE_SUCCESS",
			"AI debate completed successfully.",
			aiDebateService.debate(user, request.projectId(), request)
		);
	}

	@Operation(
		summary = "Run custom N-agent debate",
		description = "Runs a RAG-grounded debate with the selected subset of ORACLE, BACKEND, FRONTEND, and INSPECTOR. Agents are executed in request order, duplicate entries are ignored, and Inspector can end the loop with [?좊줎 醫낅즺]."
	)
	@SwaggerErrorResponses({ErrorCode.INVALID_INPUT, ErrorCode.UNAUTHORIZED, ErrorCode.PROJECT_ACCESS_DENIED, ErrorCode.INTERNAL_SERVER_ERROR})
	@PostMapping("/debate/custom")
	public ApiResponse<DebateResponse> customDebate(
		Authentication authentication,
		@Valid @RequestBody CustomDebateRequest request
	) {
		User user = authenticatedUser(authentication);
		projectService.validateProjectAccess(request.context().projectId(), user.getId());

		return ApiResponse.success(
			"AI_CUSTOM_DEBATE_SUCCESS",
			"Custom AI debate completed successfully.",
			aiDebateService.debate(
				user,
				request.context().projectId(),
				request.context(),
				request.agents(),
				request.maxRounds()
			)
		);
	}

	@Operation(
		summary = "Ask one selected AI agent",
		description = "Asks exactly one selected agent a RAG-grounded question using the VS Code editor context."
	)
	@SwaggerErrorResponses({ErrorCode.INVALID_INPUT, ErrorCode.UNAUTHORIZED, ErrorCode.PROJECT_ACCESS_DENIED, ErrorCode.INTERNAL_SERVER_ERROR})
	@PostMapping("/agents/{agent}/ask")
	public ApiResponse<SingleAgentResponse> askAgent(
		Authentication authentication,
		@Parameter(description = "Agent key: ORACLE, BACKEND, FRONTEND, or INSPECTOR")
		@PathVariable AiAgentType agent,
		@Valid @RequestBody EditorContextDto request
	) {
		User user = authenticatedUser(authentication);
		projectService.validateProjectAccess(request.projectId(), user.getId());

		return ApiResponse.success(
			"AI_AGENT_ASK_SUCCESS",
			"AI agent answer completed successfully.",
			aiDebateService.askAgent(user, request.projectId(), agent, request)
		);
	}

	private User authenticatedUser(Authentication authentication) {
		if (authentication == null || !authentication.isAuthenticated()) {
			throw new ApiException(ErrorCode.UNAUTHORIZED);
		}
		return userService.getUserEntityByEmail(authentication.getName());
	}
}
