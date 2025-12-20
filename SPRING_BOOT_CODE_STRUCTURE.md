# Spring Boot + React Full-Stack Code Pattern

## Project Structure

### Backend (Spring Boot)
```
src/main/java/com/example/demo/
├── config/              # Configuration classes
├── controller/          # REST Controllers
├── dto/                 # Data Transfer Objects
│   ├── request/        # Request DTOs
│   ├── response/       # Response DTOs
│   └── internal/       # Internal DTOs (Kafka/RabbitMQ)
├── entities/           # JPA Entities (PostgreSQL)
├── document/           # MongoDB Documents
├── exception/          # Exception handling
├── mapper/             # MapStruct Mappers
├── repository/         # Data access layer
│   ├── jpa/           # JPA Repositories
│   └── mongo/         # MongoDB Repositories
├── service/            # Business logic layer
├── filter/             # Custom filters
├── annotation/         # Custom annotations
├── util/               # Utility classes
├── enums/              # Enums
└── consumer/           # Message consumers

src/main/resources/
├── application.yml
├── db/
│   └── changelog/      # Liquibase migrations
└── mongo/              # MongoDB seed data
```

### Frontend (React + TypeScript)
```
src/
├── api/                # API clients
├── components/         # React components
├── pages/              # Page components
├── hooks/              # Custom hooks
│   └── queries/       # React Query hooks
├── stores/             # Zustand stores
├── types/              # TypeScript types
│   └── dto/           # DTO types
├── services/           # Services (WebSocket, etc.)
├── providers/          # Context providers
├── routes/             # Route definitions
└── lib/                # Utilities
```

---

## Backend Patterns

### 1. Entities (JPA - PostgreSQL)

```java
package com.example.demo.entities;

import com.example.demo.enums.EntityStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;
import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "email", nullable = false)
    private String email;
    
    @Column(name = "password_hash", nullable = false)
    private String passwordHash;
    
    @ColumnDefault("false")
    @Column(name = "is_premium", nullable = false)
    private Boolean isPremium = false;
    
    @ColumnDefault("now()")
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    
    @ColumnDefault("now()")
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
    
    @Enumerated(EnumType.STRING)
    @ColumnDefault("'ACTIVE'")
    @Column(name = "status", nullable = false, length = 20)
    private EntityStatus status = EntityStatus.ACTIVE;
    
    @Column(name = "deleted_at")
    private Instant deletedAt;
    
    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL)
    private Profile profile;
}
```

**Key Points:**
- Use `@Getter`, `@Setter` from Lombok
- Column naming: snake_case with `@Column(name = "...")`
- Timestamps: `Instant` type
- Soft delete: `status` + `deletedAt`
- Relationships: `@OneToOne`, `@OneToMany`, `@ManyToOne`

---

### 2. Documents (MongoDB)

```java
package com.example.demo.document;

import com.example.demo.enums.EntityStatus;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.index.Indexed;
import java.time.Instant;
import java.util.List;

@Data
@Document(collection = "posts")
public class Post {
    @Id
    private String id;
    
    private String slug;
    
    @Indexed
    private Integer userId;
    
    private String content;
    private List<MediaItem> media;
    private Instant createdAt;
    private Instant updatedAt;
    private Integer likes;
    private Integer commentsCount;
    private EntityStatus status = EntityStatus.ACTIVE;
    private Instant deletedAt;
    
    @Data
    public static class MediaItem {
        private String type;
        private String url;
    }
}
```

**Key Points:**
- Use `@Data` from Lombok
- `@Document(collection = "...")`
- `@Indexed` for frequently queried fields
- Nested classes for embedded documents
- Soft delete pattern same as JPA

---

### 3. DTOs (Request/Response)

```java
// Request DTO
package com.example.demo.dto.request;

import lombok.Data;
import jakarta.validation.constraints.*;

@Data
public class CreatePostRequest {
    @NotBlank(message = "Content cannot be blank")
    @Size(max = 10000, message = "Content too long")
    private String content;
    
    private List<MediaItemRequest> media;
}

// Response DTO
package com.example.demo.dto.response;

import lombok.Data;
import java.time.Instant;
import java.util.List;

@Data
public class PostResponse {
    private String id;
    private String slug;
    private Integer userId;
    private String content;
    private List<MediaItemResponse> media;
    private Instant createdAt;
    private Instant updatedAt;
    private Integer likes;
    private Integer commentsCount;
}
```

**Key Points:**
- Separate request/response DTOs
- Use validation annotations on request DTOs
- No validation on response DTOs
- Use Lombok `@Data`

---

### 4. ApiResponse Wrapper

```java
package com.example.demo.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.Setter;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Getter
@Setter
public class ApiResponse<T> {
    private int code = 1000;
    private String message;
    private T result;

    public static <T> ApiResponse<T> success(T result, String message) {
        ApiResponse<T> response = new ApiResponse<>();
        response.setCode(1000);
        response.setMessage(message);
        response.setResult(result);
        return response;
    }

    public static <T> ApiResponse<T> success(String message) {
        ApiResponse<T> response = new ApiResponse<>();
        response.setCode(1000);
        response.setMessage(message);
        return response;
    }
}
```

**Usage in Controller:**
```java
return ApiResponse.success(postService.getAllPosts(), "Posts retrieved successfully");
```

---

### 5. Exception Handling

