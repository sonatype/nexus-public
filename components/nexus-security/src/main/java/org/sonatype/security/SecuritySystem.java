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
package org.sonatype.security;

import java.util.List;
import java.util.Set;

import org.sonatype.configuration.validation.InvalidConfigurationException;
import org.sonatype.security.authentication.AuthenticationException;
import org.sonatype.security.authorization.AuthorizationException;
import org.sonatype.security.authorization.AuthorizationManager;
import org.sonatype.security.authorization.NoSuchAuthorizationManagerException;
import org.sonatype.security.authorization.Privilege;
import org.sonatype.security.authorization.Role;
import org.sonatype.security.usermanagement.InvalidCredentialsException;
import org.sonatype.security.usermanagement.NoSuchUserManagerException;
import org.sonatype.security.usermanagement.RoleIdentifier;
import org.sonatype.security.usermanagement.User;
import org.sonatype.security.usermanagement.UserNotFoundException;
import org.sonatype.security.usermanagement.UserSearchCriteria;

import org.apache.shiro.authc.AuthenticationInfo;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.mgt.RealmSecurityManager;
import org.apache.shiro.subject.PrincipalCollection;
import org.apache.shiro.subject.Subject;

/**
 * This is a facade around all things security ( authentication, authorization, user management, and configuration ).
 * It
 * is meant to be the single point of access.
 *
 * @author Brian Demers
 */
public interface SecuritySystem
{

  /**
   * Starts the SecuritySystem. Before this method is called the state is unknown.
   */
  void start();

  /**
   * Stops the SecuritySystem. Provides a way to clean up resources.
   */
  void stop();

  // *********************
  // * authentication
  // *********************

  /**
   * Authenticates a user and logs them in. If successful returns a Subject.
   *
   * @return the Subject representing the logged in user.
   * @throws AuthenticationException if the user can not be authenticated
   */
  public Subject login(AuthenticationToken token)
      throws AuthenticationException;

  /**
   * Authenticates a user and does NOT log them in. If successful returns a AuthenticationInfo.
   *
   * @return the AuthenticationInfo for this request.
   */
  public AuthenticationInfo authenticate(AuthenticationToken token)
      throws AuthenticationException;

  // /**
  // * This method sets the current thread to use the <code>principal</code> passed in.
  // * You must log the user out subject.logout() when your done.
  // *
  // * @param principal The account to login in as.
  // * @return The subject that was created as a result of the principal.
  // */
  // public Subject runAs( PrincipalCollection principal );

  /**
   * Finds the current logged in Subject.
   */
  public Subject getSubject();

  /**
   * Logs the subject out.
   */
  public void logout(Subject subject);

  // *********************
  // * authorization
  // *********************

  /**
   * Checks if principal has a permission.
   *
   * @return true only if the principal has the permission.
   */
  public boolean isPermitted(PrincipalCollection principal, String permission);

  /**
   * Checks if principal has a list of permission.
   *
   * @param permissions list of permission to check.
   * @return A boolean array, the results in the array match the order of the permission
   */
  public boolean[] isPermitted(PrincipalCollection principal, List<String> permissions);

  /**
   * Checks if principal has a permission, throws an AuthorizationException otherwise.
   */
  public void checkPermission(PrincipalCollection principal, String permission)
      throws AuthorizationException;

  /**
   * Checks if principal has a list of permissions, throws an AuthorizationException unless the principal has all
   * permissions.
   */
  public void checkPermission(PrincipalCollection principal, List<String> permissions)
      throws AuthorizationException;

  /**
   * Checks if a principal has a role.
   *
   * @return true if the principal has this role.
   */
  public boolean hasRole(PrincipalCollection principals, String role);

  // ******************************
  // * Role permission management
  // ******************************

  /**
   * Lists all roles defined in the system. NOTE: this method could be slow if there is a large list of roles coming
   * from an external source (such as a database).
   *
   * @return All the roles defined in the system.
   */
  public Set<Role> listRoles();

  /**
   * NOTE: this method could be slow if there is a large list of roles coming from an external source (such as a
   * database).
   *
   * @param sourceId The identifier of an {@link AuthorizationManager}.
   * @return All the roles defined by an {@link AuthorizationManager}.
   */
  public Set<Role> listRoles(String sourceId)
      throws NoSuchAuthorizationManagerException;

  // *********************
  // * user management
  // *********************

  /**
   * Adds a new User to the system. The users password will be generated.<BR/>
   * Note: User.source must be set to specify where the user will be created.
   *
   * @param user User to be created.
   * @return The User that was just created.
   */
  User addUser(User user)
      throws NoSuchUserManagerException, InvalidConfigurationException;

  /**
   * Adds a new User to the system.<BR/>
   * Note: User.source must be set to specify where the user will be created.
   *
   * @param user     User to be created.
   * @param password The users initial password.
   * @return The User that was just created.
   */
  User addUser(User user, String password)
      throws NoSuchUserManagerException, InvalidConfigurationException;

