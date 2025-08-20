package org.bobj.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.bobj.order.consumer.OrderQueueConsumer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
@PropertySource(
    value = {
        "classpath:/application.properties",
        "file:/usr/local/tomcat/conf/application.env"
    },
    ignoreResourceNotFound = true   // 파일 없어도 무시
)
public class RedisConfig {

    @Value("${spring.redis.host}")
    private String host;

    @Value("${spring.redis.port}")
    private int port;

    // Pub/Sub 채널 이름
    private static final String ORDER_EVENT_CHANNEL = "order:events";

//    @Bean
//    public static PropertySourcesPlaceholderConfigurer pspc() {
//        PropertySourcesPlaceholderConfigurer p = new PropertySourcesPlaceholderConfigurer();
//        p.setIgnoreResourceNotFound(true);
//        p.setIgnoreUnresolvablePlaceholders(true);
//        return p;
//    }


    @Bean
    public RedisConnectionFactory redisConnectionFactory() {
        // redis 연결 정보(host, port, password)
        RedisStandaloneConfiguration redisStandaloneConfiguration = new RedisStandaloneConfiguration(
                host, port);
        return new LettuceConnectionFactory(redisStandaloneConfiguration);
    }

    @Bean
    public RedisTemplate<String, Object> redisTemplate() {
        RedisTemplate<String, Object> template = new RedisTemplate<>();

        // 데이터를 받아 올 RedisConnection 설정
        template.setConnectionFactory(redisConnectionFactory());

        // JSON 직렬화를 위한 Serializer (ObjectMapper 사용)
        GenericJackson2JsonRedisSerializer jsonRedisSerializer = new GenericJackson2JsonRedisSerializer(objectMapper());

        // Key는 String, Value는 JSON으로 직렬화/역직렬화 설정
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(jsonRedisSerializer);

        // Hash Key도 String, Hash Value도 JSON으로 직렬화/역직렬화 설정
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(jsonRedisSerializer);

        template.afterPropertiesSet();

        return template;
    }

    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        return objectMapper;
    }

    @Bean
    public StringRedisTemplate stringRedisTemplate(RedisConnectionFactory redisConnectionFactory) {
        return new StringRedisTemplate(redisConnectionFactory);
    }


    @Bean
    public MessageListenerAdapter listenerAdapter(OrderQueueConsumer consumer) {
        return new MessageListenerAdapter(consumer, "onMessage");
    }

    @Bean
    public RedisMessageListenerContainer container(RedisConnectionFactory connectionFactory,
                                                   MessageListenerAdapter listenerAdapter) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        // "order:events" 채널을 구독하도록 설정
        container.addMessageListener(listenerAdapter, new ChannelTopic(ORDER_EVENT_CHANNEL));
        return container;
    }

}