```java
// ErrorCode Enum
package com.example.demo.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;

@Getter
public enum ErrorCode {
    UNEXPECTED_EXCEPTION(9999, "Undefined exception", HttpStatus.INTERNAL_SERVER_ERROR),
    USER_NOT_FOUND(1001, "User not found", HttpStatus.NOT_FOUND),
    POST_NOT_FOUND(2001, "Post not found", HttpStatus.NOT_FOUND),
    INVALID_CREDENTIALS(1004, "Invalid email or password", HttpStatus.UNAUTHORIZED),
    PREMIUM_REQUIRED(1008, "Premium subscription required", HttpStatus.FORBIDDEN);
    
    private int code;
    private String message;
    private HttpStatusCode statusCode;

    ErrorCode(int code, String message, HttpStatusCode statusCode) {
        this.code = code;
        this.message = message;
        this.statusCode = statusCode;
    }
}

// AppException
package com.example.demo.exception;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AppException extends RuntimeException {
    private ErrorCode errorCode;
    
    public AppException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }
}

// GlobalExceptionHandler
package com.example.demo.exception;

import com.example.demo.dto.response.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(AppException.class)
    public ResponseEntity<ApiResponse<Void>> handleAppException(AppException ex) {
        ApiResponse<Void> response = new ApiResponse<>();
        response.setCode(ex.getErrorCode().getCode());
        response.setMessage(ex.getErrorCode().getMessage());
        return ResponseEntity
                .status(ex.getErrorCode().getStatusCode())
                .body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGenericException(Exception ex) {
        ApiResponse<Void> response = new ApiResponse<>();
        response.setCode(ErrorCode.UNEXPECTED_EXCEPTION.getCode());
        response.setMessage(ErrorCode.UNEXPECTED_EXCEPTION.getMessage());
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(response);
    }
}
```

**Usage:**
```java
throw new AppException(ErrorCode.USER_NOT_FOUND);
```

---

### 6. MapStruct Mappers

```java
package com.example.demo.mapper;

import com.example.demo.document.Post;
import com.example.demo.dto.request.CreatePostRequest;
import com.example.demo.dto.request.UpdatePostRequest;
import com.example.demo.dto.response.PostResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import java.util.List;

@Mapper(componentModel = "spring")
public interface PostMapper {
    PostResponse toResponse(Post post);
    
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Post toDocument(CreatePostRequest request);
    
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "userId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    void updateDocumentFromRequest(UpdatePostRequest request, @MappingTarget Post post);
    
    List<PostResponse> toResponseList(List<Post> posts);
}
```

**Key Points:**
- `componentModel = "spring"` for Spring injection
- `@Mapping(target = "...", ignore = true)` for fields not in request
- `@MappingTarget` for update operations
- List mapping methods

---

### 7. Service Layer

```java
package com.example.demo.service;

import com.example.demo.dto.request.CreatePostRequest;
import com.example.demo.dto.response.PostResponse;
import com.example.demo.exception.AppException;
import com.example.demo.exception.ErrorCode;
import com.example.demo.mapper.PostMapper;
import com.example.demo.repository.mongo.PostRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class PostService {
    
    private final PostRepository postRepository;
    private final PostMapper postMapper;
    
    public List<PostResponse> getAllPosts() {
        List<Post> posts = postRepository.findByStatusOrderByCreatedAtDesc(EntityStatus.ACTIVE);
        return postMapper.toResponseList(posts);
    }
    
    public PostResponse getPostById(String id) {
        Post post = postRepository.findByIdAndStatus(id, EntityStatus.ACTIVE)
                .orElseThrow(() -> new AppException(ErrorCode.POST_NOT_FOUND));
        return postMapper.toResponse(post);
    }
    
    @Transactional
    public PostResponse createPost(CreatePostRequest request) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        
        Post post = postMapper.toDocument(request);
        post.setUserId(currentUserId.intValue());
        post.setCreatedAt(Instant.now());
        post.setUpdatedAt(Instant.now());
        post.setStatus(EntityStatus.ACTIVE);
        
        Post savedPost = postRepository.save(post);
        return postMapper.toResponse(savedPost);
    }
}
```

**Key Points:**
- `@RequiredArgsConstructor` for constructor injection
- `@Slf4j` for logging
- `@Transactional` for write operations
- Use mapper for conversions
- Extract userId from SecurityContext
- Set timestamps and status manually

---

### 8. Controller Layer

```java
package com.example.demo.controller;

import com.example.demo.dto.request.CreatePostRequest;
import com.example.demo.dto.response.ApiResponse;
import com.example.demo.dto.response.PostResponse;
import com.example.demo.service.PostService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/posts")
@RequiredArgsConstructor
public class PostController {
    
    private final PostService postService;
    
    @GetMapping
    public ApiResponse<List<PostResponse>> getAllPosts() {
        return ApiResponse.success(postService.getAllPosts(), "Posts retrieved successfully");
    }
    
    @GetMapping("/{id}")
    public ApiResponse<PostResponse> getPost(@PathVariable String id) {
        return ApiResponse.success(postService.getPostById(id), "Post retrieved successfully");
    }
    
    @PostMapping
    public ApiResponse<PostResponse> createPost(@RequestBody CreatePostRequest request) {
        return ApiResponse.success(postService.createPost(request), "Post created successfully");
    }
    
    @PutMapping("/{id}")
    public ApiResponse<PostResponse> updatePost(
            @PathVariable String id,
            @RequestBody UpdatePostRequest request) {
        return ApiResponse.success(postService.updatePost(id, request), "Post updated successfully");
    }
    
    @DeleteMapping("/{id}")
    public ApiResponse<Void> deletePost(@PathVariable String id) {
        postService.deletePost(id);
        return ApiResponse.success("Post deleted successfully");
    }
}
```

