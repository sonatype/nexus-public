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
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;
import javax.servlet.http.HttpServletRequest;

import org.sonatype.nexus.security.ClientInfo;
import org.sonatype.nexus.security.ClientInfoProvider;
import org.sonatype.nexus.security.UserIdHelper;

import com.google.common.net.HttpHeaders;
import com.google.inject.OutOfScopeException;
import com.google.inject.ProvisionException;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Default {@link ClientInfoProvider}
 *
 * @since 3.0
 */
@Named
@Singleton
public class ClientInfoProviderImpl
    implements ClientInfoProvider
{
  private final Provider<HttpServletRequest> httpRequestProvider;

  private final ThreadLocal<String> remoteIp = new ThreadLocal<>();

  private final ThreadLocal<String> userId = new ThreadLocal<>();

  @Inject
  public ClientInfoProviderImpl(final Provider<HttpServletRequest> httpRequestProvider) {
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
    catch (ProvisionException | OutOfScopeException e) {
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
