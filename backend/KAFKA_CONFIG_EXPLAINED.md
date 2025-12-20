# Kafka Configuration Explained

## Tổng quan

File `KafkaConfig.java` cấu hình Spring Kafka để xử lý messaging trong hệ thống chat real-time. Configuration này thiết lập cách ứng dụng gửi (produce) và nhận (consume) messages từ Apache Kafka.

## Kiến trúc Kafka trong hệ thống

```
┌─────────────┐         ┌──────────────┐         ┌─────────────┐
│   Service   │────────►│ KafkaTemplate│────────►│   Kafka     │
│   Layer     │ publish │  (Producer)  │         │   Broker    │
└─────────────┘         └──────────────┘         └─────────────┘
                                                         │
                                                         │
                                                         ▼
┌─────────────┐         ┌──────────────┐         ┌─────────────┐
│   Service   │◄────────│@KafkaListener│◄────────│   Kafka     │
│   Layer     │ process │  (Consumer)  │         │   Topics    │
└─────────────┘         └──────────────┘         └─────────────┘
```

## Chi tiết các Bean Configuration

### 1. ProducerFactory Bean

```java
@Bean
@SuppressWarnings("deprecation")
public ProducerFactory<String, ChatMessage> producerFactory() {
    Map<String, Object> config = new HashMap<>();
    config.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
    config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
    config.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
    
    return new DefaultKafkaProducerFactory<>(config);
}
```

**Chức năng:**
- Tạo factory để produce (gửi) messages lên Kafka
- Định nghĩa cách serialize data trước khi gửi

**Configuration chi tiết:**
- `BOOTSTRAP_SERVERS_CONFIG`: Địa chỉ Kafka broker (localhost:9092)
- `KEY_SERIALIZER_CLASS_CONFIG`: Serialize key thành String
- `VALUE_SERIALIZER_CLASS_CONFIG`: Serialize ChatMessage object thành JSON

**Cách hoạt động:**
```
ChatMessage object → JsonSerializer → JSON bytes → Kafka Topic
```

**Sử dụng:**
```java
// Trong Service
kafkaTemplate.send("chat-input", conversationId, chatMessage);
```

---

### 2. KafkaTemplate Bean

```java
@Bean
public KafkaTemplate<String, ChatMessage> kafkaTemplate() {
    return new KafkaTemplate<>(producerFactory());
}
```

**Chức năng:**
- Template pattern để gửi messages lên Kafka
- Wrapper cao cấp hơn ProducerFactory, dễ sử dụng hơn

**API chính:**
```java
// Gửi message với key (conversationId) để đảm bảo ordering
kafkaTemplate.send(topic, key, message);

// Gửi message đơn giản
kafkaTemplate.send(topic, message);

// Gửi với callback
kafkaTemplate.send(topic, message).addCallback(
    success -> log.info("Sent"),
    failure -> log.error("Failed")
);
```

**Ví dụ thực tế:**
```java
@Service
public class ChatService {
    private final KafkaTemplate<String, ChatMessage> kafkaTemplate;
    
    public void sendMessage(ChatMessage message) {
        // Gửi message lên topic "chat-input"
        // Key = conversationId để messages cùng conversation vào cùng partition
        kafkaTemplate.send("chat-input", message.getConversationId(), message);
    }
}
```

---

### 3. ConsumerFactory Bean

```java
@Bean
@SuppressWarnings("deprecation")
public ConsumerFactory<String, ChatMessage> consumerFactory() {
    Map<String, Object> config = new HashMap<>();
    config.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
    config.put(ConsumerConfig.GROUP_ID_CONFIG, "chat-service");
    config.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
    config.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
    config.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ErrorHandlingDeserializer.class);
    config.put(ErrorHandlingDeserializer.VALUE_DESERIALIZER_CLASS, JsonDeserializer.class.getName());
    config.put(JsonDeserializer.TRUSTED_PACKAGES, "com.example.demo.*");
    config.put(JsonDeserializer.VALUE_DEFAULT_TYPE, ChatMessage.class.getName());
    
    return new DefaultKafkaConsumerFactory<>(config);
}
```

**Chức năng:**
- Tạo factory để consume (nhận) messages từ Kafka
- Định nghĩa cách deserialize data khi nhận

**Configuration chi tiết:**

| Config | Giá trị | Ý nghĩa |
|--------|---------|---------|
| `BOOTSTRAP_SERVERS_CONFIG` | localhost:9092 | Địa chỉ Kafka broker |
| `GROUP_ID_CONFIG` | chat-service | Consumer group ID - các consumer cùng group sẽ chia sẻ partitions |
| `AUTO_OFFSET_RESET_CONFIG` | earliest | Khi consumer mới join, đọc từ message đầu tiên |
| `KEY_DESERIALIZER_CLASS_CONFIG` | StringDeserializer | Deserialize key từ bytes → String |
| `VALUE_DESERIALIZER_CLASS_CONFIG` | ErrorHandlingDeserializer | Wrapper để handle lỗi deserialization |
| `VALUE_DESERIALIZER_CLASS` | JsonDeserializer | Deserialize JSON → ChatMessage object |
| `TRUSTED_PACKAGES` | com.example.demo.* | Chỉ cho phép deserialize classes từ package này (security) |
| `VALUE_DEFAULT_TYPE` | ChatMessage.class | Type mặc định khi deserialize |