**Key Points:**
- `@RestController` + `@RequestMapping`
- `@RequiredArgsConstructor` for service injection
- Return `ApiResponse<T>` wrapper
- Use `@PathVariable`, `@RequestBody`, `@RequestParam`

---
 
###
 9. JWT Authentication with JOSE (Nimbus)

```java
// JwtService
package com.example.demo.service;

import com.example.demo.entities.User;
import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.util.Date;

@Service
public class JwtService {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.access-token-expiration}")
    private Long accessTokenExpiration;

    public String generateAccessToken(User user) {
        try {
            Date now = new Date();
            Date expiryDate = new Date(now.getTime() + accessTokenExpiration);

            JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
                    .subject(user.getId().toString())
                    .claim("email", user.getEmail())
                    .claim("premium", user.getIsPremium())
                    .issueTime(now)
                    .expirationTime(expiryDate)
                    .build();

            SignedJWT signedJWT = new SignedJWT(
                    new JWSHeader(JWSAlgorithm.HS256),
                    claimsSet
            );

            JWSSigner signer = new MACSigner(secret.getBytes());
            signedJWT.sign(signer);

            return signedJWT.serialize();
        } catch (JOSEException e) {
            throw new RuntimeException("Error generating access token", e);
        }
    }

    public JWTClaimsSet validateToken(String token) {
        try {
            SignedJWT signedJWT = SignedJWT.parse(token);
            JWSVerifier verifier = new MACVerifier(secret.getBytes());
            
            if (!signedJWT.verify(verifier)) {
                throw new RuntimeException("Invalid token signature");
            }

            JWTClaimsSet claimsSet = signedJWT.getJWTClaimsSet();
            
            if (claimsSet.getExpirationTime().before(new Date())) {
                throw new RuntimeException("Token has expired");
            }

            return claimsSet;
        } catch (Exception e) {
            throw new RuntimeException("Error validating token", e);
        }
    }
}
```

---

### 10. Spring Security Configuration

```java
package com.example.demo.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.util.List;

@Configuration
public class SecurityConfig {

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers("/ws/**").permitAll()
                        .anyRequest().authenticated()
                )
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.decoder(jwtDecoder()))
                );

        return http.build();
    }

    @Bean
    public JwtDecoder jwtDecoder() {
        SecretKey secretKey = new SecretKeySpec(jwtSecret.getBytes(), "HmacSHA256");
        return NimbusJwtDecoder.withSecretKey(secretKey)
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of("http://localhost:5000"));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }
}
```

---

### 11. Custom Annotation for Premium Authorization

```java
// Custom Annotation
package com.example.demo.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface RequiresPremium {
}

// Filter
package com.example.demo.filter;

import com.example.demo.annotation.RequiresPremium;
import com.example.demo.exception.AppException;
import com.example.demo.exception.ErrorCode;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

@Component
public class PremiumAuthorizationFilter extends OncePerRequestFilter {

    private final RequestMappingHandlerMapping handlerMapping;

    public PremiumAuthorizationFilter(RequestMappingHandlerMapping handlerMapping) {
        this.handlerMapping = handlerMapping;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) {

        HandlerMethod handlerMethod = getHandlerMethod(request);
        
        if (handlerMethod != null) {
            boolean requiresPremium = handlerMethod.hasMethodAnnotation(RequiresPremium.class) ||
                                    handlerMethod.getBeanType().isAnnotationPresent(RequiresPremium.class);
            
            if (requiresPremium) {
                Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
                
                if (authentication != null && authentication.getPrincipal() instanceof Jwt jwt) {
                    Boolean isPremium = jwt.getClaim("premium");
                    if (isPremium == null || !isPremium) {
                        throw new AppException(ErrorCode.PREMIUM_REQUIRED);
                    }
                }
            }
        }

        filterChain.doFilter(request, response);
    }
}
```

**Usage:**
```java
@GetMapping("/premium-content")
@RequiresPremium
public ApiResponse<String> getPremiumContent() {
    return ApiResponse.success("Premium content");
}
```

---

### 12. Database Migrations

#### Liquibase (PostgreSQL)

```yaml
# db/changelog/db.changelog-master.yml
databaseChangeLog:
  - include:
      file: init-data-changelog.sql
      relativeToChangelogFile: true
  - include:
      file: add-user-id-to-profile.sql
      relativeToChangelogFile: true
```

```sql
-- db/changelog/init-data-changelog.sql
--liquibase formatted sql

--changeset author:1
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    is_premium BOOLEAN DEFAULT false NOT NULL,
    created_at TIMESTAMP DEFAULT now() NOT NULL,
    updated_at TIMESTAMP DEFAULT now() NOT NULL,
    status VARCHAR(20) DEFAULT 'ACTIVE' NOT NULL,
    deleted_at TIMESTAMP
);

--changeset author:2
CREATE TABLE profiles (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT UNIQUE REFERENCES users(id),
    full_name VARCHAR(255),
    avatar_url TEXT,
    bio TEXT,
    birthday DATE,
    updated_at TIMESTAMP DEFAULT now(),
    status VARCHAR(20) DEFAULT 'ACTIVE' NOT NULL,
    deleted_at TIMESTAMP
);
```

