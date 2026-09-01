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
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

import org.springframework.beans.factory.annotation.Autowired;
import org.sonatype.nexus.plugins.defaultrole.DefaultRoleRealm;
import org.sonatype.nexus.rapture.StateContributor;
import org.sonatype.nexus.security.SecuritySystem;
import org.sonatype.nexus.security.realm.RealmManager;
import org.sonatype.nexus.security.role.Role;

import org.apache.shiro.SecurityUtils;
import org.apache.shiro.subject.Subject;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.sonatype.nexus.security.user.UserManager.DEFAULT_SOURCE;

@Primary
@Component
public class DefaultRoleStateContributor
    implements StateContributor
{
  protected final Logger log = LoggerFactory.getLogger(getClass());

  private final DefaultRoleRealm defaultRoleRealm;

  private final SecuritySystem securitySystem;

  private final RealmManager realmManager;

  /**
   * NEXUS-53915: {@link #getState()} runs on every application-state poll (every authenticated UI session polls
   * every few seconds). Remember the last missing role we warned about so the WARN is emitted only when the
   * misconfiguration first appears or changes, rather than on every poll. Reset when the configuration is healthy
   * so a subsequent breakage is logged again.
   */
  private final AtomicReference<String> lastWarnedMissingRole = new AtomicReference<>();

  @Autowired
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
        Role matched = securitySystem.listRoles(DEFAULT_SOURCE)
            .stream()
            .filter(role -> role.getRoleId().equals(defaultRoleRealm.getRole()))
            .findFirst()
            .orElse(null);
        if (matched == null) {
          // NEXUS-53915: the configured default role was removed. Previously this dereferenced null and threw an
          // NPE that was swallowed at DEBUG, leaving no trace at default log levels. Surface it as a WARN and
          // return a clean empty state so the UI simply omits the default-role indicator. Because this method is
          // polled every few seconds by every authenticated UI session, only log when the missing role first
          // appears or changes to avoid flooding the log with an identical WARN on every poll.
          String missingRole = defaultRoleRealm.getRole();
          if (!Objects.equals(missingRole, lastWarnedMissingRole.getAndSet(missingRole))) {
            log.warn("Configured default role '{}' does not exist; omitting default role from application state",
                missingRole);
          }
          return Collections.emptyMap();
        }
        // Configuration is healthy again; re-arm so a future breakage is logged once more.
        lastWarnedMissingRole.set(null);
        Map<String, Object> defaultRole = new HashMap<>(2);
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
