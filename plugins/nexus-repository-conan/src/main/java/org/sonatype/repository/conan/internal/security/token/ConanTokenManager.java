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
package org.sonatype.repository.conan.internal.security.token;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.security.SecurityHelper;
import org.sonatype.nexus.security.authc.apikey.ApiKeyStore;
import org.sonatype.nexus.security.token.BearerTokenManager;

import org.apache.shiro.subject.PrincipalCollection;
import org.apache.shiro.subject.Subject;

import static org.sonatype.nexus.security.anonymous.AnonymousHelper.isAnonymous;

/**
 * @since 3.next
 */
@Named
@Singleton
public class ConanTokenManager
    extends BearerTokenManager
{
  @Inject
  public ConanTokenManager(final ApiKeyStore apiKeyStore,
                           final SecurityHelper securityHelper)
  {
    super(apiKeyStore, securityHelper, ConanToken.NAME);
  }

  /**
   * Verifies passed in principal/credentials combo, and creates (if not already exists) a npm token mapped to given
   * principal and returns the newly created token.
   */
  public String login() {
    Subject subject = securityHelper.subject();
    boolean authenticated = subject.getPrincipal() != null && subject.isAuthenticated();
    if (authenticated || isAnonymous(subject)) {
      PrincipalCollection principals = subject.getPrincipals();
      return super.createToken(principals);
    }
    return null;
  }

  public String user() {
    Subject subject = securityHelper.subject();
    boolean authenticated = subject.getPrincipal() != null && subject.isAuthenticated();
    if (authenticated) {
      return subject.getPrincipals().getPrimaryPrincipal().toString();
    }
    return null;
  }
}
