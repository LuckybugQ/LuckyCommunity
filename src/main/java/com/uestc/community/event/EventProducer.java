package com.uestc.community.event;

import com.alibaba.fastjson.JSONObject;
import com.uestc.community.entity.Event;
import com.uestc.community.entity.Message;
import com.uestc.community.service.MessageService;
import com.uestc.community.util.CommunityConstant;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Component
public class EventProducer implements CommunityConstant {

    @Autowired
    private KafkaTemplate kafkaTemplate;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private MessageService messageService;

    // 处理事件
    public void fireEvent(Event event) {
        // 将事件发布到指定的主题
        // 发送站内通知
        if(event.getTopic()==TOPIC_FOLLOW || event.getTopic()==TOPIC_LIKE || event.getTopic()==TOPIC_COMMENT){
            Message message = new Message();
            message.setFromId(SYSTEM_USER_ID);
            message.setToId(event.getEntityUserId());
            message.setConversationId(event.getTopic());
            message.setCreateTime(new Date());

            Map<String, Object> content = new HashMap<>();
            content.put("userId", event.getUserId());
            content.put("entityType", event.getEntityType());
            content.put("entityId", event.getEntityId());

            if (!event.getData().isEmpty()) {
                for (Map.Entry<String, Object> entry : event.getData().entrySet()) {
                    content.put(entry.getKey(), entry.getValue());
                }
            }

            message.setContent(JSONObject.toJSONString(content));
            messageService.addMessage(message);
        }
    }

    // 处理事件
    public void fireEventByKafka(Event event) {
        // 将事件发布到指定的主题
        kafkaTemplate.send(event.getTopic(), JSONObject.toJSONString(event));
    }

    // 处理事件
    public void fireEventByRabbit(Event event) {
        rabbitTemplate.convertAndSend(event.getTopic(), JSONObject.toJSONString(event));
    }
}
