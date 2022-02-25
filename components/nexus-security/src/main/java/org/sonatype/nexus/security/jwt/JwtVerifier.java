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
package org.sonatype.nexus.security.jwt;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.security.JwtHelper;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;

/**
 * Perform the verification against the given JWT,
 *
 * @since 3.38
 */
public class JwtVerifier
    extends ComponentSupport
{
  private final Algorithm algorithm;

  private final JWTVerifier verifier;

  private final String secret;

  public JwtVerifier(final String secret) {
    this.secret = secret;
    this.algorithm = Algorithm.HMAC256(secret);
    this.verifier = JWT.require(algorithm)
        .withIssuer(JwtHelper.ISSUER)
        .build();
  }

  /**
   * Perform the verification against the given Token.
   *
   * @param jwt the JWT to verify.
   * @return the {@link DecodedJWT} object.
   */
  public DecodedJWT verify(final String jwt) throws JwtVerificationException {
    DecodedJWT decodedJWT;
    try {
      decodedJWT = verifier.verify(jwt);
    }
    catch (JWTVerificationException e) {
      String errorMsg = "Can't verify the token";
      log.debug(errorMsg, e);
      throw new JwtVerificationException(errorMsg);
    }
    return decodedJWT;
  }

  public Algorithm getAlgorithm() {
    return algorithm;
  }

  public String getSecret() {
    return secret;
  }
}