#### Mongock (MongoDB)

```java
package com.example.demo.config.migration;

import com.example.demo.document.Conversation;
import io.mongock.api.annotations.ChangeUnit;
import io.mongock.api.annotations.Execution;
import io.mongock.api.annotations.RollbackExecution;
import org.springframework.data.mongodb.core.MongoTemplate;

@ChangeUnit(id = "create-chat-collections", order = "001", author = "system")
public class DatabaseChangeLog001_CreateChatCollections {

    @Execution
    public void createCollections(MongoTemplate mongoTemplate) {
        if (!mongoTemplate.collectionExists("conversations")) {
            mongoTemplate.createCollection("conversations");
        }
        if (!mongoTemplate.collectionExists("messages")) {
            mongoTemplate.createCollection("messages");
        }
    }

    @RollbackExecution
    public void rollback(MongoTemplate mongoTemplate) {
        mongoTemplate.dropCollection("conversations");
        mongoTemplate.dropCollection("messages");
    }
}
```

**application.yml:**
```yaml
mongock:
  migration-scan-package:
    - com.example.demo.config.migration
  throw-exception-if-cannot-obtain-lock: true
  transaction-enabled: false
```

---

### 13. WebSocket Configuration

```java
package com.example.demo.config;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.*;

@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Value("${spring.websocket.allowed-origins}")
    private String allowedOrigins;
    
    private final WebSocketAuthInterceptor webSocketAuthInterceptor;

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.enableSimpleBroker("/topic", "/queue");
        config.setApplicationDestinationPrefixes("/app");
        config.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOrigins(allowedOrigins)
                .withSockJS();
    }
    
    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(webSocketAuthInterceptor);
    }
}
```

**WebSocket Controller:**
```java
@Controller
@RequiredArgsConstructor
public class ChatWebSocketController {
    
    private final SimpMessagingTemplate messagingTemplate;
    
    @MessageMapping("/chat.typing")
    public void handleTyping(TypingIndicator indicator, Principal principal) {
        messagingTemplate.convertAndSend(
            "/topic/conversation." + indicator.getConversationId(),
            indicator
        );
    }
}
```

---

### 14. RabbitMQ Configuration

```java
package com.example.demo.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String CHAT_EXCHANGE = "chat.exchange";
    public static final String CHAT_INPUT_QUEUE = "chat.input.queue";
    public static final String CHAT_OUTPUT_QUEUE = "chat.output.queue";
    public static final String CHAT_INPUT_ROUTING_KEY = "chat.input";
    public static final String CHAT_OUTPUT_ROUTING_KEY = "chat.output";

    @Bean
    public TopicExchange chatExchange() {
        return new TopicExchange(CHAT_EXCHANGE, true, false);
    }

    @Bean
    public Queue chatInputQueue() {
        return new Queue(CHAT_INPUT_QUEUE, true);
    }

    @Bean
    public Binding chatInputBinding(Queue chatInputQueue, TopicExchange chatExchange) {
        return BindingBuilder.bind(chatInputQueue)
                .to(chatExchange)
                .with(CHAT_INPUT_ROUTING_KEY);
    }

    @Bean
    public Jackson2JsonMessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(messageConverter());
        return template;
    }
}
```

**Consumer:**
```java
@Component
@RequiredArgsConstructor
@Slf4j
public class ChatRabbitMQConsumer {
    
    private final ChatService chatService;
    private final SimpMessagingTemplate messagingTemplate;

    @RabbitListener(queues = RabbitMQConfig.CHAT_INPUT_QUEUE)
    public void consumeMessage(ChatMessage chatMessage) {
        log.info("Consuming message from RabbitMQ: {}", chatMessage);
        
        chatService.processMessage(chatMessage);
    }
    
    @RabbitListener(queues = RabbitMQConfig.CHAT_OUTPUT_QUEUE)
    public void deliverMessage(ChatMessage chatMessage) {
        log.info("Delivering message via WebSocket: {}", chatMessage);
        
        MessageResponse response = chatService.convertToMessageResponse(chatMessage);
        
        messagingTemplate.convertAndSendToUser(
            chatMessage.getSenderId().toString(),
            "/queue/messages",
            response
        );
    }
}
```

**Publisher:**
```java
rabbitTemplate.convertAndSend(
    RabbitMQConfig.CHAT_EXCHANGE,
    RabbitMQConfig.CHAT_INPUT_ROUTING_KEY,
    chatMessage
);
```

---

### 15. application.yml Configuration

```yaml
server:
  port: 8080

spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/postgres
    username: postgres
    password: your_password
    driver-class-name: org.postgresql.Driver

  jpa:
    hibernate:
      ddl-auto: none
      naming:
        physical-strategy: org.hibernate.boot.model.naming.PhysicalNamingStrategyStandardImpl
    show-sql: true

  mongodb:
    host: localhost
    port: 27017
    database: my_db
    username: mongodb
    password: your_password
    authentication-database: my_db

  rabbitmq:
    host: localhost
    port: 5672
    username: guest
    password: guest

  websocket:
    allowed-origins: http://localhost:5000

jwt:
  secret: ${JWT_SECRET:your-secret-key}
  access-token-expiration: 900000  # 15 minutes
  refresh-token-expiration: 604800000  # 7 days

cloudinary:
  cloud_name: your_cloud_name
  api_key: your_api_key
  api_secret: your_api_secret

mongock:
  migration-scan-package:
    - com.example.demo.config.migration
  throw-exception-if-cannot-obtain-lock: true
  transaction-enabled: false
```

