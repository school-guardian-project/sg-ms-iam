package com.school_guardian.ms_iam.infrastructure.config;

import com.school_guardian.ms_iam.domain.port.out.TokenDenyList;
import com.school_guardian.ms_iam.domain.port.out.TokenProvider;
import com.school_guardian.ms_iam.infrastructure.security.InMemoryTokenDenyList;
import com.school_guardian.ms_iam.infrastructure.security.JwtTokenProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AuthConfig {

    @Bean
    public TokenProvider tokenProvider(
        @org.springframework.beans.factory.annotation.Value("${app.jwt.secret}") String secret,
        @org.springframework.beans.factory.annotation.Value("${app.jwt.access-token-expiration-minutes:15}") long accessExp,
        @org.springframework.beans.factory.annotation.Value("${app.jwt.refresh-token-expiration-days:30}") long refreshExp
    ) {
        return new JwtTokenProvider(secret, accessExp, refreshExp);
    }

    @Bean
    public TokenDenyList tokenDenyList() {
        return new InMemoryTokenDenyList();
    }
}