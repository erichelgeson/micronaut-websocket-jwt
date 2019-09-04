package com.sproutary;

import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.security.annotation.Secured;
import io.micronaut.security.rules.SecurityRule;
import io.micronaut.websocket.WebSocketBroadcaster;
import io.reactivex.Flowable;

import javax.inject.Inject;
import java.util.HashMap;

import static com.sproutary.WebSocketServer.*;
import static io.reactivex.Flowable.*;

@Secured(SecurityRule.IS_ANONYMOUS)
@Controller("/send")
public class SendController {
    @Inject WebSocketBroadcaster broadcaster;

    @Get(produces = MediaType.APPLICATION_JSON)
    Flowable<HashMap> index() {
        var map = new HashMap<>();
        map.put("key", "value");

        return merge(
                broadcaster.broadcast(map, toUser("foo@bar.com")),
                broadcaster.broadcast(map, toUser("k@cc.com"))
        );
    }

    @Get(uri = "center", produces = MediaType.APPLICATION_JSON)
    Flowable<HashMap> center() {
        var map1 = new HashMap<>();
        map1.put("type", "foo");
        var map2 = new HashMap<>();
        map2.put("type", "bar");

        return merge(
                broadcaster.broadcast(map1, toCenterRoles(48L, "ROLE_CARECENTER")),
                broadcaster.broadcast(map2, toCenter(48L))
        );
    }
}
