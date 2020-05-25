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
package org.sonatype.nexus.internal.security.model

import javax.inject.Named

import org.sonatype.nexus.content.testsuite.groups.SQLTestGroup
import org.sonatype.nexus.datastore.api.DataSessionSupplier
import org.sonatype.nexus.datastore.api.DataStoreManager
import org.sonatype.nexus.security.config.AdminPasswordFileManager
import org.sonatype.nexus.security.config.CUser
import org.sonatype.nexus.security.config.MemorySecurityConfiguration
import org.sonatype.nexus.security.config.SecurityConfiguration
import org.sonatype.nexus.security.config.SecurityConfigurationSource
import org.sonatype.nexus.security.privilege.DuplicatePrivilegeException
import org.sonatype.nexus.security.privilege.NoSuchPrivilegeException
import org.sonatype.nexus.security.role.DuplicateRoleException
import org.sonatype.nexus.security.role.NoSuchRoleException
import org.sonatype.nexus.security.user.DuplicateUserException
import org.sonatype.nexus.security.user.NoSuchRoleMappingException
import org.sonatype.nexus.security.user.UserNotFoundException
import org.sonatype.nexus.testdb.DataSessionRule
import org.sonatype.nexus.transaction.TransactionModule
import org.sonatype.nexus.transaction.UnitOfWork

import com.google.inject.Guice
import com.google.inject.Provides
import org.apache.shiro.authc.credential.PasswordService
import org.junit.Rule
import org.junit.experimental.categories.Category
import spock.lang.Specification
import spock.lang.Unroll

import static org.mockito.Mockito.mock
import static org.mockito.Mockito.when

/**
 * Tests for {@link SecurityConfigurationSourceImpl}.
 */