**Cách hoạt động:**
```
Kafka Topic → JSON bytes → JsonDeserializer → ChatMessage object
                              ↓ (nếu lỗi)
                    ErrorHandlingDeserializer → Log error & skip
```

**Error Handling:**
- `ErrorHandlingDeserializer` wrap `JsonDeserializer`
- Nếu deserialize thất bại → log error và skip message (không crash app)
- Rất quan trọng cho production để tránh poison messages

---

### 4. KafkaListenerContainerFactory Bean

```java
@Bean
public ConcurrentKafkaListenerContainerFactory<String, ChatMessage> kafkaListenerContainerFactory() {
    ConcurrentKafkaListenerContainerFactory<String, ChatMessage> factory = 
        new ConcurrentKafkaListenerContainerFactory<>();
    factory.setConsumerFactory(consumerFactory());
    return factory;
}
```

**Chức năng:**
- Factory để tạo listener containers cho `@KafkaListener`
- Quản lý threading và concurrency cho consumers

**Sử dụng với @KafkaListener:**
```java
@Component
public class ChatKafkaConsumer {
    
    @KafkaListener(
        topics = "chat-input",
        groupId = "chat-processor",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumeInputMessage(ChatMessage message) {
        // Xử lý message
        log.info("Received: {}", message);
        chatService.processMessage(message);
    }
}
```

**Concurrency:**
```java
// Có thể config số threads xử lý đồng thời
factory.setConcurrency(3); // 3 threads consume cùng lúc
```

---

## Message Flow trong hệ thống Chat

### Gửi Message (Producer Flow)

```
1. User gửi message qua WebSocket
   ↓
2. ChatService.sendMessage() được gọi
   ↓
3. KafkaTemplate.send("chat-input", conversationId, chatMessage)
   ↓
4. ProducerFactory serialize ChatMessage → JSON
   ↓
5. Message được gửi lên Kafka topic "chat-input"
   ↓
6. Kafka lưu message vào partition (dựa trên conversationId key)
```

### Nhận Message (Consumer Flow)

```
1. Kafka có message mới trong topic "chat-input"
   ↓
2. @KafkaListener được trigger
   ↓
3. ConsumerFactory deserialize JSON → ChatMessage
   ↓
4. ChatKafkaConsumer.consumeInputMessage() được gọi
   ↓
5. ChatService.processMessage() xử lý logic
   ↓
6. Lưu vào MongoDB
   ↓
7. Publish lên topic "chat-output"
   ↓
8. WebSocket broadcast đến clients
```

---

## Kafka Topics trong hệ thống

### chat-input Topic
- **Mục đích**: Nhận messages mới từ clients
- **Producer**: ChatService (khi user gửi message)
- **Consumer**: ChatKafkaConsumer (xử lý và persist)
- **Partition Key**: conversationId (đảm bảo messages cùng conversation có thứ tự)

### chat-output Topic
- **Mục đích**: Broadcast messages đã xử lý
- **Producer**: ChatService (sau khi lưu MongoDB)
- **Consumer**: ChatKafkaConsumer (broadcast qua WebSocket)
- **Partition Key**: conversationId

---

## Tại sao dùng Kafka?

### 1. Scalability (Khả năng mở rộng)
```
┌─────────┐     ┌─────────┐     ┌─────────┐
│ Service │────►│  Kafka  │────►│ Service │
│ Node 1  │     │ Cluster │     │ Node 1  │
└─────────┘     │         │     └─────────┘
                │         │
┌─────────┐     │         │     ┌─────────┐
│ Service │────►│         │────►│ Service │
│ Node 2  │     │         │     │ Node 2  │
└─────────┘     └─────────┘     └─────────┘
```
- Nhiều service instances có thể produce/consume cùng lúc
- Kafka tự động load balance giữa consumers

### 2. Durability (Độ bền)
- Messages được lưu trên disk
- Nếu consumer crash, messages không bị mất
- Có thể replay messages từ offset cũ

### 3. Decoupling (Tách biệt)
- Producer không cần biết consumer
- Có thể thêm consumers mới mà không ảnh hưởng producers
- Dễ dàng thêm features mới (analytics, logging, etc.)

### 4. Ordering Guarantee (Đảm bảo thứ tự)
- Messages cùng partition key (conversationId) được xử lý theo thứ tự
- Quan trọng cho chat để messages không bị đảo lộn

---

