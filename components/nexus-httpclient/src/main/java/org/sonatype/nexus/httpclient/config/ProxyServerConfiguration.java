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

import javax.annotation.Nullable;
import javax.validation.Valid;

import org.sonatype.nexus.validation.constraint.PortNumber;

import javax.validation.constraints.NotBlank;

/**
 * Proxy-server configuration.
 *
 * @since 3.0
 */
public class ProxyServerConfiguration
    implements Cloneable
{
  private boolean enabled;

  @NotBlank
  private String host;

  @PortNumber
  private int port;

  /**
   * @see AuthenticationConfigurationDeserializer
   */
  @Valid
  @Nullable
  private AuthenticationConfiguration authentication;

  public boolean isEnabled() {
    return enabled;
  }

  public void setEnabled(final boolean enabled) {
    this.enabled = enabled;
  }

  public String getHost() {
    return host;
  }

  public void setHost(final String host) {
    this.host = host;
  }

  public int getPort() {
    return port;
  }

  public void setPort(final int port) {
    this.port = port;
  }

  @Nullable
  public AuthenticationConfiguration getAuthentication() {
    return authentication;
  }

  public void setAuthentication(@Nullable final AuthenticationConfiguration authentication) {
    this.authentication = authentication;
  }

  public ProxyServerConfiguration copy() {
    try {
      ProxyServerConfiguration copy = (ProxyServerConfiguration) clone();
      if (authentication != null) {
        copy.authentication = authentication.copy();
      }
      return copy;
    }
    catch (CloneNotSupportedException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public String toString() {
    return getClass().getSimpleName() + "{" +
        "enabled=" + enabled +
        ", host='" + host + '\'' +
        ", port=" + port +
        ", authentication=" + authentication +
        '}';
  }
}
