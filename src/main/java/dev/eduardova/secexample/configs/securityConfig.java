package dev.eduardova.secexample.configs;

import java.util.Collection;
import java.util.HashSet;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.oauth2.client.OAuth2LoginConfigurer;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.authority.mapping.GrantedAuthoritiesMapper;
import org.springframework.security.oauth2.client.oidc.web.logout.OidcClientInitiatedLogoutSuccessHandler;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.core.oidc.user.OidcUserAuthority;
import org.springframework.security.oauth2.core.user.OAuth2UserAuthority;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(jsr250Enabled = true, securedEnabled = true)
public class SecurityConfig {

    private static final String REALM_ROLES = "realm_roles";

    @Autowired
    private ClientRegistrationRepository clientRegistrationRepository;

    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE)
    public SecurityFilterChain uiFilterChain(HttpSecurity http) throws Exception {

        return http
            .authorizeHttpRequests()
            .requestMatchers("/", "/css/**", "/js/**").permitAll()
            .anyRequest().authenticated()
            .and()
            .oauth2Login(this::getConfigurer)
            .logout(logout -> {
                logout.logoutSuccessHandler(oidcLogoutSuccessHandler());
                logout.invalidateHttpSession(true);
                logout.clearAuthentication(true);
                logout.deleteCookies("JSESSIONID");
            })
            .build();
    }

    private OAuth2LoginConfigurer<HttpSecurity> getConfigurer(OAuth2LoginConfigurer<HttpSecurity> oauth2) {
        return oauth2.userInfoEndpoint(userInfo -> userInfo.userAuthoritiesMapper(userAuthoritiesMapper()));
    }

    private GrantedAuthoritiesMapper userAuthoritiesMapper() {

        return authorities -> {
            var mappedAuthorities = new HashSet<GrantedAuthority>();
            authorities.forEach(authority -> {
                if (OidcUserAuthority.class.isInstance(authority)) {
                    var oidcUserAuthority = (OidcUserAuthority) authority;
                    var userInfo = oidcUserAuthority.getUserInfo();
                    if (userInfo.hasClaim(REALM_ROLES)) {
                        var roles = userInfo.getClaimAsStringList(REALM_ROLES);
                        mappedAuthorities.addAll(generateAuthoritiesFromClaim(roles));
                    }
                }
                else if (OAuth2UserAuthority.class.isInstance(authority)) {
                    var oauth2UserAuthority = (OAuth2UserAuthority) authority;
                    var userAttributes = oauth2UserAuthority.getAttributes();
                    if (userAttributes.containsKey(REALM_ROLES)) {
                        var roles = (Collection<String>) userAttributes.get(REALM_ROLES);
                        mappedAuthorities.addAll(generateAuthoritiesFromClaim(roles));
                    }
                }
            });
            return mappedAuthorities;
        };
    }

    private Collection<GrantedAuthority> generateAuthoritiesFromClaim(Collection<String> roles) {
        return roles.stream()
            .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
            .collect(Collectors.toList());
    }

    private LogoutSuccessHandler oidcLogoutSuccessHandler() {
        var handler = new OidcClientInitiatedLogoutSuccessHandler(clientRegistrationRepository);
        handler.setPostLogoutRedirectUri("http://localhost:8086/");
        return handler;
    }
}