---

## Frontend Patterns (React + TypeScript)

### 1. Folder Structure

```
src/
├── api/                    # API clients
│   ├── axiosClient.ts     # Axios instance with interceptors
│   ├── authService.ts     # Auth API
│   ├── postApi.ts         # Post API
│   └── chatApi.ts         # Chat API
├── components/             # Reusable components
├── pages/                  # Page components
├── hooks/                  # Custom hooks
│   └── queries/           # React Query hooks
│       ├── usePostQueries.ts
│       └── useCommentQueries.ts
├── stores/                 # Zustand stores
│   ├── authStore.ts
│   └── chatStore.ts
├── types/                  # TypeScript types
│   └── dto/               # DTO types
│       ├── api-response.dto.ts
│       ├── user.dto.ts
│       ├── chat.dto.ts
│       └── request.dto.ts
├── services/               # Services
│   └── websocket.service.ts
├── providers/              # Context providers
│   └── WebSocketProvider.tsx
└── routes/                 # Route definitions
    └── routes.tsx
```

---

### 2. DTO Types

```typescript
// api-response.dto.ts
export interface ApiResponse<T> {
  code: number;
  message: string;
  result: T;
}

// user.dto.ts
export interface UserDTO {
  id: number;
  email: string;
  createdAt: string;
  updatedAt: string;
}

export interface AuthResponse {
  accessToken: string;
  refreshToken: string;
  userId: number;
  email: string;
  isPremium: boolean;
}

// request.dto.ts
export interface CreatePostRequest {
  content: string;
  media: MediaItemDTO[];
}

export interface UpdatePostRequest {
  content: string;
  media: MediaItemDTO[];
}
```

---

### 3. Axios Client with Token Refresh

```typescript
import axios, { AxiosError, InternalAxiosRequestConfig } from 'axios';
import { useAuthStore } from '../stores/authStore';

const axiosClient = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api',
  headers: {
    'Content-Type': 'application/json',
  },
  timeout: 10000,
});

let isRefreshing = false;
let failedQueue: Array<{
  resolve: (value?: unknown) => void;
  reject: (reason?: unknown) => void;
}> = [];

const processQueue = (error: Error | null, token: string | null = null) => {
  failedQueue.forEach((prom) => {
    if (error) {
      prom.reject(error);
    } else {
      prom.resolve(token);
    }
  });
  failedQueue = [];
};

axiosClient.interceptors.request.use(
  (config) => {
    const token = useAuthStore.getState().accessToken;
    if (token && config.headers) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => Promise.reject(error)
);

axiosClient.interceptors.response.use(
  (response) => response,
  async (error: AxiosError) => {
    const originalRequest = error.config as InternalAxiosRequestConfig & { _retry?: number };

    if (error.response?.status === 401) {
      if (originalRequest.url?.includes('/auth/')) {
        return Promise.reject(error);
      }

      originalRequest._retry = originalRequest._retry || 0;

      if (originalRequest._retry >= 3) {
        useAuthStore.getState().clearAuthData();
        window.location.href = '/login';
        return Promise.reject(error);
      }

      originalRequest._retry += 1;

      if (isRefreshing) {
        return new Promise((resolve, reject) => {
          failedQueue.push({ resolve, reject });
        })
          .then((token) => {
            if (originalRequest.headers) {
              originalRequest.headers.Authorization = `Bearer ${token}`;
            }
            return axiosClient(originalRequest);
          })
          .catch((err) => Promise.reject(err));
      }

      isRefreshing = true;

      const refreshToken = useAuthStore.getState().refreshToken;

      if (!refreshToken) {
        isRefreshing = false;
        processQueue(new Error('No refresh token'), null);
        useAuthStore.getState().clearAuthData();
        window.location.href = '/login';
        return Promise.reject(error);
      }

      try {
        const response = await axios.post(
          `${import.meta.env.VITE_API_BASE_URL}/auth/refresh`,
          { refreshToken }
        );

        const authResponse = response.data.result;
        const { accessToken, refreshToken: newRefreshToken } = authResponse;

        useAuthStore.getState().updateTokens(accessToken, newRefreshToken);
        axiosClient.defaults.headers.common['Authorization'] = `Bearer ${accessToken}`;

        if (originalRequest.headers) {
          originalRequest.headers.Authorization = `Bearer ${accessToken}`;
        }

        processQueue(null, accessToken);

        if (window.queryClient) {
          window.queryClient.invalidateQueries();
        }

        return axiosClient(originalRequest);
      } catch (refreshError) {
        processQueue(refreshError as Error, null);
        useAuthStore.getState().clearAuthData();
        window.location.href = '/login';
        return Promise.reject(refreshError);
      } finally {
        isRefreshing = false;
      }
    }

    return Promise.reject(error);
  }
);

export default axiosClient;
```

---

### 4. API Service

