/*
 * Sonatype Nexus (TM) Open Source Version
 * Copyright (c) 2008-present Sonatype, Inc.
 * All rights reserved. Includes the third-party code listed at http://links.sonatype.com/products/nexus/oss/attributions.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse Public License Version 1.0,
 * which accompanies this distribution and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Sonatype Nexus (TM) Professional Version is available from Sonatype, Inc. "Sonatype" and "Sonatype Nexus" are trademarks
 * of Sonatype, Inc. Apache Maven is a trademark of the Apache Software Foundation. M2eclipse is a trademark of the
 * Eclipse Foundation. All other trademarks are the property of their respective owners.
 */
package org.sonatype.nexus.security;

import java.util.Date;
import java.util.Optional;
import java.util.UUID;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.common.text.Strings2;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import org.apache.shiro.subject.Subject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * JWT helper to create, decode and verify token
 *
 * @since 3.next
 */
@Named
@Singleton
public class JwtHelper
{
  public static final String JWT_COOKIE_NAME = "nx-jwt";

  public static final String ISSUER = "sonatype";

  public static final String REALM = "realm";

  public static final String USER = "user";

  public static final String USER_SESSION_ID = "userSessionId";

  private static final Logger log = LoggerFactory.getLogger(JwtHelper.class);

  private static final long MILLIS_PER_SECOND = 1000;

  private final Algorithm algorithm;

  private final int expiry;

  private final JWTVerifier verifier;

  @Inject
  public JwtHelper(@Named("${nexus.jwt.expiry:-1800}") final int expiry,
                   @Nullable @Named("${nexus.jwt.secret}") final String secret) {
    this.expiry = expiry;
    String secretValue = secret;
    if (Strings2.isBlank(secretValue)) {
      secretValue = UUID.randomUUID().toString();
    }
    this.algorithm = Algorithm.HMAC256(secretValue);
    this.verifier = JWT.require(algorithm)
        .withIssuer(JwtHelper.ISSUER)
        .build();
  }

  /**
   * Generates a new JWT
   */
  public String createJwt(final Subject subject) {
    checkNotNull(subject);

    String username = subject.getPrincipal().toString();
    Optional<String> realm = subject.getPrincipals().getRealmNames().stream().findFirst();

    Date issuedAt = new Date();
    Date expiresAt = new Date(issuedAt.getTime() + getExpiryInMillis());
    String userSessionId = UUID.randomUUID().toString();

    return JWT.create()
        .withIssuer(ISSUER)
        .withClaim(USER, username)
        .withClaim(REALM, realm.orElse(null))
        .withClaim(USER_SESSION_ID, userSessionId)
        .withIssuedAt(issuedAt)
        .withExpiresAt(expiresAt)
        .sign(algorithm);
  }

  /**
   * Verifies and decode token
   */
  public Optional<DecodedJWT> verifyJwt(final String jwt) {
    DecodedJWT decodedJWT;
    try {
      decodedJWT = verifier.verify(jwt);
    }
    catch (Exception e) {
      log.error("Invalid token", e);
      return Optional.empty();
    }
    return Optional.of(decodedJWT);
  }

  /**
   * Gets expiry in seconds
   */
  public int getExpiryInSec() {
    return expiry;
  }

  /**
   * Gets expiry in milliseconds
   */
  public long getExpiryInMillis() {
    return expiry * MILLIS_PER_SECOND;
  }
}
