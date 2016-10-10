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
package org.sonatype.nexus.coreui.internal.atlas

import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton
import javax.ws.rs.GET
import javax.ws.rs.NotFoundException
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType

import org.sonatype.goodies.common.ComponentSupport
import org.sonatype.nexus.rest.Resource
import org.sonatype.nexus.security.SecuritySystem
import org.sonatype.nexus.security.privilege.NoSuchPrivilegeException
import org.sonatype.nexus.security.role.NoSuchRoleException
import org.sonatype.nexus.security.user.User
import org.sonatype.nexus.security.user.UserNotFoundException

import org.apache.shiro.authz.annotation.RequiresPermissions

import static com.google.common.base.Preconditions.checkNotNull
import static com.google.common.base.Preconditions.checkState

/**
 * Renders security diagnostic information.
 *
 * @since 2.7
 */
@Named
@Singleton
@Path(SecurityDiagnosticResource.RESOURCE_URI)
@Produces(MediaType.APPLICATION_JSON)
class SecurityDiagnosticResource
    extends ComponentSupport
    implements Resource
{
  static final String RESOURCE_URI = '/atlas/security-diagnostic'

  private final SecuritySystem securitySystem

  @Inject
  SecurityDiagnosticResource(final SecuritySystem securitySystem) {
    this.securitySystem = checkNotNull(securitySystem)
  }

  /**
   * Renders security diagnostic information for a specific user.
   */
  @GET
  @Path('user/{userId}')
  @RequiresPermissions('nexus:atlas:read')
  Map userDiagnostic(final @PathParam('userId') String userId) {
    log.info 'Generating security diagnostics for user: {}', userId

    def authzman = securitySystem.getAuthorizationManager('default')
    checkState(authzman != null, 'Unable to access default authorization manager')

    // convert object into map, sans special properties or excluded keys
    def mappify = { obj, Set excludes = [] ->
      obj.metaPropertyValues
          .findAll { value -> !(value.name in (excludes += ['class', 'metaClass'])) }
          .collectEntries { [it.name, it.value] }
    }

    // add details for a privilege by id
    def explainPrivilege = { data, String id ->
      try {
        def privilege = authzman.getPrivilege(id)
        data[id] = mappify(privilege, ['id'] as Set)
      }
      catch (NoSuchPrivilegeException e) {
        data[id] = "ERROR: Failed to resolve privilege: $id caused by: $e".toString()
      }
    }

    // add details for a role by id (pre-defined to support recursion)
    def explainRole
    explainRole = { data, String id ->
      def role
      try {
        role = authzman.getRole(id)
      }
      catch (NoSuchRoleException e) {
        data[id] = "ERROR: Failed to resolve role: $id caused by: $e".toString()
        return
      }
      data[id] = mappify(role, ['roleId', 'roles', 'privileges'] as Set)

      // add details for nested roles
      if (role.roles) {
        data[id].roles = [:]
        role.roles.each {
          explainRole(data[id].roles, it) // recurs
        }
      }

      // add details for each privilege
      if (role.privileges) {
        data[id].privileges = [:]
        role.privileges.each {
          explainPrivilege(data[id].privileges, it)
        }
      }
    }

    // add details for given user
    def explainUser = { data, user ->
      data.user = mappify(user, ['roles'] as Set)

      data.user.roles = [:]
      user.roles.each {
        explainRole(data.user.roles, it.roleId)
      }
    }

    // look the user up
    User user
    try {
      user = securitySystem.getUser(userId)
    }
    catch (UserNotFoundException e) {
      log.debug('User not found: {}', userId, e)
      throw new NotFoundException('User not found')
    }

    // explain user+role+priv details
    def data = [:]
    explainUser(data, user)

    return data
  }
}