## Configuration trong application.yml

```yaml
spring:
  kafka:
    bootstrap-servers: localhost:9092
    consumer:
      group-id: chat-service
      auto-offset-reset: earliest
      key-deserializer: org.apache.kafka.common.serialization.StringDeserializer
      value-deserializer: org.springframework.kafka.support.serializer.JsonDeserializer
      properties:
        spring.json.trusted.packages: com.example.demo.*
    producer:
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      value-serializer: org.springframework.kafka.support.serializer.JsonSerializer
```

**Lưu ý:**
- KafkaConfig.java override một số settings từ application.yml
- Có thể dùng pure application.yml config, nhưng Java config cho flexibility cao hơn

---

## Migration to JacksonJsonSerializer/Deserializer

### Tại sao dùng JacksonJsonSerializer thay vì JsonSerializer?

Spring Kafka 4.0 đã deprecated:
- `JsonSerializer`/`JsonDeserializer` (old API)

Và thay thế bằng:
- `JacksonJsonSerializer`/`JacksonJsonDeserializer` (new API)

**Lợi ích của API mới:**
- Tích hợp tốt hơn với Jackson 3.x (tools.jackson)
- API rõ ràng và type-safe hơn
- Không có deprecation warnings
- Future-proof cho Spring Kafka versions tiếp theo

**Cách sử dụng:**
```java
// Producer
new JacksonJsonSerializer<>()  // Sử dụng default JsonMapper

// Consumer
new JacksonJsonDeserializer<>(ChatMessage.class, false)  // false = không dùng headers
jsonDeserializer.addTrustedPackages("com.example.demo.*");  // Security
```

---

## Testing Kafka Configuration

### Unit Test với Embedded Kafka

```java
@SpringBootTest
@EmbeddedKafka(partitions = 1, topics = {"chat-input", "chat-output"})
class KafkaConfigTest {
    
    @Autowired
    private KafkaTemplate<String, ChatMessage> kafkaTemplate;
    
    @Test
    void testSendMessage() {
        ChatMessage message = new ChatMessage();
        message.setContent("Test");
        
        kafkaTemplate.send("chat-input", "conv-1", message);
        
        // Verify message được gửi thành công
    }
}
```

### Integration Test

```java
@SpringBootTest
@Testcontainers
class ChatKafkaIntegrationTest {
    
    @Container
    static KafkaContainer kafka = new KafkaContainer(
        DockerImageName.parse("confluentinc/cp-kafka:7.5.0")
    );
    
    @Test
    void testEndToEndMessageFlow() {
        // Test full flow: send → Kafka → consume → process
    }
}
```

---

## Troubleshooting

### Lỗi thường gặp:

**1. Connection refused**
```
Error: Connection to node -1 could not be established
```
**Giải pháp:** Kiểm tra Kafka đã chạy chưa
```bash
docker-compose up kafka
```

**2. Deserialization error**
```
Error: Cannot deserialize value of type ChatMessage
```
**Giải pháp:** Kiểm tra `TRUSTED_PACKAGES` config

**3. Consumer lag**
```
Consumer group chat-service has lag of 1000 messages
```
**Giải pháp:** Tăng concurrency hoặc optimize processing logic

---

## Best Practices

### 1. Partition Key Strategy
```java
// ✅ GOOD: Dùng conversationId làm key
kafkaTemplate.send("chat-input", message.getConversationId(), message);

// ❌ BAD: Không có key → messages bị random partition
kafkaTemplate.send("chat-input", message);
```

### 2. Error Handling
```java
// ✅ GOOD: Dùng ErrorHandlingDeserializer
config.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, 
    ErrorHandlingDeserializer.class);

// ❌ BAD: Không handle error → poison message crash app
```

### 3. Idempotent Producer
```java
// ✅ GOOD: Enable idempotence để tránh duplicate
config.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
```

### 4. Monitoring
```java
// Thêm metrics để monitor
@Bean
public MeterRegistry meterRegistry() {
    return new SimpleMeterRegistry();
}
```

---

## Tài liệu tham khảo

- [Spring Kafka Documentation](https://docs.spring.io/spring-kafka/reference/)
- [Apache Kafka Documentation](https://kafka.apache.org/documentation/)
- [Spring Messaging](https://docs.spring.io/spring-framework/reference/integration/messaging.html)

---

## Tóm tắt

KafkaConfig thiết lập infrastructure để:
1. **Gửi messages** (Producer) - Serialize ChatMessage → JSON → Kafka
2. **Nhận messages** (Consumer) - Kafka → JSON → Deserialize → ChatMessage
3. **Error handling** - ErrorHandlingDeserializer để tránh crash
4. **Ordering** - Partition key đảm bảo messages cùng conversation có thứ tự
5. **Scalability** - Hỗ trợ multiple instances và load balancing

Configuration này là nền tảng cho real-time chat system với Kafka messaging.
