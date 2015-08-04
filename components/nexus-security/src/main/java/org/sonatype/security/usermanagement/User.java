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

/**
 * A user contains attributes, userId, name, email address, roles, etc.
 *
 * @author Brian Demers
 */
public interface User
{
  /**
   * @return The users Id.
   */
  public String getUserId();

  /**
   * Set the user Id.
   */
  public void setUserId(String userId);

  /**
   * @return the Users name.
   * @deprecated use getFirstName, and getLastName
   */
  @Deprecated
  public String getName();

  /**
   * Sets the users name.
   *
   * @deprecated use setFirstName, and setLastName
   */
  @Deprecated
  public void setName(String name);

  /**
   * Gets the users first name.
   */
  public String getFirstName();

  /**
   * Sets the users first name.
   */
  public void setFirstName(String firstName);

  /**
   * Gets the users last name.
   */
  public String getLastName();

  /**
   * Sets the users last name.
   */
  public void setLastName(String lastName);

  /**
   * @return the users email address.
   */
  public String getEmailAddress();

  /**
   * Set the users email address.
   */
  public void setEmailAddress(String emailAddress);

  /**
   * @return the users source Id.
   */
  public String getSource();

  /**
   * Set the users source.
   */
  public void setSource(String source);

  /**
   * Adds a role Identifier to the user.
   */
  public void addRole(RoleIdentifier roleIdentifier);

  /**
   * Remove a Role Identifier from the user.
   */
  public boolean removeRole(RoleIdentifier roleIdentifier);

  /**
   * Adds a set of RoleIdentifier to the user.
   */
  public void addAllRoles(Set<RoleIdentifier> roleIdentifiers);

  /**
   * @return returns all the users roles.
   */
  public Set<RoleIdentifier> getRoles();

  /**
   * Sets the users roles.
   */
  public void setRoles(Set<RoleIdentifier> roles);

  /**
   * @return the users status.
   */
  public UserStatus getStatus();

  /**
   * Sets the users status.
   */
  public void setStatus(UserStatus status);

}
