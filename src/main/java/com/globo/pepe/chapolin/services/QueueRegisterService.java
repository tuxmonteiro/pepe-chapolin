/*
 * Copyright (c) 2019 Globo.com - ATeam
 * All rights reserved.
 *
 * This source is subject to the Apache License, Version 2.0.
 * Please see the LICENSE file for more information.
 *
 * Authors: See AUTHORS file
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.globo.pepe.chapolin.services;

import static com.globo.pepe.common.util.Constants.PACK_NAME;
import static com.globo.pepe.common.util.Constants.TRIGGER_PREFIX;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.globo.pepe.common.services.AmqpService;
import com.globo.pepe.common.services.JsonLoggerService;
import com.globo.pepe.common.services.JsonLoggerService.JsonLogger;
import com.rabbitmq.http.client.domain.QueueInfo;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.amqp.rabbit.listener.api.ChannelAwareMessageListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class QueueRegisterService {

    private final AmqpService amqpService;
    private final ObjectMapper mapper;
    private final JsonLoggerService jsonLoggerService;
    private final StackstormService stackstormService;

    @Value("${pepe.event.ttl}")
    private Long eventTtl;

    public QueueRegisterService(
        AmqpService amqpService,
        ObjectMapper mapper,
        JsonLoggerService jsonLoggerService,
        StackstormService stackstormService) {

        this.amqpService = amqpService;
        this.mapper = mapper;
        this.jsonLoggerService = jsonLoggerService;
        this.stackstormService = stackstormService;
    }

    public void register(String queue) {
        JsonLogger logger = jsonLoggerService.newLogger(getClass());
        final ChannelAwareMessageListener messageListener = (message, channel) -> {
            boolean wasSend = false;
            long deliveryTag = message.getMessageProperties().getDeliveryTag();
            try {
                byte[] messageBody = message.getBody();
                logger.message("send " + new String(messageBody)).put("queue", queue).sendInfo();
                wasSend = stackstormService.send(mapper.readTree(messageBody));
            } catch (IOException e) {
                logger.message(e.getMessage()).sendError(e);
            }
            if (wasSend) {
                channel.basicAck(deliveryTag, false);
            } else {
                channel.basicNack(deliveryTag, false, true);
            }
        };
        amqpService.newQueue(queue);
        amqpService.prepareListenersMap(queue);
        amqpService.registerListener(queue, messageListener);
    }

    @Scheduled(fixedDelayString = "${pepe.chapolin.sync_delay}")
    public void syncDelay() {
        JsonLogger logger = jsonLoggerService.newLogger(getClass());
        if (logger.isDebugEnabled()) {
            logger.message("syncronizing queues").sendDebug();
        }

        final Set<String> queues = amqpService.queuesFromRabbit(PACK_NAME + "." + TRIGGER_PREFIX + ".")
            .stream().map(QueueInfo::getName).collect(Collectors.toSet());
        queues.stream().filter(q -> !amqpService.hasQueue(q)).forEach(this::register);
        final Set<String> queuesRemoved = new HashSet<>(amqpService.queuesRegistered());
        queuesRemoved.removeAll(queues);
        queuesRemoved.forEach(amqpService::stopListener);
    }

}
