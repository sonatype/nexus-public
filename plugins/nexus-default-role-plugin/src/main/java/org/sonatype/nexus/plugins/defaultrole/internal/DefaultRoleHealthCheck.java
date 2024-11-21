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
package org.sonatype.nexus.plugins.defaultrole.internal;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.plugins.defaultrole.DefaultRoleRealm;
import org.sonatype.nexus.security.SecuritySystem;
import org.sonatype.nexus.security.realm.RealmManager;
import org.sonatype.nexus.security.role.Role;

import com.codahale.metrics.health.HealthCheck;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.sonatype.nexus.security.user.UserManager.DEFAULT_SOURCE;

/**
 * Will throw up a health check error when the default role in the realm isn't available
 *
 * @since 3.22
 */
@Named("DefaultRoleRealm")
@Singleton
public class DefaultRoleHealthCheck
    extends HealthCheck
{
  private final RealmManager realmManager;

  private final DefaultRoleRealm defaultRoleRealm;

  private final SecuritySystem securitySystem;

  @Inject
  public DefaultRoleHealthCheck(
      final RealmManager realmManager,
      final DefaultRoleRealm defaultRoleRealm,
      final SecuritySystem securitySystem)
  {
    this.realmManager = checkNotNull(realmManager);
    this.defaultRoleRealm = checkNotNull(defaultRoleRealm);
    this.securitySystem = checkNotNull(securitySystem);
  }

  @Override
  protected Result check() throws Exception {
    if (!realmManager.isRealmEnabled(DefaultRoleRealm.NAME)) {
      return Result.healthy("Default Role Realm not in use.");
    }

    if (defaultRoleRealm.getRole() == null) {
      return Result.unhealthy("Default Role Realm is enabled but not configured.");
    }

    Role matched = securitySystem.listRoles(DEFAULT_SOURCE)
        .stream()
        .filter(role -> role.getRoleId().equals(defaultRoleRealm.getRole()))
        .findFirst()
        .orElse(null);

    if (matched == null) {
      return Result
          .unhealthy("Default Role Realm configured to use role %s which doesn't exist.", defaultRoleRealm.getRole());
    }

    return Result.healthy("Default Role Realm configured to use role %s.", defaultRoleRealm.getRole());
  }
}
