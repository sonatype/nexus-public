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

import org.sonatype.nexus.common.entity.Entity;

import com.google.common.base.Throwables;

/**
 * HTTP-client configuration.
 *
 * @since 3.0
 */
public class HttpClientConfiguration
    extends Entity
    implements Cloneable
{
  @Valid
  @Nullable
  private ConnectionConfiguration connection;

  @Valid
  @Nullable
  private ProxyConfiguration proxy;

  /**
   * @see AuthenticationConfigurationDeserializer
   */
  @Valid
  @Nullable
  private AuthenticationConfiguration authentication;

  @Nullable
  public ConnectionConfiguration getConnection() {
    return connection;
  }

  public void setConnection(@Nullable final ConnectionConfiguration connection) {
    this.connection = connection;
  }

  @Nullable
  public ProxyConfiguration getProxy() {
    return proxy;
  }

  public void setProxy(@Nullable final ProxyConfiguration proxy) {
    this.proxy = proxy;
  }

  @Nullable
  public AuthenticationConfiguration getAuthentication() {
    return authentication;
  }

  public void setAuthentication(@Nullable final AuthenticationConfiguration authentication) {
    this.authentication = authentication;
  }

  public HttpClientConfiguration copy() {
    try {
      HttpClientConfiguration copy = (HttpClientConfiguration) clone();
      if (connection != null) {
        copy.connection = connection.copy();
      }
      if (proxy != null) {
        copy.proxy = proxy.copy();
      }
      if (authentication != null) {
        copy.authentication = authentication.copy();
      }
      return copy;
    }
    catch (CloneNotSupportedException e) {
      throw Throwables.propagate(e);
    }
  }

  @Override
  public String toString() {
    return getClass().getSimpleName() + "{" +
        "connection=" + connection +
        ", proxy=" + proxy +
        ", authentication=" + authentication +
        '}';
  }
}
