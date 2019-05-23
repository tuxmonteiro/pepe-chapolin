package com.globo.pepe.chapolin.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.fridujo.rabbitmq.mock.MockConnectionFactory;
import com.globo.pepe.chapolin.configuration.HttpClientConfiguration;
import com.globo.pepe.chapolin.mocks.AmqpMockConfiguration;
import com.globo.pepe.common.services.AmqpService;
import com.globo.pepe.common.services.JsonLoggerService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.amqp.rabbit.connection.AbstractConnectionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.support.AnnotationConfigContextLoader;

import java.util.concurrent.TimeUnit;

import static com.globo.pepe.common.util.Constants.PACK_NAME;
import static com.globo.pepe.common.util.Constants.TRIGGER_PREFIX;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;

@RunWith(SpringRunner.class)
@SpringBootTest
@ContextConfiguration(classes = {
    QueueRegisterService.class,
    StackstormService.class,
    RequestService.class,
    AmqpService.class,
    AmqpMockConfiguration.class,
    JsonLoggerService.class,
    ObjectMapper.class,
    JsonSchemaGeneratorService.class,
    HttpClientConfiguration.class,
    StackstormAuthService.class
}, loader = AnnotationConfigContextLoader.class)
@TestPropertySource(properties = {
        "pepe.logging.tags=default",
        "pepe.event.ttl=10000",
        "pepe.chapolin.sleep_interval_on_fail=1"
})
public class QueueRegisterServiceTests {

    @Autowired
    public AmqpService amqpService;

    @Autowired
    public QueueRegisterService queueRegisterService;

    @MockBean
    public StackstormService stackstormService;

    private final String project = "pepe";

    public void setup() {
        stackstormService = Mockito.mock(StackstormService.class);
    }

    @Test
    public void connectionFactoryIsMock() {
        assertTrue(((AbstractConnectionFactory)amqpService.connectionFactory()).getRabbitConnectionFactory() instanceof MockConnectionFactory);
    }

    @Test
    public void sendMessageSuccessTest() throws InterruptedException {
        String queueName = PACK_NAME + "." + TRIGGER_PREFIX + "." + project + "." + "test";
        boolean result = false;
        when(stackstormService.send(any(JsonNode.class))).thenReturn(result = invert(result));
        queueRegisterService.register(queueName);
        amqpService.convertAndSend(queueName, "{}", 10000);
        TimeUnit.SECONDS.sleep(2);
        assertTrue(result);
        amqpService.stopListener(queueName);

    }

    @Test
    public void sendMessageFailTest() throws InterruptedException {
        String queueName = PACK_NAME + "." + TRIGGER_PREFIX + "." + project + "." + "test";
        boolean result = true;
        when(stackstormService.send(any(JsonNode.class))).thenReturn(result = invert(result));
        queueRegisterService.register(queueName);
        amqpService.convertAndSend(queueName, "{}", 10000);
        TimeUnit.SECONDS.sleep(2);
        assertFalse(result);
        amqpService.stopListener(queueName);

    }

    private boolean invert(boolean test) {
        return !test;
    }
}
