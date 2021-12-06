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

import java.util.Collections;
import java.util.Date;
import java.util.Optional;

import org.sonatype.goodies.testsupport.TestSupport;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.TokenExpiredException;
import com.auth0.jwt.interfaces.Claim;
import com.auth0.jwt.interfaces.DecodedJWT;
import org.apache.shiro.subject.PrincipalCollection;
import org.apache.shiro.subject.Subject;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;
import static org.sonatype.nexus.security.JwtHelper.ISSUER;
import static org.sonatype.nexus.security.JwtHelper.REALM;
import static org.sonatype.nexus.security.JwtHelper.USER;
import static org.sonatype.nexus.security.JwtHelper.USER_SESSION_ID;

public class JwtHelperTest
    extends TestSupport
{
  @Mock
  private Subject subject;

  @Mock
  private PrincipalCollection principals;

  private JwtHelper underTest;

  @Before
  public void setup() {
    underTest = new JwtHelper(300, "secret");
    when(subject.getPrincipal()).thenReturn("admin");
    when(subject.getPrincipals()).thenReturn(principals);
    when(principals.getRealmNames()).thenReturn(Collections.singleton("NexusAuthorizingRealm"));
  }

  @Test
  public void testCreateJwtToken() {
    String jwtToken = underTest.createJwt(subject);
    assertNotNull(jwtToken);
    DecodedJWT decode;
    try {
      decode = JWT.decode(jwtToken);
    }
    catch (Exception e) {
      throw new IllegalArgumentException("Invalid token");
    }

    Claim user = decode.getClaim(USER);
    Claim userId = decode.getClaim(USER_SESSION_ID);
    Claim issuer = decode.getClaim("iss");
    Claim realm = decode.getClaim(REALM);

    assertEquals("admin", user.asString());
    assertNotNull(userId.asString());
    assertEquals(ISSUER, issuer.asString());
    assertEquals("NexusAuthorizingRealm", realm.asString());
  }

  @Test
  public void testVerifyJwt_success() {
    Date expiresAt = new Date(new Date().getTime() + 100000);
    String jwt = JWT.create()
        .withIssuer(ISSUER)
        .withExpiresAt(expiresAt)
        .sign(Algorithm.HMAC256("secret"));
    Optional<DecodedJWT> decodedJWT = underTest.verifyJwt(jwt);
    assertTrue(decodedJWT.isPresent());

    DecodedJWT decoded = decodedJWT.get();
    assertEquals(ISSUER, decoded.getClaim("iss").asString());
  }

  @Test
  public void testVerifyJwt_tokenExpired() {
    Date expiresAt = new Date(new Date().getTime() - 100000);
    String jwt = JWT.create()
        .withIssuer(ISSUER)
        .withExpiresAt(expiresAt)
        .sign(Algorithm.HMAC256("secret"));
    Optional<DecodedJWT> decodedJWT = underTest.verifyJwt(jwt);
    assertFalse(decodedJWT.isPresent());
  }
}
