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

import java.net.MalformedURLException;
import java.util.Set;
import java.util.SortedSet;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.security.authentication.AuthenticationException;
import org.sonatype.security.ldap.LdapAuthenticator;
import org.sonatype.security.ldap.dao.LdapAuthConfiguration;
import org.sonatype.security.ldap.dao.LdapDAOException;
import org.sonatype.security.ldap.dao.LdapGroupDAO;
import org.sonatype.security.ldap.dao.LdapUser;
import org.sonatype.security.ldap.dao.LdapUserDAO;
import org.sonatype.security.ldap.dao.NoLdapUserRolesFoundException;
import org.sonatype.security.ldap.dao.NoSuchLdapGroupException;
import org.sonatype.security.ldap.dao.NoSuchLdapUserException;
import org.sonatype.security.ldap.realms.connector.DefaultLdapConnector;
import org.sonatype.security.ldap.realms.connector.LdapConnector;
import org.sonatype.security.ldap.realms.persist.LdapConfiguration;
import org.sonatype.security.ldap.realms.persist.model.CConnectionInfo;
import org.sonatype.security.ldap.realms.tools.LdapURL;
import org.sonatype.sisu.goodies.common.ComponentSupport;

import org.apache.shiro.realm.ldap.LdapContextFactory;
import org.codehaus.plexus.util.StringUtils;

import static com.google.common.base.Preconditions.checkNotNull;

@Singleton
@Named
public class SimpleLdapManager
    extends ComponentSupport
    implements LdapManager
{
  private final LdapAuthenticator ldapAuthenticator;

  private final LdapUserDAO ldapUserManager;

  private final LdapGroupDAO ldapGroupManager;

  private final LdapConfiguration ldapConfiguration;

  private LdapConnector ldapManagerStrategy;

  @Inject
  public SimpleLdapManager(LdapAuthenticator ldapAuthenticator,
                           LdapUserDAO ldapUserManager,
                           LdapGroupDAO ldapGroupManager,
                           LdapConfiguration ldapConfiguration)
  {
    this.ldapAuthenticator = checkNotNull(ldapAuthenticator);
    this.ldapUserManager = checkNotNull(ldapUserManager);
    this.ldapGroupManager = checkNotNull(ldapGroupManager);
    this.ldapConfiguration = checkNotNull(ldapConfiguration);
  }

  @Override
  public SortedSet<String> getAllGroups()
      throws LdapDAOException
  {
    return this.getLdapManagerConnector().getAllGroups();
  }

  @Override
  public SortedSet<LdapUser> getAllUsers()
      throws LdapDAOException
  {
    return this.getLdapManagerConnector().getAllUsers();
  }

  @Override
  public String getGroupName(String groupId)
      throws LdapDAOException, NoSuchLdapGroupException
  {
    return this.getLdapManagerConnector().getGroupName(groupId);
  }

  @Override
  public LdapUser getUser(String username)
      throws NoSuchLdapUserException,
             LdapDAOException
  {
    return this.getLdapManagerConnector().getUser(username);
  }

  @Override
  public Set<String> getUserRoles(String userId)
      throws LdapDAOException, NoLdapUserRolesFoundException
  {
    return this.getLdapManagerConnector().getUserRoles(userId);
  }

  @Override
  public SortedSet<LdapUser> getUsers(int userCount)
      throws LdapDAOException
  {
    return this.getLdapManagerConnector().getUsers(userCount);
  }

  @Override
  public SortedSet<LdapUser> searchUsers(String username, Set<String> roleIds)
      throws LdapDAOException
  {
    return this.getLdapManagerConnector().searchUsers(username, roleIds);
  }

  private LdapConnector getLdapManagerConnector()
      throws LdapDAOException
  {
    if (this.ldapManagerStrategy == null) {
      this.ldapManagerStrategy = new DefaultLdapConnector(
          "test",
          this.ldapUserManager,
          this.ldapGroupManager,
          this.getLdapContextFactory(),
          this.getLdapAuthConfiguration());
    }
    return this.ldapManagerStrategy;
  }

  protected LdapConfiguration getLdapConfiguration() {
    return this.ldapConfiguration;
  }

  protected LdapAuthConfiguration getLdapAuthConfiguration() {
    return this.getLdapConfiguration().getLdapAuthConfiguration();
  }

  protected LdapContextFactory getLdapContextFactory()
      throws LdapDAOException
  {
    DefaultLdapContextFactory defaultLdapContextFactory = new DefaultLdapContextFactory();

    if (this.getLdapConfiguration() == null || this.getLdapConfiguration().readConnectionInfo() == null) {
      throw new LdapDAOException("Ldap connection is not configured.");
    }

    CConnectionInfo connInfo = this.getLdapConfiguration().readConnectionInfo();

    String url;
    try {
      url = new LdapURL(connInfo.getProtocol(), connInfo.getHost(), connInfo.getPort(), connInfo.getSearchBase())
          .toString();
    }
    catch (MalformedURLException e) {
      // log an error, because the user could still log in and fix the config.
      this.log.error("LDAP Configuration is Invalid.");
      throw new LdapDAOException("Invalid LDAP URL: " + e.getMessage());
    }

    defaultLdapContextFactory.setUsePooling(true);
    defaultLdapContextFactory.setUrl(url);
    defaultLdapContextFactory.setSystemUsername(connInfo.getSystemUsername());
    defaultLdapContextFactory.setSystemPassword(connInfo.getSystemPassword());
    defaultLdapContextFactory.setSearchBase(connInfo.getSearchBase());
    defaultLdapContextFactory.setAuthentication(connInfo.getAuthScheme());

    return defaultLdapContextFactory;
  }

  @Override
  public LdapUser authenticateUser(String userId, String password) throws AuthenticationException {
    try {
      LdapUser ldapUser = this.getUser(userId);

      String authScheme = this.getLdapConfiguration().readConnectionInfo().getAuthScheme();

      if (StringUtils.isEmpty(this
          .getLdapConfiguration().readUserAndGroupConfiguration().getUserPasswordAttribute())) {
        // auth with bind

        this.ldapAuthenticator.authenticateUserWithBind(
            ldapUser,
            password,
            this.getLdapContextFactory(),
            authScheme);
      }
      else {
        // auth by checking password,
        this.ldapAuthenticator.authenticateUserWithPassword(ldapUser, password);
      }

      // everything was successful
      return ldapUser;
    }
    catch (Exception e) {
      if (this.log.isDebugEnabled()) {
        this.log.debug("Failed to find user: " + userId, e);
      }
    }
    throw new AuthenticationException("User: " + userId + " could not be authenticated.");
  }

}
