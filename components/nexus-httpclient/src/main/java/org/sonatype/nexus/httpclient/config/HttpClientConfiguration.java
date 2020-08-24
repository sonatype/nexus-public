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

import org.apache.http.client.AuthenticationStrategy;
import org.apache.http.client.RedirectStrategy;

/**
 * HTTP-client configuration.
 *
 * @since 3.0
 */
public interface HttpClientConfiguration
{
  @Nullable
  ConnectionConfiguration getConnection();

  void setConnection(@Nullable final ConnectionConfiguration connection);

  @Nullable
  ProxyConfiguration getProxy();

  void setProxy(@Nullable final ProxyConfiguration proxy);

  @Nullable
  AuthenticationConfiguration getAuthentication();

  void setAuthentication(@Nullable final AuthenticationConfiguration authentication);

  @Nullable
  RedirectStrategy getRedirectStrategy();

  void setRedirectStrategy(@Nullable final RedirectStrategy redirectStrategy);

  @Nullable
  AuthenticationStrategy getAuthenticationStrategy();

  void setAuthenticationStrategy(@Nullable final AuthenticationStrategy authenticationStrategy);

  Boolean getNormalizeUri();

  void setNormalizeUri(final Boolean normalizeUri);

  Boolean getDisableContentCompression();

  void setDisableContentCompression(final Boolean disableContentCompression);

  HttpClientConfiguration copy();
}