```typescript
// authService.ts
import axiosClient from './axiosClient';
import type { LoginRequest, AuthResponse } from '../types/dto';
import { useAuthStore } from '../stores/authStore';

class AuthApi {
  async login(email: string, password: string): Promise<AuthResponse> {
    const request: LoginRequest = { email, password };
    const response = await axiosClient.post<{ result: AuthResponse }>('/auth/login', request);
    
    useAuthStore.getState().setAuthData(response.data.result);
    
    return response.data.result;
  }

  async register(email: string, password: string): Promise<AuthResponse> {
    const request: RegisterRequest = { email, password };
    const response = await axiosClient.post<{ result: AuthResponse }>('/auth/register', request);
    
    useAuthStore.getState().setAuthData(response.data.result);
    
    return response.data.result;
  }

  logout(): void {
    useAuthStore.getState().clearAuthData();
  }

  isAuthenticated(): boolean {
    return useAuthStore.getState().isAuthenticated();
  }
}

export const authApi = new AuthApi();

// postApi.ts
import axiosClient from './axiosClient';
import type { ApiResponse } from '@/types/dto/api-response.dto';
import type { PostResponse, CreatePostRequest } from '@/types/dto';

export const postApi = {
  getAllPosts: async (): Promise<ApiResponse<PostResponse[]>> => {
    const response = await axiosClient.get<ApiResponse<PostResponse[]>>('/posts');
    return response.data;
  },

  getPostById: async (id: string): Promise<ApiResponse<PostResponse>> => {
    const response = await axiosClient.get<ApiResponse<PostResponse>>(`/posts/${id}`);
    return response.data;
  },

  createPost: async (data: CreatePostRequest): Promise<ApiResponse<PostResponse>> => {
    const response = await axiosClient.post<ApiResponse<PostResponse>>('/posts', data);
    return response.data;
  },

  updatePost: async (id: string, data: UpdatePostRequest): Promise<ApiResponse<PostResponse>> => {
    const response = await axiosClient.put<ApiResponse<PostResponse>>(`/posts/${id}`, data);
    return response.data;
  },

  deletePost: async (id: string): Promise<ApiResponse<void>> => {
    const response = await axiosClient.delete<ApiResponse<void>>(`/posts/${id}`);
    return response.data;
  },
};
```

---

### 5. Zustand Store

```typescript
// authStore.ts
import { create } from 'zustand';
import { persist } from 'zustand/middleware';
import type { AuthResponse } from '../types/dto';

interface AuthState {
  accessToken: string | null;
  refreshToken: string | null;
  userId: number | null;
  email: string | null;
  isPremium: boolean;
  
  setAuthData: (authResponse: AuthResponse) => void;
  clearAuthData: () => void;
  updateTokens: (accessToken: string, refreshToken: string) => void;
  isAuthenticated: () => boolean;
}

export const useAuthStore = create<AuthState>()(
  persist(
    (set, get) => ({
      accessToken: null,
      refreshToken: null,
      userId: null,
      email: null,
      isPremium: false,

      setAuthData: (authResponse: AuthResponse) => {
        set({
          accessToken: authResponse.accessToken,
          refreshToken: authResponse.refreshToken,
          userId: authResponse.userId,
          email: authResponse.email,
          isPremium: authResponse.isPremium,
        });
      },

      clearAuthData: () => {
        set({
          accessToken: null,
          refreshToken: null,
          userId: null,
          email: null,
          isPremium: false,
        });
      },

      updateTokens: (accessToken: string, refreshToken: string) => {
        set({ accessToken, refreshToken });
      },

      isAuthenticated: () => {
        return !!get().accessToken;
      },
    }),
    {
      name: 'auth-storage',
      partialize: (state) => ({
        accessToken: state.accessToken,
        refreshToken: state.refreshToken,
        userId: state.userId,
        email: state.email,
        isPremium: state.isPremium,
      }),
    }
  )
);
```

**Key Points:**
- Use `persist` middleware for localStorage
- `partialize` to select which state to persist
- Separate actions from state
- Use `get()` to access current state in actions

---

### 6. React Query Hooks

```typescript
// usePostQueries.ts
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { postApi } from '@/api/postApi';
import type { CreatePostRequest, UpdatePostRequest } from '@/types/dto/request.dto';

export const postKeys = {
  all: ['posts'] as const,
  lists: () => [...postKeys.all, 'list'] as const,
  list: (userId?: number) => userId 
    ? [...postKeys.lists(), 'user', userId] as const 
    : [...postKeys.lists(), 'all'] as const,
  details: () => [...postKeys.all, 'detail'] as const,
  detail: (id: string) => [...postKeys.details(), id] as const,
};

export const useAllPosts = () => {
  return useQuery({
    queryKey: postKeys.list(),
    queryFn: async () => {
      const response = await postApi.getAllPosts();
      return response.result;
    },
  });
};

export const usePost = (id: string) => {
  return useQuery({
    queryKey: postKeys.detail(id),
    queryFn: async () => {
      const response = await postApi.getPostById(id);
      return response.result;
    },
    enabled: !!id,
  });
};

export const useCreatePost = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (data: CreatePostRequest) => postApi.createPost(data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: postKeys.lists() });
    },
  });
};

export const useUpdatePost = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({ id, data }: { id: string; data: UpdatePostRequest }) =>
      postApi.updatePost(id, data),
    onSuccess: (_response, variables) => {
      queryClient.invalidateQueries({ queryKey: postKeys.detail(variables.id) });
      queryClient.invalidateQueries({ queryKey: postKeys.lists() });
    },
  });
};

export const useDeletePost = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (id: string) => postApi.deletePost(id),
    onSuccess: (_response, id) => {
      queryClient.removeQueries({ queryKey: postKeys.detail(id) });
      queryClient.invalidateQueries({ queryKey: postKeys.lists() });
    },
  });
};
```

