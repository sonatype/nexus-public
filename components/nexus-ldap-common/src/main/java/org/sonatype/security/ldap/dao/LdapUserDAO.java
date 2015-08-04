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
package org.sonatype.security.ldap.dao;

import java.util.SortedSet;

import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.SearchResult;
import javax.naming.ldap.LdapContext;

import org.sonatype.security.ldap.dao.password.PasswordEncoderManager;


/**
 * @author cstamas
 */
public interface LdapUserDAO
{
  public static final String REALM_KEY = "LDAP";

  public static final String USERNAME = "username";

  public static final String PASSWORD = "password";

  public static final String EMAIL = "email";

  public static final String NAME = "name";

  public static final String WEBSITE = "website";

  public PasswordEncoderManager getPasswordEncoderManager();

  public void removeUser(String username, LdapContext context, LdapAuthConfiguration configuration)
      throws NoSuchLdapUserException, LdapDAOException;

  public void updateUser(LdapUser user, LdapContext context, LdapAuthConfiguration configuration)
      throws NoSuchLdapUserException, LdapDAOException;

  public void changePassword(String username, String password, LdapContext context,
                             LdapAuthConfiguration configuration)
      throws NoSuchLdapUserException, LdapDAOException;

  public NamingEnumeration<SearchResult> searchUsers(String username, LdapContext context,
                                                     LdapAuthConfiguration configuration,
                                                     long limitCount)
      throws NamingException;

  public NamingEnumeration<SearchResult> searchUsers(LdapContext context, LdapAuthConfiguration configuration,
                                                     long limitCount)
      throws NamingException;

  public NamingEnumeration<SearchResult> searchUsers(LdapContext context, String[] returnAttributes,
                                                     LdapAuthConfiguration configuration, long limitCount)
      throws NamingException;

  public NamingEnumeration<SearchResult> searchUsers(String username, LdapContext context, String[] returnAttributes,
                                                     LdapAuthConfiguration configuration, long limitCount)
      throws NamingException;

  public SortedSet<LdapUser> getUsers(LdapContext context, LdapAuthConfiguration configuration, long limitCount)
      throws LdapDAOException;

  public SortedSet<LdapUser> getUsers(String username, LdapContext context, LdapAuthConfiguration configuration,
                                      long limitCount)
      throws LdapDAOException;

  public void createUser(LdapUser user, LdapContext context, LdapAuthConfiguration configuration)
      throws LdapDAOException;

  public LdapUser getUser(String username, LdapContext context, LdapAuthConfiguration configuration)
      throws NoSuchLdapUserException, LdapDAOException;

}
