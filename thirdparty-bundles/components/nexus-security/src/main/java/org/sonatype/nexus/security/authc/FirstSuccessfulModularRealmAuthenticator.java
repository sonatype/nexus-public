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
package org.sonatype.nexus.security.authc;

import java.util.Collection;

import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.AuthenticationInfo;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.authc.pam.ModularRealmAuthenticator;
import org.apache.shiro.realm.Realm;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This Authenticator will only try to authenticate with each realm.
 *
 * The first successful AuthenticationInfo found will be returned and other realms will not be queried.
 *
 * @see ModularRealmAuthenticator
 */
public class FirstSuccessfulModularRealmAuthenticator
    extends ModularRealmAuthenticator
{
  private static final Logger log = LoggerFactory.getLogger(FirstSuccessfulModularRealmAuthenticator.class);

  @Override
  protected AuthenticationInfo doMultiRealmAuthentication(final Collection<Realm> realms,
                                                          final AuthenticationToken token)
  {
    log.trace("Iterating through [{}] realms for PAM authentication", realms.size());

    for (Realm realm : realms) {
      // check if the realm supports this token
      if (realm.supports(token)) {
        log.trace("Attempting to authenticate token [{}] using realm of type [{}]", token, realm);

        try {
          AuthenticationInfo info = realm.getAuthenticationInfo(token);
          if (info != null) {
            return info;
          }

          log.trace("Realm [{}] returned null when authenticating token [{}]", realm, token);
        }
        catch (Throwable t) {
          log.trace("Realm [{}] threw an exception during a multi-realm authentication attempt", realm, t);
        }
      }
      else {
        log.trace("Realm of type [{}] does not support token [{}]; skipping realm", realm, token);
      }
    }

    throw new AuthenticationException("Authentication token of type [" + token.getClass()
        + "] could not be authenticated by any configured realms.  Please ensure that at least one realm can "
        + "authenticate these tokens.");
  }
}
