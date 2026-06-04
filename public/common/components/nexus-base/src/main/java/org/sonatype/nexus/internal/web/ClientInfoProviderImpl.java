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
package org.sonatype.nexus.internal.web;

import javax.annotation.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import jakarta.inject.Provider;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Context;

import org.sonatype.nexus.security.ClientInfo;
import org.sonatype.nexus.security.ClientInfoProvider;
import org.sonatype.nexus.security.UserIdHelper;

import com.google.common.net.HttpHeaders;

import static com.google.common.base.Preconditions.checkNotNull;
import org.springframework.stereotype.Component;

/**
 * Default {@link ClientInfoProvider}
 *
 * @since 3.0
 */
@Component
public class ClientInfoProviderImpl
    implements ClientInfoProvider
{
  private final Provider<HttpServletRequest> httpRequestProvider;

  private final ThreadLocal<String> remoteIp = new ThreadLocal<>();

  private final ThreadLocal<String> userId = new ThreadLocal<>();

  @Autowired
  public ClientInfoProviderImpl(@Context final Provider<HttpServletRequest> httpRequestProvider) {
    this.httpRequestProvider = checkNotNull(httpRequestProvider);
  }

  @Override
  @Nullable
  public ClientInfo getCurrentThreadClientInfo() {
    try {
      HttpServletRequest request = httpRequestProvider.get();
      return ClientInfo
          .builder()
          .userId(UserIdHelper.get())
          .remoteIP(request.getRemoteAddr())
          .userAgent(request.getHeader(HttpHeaders.USER_AGENT))
          .path(request.getServletPath())
          .build();
    }
    // TODO verify request type
    catch (Exception e) {
      /*
       * This happens when called out of scope of http request.
       * Create fake ClientInfo with the custom User Id and Remote address.
       */
      return userId.get() != null && remoteIp.get() != null
          ? ClientInfo
              .builder()
              .userId(userId.get())
              .remoteIP(remoteIp.get())
              .build()
          : null;
    }
  }

  @Override
  public void setClientInfo(final String remoteIp, final String userId) {
    this.remoteIp.set(checkNotNull(remoteIp));
    this.userId.set(checkNotNull(userId));
  }

  @Override
  public void unsetClientInfo() {
    remoteIp.remove();
    userId.remove();
  }
}
