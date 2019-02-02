package com.github.dadiyang.javastomp.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * 消息中继的配置类
 *
 * @author huangxuyang
 * @date 2018/11/26
 */
@Setter
@Getter
@Component
@ConfigurationProperties(prefix = "websocket.broker")
public class BrokerConfig {
    private String username;
    private String password;
    private String virtualHost;
    private String host;
    private int port;
    /**
     * 是否使用 SimpleBroker
     * <p>
     * SimpleBrokerMessageHandler 是基于内存的broker实现，不具备集群能力
     */
    private boolean useSimpleBroker;
    private List<InetSocketAddress> addresses;
    /**
     * 中继地址供应器
     */
    private BrokerAddressSupplier brokerAddressSupplier;

    @PostConstruct
    public void init() {
        brokerAddressSupplier = new BrokerAddressSupplier();
    }

    /**
     * 将配置的 ip:port 格式的地址列表转换为 InetSocketAddress 对象列表
     */
    public void setAddresses(List<String> addresses) {
        this.addresses = addresses.stream().map(a -> {
            String[] addr = a.split(":");
            return new InetSocketAddress(addr[0], Integer.parseInt(addr[1]));
        }).collect(Collectors.toList());
        // 需要根据下标访问 addresses 因此使用ArrayList
        this.addresses = new ArrayList<>(this.addresses);
    }

    public class BrokerAddressSupplier implements Supplier<SocketAddress> {
        private AtomicInteger index = new AtomicInteger();

        /**
         * 轮询中继地址
         */
        @Override
        public SocketAddress get() {
            return addresses.get(index.getAndIncrement() % addresses.size());
        }
    }
}
