package com.weai.server.global.security.config;

import com.weai.server.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DatabaseUserDetailsService implements UserDetailsService {

	private final UserRepository userRepository;

	@Override
	public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
		com.weai.server.domain.user.domain.User user = userRepository.findByUsername(username)
			.orElseThrow(() -> new UsernameNotFoundException(
				"User with username '%s' could not be found.".formatted(username)
			));

		return User.withUsername(user.getUsername())
			.password(user.getPassword())
			.authorities(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()))
			.build();
	}
}
