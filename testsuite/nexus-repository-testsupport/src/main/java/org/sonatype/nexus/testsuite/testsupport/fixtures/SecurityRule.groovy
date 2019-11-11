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
package org.sonatype.nexus.testsuite.testsupport.fixtures

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.apache.shiro.authz.Permission
import org.junit.rules.ExternalResource
import org.sonatype.nexus.repository.security.RepositoryContentSelectorPrivilegeDescriptor
import org.sonatype.nexus.security.SecuritySystem
import org.sonatype.nexus.security.privilege.Privilege
import org.sonatype.nexus.security.role.Role
import org.sonatype.nexus.security.role.RoleIdentifier
import org.sonatype.nexus.security.user.User
import org.sonatype.nexus.security.user.UserStatus
import org.sonatype.nexus.selector.CselSelector
import org.sonatype.nexus.selector.SelectorConfiguration
import org.sonatype.nexus.selector.SelectorManager

import javax.inject.Provider

import static com.google.common.base.Preconditions.checkNotNull
import static org.sonatype.nexus.security.user.UserManager.DEFAULT_SOURCE

/**
 * @since 3.14
 */
@Slf4j
@CompileStatic
class SecurityRule
    extends ExternalResource
{
  final Provider<SecuritySystem> securitySystemProvider

  final Provider<SelectorManager> selectorManagerProvider

  final Set<Privilege> privileges = [] as Set<Privilege>

  final Set<Role> roles = [] as Set<Role>

  final Set<User> users = [] as Set<User>

  final Set<SelectorConfiguration> selectors = [] as Set<SelectorConfiguration>

  SecurityRule(final Provider<SecuritySystem> securitySystemProvider,
               final Provider<SelectorManager> selectorManagerProvider)
  {
    this.securitySystemProvider = checkNotNull(securitySystemProvider)
    this.selectorManagerProvider = checkNotNull(selectorManagerProvider)
  }

  @Override
  protected void after() {
    users.each { 
      try {
        securitySystemProvider.get().deleteUser(it.getUserId(), DEFAULT_SOURCE)
      }
      catch (Exception e) { //NOSONAR
        log.debug("Failed to cleanup user: {}", it.getUserId(), e)
      }
    }
    roles.each {
      try {
        securitySystemProvider.get().getAuthorizationManager(DEFAULT_SOURCE).deleteRole(it.getRoleId())
      }
      catch (Exception e) { //NOSONAR
        log.debug("Failed to cleanup role: {}", it.getRoleId(), e)
      }
    }
    privileges.each {
      try {
        securitySystemProvider.get().getAuthorizationManager(DEFAULT_SOURCE).deletePrivilege(it.id)
      }
      catch (Exception e) { //NOSONAR
        log.debug("Failed to cleanup privilege: {}", it.getId(), e)
      }
    }
    selectors.each {
      try {
        selectorManagerProvider.get().delete(it)
      }
      catch (Exception e) { //NOSONAR
        log.debug("Failed to cleanup content selector: {}", it.getName(), e)
      }
    }
  }

  Privilege getPrivilege(final String privilege) {
    securitySystemProvider.get().getAuthorizationManager(DEFAULT_SOURCE).listPrivileges().find {
      it.name == privilege
    }
  }

  Role getRole(final String roleId) {
    securitySystemProvider.get().getAuthorizationManager(DEFAULT_SOURCE).listRoles().find {
      it.roleId == roleId
    }
  }

  Privilege createContentSelectorPrivilege(final String name, final String selector, final String repository = '*', final String actions = '*') {
    def privilege = new Privilege(
        id: name,
        name: name,
        description: name,
        type: RepositoryContentSelectorPrivilegeDescriptor.TYPE,
        readOnly: false,
        properties: ['contentSelector': selector, 'repository': repository, 'actions': actions]
    )

    privileges << privilege

    securitySystemProvider.get().getAuthorizationManager(DEFAULT_SOURCE).addPrivilege(privilege)
  }

  /**
   * Note that the properties should be in multiples of 2 (key/value pairs)
   */
  Privilege createPrivilege(final String type, final String name, final String... properties) {
    def propMap = [:]
    for (int i = 0 ; i < properties.length ; i+=2) {
      propMap.put(properties[i], properties[i+1])
    }

    def privilege = new Privilege(
        id: name,
        name: name,
        description: name,
        type: type,
        readOnly: false,
        properties: propMap
    )

    privileges << privilege

    securitySystemProvider.get().getAuthorizationManager(DEFAULT_SOURCE).addPrivilege(privilege)
  }

  Role createRole(final String name, final String... privilegeNames) {
    createRole(name, new String[0], privilegeNames)
  }

  Role createRole(final String name, final String[] roleIds, final String[] privilegeNames) {
    Privilege[] privileges = privilegeNames.collect { String privilegeName -> getPrivilege(privilegeName) } as Privilege[]

    if (privileges.length != privilegeNames.length) {
      throw new IllegalStateException("Missing privileges names: ${privilegeNames} privileges: ${privileges}")
    }

    Role[] roles = roleIds.collect { String roleId -> getRole(roleId) } as Role[]

    if (roles.length != roleIds.length) {
      throw new IllegalStateException("Missing privileges names: ${roleIds} privileges: ${roles}")
    }

    createRole(name, roles, privileges)
  }

  Role createRole(final String name, final Privilege... privileges) {
    createRole(name, new Role[0], privileges)
  }

  Role createRole(final String name, final Role[] containedRoles, final Privilege[] privileges) {
    def role = new Role(
        roleId: name,
        source: DEFAULT_SOURCE,
        name: name,
        description: name,
        readOnly: false,
        roles: containedRoles.collect { Role r -> r.roleId } as Set<String>,
        privileges: privileges.collect { Privilege p -> p.id } as Set<String>
    )

    roles << role

    securitySystemProvider.get().getAuthorizationManager(DEFAULT_SOURCE).addRole(role)
  }

  Privilege findPrivilege(final Permission permission) {
    securitySystemProvider.get().listPrivileges().find {it.getPermission().equals(permission)}
  }

  Role findRole(final String roleId) {
    securitySystemProvider.get().getAuthorizationManager(DEFAULT_SOURCE).listRoles().find {it.getRoleId().equals(roleId)}
  }

  User createUser(final String name, final String password, final String... roles) {
    def user = new User(
        userId: name,
        source: DEFAULT_SOURCE,
        firstName: name,
        lastName: name,
        emailAddress: 'example@example.com',
        status: UserStatus.active,
        roles: roles.collect { String role -> new RoleIdentifier(DEFAULT_SOURCE, role) } as Set<RoleIdentifier>
    )

    users << user

    securitySystemProvider.get().addUser(user, password)
  }

  SelectorConfiguration createSelector(final String name, final String expression) {
    createSelector(name, name, CselSelector.TYPE, expression)
  }

  SelectorConfiguration createSelector(final String name, final String description, final String type, final String expression) {
    def selectorConfiguration = new SelectorConfiguration([
        name: name,
        description: description,
        type: type,
        attributes: ['expression': expression]
    ])

    selectors << selectorConfiguration
    selectorManagerProvider.get().create(selectorConfiguration)
    selectorConfiguration
  }
}

