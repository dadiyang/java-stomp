package com.github.dadiyang.javastomp.msg;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.messaging.simp.user.SimpUserRegistry;
import org.springframework.stereotype.Controller;

import java.security.Principal;

/**
 * STOMP 消息处理
 *
 * @author huangxuyang
 * @date 2019/2/2
 */
@Slf4j
@Controller
@RequiredArgsConstructor(onConstructor_ = {@Autowired})
public class StompController {
    private final SimpMessageSendingOperations msgOperations;
    private final SimpUserRegistry simpUserRegistry;

    /**
     * 回音消息，将用户发来的消息内容加上 Echo 前缀后推送回客户端
     */
    @MessageMapping("/echo")
    public void echo(Principal principal, Msg msg) {
        String username = principal.getName();
        msg.setContent("Echo: " + msg.getContent());
        msgOperations.convertAndSendToUser(username, "/topic/subNewMsg", msg);
        int userCount = simpUserRegistry.getUserCount();
        int sessionCount = simpUserRegistry.getUser(username).getSessions().size();
        log.info("当前本系统总在线人数: {}, 当前用户: {}, 该用户的客户端连接数: {}", userCount, username, sessionCount);
    }
}
