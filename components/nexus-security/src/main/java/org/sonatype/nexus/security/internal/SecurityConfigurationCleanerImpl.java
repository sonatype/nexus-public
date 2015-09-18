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
package org.sonatype.nexus.security.internal;

import java.util.ConcurrentModificationException;

import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.security.config.CRole;
import org.sonatype.nexus.security.config.CUserRoleMapping;
import org.sonatype.nexus.security.config.SecurityConfiguration;
import org.sonatype.nexus.security.config.SecurityConfigurationCleaner;
import org.sonatype.nexus.security.role.NoSuchRoleException;
import org.sonatype.nexus.security.user.NoSuchRoleMappingException;

/**
 * Default {@link SecurityConfigurationCleaner}.
 *
 * Removes dead references to roles and permissions in the security model.
 *
 * When a permission is removed all roles will be updated so the permission reference can removed.
 *
 * When a Role is removed references are removed from other roles and users.
 */
@Named
@Singleton
public class SecurityConfigurationCleanerImpl
    extends ComponentSupport
    implements SecurityConfigurationCleaner
{
  public void privilegeRemoved(SecurityConfiguration configuration, String privilegeId) {
    log.debug("Cleaning privilege id {} from roles.", privilegeId);
    for (CRole role : configuration.getRoles()) {
      boolean concurrentlyUpdated;
      do {
        concurrentlyUpdated = false;
        CRole currentRole = configuration.getRole(role.getId());
        if (currentRole != null && currentRole.getPrivileges().contains(privilegeId)) {
          log.debug("removing privilege {} from role {}", privilegeId, currentRole.getId());
          currentRole.removePrivilege(privilegeId);
          try {
            configuration.updateRole(currentRole);
          }
          catch (NoSuchRoleException e) {
            // role was removed in the meantime
          }
          catch (ConcurrentModificationException e) {
            concurrentlyUpdated = true;
          }
        }
      }
      while (concurrentlyUpdated);
    }
  }

  public void roleRemoved(SecurityConfiguration configuration, String roleId) {
    log.debug("Cleaning role id {} from users and roles.", roleId);
    for (CRole role : configuration.getRoles()) {
      boolean concurrentlyUpdated;
      do {
        concurrentlyUpdated = false;
        CRole currentRole = configuration.getRole(role.getId());
        if (currentRole != null && currentRole.getRoles().contains(roleId)) {
          log.debug("removing ref to role {} from role {}", roleId, currentRole.getId());
          currentRole.removeRole(roleId);
          try {
            configuration.updateRole(currentRole);
          }
          catch (NoSuchRoleException e) {
            // role was removed in the meantime
          }
          catch (ConcurrentModificationException e) {
            concurrentlyUpdated = true;
          }
        }
      }
      while (concurrentlyUpdated);
    }

    for (CUserRoleMapping mapping : configuration.getUserRoleMappings()) {
      boolean concurrentlyUpdated;
      do {
        concurrentlyUpdated = false;
        CUserRoleMapping currentMapping = configuration.getUserRoleMapping(mapping.getUserId(), mapping.getSource());
        if (currentMapping != null && currentMapping.getRoles().contains(roleId)) {
          log.debug("removing ref to role {} from user {}", currentMapping.getUserId());
          currentMapping.removeRole(roleId);
          try {
            configuration.updateUserRoleMapping(currentMapping);
          }
          catch (NoSuchRoleMappingException e) {
            // mapping was removed in the meantime
          }
          catch (ConcurrentModificationException e) {
            concurrentlyUpdated = true;
          }
        }
      }
      while (concurrentlyUpdated);
    }
  }
}
