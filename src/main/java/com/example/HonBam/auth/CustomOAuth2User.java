package com.example.HonBam.auth;

import com.example.HonBam.userapi.entity.User;
import lombok.Getter;
import net.minidev.json.annotate.JsonIgnore;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.*;

@Getter
public class CustomOAuth2User implements OAuth2User {

    @JsonIgnore
    private final Map<String, Object> attributes;

    private final Collection<? extends GrantedAuthority> authorities;
    private final String nameAttributeKey;

    @JsonIgnore
    private final User user;

    private final String email;
    private final String nickname;
    private final String profileImg;

    public CustomOAuth2User(
            User user,
            String email,
            String nickname,
            String profileImg,
            Map<String, Object> attributes,
            String nameAttributeKey,
            Collection<? extends GrantedAuthority> authorities
    ) {
        this.user = Objects.requireNonNull(user, "user must not be null");
        this.email = email;
        this.nickname = nickname;
        this.profileImg = profileImg;

        this.attributes = attributes == null ? Collections.emptyMap()
                : Collections.unmodifiableMap(new HashMap<>(attributes));
        this.nameAttributeKey = Objects.requireNonNull(nameAttributeKey, "nameAttributeKey must not be null");

        // 불변으로 래핑
        this.authorities = authorities == null ? List.of()
                : List.copyOf(authorities);
    }

    @Override
    public Map<String, Object> getAttributes() {
        return attributes;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getName() {
        // 기본: 우리 시스템의 userId
        if (user != null && user.getId() != null) return user.getId();
        // 폴백(온보딩 등 특수 케이스 대비)
        Object v = attributes.get(nameAttributeKey);
        return v == null ? "" : String.valueOf(v);
    }

    // DefaultOAuth2User와 동일한 헬퍼 (사용성 ↑)
    @SuppressWarnings("unchecked")
    public <A> A getAttribute(String name) {
        return (A) attributes.get(name);
    }

    @Override
    public String toString() {
        // 민감한 정보 제거한 요약만
        return "CustomOAuth2User{userId=" + (user != null ? user.getId() : "null")
                + ", email=" + email + ", nickname=" + nickname + "}";
    }

}
