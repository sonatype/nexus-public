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
package org.sonatype.nexus.security;

import java.util.List;

import org.sonatype.nexus.common.script.ScriptApi;
import org.sonatype.nexus.security.anonymous.AnonymousConfiguration;
import org.sonatype.nexus.security.authz.NoSuchAuthorizationManagerException;
import org.sonatype.nexus.security.role.Role;
import org.sonatype.nexus.security.user.NoSuchUserManagerException;
import org.sonatype.nexus.security.user.User;
import org.sonatype.nexus.security.user.UserNotFoundException;

/**
 * Security provisioning capabilities of the repository manager.
 *
 * @since 3.0
 */
public interface SecurityApi
    extends ScriptApi
{
  @Override
  default String getName() {
    return "security";
  }

  /**
   * Set whether or not to allow anonymous access to the system.
   */
  AnonymousConfiguration setAnonymousAccess(boolean enabled);

  /**
   * Add a new User to the system.
   */
  User addUser(
      String id,
      String firstName,
      String lastName,
      String email,
      boolean active,
      String password,
      List<String> roleIds) throws NoSuchUserManagerException;

  /**
   * Add a new Role to the system.
   */
  Role addRole(
      String id,
      String name,
      String description,
      List<String> privileges,
      List<String> roles) throws NoSuchAuthorizationManagerException;

  /**
   * Set the Roles on a given User.
   */
  User setUserRoles(String userId, List<String> roleIds) throws UserNotFoundException, NoSuchUserManagerException;

}
