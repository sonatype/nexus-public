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
package org.sonatype.nexus.repository.site.plugin.config;

import java.util.List;
import java.util.Map;

import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.repository.site.plugin.SiteRepository;
import org.sonatype.security.model.CPrivilege;
import org.sonatype.security.model.CProperty;
import org.sonatype.security.model.CRole;
import org.sonatype.security.model.CUserRoleMapping;
import org.sonatype.security.model.Configuration;
import org.sonatype.security.realms.tools.SecurityConfigurationModifier;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

/**
 * Modifies security.xml if needed:
 * - changes roles from "maven-site-*" to "site-*"
 * - changes privileges from "maven-site-*" to "site-*"
 *
 * @since site-repository 1.0
 */
@Named
@Singleton
public class SiteRepositorySecurityConfigurationModifier
    implements SecurityConfigurationModifier
{

  private Map<String, String> roleMappings;

  private Map<String, String> privilegesMappings;

  public SiteRepositorySecurityConfigurationModifier() {
    roleMappings = Maps.newHashMap();
    roleMappings.put("maven-site-all-read", "site-all-read");
    roleMappings.put("maven-site-all-full", "site-all-full");
    roleMappings.put("maven-site-all-view", "site-all-view");

    privilegesMappings = Maps.newHashMap();
    privilegesMappings.put("maven-site-create", "site-create");
    privilegesMappings.put("maven-site-read", "site-read");
    privilegesMappings.put("maven-site-update", "site-update");
    privilegesMappings.put("maven-site-delete", "site-delete");
  }

  @Override
  public boolean apply(final Configuration configuration) {
    boolean modified = false;
    final List<CRole> roles = configuration.getRoles();
    if (roles != null && roles.size() > 0) {
      for (final CRole role : roles) {
        final List<String> roleRoles = role.getRoles();
        if (roleRoles != null && roleRoles.size() > 0) {
          final List<String> newRoles = Lists.newArrayList();
          for (final String roleRole : roleRoles) {
            final String newRole = roleMappings.get(roleRole);
            if (newRole != null) {
              newRoles.add(newRole);
              modified = true;
            }
            else {
              newRoles.add(roleRole);
            }
          }
          role.getRoles().clear();
          role.getRoles().addAll(newRoles);
        }
        final List<String> rolePrivileges = role.getPrivileges();
        if (rolePrivileges != null && rolePrivileges.size() > 0) {
          final List<String> newPrivileges = Lists.newArrayList();
          for (final String rolePrivilege : rolePrivileges) {
            final String newPrivilege = privilegesMappings.get(rolePrivilege);
            if (newPrivilege != null) {
              newPrivileges.add(newPrivilege);
              modified = true;
            }
            else {
              newPrivileges.add(rolePrivilege);
            }
          }
          role.getPrivileges().clear();
          role.getPrivileges().addAll(newPrivileges);
        }
      }
    }

    final List<CPrivilege> privileges = configuration.getPrivileges();
    if (privileges != null && privileges.size() > 0) {
      for (final CPrivilege privilege : privileges) {
        final List<CProperty> privilegeProps = privilege.getProperties();
        if (privilegeProps != null && privilegeProps.size() > 0) {
          for (final CProperty privilegeProp : privilegeProps) {
            if ("maven-site".equals(privilegeProp.getValue())) {
              privilegeProp.setValue(SiteRepository.ID);
              modified = true;
            }
          }
        }
      }
    }

    final List<CUserRoleMapping> userRoleMappings = configuration.getUserRoleMappings();
    if (userRoleMappings != null && userRoleMappings.size() > 0) {
      for (final CUserRoleMapping userRoleMapping : userRoleMappings) {
        final List<String> userRoleMappingRoles = userRoleMapping.getRoles();
        if (userRoleMappingRoles != null && userRoleMappingRoles.size() > 0) {
          final List<String> newRoles = Lists.newArrayList();
          for (final String userRoleMappingRole : userRoleMappingRoles) {
            final String newRole = roleMappings.get(userRoleMappingRole);
            if (newRole != null) {
              newRoles.add(newRole);
              modified = true;
            }
            else {
              newRoles.add(userRoleMappingRole);
            }
          }
          userRoleMapping.getRoles().clear();
          userRoleMapping.getRoles().addAll(newRoles);
        }
      }
    }

    return modified;
  }

}
