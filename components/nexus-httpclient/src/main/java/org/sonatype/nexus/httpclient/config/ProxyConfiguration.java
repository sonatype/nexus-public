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

import java.util.Arrays;

import javax.annotation.Nullable;
import javax.validation.Valid;

/**
 * Proxy configuration.
 *
 * @since 3.0
 */
public class ProxyConfiguration
    implements Cloneable
{
  @Valid
  @Nullable
  private ProxyServerConfiguration http;

  @Valid
  @Nullable
  private ProxyServerConfiguration https;

  @NonProxyHosts
  @Nullable
  private String[] nonProxyHosts;

  @Nullable
  public ProxyServerConfiguration getHttp() {
    return http;
  }

  public void setHttp(@Nullable final ProxyServerConfiguration http) {
    this.http = http;
  }

  @Nullable
  public ProxyServerConfiguration getHttps() {
    return https;
  }

  public void setHttps(@Nullable final ProxyServerConfiguration https) {
    this.https = https;
  }

  @Nullable
  public String[] getNonProxyHosts() {
    return nonProxyHosts;
  }

  public void setNonProxyHosts(@Nullable final String[] nonProxyHosts) {
    this.nonProxyHosts = nonProxyHosts;
  }

  public ProxyConfiguration copy() {
    try {
      ProxyConfiguration copy = (ProxyConfiguration) clone();
      if (http != null) {
        copy.http = http.copy();
      }
      if (https != null) {
        copy.https = https.copy();
      }
      if (nonProxyHosts != null) {
        copy.nonProxyHosts = nonProxyHosts.clone();
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
        "http=" + http +
        ", https=" + https +
        ", nonProxyHosts=" + Arrays.toString(nonProxyHosts) +
        '}';
  }
}