  /**
   * Get a User by id and source.
   *
   * @param userId   Id of the user to return.
   * @param soruceId the Id of the source to get the user from.
   * @return The user
   */
  User getUser(String userId, String sourceId)
      throws UserNotFoundException, NoSuchUserManagerException;

  /**
   * Get a User by id. This will search all sources (in order) looking for it. The first one found will be returned.
   * TODO: we should consider removing this in favor of its sibling that takes a source.
   *
   * @param userId Id of the user to return.
   * @return The user
   */
  User getUser(String userId)
      throws UserNotFoundException;

  /**
   * Updates a new User to the system.<BR/>
   * Note: User.source must be set to specify where the user will be modified.
   *
   * @param user User to be updated.
   * @return The User that was just updated.
   */
  User updateUser(User user)
      throws UserNotFoundException, NoSuchUserManagerException, InvalidConfigurationException;

  /**
   * Remove a user based on the Id.
   *
   * @param userId The id of the user to be removed.
   * @Deprecated use deleteUser( String userId, String source )
   */
  @Deprecated
  void deleteUser(String userId)
      throws UserNotFoundException;

  /**
   * Removes a user based on the userId and sourceId.
   *
   * @param userId   The id of the user to be removed.
   * @param sourceId The sourceId of the user to be removed.
   */
  void deleteUser(String userId, String sourceId)
      throws UserNotFoundException, NoSuchUserManagerException;

  /**
   * Returns a list of {@link RoleIdentifiers} which represents all the roles a given user has.
   *
   * @param userId   The Id of the user.
   * @param sourceId The source Id of the user.
   * @return All roles for a given user.
   */
  Set<RoleIdentifier> getUsersRoles(String userId, String sourceId)
      throws UserNotFoundException, NoSuchUserManagerException;

  /**
   * Sets the list of roles a user has.
   *
   * @param userId          The id of the user.
   * @param sourceId        The sourceId where the user is located.
   * @param roleIdentifiers The list of roles to give the user.
   */
  void setUsersRoles(String userId, String sourceId, Set<RoleIdentifier> roleIdentifiers)
      throws InvalidConfigurationException, UserNotFoundException;

  /**
   * Retrieve all Users . NOTE: This could be slow if there lots of users coming from external realms (a database).
   *
   * @deprecated use searchUsers.
   */
  @Deprecated
  Set<User> listUsers();

  /**
   * Searches for Users by criteria.
   */
  public Set<User> searchUsers(UserSearchCriteria criteria);

  // *********************
  // * change password
  // *********************

  /**
   * Sends an email to a user to recover his/her password.
   *
   * @param email The email address of the user.
   */
  void forgotUsername(String email)
      throws UserNotFoundException;

  /**
   * Updates a users password.
   *
   * @param userId      The id of the user.
   * @param oldPassword The user's current password.
   * @param newPassword The user's new password.
   */
  void changePassword(String userId, String oldPassword, String newPassword)
      throws UserNotFoundException, InvalidCredentialsException, InvalidConfigurationException;

  /**
   * Updates a users password. NOTE: This method does not require the old password to be known, it is meant for
   * administrators a users password.
   *
   * @param userId      The id of the user.
   * @param newPassword The user's new password.
   */
  void changePassword(String userId, String newPassword)
      throws UserNotFoundException, InvalidConfigurationException;

  // *********************
  // * Authorization Management
  // *********************

  /**
   * List all privileges in the system.
   *
   * @return A set of all the privileges in the system.
   */
  public Set<Privilege> listPrivileges();

  public AuthorizationManager getAuthorizationManager(String source)
      throws NoSuchAuthorizationManagerException;

  // //
  // Application configuration, TODO: I don't think all of these need to be exposed, but they currently are
  // //

  /**
   * Get the currently configured realms.
   *
   * @return The currently configured realms.
   */
  List<String> getRealms();

  /**
   * Set the currently configured realms.
   */
  void setRealms(List<String> realms)
      throws InvalidConfigurationException;

  /**
   * Return true if anonymous access is enabled.
   *
   * @return true if anonymous access is enabled.
   */
  boolean isAnonymousAccessEnabled();

  /**
   * Set Anonymous access enabled.
   */
  void setAnonymousAccessEnabled(boolean enabled);

  /**
   * Returns the name of the anonymous users. Could be something other then 'anonymous', for example Active Directory
   * uses 'Guest' TODO: consider removing this method.
   */
  String getAnonymousUsername();

  /**
   * Sets the name of the anonymous users. Could be something other then 'anonymous', for example Active Directory
   * uses 'Guest' TODO: consider removing this method.
   */
  void setAnonymousUsername(String anonymousUsername)
      throws InvalidConfigurationException;

  /**
   * Gets the anonymous user password.
   */
  String getAnonymousPassword();

  /**
   * Sets the anonymous user password.
   */
  void setAnonymousPassword(String anonymousPassword)
      throws InvalidConfigurationException;

  /**
   * Returns the configured shiro SecurityManager
   */
  public RealmSecurityManager getSecurityManager();

}
