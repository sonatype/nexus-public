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
package org.sonatype.security.usermanagement;

import java.util.Set;

import org.sonatype.configuration.validation.InvalidConfigurationException;

import org.apache.shiro.realm.Realm;

/**
 * A DAO for users comming from a given source.
 *
 * @author Brian Demers
 */
public interface UserManager
{

  /**
   * Get the source string of this UserManager
   */
  String getSource();

  /**
   * The name of the {@link Realm} is assocated with.
   */
  String getAuthenticationRealmName();

  /**
   * If this UserManager is writable.
   */
  boolean supportsWrite();

  /**
   * Retrieve all User objects
   */
  Set<User> listUsers();

  /**
   * Retrieve all userids (if managing full object list is to heavy handed)
   */
  Set<String> listUserIds();

  /**
   * Add a user.
   */
  User addUser(User user, String password)
      throws InvalidConfigurationException;

  /**
   * Update a user.
   */
  User updateUser(User user)
      throws UserNotFoundException, InvalidConfigurationException;

  /**
   * Delete a user based on id.
   */
  void deleteUser(String userId)
      throws UserNotFoundException;

  /**
   * Searches for Subject objects by a criteria.
   */
  Set<User> searchUsers(UserSearchCriteria criteria);

  /**
   * Get a Subject object by id
   */
  User getUser(String userId)
      throws UserNotFoundException;

  /**
   * Update a users password.
   */
  void changePassword(String userId, String newPassword)
      throws UserNotFoundException, InvalidConfigurationException;
}
