package com.example.demo.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import java.util.HashMap;
import java.util.Map;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
@Configuration
public class RabbitMQConfig {

    public static final String CHAT_EXCHANGE = "chat.exchange";
    public static final String CHAT_INPUT_QUEUE = "chat.input.queue";
    public static final String CHAT_OUTPUT_QUEUE = "chat.output.queue";
    public static final String CHAT_INPUT_ROUTING_KEY = "chat.input";
    public static final String CHAT_OUTPUT_ROUTING_KEY = "chat.output";
    
    public static final String CHAT_DLX_EXCHANGE = "chat.dlx.exchange";
    public static final String CHAT_DLQ_QUEUE = "chat.dlq.queue";
    public static final String CHAT_DLQ_ROUTING_KEY = "chat.dlq";

    @Bean
    public TopicExchange chatExchange() {
        return new TopicExchange(CHAT_EXCHANGE, true, false);
    }

    @Bean
    public TopicExchange deadLetterExchange() {
        return new TopicExchange(CHAT_DLX_EXCHANGE, true, false);
    }

    @Bean
    public Queue chatInputQueue() {
        Map<String, Object> args = new HashMap<>();
        args.put("x-dead-letter-exchange", CHAT_DLX_EXCHANGE);
        args.put("x-dead-letter-routing-key", CHAT_DLQ_ROUTING_KEY);
        return new Queue(CHAT_INPUT_QUEUE, true, false, false, args);
    }

    @Bean
    public Queue chatOutputQueue() {
        Map<String, Object> args = new HashMap<>();
        args.put("x-dead-letter-exchange", CHAT_DLX_EXCHANGE);
        args.put("x-dead-letter-routing-key", CHAT_DLQ_ROUTING_KEY);
        return new Queue(CHAT_OUTPUT_QUEUE, true, false, false, args);
    }

    @Bean
    public Queue deadLetterQueue() {
        return new Queue(CHAT_DLQ_QUEUE, true);
    }

    @Bean
    public Binding chatInputBinding(Queue chatInputQueue, TopicExchange chatExchange) {
        return BindingBuilder.bind(chatInputQueue)
                .to(chatExchange)
                .with(CHAT_INPUT_ROUTING_KEY);
    }

    @Bean
    public Binding chatOutputBinding(Queue chatOutputQueue, TopicExchange chatExchange) {
        return BindingBuilder.bind(chatOutputQueue)
                .to(chatExchange)
                .with(CHAT_OUTPUT_ROUTING_KEY);
    }

    @Bean
    public Binding deadLetterBinding(Queue deadLetterQueue, TopicExchange deadLetterExchange) {
        return BindingBuilder.bind(deadLetterQueue)
                .to(deadLetterExchange)
                .with(CHAT_DLQ_ROUTING_KEY);
    }

    @Bean
    public JacksonJsonMessageConverter messageConverter() {
        return new JacksonJsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, JacksonJsonMessageConverter messageConverter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(messageConverter);
        return template;
    }
    
    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            ConnectionFactory connectionFactory, 
            JacksonJsonMessageConverter messageConverter) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(messageConverter);
        return factory;
    }

    @Bean
    public RabbitAdmin rabbitAdmin(ConnectionFactory connectionFactory) {
        return new RabbitAdmin(connectionFactory);
    }
}
