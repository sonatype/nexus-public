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
package org.sonatype.security.realms.tools;

import java.util.List;

import javax.enterprise.inject.Typed;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.security.model.CRole;
import org.sonatype.security.model.CUserRoleMapping;
import org.sonatype.sisu.goodies.common.ComponentSupport;

/**
 * Removes dead references to roles and permissions in the security model. When a permission is removed all roles will
 * be updated so the permission reference can removed. When a Role is removed references are removed from other roles
 * and users.
 *
 * @author Brian Demers
 */
@Singleton
@Typed(SecurityConfigurationCleaner.class)
@Named("default")
public class DefaultSecurityConfigurationCleaner
    extends ComponentSupport
    implements SecurityConfigurationCleaner
{
  public void privilegeRemoved(EnhancedConfiguration configuration, String privilegeId) {
    log.debug("Cleaning privilege id {} from roles.", privilegeId);
    List<CRole> roles = configuration.getRoles();

    for (CRole role : roles) {
      if (role.getPrivileges().contains(privilegeId)) {
        log.debug("removing privilege {} from role {}", privilegeId, role.getId());
        role.getPrivileges().remove(privilegeId);
        configuration.removeRoleById(role.getId());
        configuration.addRole(role);
      }
    }
  }

  public void roleRemoved(EnhancedConfiguration configuration, String roleId) {
    log.debug("Cleaning role id {} from users and roles.", roleId);
    List<CRole> roles = configuration.getRoles();

    for (CRole role : roles) {
      if (role.getRoles().contains(roleId)) {
        log.debug("removing ref to role {} from role {}", roleId, role.getId());
        role.getRoles().remove(roleId);
        configuration.removeRoleById(role.getId());
        configuration.addRole(role);
      }
    }

    List<CUserRoleMapping> mappings = configuration.getUserRoleMappings();

    for (CUserRoleMapping mapping : mappings) {
      if (mapping.getRoles().contains(roleId)) {
        log.debug("removing ref to role {} from user {}", mapping.getUserId());
        mapping.removeRole(roleId);
        configuration.removeUserRoleMappingByUserId(mapping.getUserId(), mapping.getSource());
        configuration.addUserRoleMapping(mapping);
      }
    }
  }
}
