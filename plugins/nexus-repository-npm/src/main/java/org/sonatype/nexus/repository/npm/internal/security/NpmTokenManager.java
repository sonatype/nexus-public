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
package org.sonatype.nexus.repository.npm.internal.security;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.repository.npm.security.NpmToken;

import org.sonatype.nexus.security.SecurityHelper;
import org.sonatype.nexus.security.authc.apikey.ApiKeyStore;
import org.sonatype.nexus.security.token.BearerTokenManager;

import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.AuthenticationInfo;
import org.apache.shiro.authc.UsernamePasswordToken;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * npm api key manager.
 *
 * @since 3.0
 */
@Named
@Singleton
public class NpmTokenManager
    extends BearerTokenManager
{
  @Inject
  public NpmTokenManager(final ApiKeyStore apiKeyStore, final SecurityHelper securityHelper) {
    super(apiKeyStore, securityHelper, NpmToken.NAME);
  }

  /**
   * Verifies passed in principal/credentials combo, and creates (if not already exists) a npm token mapped to given
   * principal and returns the newly created token.
   */
  public String login(final String username, final String password) {
    checkNotNull(username);
    checkNotNull(password);

    try {
      AuthenticationInfo authenticationInfo = securityHelper.getSecurityManager().authenticate(
          new UsernamePasswordToken(username, password));
      return super.createToken(authenticationInfo.getPrincipals());
    }
    catch (AuthenticationException e) {
      log.debug("Bad credentials provided for npm token creation", e);
      return null;
    }
  }

  /**
   * Removes any npm API Key token for current user, if exists, and returns {@code true}.
   */
  public boolean logout() {
    return super.deleteToken();
  }
}