@Category(SQLTestGroup.class)
class SecurityConfigurationSourceImplTest
    extends Specification
{
  final String PASSWORD1 = '$shiro1$SHA-512$1024$NYQKemFvZqat9CepP2xO9A==$4m4dBi9f/EtJLpJSW6/7+IVxW3wHR4RNeGtbopiH+D5tlVDFqNKo667eMnqWUxFrRz4Y4IQvn5hv/BnWmEfN0Q=='

  @Rule
  DataSessionRule sessionRule = new DataSessionRule()
      .access(CPrivilegeDAO)
      .access(CRoleDAO)
      .access(CUserDAO)
      .access(CUserRoleMappingDAO)

  PasswordService passwordService = Mock()

  AdminPasswordFileManager adminPasswordFileManager = Mock()

  SecurityConfigurationSourceImpl underTest

  def setup() {
    passwordService.encryptPassword(_) >> "encrypted"
    adminPasswordFileManager.readFile() >> "password"

    MemorySecurityConfiguration defaults = new MemorySecurityConfiguration()

    defaults.addPrivilege(
        new CPrivilegeData(id: 'privilege1', name: 'Privilege1', description: 'Privilege One', type: 'application'))
    defaults.addPrivilege(
        new CPrivilegeData(id: 'privilege2', name: 'Privilege2', description: 'Privilege Two', type: 'application'))
    defaults.addPrivilege(
        new CPrivilegeData(id: 'privilege3', name: 'Privilege2', description: 'Privilege Three', type: 'application'))
    defaults.addRole(new CRoleData(id: 'role1', name: 'Role1', description: 'Role One'))
    defaults.addRole(new CRoleData(id: 'role2', name: 'Role2', description: 'Role Two'))
    defaults.addUser(new CUserData(id: 'user1', firstName: 'User', lastName: 'One', email: 'test@example.com',
        status: CUser.STATUS_ACTIVE, password: PASSWORD1))

    SecurityConfigurationSource defaultSource = mock(SecurityConfigurationSource)
    when(defaultSource.getConfiguration()).thenReturn(defaults)

    underTest = Guice.createInjector(new TransactionModule()
    {
      @Provides
      DataSessionSupplier getDataSessionSupplier() {
        return sessionRule
      }

      @Provides
      @Named("static")
      SecurityConfigurationSource getStaticSecurityConfigurationSource() {
        return defaultSource
      }

      @Override
      protected void configure() {
        super.configure()
        bind(SecurityConfiguration).to(SecurityConfigurationImpl)
      }
    }).getInstance(SecurityConfigurationSourceImpl)

    UnitOfWork.beginBatch({ sessionRule.openSession(DataStoreManager.CONFIG_DATASTORE_NAME) })
    System.setProperty('nexus.orient.store.config', 'false')
    underTest.start()
    underTest.loadConfiguration()
  }

  def cleanup() {
    System.clearProperty('nexus.orient.store.config')
    UnitOfWork.end()
  }

  def 'test loading of defaults'() {
    when:
      def defaultPrivileges = underTest.configuration.getPrivileges()
      def defaultRoles = underTest.configuration.getRoles()
      def defaultUsers = underTest.configuration.getUsers()
    then:
      defaultPrivileges.size() == 3
      defaultRoles.size() == 2
      defaultUsers.size() == 1
  }

  def 'updatePrivilege should persist privilege'() {
    given:
      underTest.configuration.addPrivilege(new CPrivilegeData(id: 'test', name: 'test', type: 'test'))
      def privilege = underTest.configuration.getPrivilege('test')
      def newPrivilege = new CPrivilegeData(id: 'new', name: 'new', type: 'test')

    when: 'updatePrivilege is called'
      privilege.name = 'foo'
      underTest.configuration.updatePrivilege(privilege)

    then: 'privilege is persisted'
      underTest.configuration.getPrivilege('test').name == 'foo'

    when: 'getPrivileges is called'
      def privileges = underTest.configuration.privileges

    then: 'the new privilege is returned along with the defaults'
      privileges.size() == 4

    when: 'updatePrivilege is called on privilege that has not been saved'
      underTest.configuration.updatePrivilege(newPrivilege)

    then: 'exception is thrown'
      thrown(NoSuchPrivilegeException)
  }

  def 'adding privilege with same id throws exception'() {
    given:
      underTest.configuration.addPrivilege(new CPrivilegeData(id: 'test', name: 'test', type: 'test'))
    when:
      underTest.configuration.addPrivilege(new CPrivilegeData(id: 'test', name: 'test2', type: 'test2'))
    then:
      thrown(DuplicatePrivilegeException)
  }

  def 'updateRole should persist role'() {
    given:
      underTest.configuration.addRole(
          new CRoleData(id: 'test', name: 'test', description: 'test', privileges: ['priv1'], roles: ['role1', 'role2']))
      def role = underTest.configuration.getRole('test')
      def newRole = new CRoleData(id: 'new', name: 'new', description: 'test', privileges: [], roles: ['role3'])

    when: 'updateRole is called'
      role.name = 'foo'
      underTest.configuration.updateRole(role)

    then: 'role is persisted'
      underTest.configuration.getRole('test').name == 'foo'

    when: 'getRoles is called'
      def roles = underTest.configuration.roles

    then: 'the new role is returned along with the defaults'
      roles.size() == 3

    when: 'updateRole is called on role that has not been saved'
      underTest.configuration.updateRole(newRole)

    then: 'exception is thrown'
      thrown(NoSuchRoleException)
  }

  def 'adding role with same id throws exception'() {
    given:
      underTest.configuration.addRole(
          new CRoleData(id: 'test', name: 'test', description: 'test', privileges: [], roles: []))
    when:
      underTest.configuration.addRole(
          new CRoleData(id: 'test', name: 'test2', description: 'test2', privileges: [], roles: []))
    then:
      thrown(DuplicateRoleException)
  }

  def 'updateUser should persist user'() {
    given:
      def user1 = underTest.configuration.getUser('user1')
      def newUser = new CUserData(id: 'new')

    when: 'updateUser is called'
      user1.firstName = 'foo'
      underTest.configuration.updateUser(user1)

    then: 'user is persisted'
      underTest.configuration.getUser('user1').firstName == 'foo'

    when: 'updateUser is called on user that has not been saved'
      underTest.configuration.updateUser(newUser)

    then: 'exception is thrown'
      thrown(UserNotFoundException)
  }

  def 'adding user with same id throws exception'() {
    given:
      underTest.configuration.addUser(
          new CUserData(id: 'test', firstName: 'first', lastName: 'last', email: 'test@example.com',
              status: CUser.STATUS_ACTIVE, password: PASSWORD1))
    when:
      underTest.configuration.addUser(
          new CUserData(id: 'test', firstName: 'first2', lastName: 'last2', email: 'test@example.com',
              status: CUser.STATUS_ACTIVE, password: PASSWORD1))
    then:
      thrown(DuplicateUserException)
  }

  def 'userRoleMappings userIds are case sensitive'() {
    given: 'an existing user role mapping'
      def roles = ['test-role'] as Set
      def userId = 'userid'
      def src = 'other'
      def newUserRoleMapping = new CUserRoleMappingData(userId: userId, source: src, roles: roles)
      underTest.configuration.addUserRoleMapping(newUserRoleMapping)

    when: 'a users roles are retrieved with different user id casing'
      def roleMapping = underTest.configuration.getUserRoleMapping(userId.toUpperCase(), src)

    then: 'the mapping is not found'
      roleMapping == null

    when: 'the mapping is updated with different user id casing'
      roleMapping = underTest.configuration.getUserRoleMapping(userId, src)
      roleMapping.userId = 'USERID'
      roleMapping.roles << 'new-role'
      underTest.configuration.updateUserRoleMapping(roleMapping)

    then: 'an error occurs'
      thrown NoSuchRoleMappingException

    when: 'the mapping is deleted with a different user id casing'
      underTest.configuration.removeUserRoleMapping(userId.toUpperCase(), src)

    then: 'the mapping is not deleted'
      underTest.configuration.getUserRoleMapping(userId, src) != null
  }

  @Unroll
  def 'userRoleMappings userIds are not case sensitive with source: \'#src\''(src) {
    given: 'an existing user role mapping'
      def roles = ['test-role'] as Set
      def userId = 'userid'
      def newUserRoleMapping = new CUserRoleMappingData(userId: userId, source: src, roles: roles)
      underTest.configuration.addUserRoleMapping(newUserRoleMapping)

    when: 'a users roles are retrieved with different user id casing'
      def roleMapping = underTest.configuration.getUserRoleMapping(userId.toUpperCase(), src)

    then: 'the mapping is found'
      roleMapping != null

    when: 'the mapping is updated with different user id casing'
      roleMapping.userId = 'USERID'
      roleMapping.roles << 'new-role'
      underTest.configuration.updateUserRoleMapping(roleMapping)

    and: 'the mapping is read again'
      roleMapping = underTest.configuration.getUserRoleMapping(userId, src)

    then: 'the new role is found'
      roleMapping.roles == roles << 'new-role'

    when: 'the mapping is deleted with a different user id casing'
      underTest.configuration.removeUserRoleMapping(userId.toUpperCase(), src)

    then: 'the mapping is removed from the system'
      underTest.configuration.getUserRoleMapping(userId, src) == null

    where:
      src << ['default', 'ldap', 'crowd']
  }
}
