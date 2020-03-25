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

import javax.annotation.Nullable;

import org.sonatype.nexus.security.user.NoSuchRoleMappingException;
import org.sonatype.nexus.security.user.UserNotFoundException;

/**
 * Security configuration.
 *
 * @since 3.0
 */
public interface SecurityConfiguration
{
  /**
   * Get all {@link CUser}s known to this configuration.
   */
  List<CUser> getUsers();

  /**
   * Get an existing {@link CUser} by its ID.
   *
   * @return the user, or null
   */
  @Nullable
  CUser getUser(String id);

  /**
   * Obtain an instance of {@link CUser} suitable for use with the underlying storage.
   */
  CUser newUser();

  /**
   * Add a new {@link CUser} to the configuration.
   *
   * Note: the underlying implementation may throw an exception if the instance of {@link CUser}
   * was not obtained by calling {@link #newUser}.
   */
  void addUser(CUser user, Set<String> roles);

  /**
   * Adds role mapping for a user idetified by the id given, and for the given source.
   * @since 3.22
   */
  void addRoleMapping(String userId, Set<String> roles, String source);

  /**
   * Update an existing {@link CUser} without modifying its assigned roles.
   *
   * Note: the underlying implementation may throw an exception if the instance of {@link CUser}
   * was not obtained from this configuration.
   *
   * @since 3.11
   */
  void updateUser(CUser user) throws UserNotFoundException;

  /**
   * Update an existing {@link CUser} and its assigned roles.
   *
   * Note: the underlying implementation may throw an exception if the instance of {@link CUser}
   * was not obtained from this configuration.
   */
  void updateUser(CUser user, Set<String> roles) throws UserNotFoundException;

  /**
   * Remove an existing {@link CUser} by its ID.
   *
   * @return true if a user was removed, false otherwise
   */
  boolean removeUser(String id);

  /**
   * Get all {@link CUserRoleMapping} instances known to this configuration.
   */
  List<CUserRoleMapping> getUserRoleMappings();

  /**
   * Get an existing {@link CUserRoleMapping} by the userId and its user source.
   *
   * @param userId the userId
   * @param source the user source (e.g. default, LDAP or Crowd)
   */
  @Nullable
  CUserRoleMapping getUserRoleMapping(String userId, String source);

  /**
   * Obtain an instance of {@link CUserRoleMapping} suitable for use with the underlying storage.
   *
   * @since 3.20
   */
  CUserRoleMapping newUserRoleMapping();

  /**
   * Add a new {@link CUserRoleMapping} to the configuration.
   *
   * Note: the underlying implementation may throw an exception if the instance of {@link CUserRoleMapping}
   * was not obtained by calling {@link #newUserRoleMapping()}.
   */
  void addUserRoleMapping(CUserRoleMapping mapping);

  /**
   * Update an existing {@link CUserRoleMapping} in the configuration.
   *
   * Note: the underlying implementation may throw an exception if the instance of {@link CUserRoleMapping}
   * was not obtained from this configuration.
   */
  void updateUserRoleMapping(CUserRoleMapping mapping) throws NoSuchRoleMappingException;

  /**
   * Remove an existing {@link CUserRoleMapping} by its ID.
   *
   * @return true if a user role mapping was removed, false otherwise
   */
  boolean removeUserRoleMapping(String userId, String source);

  /**
   * Get all {@link CPrivilege} instances known to this configuration.
   */
  List<CPrivilege> getPrivileges();

  /**
   * Get an existing {@link CPrivilege} by its ID.
   */
  @Nullable
  CPrivilege getPrivilege(String id);

  /**
   * Obtain an instance of {@link CPrivilege} suitable for use with the underlying storage.
   *
   * @since 3.21
   */
  CPrivilege newPrivilege();

  /**
   * Add a new {@link CPrivilege} to the configuration.
   *
   * Note: the underlying implementation may return a different object than the one passed as an argument if the object
   * does not match for the underlying storage.
   *
   * @return the persisted entity, may be different than the argument
   */
  CPrivilege addPrivilege(CPrivilege privilege);

  /**
   * Update an existing {@link CPrivilege} in the configuration.
   *
   * Note: the underlying implementation may throw an exception if the instance of {@link CPrivilege}
   * was not obtained from this configuration.
   */
  void updatePrivilege(CPrivilege privilege);

  /**
   * Remove an existing privilege by its ID.
   *
   * @return true if a privilege was removed, false otherwise
   */
  boolean removePrivilege(String id);

  /**
   * Get the list of {@link CRole}s known to this configuration.
   */
  List<CRole> getRoles();

  /**
   * Get an existing {@link CRole} from the configuration by its ID.
   */
  @Nullable
  CRole getRole(String id);

  /**
   * Obtain an instance of {@link CRole} suitable for use with the underlying storage.
   *
   * @since 3.20
   */
  CRole newRole();

  /**
   *
   * @since 3.20
   */
  default CRoleBuilder newRoleBuilder() {
    return new CRoleBuilder(newRole());
  }

  /**
   * Add a new {@link CRole} to the configuration.
   *
   * Note: the underlying implementation may throw an exception if the instance of {@link CRole}
   * was not obtained by calling {@link #newRole}.
   */
  void addRole(CRole role);

  /**
   * Update an existing {@link CRole} in the configuration.
   *
   * Note: the underlying implementation may throw an exception if the instance of {@link CRole}
   * was not obtained from this configuration.
   */
  void updateRole(CRole role);

  /**
   * Remove an existing role by its ID.
   *
   * @return true if a role was removed, false otherwise
   */
  boolean removeRole(String id);
}
