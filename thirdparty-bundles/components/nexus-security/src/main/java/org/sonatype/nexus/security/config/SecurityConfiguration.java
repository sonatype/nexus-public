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

import org.sonatype.nexus.security.privilege.NoSuchPrivilegeException;
import org.sonatype.nexus.security.role.NoSuchRoleException;
import org.sonatype.nexus.security.user.NoSuchRoleMappingException;
import org.sonatype.nexus.security.user.UserNotFoundException;

/**
 * Security configuration.
 *
 * @since 3.0
 */
public interface SecurityConfiguration
{
  List<CUser> getUsers();

  @Nullable
  CUser getUser(String id);

  void addUser(CUser user, Set<String> roles);

  void updateUser(CUser user, Set<String> roles) throws UserNotFoundException;

  boolean removeUser(String id);

  List<CUserRoleMapping> getUserRoleMappings();

  @Nullable
  CUserRoleMapping getUserRoleMapping(String userId, String source);

  void addUserRoleMapping(CUserRoleMapping mapping);

  void updateUserRoleMapping(CUserRoleMapping mapping) throws NoSuchRoleMappingException;

  boolean removeUserRoleMapping(String userId, String source);

  List<CPrivilege> getPrivileges();

  @Nullable
  CPrivilege getPrivilege(String id);

  void addPrivilege(CPrivilege privilege);

  void updatePrivilege(CPrivilege privilege) throws NoSuchPrivilegeException;

  boolean removePrivilege(String id);

  List<CRole> getRoles();

  @Nullable
  CRole getRole(String id);

  void addRole(CRole role);

  void updateRole(CRole role) throws NoSuchRoleException;

  boolean removeRole(String id);
}
