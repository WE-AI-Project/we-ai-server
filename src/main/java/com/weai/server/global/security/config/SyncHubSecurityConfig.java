package com.weai.server.global.security.config;

import com.weai.server.domain.user.repository.UserRepository;
import com.weai.server.global.security.jwt.JwtAuthenticationFilter;
import jakarta.servlet.http.HttpServletResponse;
import java.nio.charset.StandardCharsets;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.util.StringUtils;

/**
 * Security for the SyncHub Git Smart HTTP endpoint ({@code /api/synchub/**}), evaluated before the
 * main JSON API chain.
 *
 * Git clients cannot use the app's JSON login, so besides the usual {@code Authorization: Bearer}
 * access token (the desktop app passes it via {@code http.extraHeader}) this chain also accepts HTTP
 * Basic with the account email (or username) and password, and answers unauthenticated requests
 * with {@code 401 + WWW-Authenticate: Basic} so the git CLI prompts for credentials. Responses are
 * plain text, never the ApiResponse JSON envelope. Project membership/role is checked afterwards by
 * the Git servlet itself.
 */
@Configuration
@RequiredArgsConstructor
public class SyncHubSecurityConfig {

	public static final String REALM = "SyncHub";

	private final JwtAuthenticationFilter jwtAuthenticationFilter;
	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;

	@Bean
	@Order(1)
	public SecurityFilterChain syncHubSecurityFilterChain(HttpSecurity http) throws Exception {
		AuthenticationEntryPoint entryPoint = (request, response, exception) -> {
			response.setHeader(HttpHeaders.WWW_AUTHENTICATE, "Basic realm=\"" + REALM + "\", charset=\"UTF-8\"");
			writePlainText(response, HttpServletResponse.SC_UNAUTHORIZED, "SyncHub authentication required.");
		};
		AccessDeniedHandler accessDeniedHandler = (request, response, exception) ->
			writePlainText(response, HttpServletResponse.SC_FORBIDDEN, "SyncHub access denied.");

		http
			.securityMatcher("/api/synchub/**")
			.csrf(AbstractHttpConfigurer::disable)
			.formLogin(AbstractHttpConfigurer::disable)
			.logout(AbstractHttpConfigurer::disable)
			.requestCache(AbstractHttpConfigurer::disable)
			.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
			.authenticationManager(new ProviderManager(syncHubBasicAuthenticationProvider()))
			.httpBasic(basic -> basic.realmName(REALM).authenticationEntryPoint(entryPoint))
			.exceptionHandling(exception -> exception
				.authenticationEntryPoint(entryPoint)
				.accessDeniedHandler(accessDeniedHandler))
			.authorizeHttpRequests(authorize -> authorize.anyRequest().authenticated())
			.addFilterBefore(jwtAuthenticationFilter, BasicAuthenticationFilter.class);

		return http.build();
	}

	/**
	 * Basic credentials: email first (what users log in with), then username. The principal name is
	 * always the email so it matches what a Bearer JWT yields. Accounts without a local password
	 * (social login only) cannot use Basic and must go through the app's Bearer token.
	 */
	private DaoAuthenticationProvider syncHubBasicAuthenticationProvider() {
		UserDetailsService userDetailsService = login -> {
			com.weai.server.domain.user.domain.User user = userRepository.findByEmail(login)
				.or(() -> userRepository.findByUsername(login))
				.orElseThrow(() -> new UsernameNotFoundException("SyncHub user not found."));
			if (!StringUtils.hasText(user.getPassword()) || !StringUtils.hasText(user.getEmail())) {
				throw new UsernameNotFoundException("SyncHub user has no password login.");
			}
			return User.withUsername(user.getEmail())
				.password(user.getPassword())
				.authorities(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()))
				.build();
		};
		DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
		provider.setPasswordEncoder(passwordEncoder);
		return provider;
	}

	private static void writePlainText(HttpServletResponse response, int status, String message) throws java.io.IOException {
		response.setStatus(status);
		response.setContentType("text/plain;charset=UTF-8");
		response.getOutputStream().write((message + "\n").getBytes(StandardCharsets.UTF_8));
	}
}