**Key Points:**
- Query keys factory for centralized management
- Use `enabled` option for conditional queries
- `invalidateQueries` to refetch data
- `removeQueries` to remove from cache
- Type-safe query keys with `as const`

---

### 7. WebSocket Service

```typescript
// websocket.service.ts
import { Client, StompSubscription } from '@stomp/stompjs';
import SockJS from 'sockjs-client';

class WebSocketService {
  private client: Client | null = null;
  private subscriptions: Map<string, StompSubscription> = new Map();
  private isConnected = false;

  async connect(token: string): Promise<void> {
    return new Promise((resolve, reject) => {
      this.client = new Client({
        webSocketFactory: () => new SockJS(`${import.meta.env.VITE_WS_URL}/ws`),
        connectHeaders: {
          Authorization: `Bearer ${token}`,
        },
        debug: (str) => {
          console.log('[WebSocket]', str);
        },
        onConnect: () => {
          console.log('[WebSocket] Connected');
          this.isConnected = true;
          resolve();
        },
        onStompError: (frame) => {
          console.error('[WebSocket] Error:', frame);
          reject(new Error(frame.headers['message']));
        },
        onWebSocketClose: () => {
          console.log('[WebSocket] Closed');
          this.isConnected = false;
        },
      });

      this.client.activate();
    });
  }

  subscribe<T>(destination: string, callback: (message: T) => void): string {
    if (!this.client || !this.isConnected) {
      throw new Error('WebSocket not connected');
    }

    const subscription = this.client.subscribe(destination, (message) => {
      const data = JSON.parse(message.body);
      callback(data);
    });

    const id = `${destination}-${Date.now()}`;
    this.subscriptions.set(id, subscription);
    return id;
  }

  unsubscribe(id: string): void {
    const subscription = this.subscriptions.get(id);
    if (subscription) {
      subscription.unsubscribe();
      this.subscriptions.delete(id);
    }
  }

  send(destination: string, body: any): void {
    if (!this.client || !this.isConnected) {
      throw new Error('WebSocket not connected');
    }

    this.client.publish({
      destination,
      body: JSON.stringify(body),
    });
  }

  disconnect(): void {
    this.subscriptions.forEach((sub) => sub.unsubscribe());
    this.subscriptions.clear();
    
    if (this.client) {
      this.client.deactivate();
      this.client = null;
    }
    
    this.isConnected = false;
  }

  async reconnect(token: string): Promise<void> {
    this.disconnect();
    await this.connect(token);
  }
}

export default new WebSocketService();
```

---

### 8. WebSocket Provider

```typescript
// WebSocketProvider.tsx
import { useEffect, useRef } from 'react';
import { useQueryClient } from '@tanstack/react-query';
import { useAuthStore } from '@/stores/authStore';
import { useChatStore } from '@/stores/chatStore';
import websocketService from '@/services/websocket.service';
import { chatKeys } from '@/hooks/queries/useChatQueries';
import { toast } from 'sonner';
import type { MessageResponse, UserPresenceResponse } from '@/types/dto/chat.dto';

interface WebSocketProviderProps {
  children: React.ReactNode;
}

export const WebSocketProvider = ({ children }: WebSocketProviderProps) => {
  const queryClient = useQueryClient();
  const { userId, accessToken, isAuthenticated } = useAuthStore();
  const isInitialized = useRef(false);
  const subscriptionIds = useRef<string[]>([]);
  const previousToken = useRef<string | null>(null);

  useEffect(() => {
    if (!isAuthenticated || !accessToken || !userId) {
      return;
    }

    if (previousToken.current && previousToken.current !== accessToken && isInitialized.current) {
      console.log('[WebSocketProvider] Token refreshed, reconnecting...');
      
      const reconnectWebSocket = async () => {
        try {
          subscriptionIds.current.forEach(id => {
            websocketService.unsubscribe(id);
          });
          subscriptionIds.current = [];
          
          await websocketService.reconnect(accessToken);
          await setupSubscriptions();
          
          console.log('[WebSocketProvider] Reconnected successfully');
        } catch (error) {
          console.error('[WebSocketProvider] Reconnection failed:', error);
          isInitialized.current = false;
        }
      };
      
      reconnectWebSocket();
    }
    
    previousToken.current = accessToken;
  }, [accessToken, isAuthenticated, userId, queryClient]);

  const setupSubscriptions = async () => {
    const presenceSub = websocketService.subscribe('/user/queue/presence', (presence: UserPresenceResponse) => {
      if (presence.userId && presence.status) {
        const isOnline = presence.status === 'ONLINE';
        useChatStore.getState().updateUserPresence(presence.userId, isOnline);
      }
    });
    subscriptionIds.current.push(presenceSub);

    const messageSub = websocketService.subscribe('/user/queue/messages', (message: MessageResponse) => {
      const currentActiveConversation = useChatStore.getState().activeConversation;
      useChatStore.getState().addMessage(message.conversationId, message);
      
      if (message.conversationId !== currentActiveConversation && message.senderId !== userId) {
        useChatStore.getState().incrementUnreadCount(message.conversationId);
        toast.info(`New message from ${message.senderName}`);
      }
      
      queryClient.invalidateQueries({ queryKey: chatKeys.messages(message.conversationId, 0) });
      queryClient.invalidateQueries({ queryKey: chatKeys.conversations() });
    });
    subscriptionIds.current.push(messageSub);
  };

  useEffect(() => {
    if (!isAuthenticated || !userId || !accessToken || isInitialized.current) {
      return;
    }

    const initializeWebSocket = async () => {
      try {
        await websocketService.connect(accessToken);
        isInitialized.current = true;
        previousToken.current = accessToken;

        await setupSubscriptions();
      } catch (error) {
        console.error('[WebSocketProvider] Initialization failed:', error);
        isInitialized.current = false;
      }
    };

    initializeWebSocket();

    return () => {
      subscriptionIds.current.forEach(id => {
        websocketService.unsubscribe(id);
      });
      subscriptionIds.current = [];
      websocketService.disconnect();
      isInitialized.current = false;
    };
  }, [isAuthenticated, userId, accessToken, queryClient]);

  return <>{children}</>;
};
```

