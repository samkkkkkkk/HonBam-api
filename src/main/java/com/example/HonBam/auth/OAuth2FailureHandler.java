package com.example.HonBam.auth;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Component
@Profile("!test")
public class OAuth2FailureHandler implements AuthenticationFailureHandler {

    @Value("${app.oauth2.redirect.failure}")
    private String failureRedirectUrl;

    @Override
    public void onAuthenticationFailure(HttpServletRequest request,
                                        HttpServletResponse response,
                                        AuthenticationException exception) throws IOException, ServletException {
        String target = UriComponentsBuilder
                .fromUriString(failureRedirectUrl)
                .queryParam("error", exception.getMessage())
                .build().toUriString();
        response.sendRedirect(target);
    }
}
