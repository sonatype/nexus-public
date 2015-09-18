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
package org.sonatype.nexus.security.config;

import java.util.List;
import java.util.Set;

import org.sonatype.nexus.security.privilege.NoSuchPrivilegeException;
import org.sonatype.nexus.security.role.NoSuchRoleException;
import org.sonatype.nexus.security.user.NoSuchRoleMappingException;
import org.sonatype.nexus.security.user.UserNotFoundException;

/**
 * The ConfigurationManager is a facade in front of the security modello model. It supports CRUD operations for
 * users/roles/privileges and user to role mappings.
 *
 * Any direct calls to write-based ConfigurationManager methods will throw an IllegalStateException, as they
 * cannot be used directly in a thread-safe manner
 *
 * Direct calls to read-based ConfigurationManager methods can be called in a thread-safe manner. However, operations
 * that require multiple read-based calls should be encapsulated into an action and executed via the runRead method.
 */
public interface SecurityConfigurationManager
{
  //
  // Users
  //

  /**
   * Retrieve all users
   */
  List<CUser> listUsers();

  /**
   * Create a new user.
   */
  void createUser(CUser user, Set<String> roles);

  /**
   * Create a new user and sets the password.
   */
  void createUser(CUser user, String password, Set<String> roles);

  /**
   * Retrieve an existing user
   */
  CUser readUser(String id) throws UserNotFoundException;

  /**
   * Update an existing user. Roles are unchanged
   *
   * @param user to update
   */
  void updateUser(CUser user) throws UserNotFoundException;

  /**
   * Update an existing user and their roles
   */
  void updateUser(CUser user, Set<String> roles) throws UserNotFoundException;

  /**
   * Delete an existing user
   */
  void deleteUser(String id) throws UserNotFoundException;

  //
  // Roles
  //

  /**
   * Retrieve all roles
   */
  List<CRole> listRoles();

  /**
   * Create a new role
   */
  void createRole(CRole role);

  /**
   * Retrieve an existing role
   */
  CRole readRole(String id) throws NoSuchRoleException;

  /**
   * Update an existing role
   */
  void updateRole(CRole role) throws NoSuchRoleException;

  /**
   * Delete an existing role
   */
  void deleteRole(String id) throws NoSuchRoleException;

  void cleanRemovedRole(String roleId);

  //
  // Privileges
  //

  /**
   * Retrieve all privileges
   */
  List<CPrivilege> listPrivileges();

  /**
   * Create a new privilege
   */
  void createPrivilege(CPrivilege privilege);

  /**
   * Retrieve an existing privilege
   */
  CPrivilege readPrivilege(String id) throws NoSuchPrivilegeException;

  /**
   * Update an existing privilege
   */
  void updatePrivilege(CPrivilege privilege) throws NoSuchPrivilegeException;

  /**
   * Delete an existing privilege
   */
  void deletePrivilege(String id) throws NoSuchPrivilegeException;

  void cleanRemovedPrivilege(String privilegeId);

  //
  // User-role mapping
  //

  void createUserRoleMapping(CUserRoleMapping userRoleMapping);

  void updateUserRoleMapping(CUserRoleMapping userRoleMapping) throws NoSuchRoleMappingException;

  CUserRoleMapping readUserRoleMapping(String userId, String source) throws NoSuchRoleMappingException;

  List<CUserRoleMapping> listUserRoleMappings();

  void deleteUserRoleMapping(String userId, String source) throws NoSuchRoleMappingException;
}
