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

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;
import javax.servlet.http.HttpServletRequest;

import org.sonatype.nexus.security.ClientInfo;
import org.sonatype.nexus.security.ClientInfoProvider;

import com.google.common.net.HttpHeaders;
import com.google.inject.OutOfScopeException;
import com.google.inject.ProvisionException;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.subject.Subject;

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

  @Inject
  public ClientInfoProviderImpl(final Provider<HttpServletRequest> httpRequestProvider) {
    this.httpRequestProvider = checkNotNull(httpRequestProvider);
  }

  @Override
  public ClientInfo getCurrentThreadClientInfo() {
    try {
      HttpServletRequest request = httpRequestProvider.get();
      Subject subject = SecurityUtils.getSubject();
      return new ClientInfo(
          subject != null && subject.getPrincipal() != null ? subject.getPrincipal().toString() : null,
          request.getRemoteAddr(),
          request.getHeader(HttpHeaders.USER_AGENT)
      );
    }
    catch (ProvisionException | OutOfScopeException e) {
      return null;
    }
  }

}
