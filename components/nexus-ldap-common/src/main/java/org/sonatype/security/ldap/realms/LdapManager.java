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
package org.sonatype.security.ldap.realms;

import java.util.Set;
import java.util.SortedSet;

import org.sonatype.security.authentication.AuthenticationException;
import org.sonatype.security.ldap.dao.LdapDAOException;
import org.sonatype.security.ldap.dao.LdapUser;
import org.sonatype.security.ldap.dao.NoLdapUserRolesFoundException;
import org.sonatype.security.ldap.dao.NoSuchLdapGroupException;
import org.sonatype.security.ldap.dao.NoSuchLdapUserException;


public interface LdapManager
{
  public LdapUser authenticateUser(String userId, String password) throws AuthenticationException;

  public abstract Set<String> getUserRoles(String userId)
      throws LdapDAOException, NoLdapUserRolesFoundException;

  public abstract SortedSet<LdapUser> getAllUsers()
      throws LdapDAOException;

  public abstract SortedSet<LdapUser> getUsers(int userCount)
      throws LdapDAOException;

  public abstract LdapUser getUser(String username)
      throws NoSuchLdapUserException,
             LdapDAOException;

  public abstract SortedSet<LdapUser> searchUsers(String username, Set<String> roleIds)
      throws LdapDAOException;

  public abstract SortedSet<String> getAllGroups()
      throws LdapDAOException;

  public abstract String getGroupName(String groupId)
      throws LdapDAOException, NoSuchLdapGroupException;

}
