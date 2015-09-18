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
package org.sonatype.nexus.coreui

import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton
import javax.validation.Valid
import javax.validation.constraints.NotNull
import javax.validation.groups.Default

import org.sonatype.nexus.extdirect.DirectComponent
import org.sonatype.nexus.extdirect.DirectComponentSupport
import org.sonatype.nexus.security.SecuritySystem
import org.sonatype.nexus.security.authz.AuthorizationManager
import org.sonatype.nexus.security.role.Role
import org.sonatype.nexus.validation.Validate
import org.sonatype.nexus.validation.group.Create
import org.sonatype.nexus.validation.group.Update

import com.softwarementors.extjs.djn.config.annotations.DirectAction
import com.softwarementors.extjs.djn.config.annotations.DirectMethod
import groovy.transform.PackageScope
import org.apache.shiro.authz.annotation.RequiresAuthentication
import org.apache.shiro.authz.annotation.RequiresPermissions
import org.hibernate.validator.constraints.NotEmpty

import static org.sonatype.nexus.security.user.UserManager.DEFAULT_SOURCE

/**
 * Role {@link DirectComponent}.
 *
 * @since 3.0
 */
@Named
@Singleton
@DirectAction(action = 'coreui_Role')
class RoleComponent
    extends DirectComponentSupport
{
  @Inject
  SecuritySystem securitySystem

  @Inject
  List<AuthorizationManager> authorizationManagers

  /**
   * Retrieves roles form all available {@link AuthorizationManager}s.
   * @return a list of roles
   */
  @DirectMethod
  @RequiresPermissions('nexus:roles:read')
  List<RoleXO> read() {
    return securitySystem.listRoles(DEFAULT_SOURCE).collect {input ->
      return convert(input)
    }
  }

  /**
   * Retrieves role references form all available {@link AuthorizationManager}s.
   * @return a list of role references
   */
  @DirectMethod
  @RequiresPermissions('nexus:roles:read')
  List<ReferenceXO> readReferences() {
    return securitySystem.listRoles(DEFAULT_SOURCE).collect {input ->
      return new ReferenceXO(
          id: input.roleId,
          name: input.name
      )
    }
  }

  /**
   * Retrieves available role sources.
   * @return list of sources
   */
  @DirectMethod
  List<ReferenceXO> readSources() {
    return authorizationManagers.findResults {manager ->
      return manager.source == DEFAULT_SOURCE ? null : manager
    }.collect {manager ->
      return new ReferenceXO(
          id: manager.source,
          name: manager.source
      )
    }
  }

  /**
   * Retrieves roles from specified source.
   * @param source to retrieve roles from
   * @return a list of roles
   */
  @DirectMethod
  @RequiresPermissions('nexus:roles:read')
  @Validate
  List<RoleXO> readFromSource(@NotEmpty final String source) {
    return securitySystem.listRoles(source).collect {input ->
      return convert(input)
    }
  }

  /**
   * Creates a role.
   * @param roleXO to be created
   * @return created role
   */
  @DirectMethod
  @RequiresAuthentication
  @RequiresPermissions('nexus:roles:create')
  @Validate(groups = [Create.class, Default.class])
  RoleXO create(@NotNull @Valid final RoleXO roleXO) {
    return convert(securitySystem.getAuthorizationManager(DEFAULT_SOURCE).addRole(
        new Role(
            roleId: roleXO.id,
            source: roleXO.source,
            name: roleXO.name,
            description: roleXO.description,
            readOnly: false,
            privileges: roleXO.privileges,
            roles: roleXO.roles
        )
    ))
  }

  /**
   * Updates a role.
   * @param roleXO to be updated
   * @return updated role
   */
  @DirectMethod
  @RequiresAuthentication
  @RequiresPermissions('nexus:roles:update')
  @Validate(groups = [Update.class, Default.class])
  RoleXO update(@NotNull @Valid final RoleXO roleXO) {
    return convert(securitySystem.getAuthorizationManager(DEFAULT_SOURCE).updateRole(
        new Role(
            roleId: roleXO.id,
            version: roleXO.version,
            source: roleXO.source,
            name: roleXO.name,
            description: roleXO.description,
            readOnly: false,
            privileges: roleXO.privileges,
            roles: roleXO.roles
        )
    ))
  }

  /**
   * Deletes a role.
   * @param id of role to be deleted
   */
  @DirectMethod
  @RequiresAuthentication
  @RequiresPermissions('nexus:roles:delete')
  @Validate
  void remove(@NotEmpty final String id) {
    securitySystem.getAuthorizationManager(DEFAULT_SOURCE).deleteRole(id)
  }

  /**
   * Convert role to XO.
   */
  @PackageScope
  RoleXO convert(final Role input) {
    return new RoleXO(
        id: input.roleId,
        version: input.version,
        source: (input.source == DEFAULT_SOURCE || !input.source) ? 'Nexus' : input.source,
        name: input.name != null ? input.name : input.roleId,
        description: input.description != null ? input.description : input.roleId,
        readOnly: input.readOnly,
        privileges: input.privileges,
        roles: input.roles
    )
  }
}
