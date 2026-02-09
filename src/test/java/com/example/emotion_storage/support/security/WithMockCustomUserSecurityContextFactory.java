package com.example.emotion_storage.support.security;

import com.example.emotion_storage.global.security.principal.CustomUserPrincipal;
import java.util.Arrays;
import java.util.List;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithSecurityContextFactory;

public class WithMockCustomUserSecurityContextFactory implements WithSecurityContextFactory<WithMockCustomUser> {

    @Override
    public SecurityContext createSecurityContext(WithMockCustomUser annotation) {
        SecurityContext context = SecurityContextHolder.createEmptyContext();

        List<SimpleGrantedAuthority> authorities = Arrays.stream(annotation.roles())
                .map(r -> r.startsWith("ROLE_") ? r :"ROLE_" + r)
                .map(SimpleGrantedAuthority::new)
                .toList();

        CustomUserPrincipal principal = new CustomUserPrincipal(annotation.id(), annotation.email(), authorities);

        Authentication authentication = new UsernamePasswordAuthenticationToken(principal, null, authorities);
        context.setAuthentication(authentication);
        return context;
    }
}
