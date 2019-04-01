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
package org.sonatype.nexus.repository.security.internal;

import java.util.Optional;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.security.internal.AuthenticatingRealmImpl;
import org.sonatype.nexus.security.realm.RealmManager;

import com.codahale.metrics.health.HealthCheck;
import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.UsernamePasswordToken;
import org.apache.shiro.mgt.RealmSecurityManager;
import org.apache.shiro.realm.Realm;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Check if the default user can be used to authenticate.
 */
@Named("Default Admin Credentials")
@Singleton
public class DefaultUserHealthCheck
    extends HealthCheck
{
  private static final Logger log = LoggerFactory.getLogger(DefaultUserHealthCheck.class);

  static final String ERROR_MESSAGE = "The default admin credentials have not been changed. It is strongly recommended that the default admin password be changed.";

  private final RealmManager realmManager;

  private final RealmSecurityManager realmSecurityManager;

  @Inject
  public DefaultUserHealthCheck(final RealmManager realmManager, final RealmSecurityManager realmSecurityManager)
  {
    this.realmManager = checkNotNull(realmManager);
    this.realmSecurityManager = checkNotNull(realmSecurityManager);
  }

  @Override
  protected Result check() {
    if (!realmManager.isRealmEnabled(AuthenticatingRealmImpl.NAME)) {
      return Result.healthy();
    }

    Optional<Realm> realm = realmSecurityManager.getRealms().stream()
        .filter(r -> r.getName().equals(AuthenticatingRealmImpl.NAME)).findFirst();

    try {
      if (realm.map(r -> r.getAuthenticationInfo(new UsernamePasswordToken("admin", "admin123"))).isPresent()) {
        return Result.unhealthy(ERROR_MESSAGE);
      }
    }
    catch (AuthenticationException e) {
      log.trace("Unable to locate admin/admin123 user", e);
    }
    return Result.healthy();
  }
}
