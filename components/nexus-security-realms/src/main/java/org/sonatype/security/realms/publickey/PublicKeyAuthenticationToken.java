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
package org.sonatype.security.realms.publickey;

import java.security.PublicKey;

import org.apache.shiro.authc.AuthenticationToken;

/**
 * {@link AuthenticationToken} for a {@link PublicKey}.
 *
 * @author hugo@josefson.org
 */
public class PublicKeyAuthenticationToken
    implements AuthenticationToken
{

  private static final long serialVersionUID = -784273150987377079L;

  private final Object principal;

  private final PublicKey key;

  public PublicKeyAuthenticationToken(Object principal, PublicKey key) {
    this.principal = principal;
    this.key = key;
  }

  public Object getPrincipal() {
    return principal;
  }

  public PublicKey getCredentials() {
    return key;
  }
}
