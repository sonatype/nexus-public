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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.plugins.defaultrole.DefaultRoleRealm;
import org.sonatype.nexus.rapture.StateContributor;
import org.sonatype.nexus.security.SecuritySystem;
import org.sonatype.nexus.security.realm.RealmManager;
import org.sonatype.nexus.security.role.Role;

import org.apache.shiro.SecurityUtils;
import org.apache.shiro.subject.Subject;

import static org.sonatype.nexus.security.user.UserManager.DEFAULT_SOURCE;

@Named
@Singleton
public class DefaultRoleStateContributor
    extends ComponentSupport
    implements StateContributor
{

  private final DefaultRoleRealm defaultRoleRealm;

  private final SecuritySystem securitySystem;

  private final RealmManager realmManager;

  @Inject
  public DefaultRoleStateContributor(
      final DefaultRoleRealm defaultRoleRealm,
      final SecuritySystem securitySystem,
      final RealmManager realmManager)
  {
    this.defaultRoleRealm = defaultRoleRealm;
    this.securitySystem = securitySystem;
    this.realmManager = realmManager;
  }

  @Override
  public Map<String, Object> getState() {
    Subject subject = SecurityUtils.getSubject();
    if (realmManager.isRealmEnabled(DefaultRoleRealm.NAME) && subject != null
        && (subject.isAuthenticated() || subject.isRemembered())) {
      try {
        Map<String, Object> defaultRole = new HashMap<>(2);
        Role matched = securitySystem.listRoles(DEFAULT_SOURCE)
            .stream()
            .filter(role -> role.getRoleId().equals(defaultRoleRealm.getRole()))
            .findFirst()
            .orElse(null);
        defaultRole.put("id", matched.getRoleId());
        defaultRole.put("name", matched.getName());
        return Collections.singletonMap("defaultRole", defaultRole);
      }
      catch (Exception e) {
        log.debug("Unable to fetch default role configuration", e);
      }
    }

    return Collections.emptyMap();
  }
}
