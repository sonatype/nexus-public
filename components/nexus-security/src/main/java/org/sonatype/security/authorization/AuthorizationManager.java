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
package org.sonatype.security.authorization;

import java.util.Set;

import org.sonatype.configuration.validation.InvalidConfigurationException;

/**
 * A DAO for Roles and Privileges comming from a given source.
 *
 * @author Brian Demers
 */
public interface AuthorizationManager
{
  /**
   * The Id if this AuthorizationManager;
   */
  public String getSource();

  /**
   * If this AuthorizationManager is writable.
   */
  boolean supportsWrite();

  // **************
  // ROLE CRUDS
  // **************

  /**
   * Returns the all Roles from this AuthorizationManager. NOTE: this call could be slow when coming from an external
   * source (i.e. a database) TODO: Consider removing this method.
   */
  public Set<Role> listRoles();

  /**
   * Returns a Role base on an Id.
   */
  public Role getRole(String roleId)
      throws NoSuchRoleException;

  /**
   * Adds a role to this AuthorizationManager.
   */
  public Role addRole(Role role)
      throws InvalidConfigurationException;

  /**
   * Updates a role in this AuthorizationManager.
   */
  public Role updateRole(Role role)
      throws NoSuchRoleException, InvalidConfigurationException;

  /**
   * Removes a role in this AuthorizationManager.
   */
  public void deleteRole(String roleId)
      throws NoSuchRoleException;

  // Privilege CRUDS

  /**
   * Returns the all Privileges from this AuthorizationManager.
   */
  public Set<Privilege> listPrivileges();

  /**
   * Returns a Privilege base on an Id.
   */
  public Privilege getPrivilege(String privilegeId)
      throws NoSuchPrivilegeException;

  /**
   * Adds a Privilege to this AuthorizationManager.
   */
  public Privilege addPrivilege(Privilege privilege)
      throws InvalidConfigurationException;

  /**
   * Updates a Privilege in this AuthorizationManager.
   */
  public Privilege updatePrivilege(Privilege privilege)
      throws NoSuchPrivilegeException, InvalidConfigurationException;

  /**
   * Removes a Privilege in this AuthorizationManager.
   */
  public void deletePrivilege(String privilegeId)
      throws NoSuchPrivilegeException;
}
