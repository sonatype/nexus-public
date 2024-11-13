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
package org.sonatype.nexus.security.token;

import javax.inject.Inject;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.security.SecurityHelper;
import org.sonatype.nexus.security.authc.apikey.ApiKey;
import org.sonatype.nexus.security.authc.apikey.ApiKeyService;

import org.apache.shiro.subject.PrincipalCollection;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Allows the logic for managing bearer tokens to be shared between formats and it is intended that this will be
 * subclassed and a format-specific concrete implementation provided.
 *
 * @since 3.6
 */
public abstract class BearerTokenManager
    extends ComponentSupport
{
  protected final ApiKeyService apiKeyService;

  protected final SecurityHelper securityHelper;

  private final String format;

  @Inject
  public BearerTokenManager(final ApiKeyService apiKeyService,
                            final SecurityHelper securityHelper,
                            final String format)
  {
    this.apiKeyService = checkNotNull(apiKeyService);
    this.securityHelper = checkNotNull(securityHelper);
    this.format = checkNotNull(format);
  }

  /**
   * Creates (if not already exists) a bearer token mapped to given principal and returns the newly created token.
   */
  protected String createToken(final PrincipalCollection principals) {
    checkNotNull(principals);
    char[] apiKey = apiKeyService.getApiKey(format, principals).map(ApiKey::getApiKey).orElse(null);
    if (apiKey != null) {
      return format + "." + new String(apiKey);
    }
    return format + "." + new String(apiKeyService.createApiKey(format, principals));
  }

  /**
   * Removes any Bearer token for current user, if exists, and returns {@code true}.
   */
  public boolean deleteToken() {
    final PrincipalCollection principals = securityHelper.subject().getPrincipals();
    if (apiKeyService.getApiKey(format, principals).isPresent()) {
      apiKeyService.deleteApiKey(format, securityHelper.subject().getPrincipals());
      return true;
    }
    return false;
  }
}
