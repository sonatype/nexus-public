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
package org.sonatype.nexus.httpclient.config;

import javax.validation.constraints.NotNull;

import org.sonatype.nexus.common.text.Strings2;
import org.sonatype.nexus.crypto.secrets.Secret;

/**
 * Bearer Token authentication configuration
 *
 * @since 3.20
 */
public class BearerTokenAuthenticationConfiguration
  extends AuthenticationConfiguration
{
  public static final String TYPE = "bearerToken";

  @NotNull
  private String bearerToken;

  public BearerTokenAuthenticationConfiguration() {
    super(TYPE);
  }

  public String getBearerToken() {
    return bearerToken;
  }

  public void setBearerToken(final String bearerToken) {
    this.bearerToken = bearerToken;
  }

  @Override
  public Secret getSecret() {
    return null;
  }

  @Override
  public String toString() {
    return getClass().getSimpleName() + "{" +
        "bearerToken='" + Strings2.MASK + '\'' +
        '}';
  }
}