---

### 9. App.tsx Setup

```typescript
// App.tsx
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { BrowserRouter } from 'react-router-dom';
import { WebSocketProvider } from './providers/WebSocketProvider';
import { Toaster } from 'sonner';
import AppRoutes from './routes/routes';

const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      retry: 1,
      refetchOnWindowFocus: false,
      staleTime: 5 * 60 * 1000,
    },
  },
});

// Make queryClient available globally for axios interceptor
declare global {
  interface Window {
    queryClient: QueryClient;
  }
}
window.queryClient = queryClient;

function App() {
  return (
    <QueryClientProvider client={queryClient}>
      <BrowserRouter>
        <WebSocketProvider>
          <AppRoutes />
          <Toaster position="top-right" />
        </WebSocketProvider>
      </BrowserRouter>
    </QueryClientProvider>
  );
}

export default App;
```

---

### 10. Environment Variables

```env
# .env
VITE_API_BASE_URL=http://localhost:8080/api
VITE_WS_URL=http://localhost:8080

# .env.example
VITE_API_BASE_URL=http://localhost:8080/api
VITE_WS_URL=http://localhost:8080
```

---

## Common Patterns

### Data Flow

**Backend:**
1. Controller receives request
2. Controller calls Service
3. Service uses Repository to fetch/save data
4. Service uses Mapper to convert Entity/Document ↔ DTO
5. Service returns DTO to Controller
6. Controller wraps in ApiResponse and returns

**Frontend:**
1. Component calls React Query hook
2. Hook calls API service
3. API service uses axiosClient
4. axiosClient adds JWT token
5. Response unwrapped from ApiResponse
6. Data cached by React Query
7. Component renders with data

### Authentication Flow

1. User logs in → Backend generates JWT tokens
2. Frontend stores tokens in Zustand (persisted to localStorage)
3. axiosClient adds token to all requests
4. On 401 error → axiosClient refreshes token automatically
5. New tokens stored → All failed requests retried
6. On refresh failure → Redirect to login

### WebSocket Flow

1. User authenticates → Frontend connects WebSocket with JWT
2. Backend validates JWT in WebSocket interceptor
3. Frontend subscribes to user-specific queues
4. Backend publishes messages to user queues
5. Frontend receives messages → Updates Zustand store
6. React Query cache invalidated → UI updates

---

## Best Practices

### Backend

1. **No comments in code** - Code should be self-explanatory
2. **Ensure correct imports** - Always verify imports before testing
3. **Data flow validation** - Verify request/response flow
4. **Use Lombok** - Reduce boilerplate with `@Data`, `@RequiredArgsConstructor`, `@Slf4j`
5. **Soft delete** - Use `status` + `deletedAt` fields
6. **Timestamps** - Use `Instant` type for all timestamps
7. **MapStruct** - Use for all Entity/Document ↔ DTO conversions
8. **ApiResponse wrapper** - Wrap all controller responses
9. **Custom exceptions** - Use `AppException` with `ErrorCode` enum
10. **Transaction management** - Use `@Transactional` for write operations

### Frontend

1. **No comments in code** - Code should be self-explanatory
2. **Type safety** - Define all DTOs matching backend
3. **Query keys factory** - Centralize React Query keys
4. **Zustand for global state** - Auth, chat, etc.
5. **React Query for server state** - API data caching
6. **Axios interceptors** - Handle token refresh automatically
7. **WebSocket reconnection** - Handle token refresh in WebSocket
8. **Error handling** - Use toast notifications
9. **Loading states** - Use React Query loading states
10. **Optimistic updates** - Update UI before server response when appropriate

---

## Testing Checklist

### Backend
- [ ] Imports are correct
- [ ] MapStruct mappers compile without errors
- [ ] Database migrations run successfully
- [ ] JWT token generation/validation works
- [ ] API endpoints return correct ApiResponse structure
- [ ] Exception handling works correctly
- [ ] WebSocket connection established
- [ ] RabbitMQ messages published/consumed

### Frontend
- [ ] Types match backend DTOs
- [ ] Axios interceptor refreshes tokens
- [ ] React Query hooks fetch data correctly
- [ ] Zustand stores persist to localStorage
- [ ] WebSocket connects and receives messages
- [ ] Token refresh doesn't break WebSocket
- [ ] Protected routes redirect to login
- [ ] Error messages display correctly

---

**End of Document**
