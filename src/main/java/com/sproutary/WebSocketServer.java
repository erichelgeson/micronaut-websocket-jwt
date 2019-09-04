package com.sproutary;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.micronaut.core.convert.ConversionContext;
import io.micronaut.core.convert.TypeConverter;
import io.micronaut.security.annotation.Secured;
import io.micronaut.security.rules.SecurityRule;
import io.micronaut.security.token.jwt.validator.AuthenticationJWTClaimsSetAdapter;
import io.micronaut.security.utils.SecurityService;
import io.micronaut.websocket.CloseReason;
import io.micronaut.websocket.WebSocketBroadcaster;
import io.micronaut.websocket.WebSocketSession;
import io.micronaut.websocket.annotation.OnClose;
import io.micronaut.websocket.annotation.OnMessage;
import io.micronaut.websocket.annotation.OnOpen;
import io.micronaut.websocket.annotation.ServerWebSocket;
import io.reactivex.Flowable;
import net.minidev.json.JSONArray;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import java.io.IOException;
import java.security.Principal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Optional;
import java.util.concurrent.Flow;
import java.util.function.Predicate;

@Secured(SecurityRule.IS_AUTHENTICATED)
@ServerWebSocket("/ws/{centerId}/{user}")
class WebSocketServer {
    private static final Logger LOG = LoggerFactory.getLogger(WebSocketServer.class);
    private static final ArrayList EMPTY_LIST = new ArrayList<>();

    @Inject WebSocketBroadcaster broadcaster;
    @Inject SecurityService securityService;

    @OnOpen
    void onOpen(Long centerId, String user, WebSocketSession session) {
        var username = securityService.username().orElse(null);
        var myCenter = securityService.getAuthentication()
                .map(a -> a.getAttributes().getOrDefault("currentCareCenterId", null))
                .orElse(null);

        if (!user.equalsIgnoreCase(username)) {
            LOG.info("Closing as user is not valid");
            session.close(new CloseReason(1101, "Not Authorized for this topic. #1101"));
        }

        if (myCenter != centerId) {
            LOG.info("Closing as center is not valid");
            session.close(new CloseReason(1102, "Not Authorized for this topic. #1102"));
        }
    }

    @OnMessage
    Publisher<HashMap<String, Object>> onMessage(
            Long centerId,
            Message message) {
        if (message.ping) {
            return Flowable::just;
        }
        var username = securityService.username();

        var map = new HashMap<String, Object>();
        map.put("user", "" + username + "");
        map.put("centerId", centerId);
        map.put("type", message.type);
        map.put("value", message.value);

        return broadcaster.broadcast(map, toCenter(centerId));
    }

    @OnClose
    Publisher<String> onClose(
            Long centerId,
            WebSocketSession session) {
        Optional<String> username = securityService.username();
        String msg = "[" + username + "] Disconnected!";
        LOG.info(msg);
        return broadcaster.broadcast(msg, toCenter(centerId));
    }

    static Predicate<WebSocketSession> toCenterRoles(@NotNull Long centerId,
                                                     @NotNull @Pattern(regexp="^ROLE_(\\w)+") String role) {
        return s -> centerId.equals(s.getUriVariables().get("centerId", Long.class, null)) &&
                ((JSONArray)
                        ((AuthenticationJWTClaimsSetAdapter) s.getUserPrincipal().orElseThrow())
                                .getAttributes().getOrDefault("roles", EMPTY_LIST)
                ).contains(role);
    }

    static Predicate<WebSocketSession> toCenter(@NotNull Long centerId) {
        return s -> centerId.equals(s.getUriVariables().get("centerId", Long.class, null));
    }

    static Predicate<WebSocketSession> toUser(@NotNull String username) {
        return s -> username.equalsIgnoreCase( s.getUserPrincipal().map(Principal::getName).get() );
    }
}

class NoOpPublisher implements Publisher<HashMap<String, Object>> {
    @Override
    public void subscribe(Subscriber<? super HashMap<String, Object>> s) { }
}

class Message {
    String type;
    String value;
    Boolean ping = false;

    public void setPing(Boolean ping) {
        this.ping = ping;
    }

    public void setType(String type) {
        this.type = type;
    }

    public void setValue(String value) {
        this.value = value;
    }
}

@Singleton
class StringToMessageConverter implements TypeConverter<String, Message> {

    @Inject ObjectMapper objectMapper;

    @Override
    public Optional<Message> convert(String object, Class<Message> targetType, ConversionContext context) {
        try {
            return Optional.of(objectMapper.readValue(object, targetType));
        } catch (IOException e) {
            e.printStackTrace();
            return Optional.empty();
        }
    }
}