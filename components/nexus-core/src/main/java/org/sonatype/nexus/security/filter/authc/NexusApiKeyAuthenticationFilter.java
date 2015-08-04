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
package org.sonatype.nexus.security.filter.authc;

import java.util.Collections;
import java.util.Map;

import javax.inject.Inject;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;

import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.web.filter.authc.AuthenticatingFilter;
import org.apache.shiro.web.util.WebUtils;

/**
 * {@link AuthenticatingFilter} that looks for credentials in known {@link NexusApiKey} HTTP headers.
 */
public class NexusApiKeyAuthenticationFilter
    extends NexusHttpAuthenticationFilter
{
  @Inject
  private Map<String, NexusApiKey> apiKeys = Collections.emptyMap();

  @Override
  protected boolean isLoginAttempt(ServletRequest request, ServletResponse response) {
    final HttpServletRequest http = WebUtils.toHttp(request);
    for (final String key : apiKeys.keySet()) {
      if (null != http.getHeader(key)) {
        return true;
      }
    }
    return super.isLoginAttempt(request, response);
  }

  @Override
  protected AuthenticationToken createToken(final ServletRequest request, final ServletResponse response) {
    final HttpServletRequest http = WebUtils.toHttp(request);
    for (final String key : apiKeys.keySet()) {
      final String token = http.getHeader(key);
      if (null != token) {
        return new NexusApiKeyAuthenticationToken(key, token.toCharArray(), request.getRemoteHost());
      }
    }
    return super.createToken(request, response);
  }
}
