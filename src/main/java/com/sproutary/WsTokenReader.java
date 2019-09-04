package com.sproutary;

import io.micronaut.context.annotation.Requires;
import io.micronaut.core.util.StringUtils;
import io.micronaut.http.HttpParameters;
import io.micronaut.http.HttpRequest;
import io.micronaut.security.token.jwt.bearer.BearerTokenConfigurationProperties;
import io.micronaut.security.token.jwt.cookie.JwtCookieTokenReader;
import io.micronaut.security.token.reader.TokenReader;

import javax.inject.Singleton;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

/**
 * Reads JWT token from token param.
 *
 * @author Eric Helgeson
 */
@Requires(property = BearerTokenConfigurationProperties.PREFIX + ".enabled", notEquals = StringUtils.FALSE)
@Singleton
public class WsTokenReader implements TokenReader {
    private static final Logger LOG = LoggerFactory.getLogger(WsTokenReader.class);

    private static final Integer ORDER = JwtCookieTokenReader.ORDER - 100;

    @Override
    public int getOrder() {
        return ORDER;
    }

    @Override
    public Optional<String> findToken(HttpRequest<?> request) {
        LOG.debug("Looking for bearer token in params");
        HttpParameters params = request.getParameters();
        return params.getFirst("token");
    }
}
