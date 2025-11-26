package com.example.HonBam.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "auth")
@Getter
@Setter
public class AuthProperties {

    private final Token token = new Token();
    private final Cookie cookie = new Cookie();

    @Getter
    @Setter
    public static class Token {
        private long accessExpireMinutes;
        private long refreshExpireDays;

        public Duration getAccessExpireDuration() {
            return Duration.ofMinutes(accessExpireMinutes);
        }

        public Duration getRefreshExpireDuration() {
            return Duration.ofDays(refreshExpireDays);
        }
    }

    @Getter
    public static class Cookie {
        private final CookieSetting access = new CookieSetting();
        private final CookieSetting refresh = new CookieSetting();
    }


    @Getter
    @Setter
    public static class CookieSetting {
        private Integer maxAgeMinutes;
        private Integer maxAgeDays;
        private boolean httpOnly;
        private boolean secure;
        private String sameSite;
        private String path;
        private String domain;
    }
}
