package com.example.HonBam.config;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
@RequiredArgsConstructor
public class RedisConfig {

    @Value("${spring.data.redis.host}")
    private String host;

    @Value("${spring.data.redis.port}")
    private int port;

    @Value("${spring.data.redis.timeout}")
    private String timeout;


    // Redis 연결을 위한 LettuceConnectionFactory 빈 등록
    @Bean
    public LettuceConnectionFactory redisConnectionFactory() {
        return new LettuceConnectionFactory(host, port);
    }

    // Redis 템플릿 설정 (JSON 형식으로 직렬화)
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new GenericJackson2JsonRedisSerializer());
        return template;
    }

    // MessageListener를 직접 구현하여 RedisMessageListenerContainer에 추가하는 방식
    // 직관적이며 불필요한 설정을 줄일 수 있음.
//    @Bean
//    public RedisMessageListenerContainer redisMessageListenerContainer() {
//        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
//        container.setConnectionFactory(redisConnectionFactory);
//        container.addMessageListener(redisSubscriber, new PatternTopic("chat"));
//        return container;
//    }

//    // Redis의 pub/sub 메시지 리스너 컨테이너 설정 (RabbitMQ 사용으로 주석 처리)
//    // MessageListenerAdapter를 사용하여 RedisSubscriber의 OnMessage를 구현하는 방식
//    @Bean
//    public RedisMessageListenerContainer redisContainer(RedisConnectionFactory connectionFactory, MessageListenerAdapter listenerAdapter) {
//        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
//        container.setConnectionFactory(connectionFactory);
//        container.addMessageListener(listenerAdapter, new PatternTopic("chat")); // "chat" 토픽 구독
//        return container;
//    }
//
//    // Redis 메시지를 받을 리스너 설정 (RabbitMQ 사용으로 주석 처리)
//    @Bean
//    public MessageListenerAdapter listenerAdapter(RedisSubscriber subscriber) {
//        return new MessageListenerAdapter(subscriber, "onMessage");
//    }

}
