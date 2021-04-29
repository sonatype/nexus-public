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
package org.sonatype.nexus.internal.security;

import java.util.Arrays;

import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.security.Roles;
import org.sonatype.nexus.security.config.MemorySecurityConfiguration;
import org.sonatype.nexus.security.config.SecurityContributor;
import org.sonatype.nexus.security.config.memory.MemoryCPrivilege;
import org.sonatype.nexus.security.config.memory.MemoryCPrivilege.MemoryCPrivilegeBuilder;
import org.sonatype.nexus.security.config.memory.MemoryCRole;

/**
 * Default Nexus security configuration.
 *
 * @since 3.0
 */
@Named
@Singleton
public class NexusSecurityContributor
    implements SecurityContributor
{
  private static final String SETTINGS_DOMAIN = "settings";

  private static final String BUNDLES_DOMAIN = "bundles";

  private static final String PRIVILEGES_DOMAIN = "privileges";

  private static final String ROLES_DOMAIN = "roles";

  private static final String USERS_DOMAIN = "users";

  private static final String CREATE_ACTION = "create,read";

  private static final String CREATE_ONLY_ACTION = "create";

  private static final String READ_ACTION = "read";

  private static final String UPDATE_ACTION = "update,read";

  private static final String DELETE_ACTION = "delete,read";

  private static final String ALL = "*";

  @Override
  public MemorySecurityConfiguration getContribution() {
    MemorySecurityConfiguration configuration = new MemorySecurityConfiguration();

    configuration.addPrivilege(createWildcardPriv("nx-all", "All permissions", "nexus:*"));

    configuration.addPrivilege(createAppPriv("nx-settings-all", "All permissions for Settings", SETTINGS_DOMAIN, ALL));
    configuration
        .addPrivilege(createAppPriv("nx-settings-read", "Read permission for Settings", SETTINGS_DOMAIN, READ_ACTION));
    configuration
        .addPrivilege(
            createAppPriv("nx-settings-update", "Update permission for Settings", SETTINGS_DOMAIN, UPDATE_ACTION));

    configuration.addPrivilege(createAppPriv("nx-bundles-all", "All permissions for Bundles", BUNDLES_DOMAIN, ALL));
    configuration
        .addPrivilege(createAppPriv("nx-bundles-read", "Read permission for Bundles", BUNDLES_DOMAIN, READ_ACTION));

    configuration.addPrivilege(createAppPriv("nx-search-read", "Read permission for Search", "search", READ_ACTION));

    configuration.addPrivilege(createAppPriv("nx-apikey-all", "All permissions for APIKey", "apikey", ALL));

    configuration
        .addPrivilege(createAppPriv("nx-privileges-all", "All permissions for Privileges", PRIVILEGES_DOMAIN, ALL));
    configuration.addPrivilege(
        createAppPriv("nx-privileges-create", "Create permission for Privileges", PRIVILEGES_DOMAIN, CREATE_ACTION));
    configuration
        .addPrivilege(
            createAppPriv("nx-privileges-read", "Read permission for Privileges", PRIVILEGES_DOMAIN, READ_ACTION));
    configuration.addPrivilege(
        createAppPriv("nx-privileges-update", "Update permission for Privileges", PRIVILEGES_DOMAIN, UPDATE_ACTION));
    configuration.addPrivilege(
        createAppPriv("nx-privileges-delete", "Delete permission for Privileges", PRIVILEGES_DOMAIN, DELETE_ACTION));

    configuration.addPrivilege(createAppPriv("nx-roles-all", "All permissions for Roles", ROLES_DOMAIN, ALL));
    configuration
        .addPrivilege(createAppPriv("nx-roles-create", "Create permission for Roles", ROLES_DOMAIN, CREATE_ACTION));
    configuration.addPrivilege(createAppPriv("nx-roles-read", "Read permission for Roles", ROLES_DOMAIN, READ_ACTION));
    configuration
        .addPrivilege(createAppPriv("nx-roles-update", "Update permission for Roles", ROLES_DOMAIN, UPDATE_ACTION));
    configuration
        .addPrivilege(createAppPriv("nx-roles-delete", "Delete permission for Roles", ROLES_DOMAIN, DELETE_ACTION));

    configuration.addPrivilege(createAppPriv("nx-users-all", "All permissions for Users", USERS_DOMAIN, ALL));
    configuration
        .addPrivilege(createAppPriv("nx-users-create", "Create permission for Users", USERS_DOMAIN, CREATE_ACTION));
    configuration.addPrivilege(createAppPriv("nx-users-read", "Read permission for Users", USERS_DOMAIN, READ_ACTION));
    configuration
        .addPrivilege(createAppPriv("nx-users-update", "Update permission for Users", USERS_DOMAIN, UPDATE_ACTION));
    configuration
        .addPrivilege(createAppPriv("nx-users-delete", "Delete permission for Users", USERS_DOMAIN, DELETE_ACTION));
    
    // FIXME: Sort out what the use-case is for this distinct permission, consider nexus:users:change-password?
    configuration
        .addPrivilege(createAppPriv("nx-userschangepw", "Change password permission", "userschangepw", CREATE_ACTION));

    configuration
        .addPrivilege(
            createAppPriv("nx-component-upload", "Upload component permission", "component", CREATE_ONLY_ACTION));

    configuration.addRole(createRole(Roles.ADMIN_ROLE_ID, "Administrator Role", "nx-all"));
    configuration.addRole(createRole(Roles.ANONYMOUS_ROLE_ID, "Anonymous Role", "nx-search-read", "nx-healthcheck-read",
        "nx-repository-view-*-*-browse", "nx-repository-view-*-*-read"));

    return configuration;
  }

  private MemoryCPrivilege createWildcardPriv(final String id, final String description, final String pattern) {
    return new MemoryCPrivilegeBuilder(id).description(description).type("wildcard").property("pattern", pattern)
        .build();
  }

  private MemoryCPrivilege createAppPriv(
      final String id,
      final String description,
      final String domain,
      final String actions)
  {
    return new MemoryCPrivilegeBuilder(id).description(description).type("application").property("domain", domain)
        .property("actions", actions).build();
  }

  private MemoryCRole createRole(final String id, final String description, final String... privileges) {
    MemoryCRole role = new MemoryCRole();
    role.setId(id);
    role.setDescription(description);
    Arrays.stream(privileges).forEach(role::addPrivilege);
    return role;
  }
}
