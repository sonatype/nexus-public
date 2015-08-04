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
package org.sonatype.security.usermanagement.xml;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.enterprise.inject.Typed;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.security.SecuritySystem;
import org.sonatype.security.model.CUserRoleMapping;
import org.sonatype.security.realms.tools.ConfigurationManager;
import org.sonatype.security.usermanagement.AbstractReadOnlyUserManager;
import org.sonatype.security.usermanagement.NoSuchUserManagerException;
import org.sonatype.security.usermanagement.User;
import org.sonatype.security.usermanagement.UserManager;
import org.sonatype.security.usermanagement.UserNotFoundException;
import org.sonatype.security.usermanagement.UserSearchCriteria;

import org.codehaus.plexus.util.CollectionUtils;
import org.codehaus.plexus.util.StringUtils;
import org.eclipse.sisu.Description;

/**
 * A user manger that returns all users that have roles defined in the security.xml file. This allows you to easily
 * search for users that have added roles. For example if your users generally come from an external Realm (LDAP) you
 * could see which users have had roles added to them.
 *
 * @author Brian Demers
 */
@Singleton
@Typed(UserManager.class)
@Named("allConfigured")
@Description("All Configured Users")
public class ConfiguredUsersUserManager
    extends AbstractReadOnlyUserManager
{
  private final SecuritySystem securitySystem;

  private final ConfigurationManager configuration;

  public static final String SOURCE = "allConfigured";

  @Inject
  public ConfiguredUsersUserManager(SecuritySystem securitySystem,
                                    @Named("default") ConfigurationManager configuration)
  {
    this.securitySystem = securitySystem;
    this.configuration = configuration;
  }

  public String getSource() {
    return SOURCE;
  }

  public Set<User> listUsers() {
    Set<User> users = new HashSet<User>();

    List<CUserRoleMapping> userRoleMappings = this.configuration.listUserRoleMappings();
    for (CUserRoleMapping userRoleMapping : userRoleMappings) {
      try {
        User user = this.getSecuritySystem().getUser(userRoleMapping.getUserId(), userRoleMapping.getSource());
        if (user != null) {
          users.add(user);
        }
      }
      catch (UserNotFoundException e) {
        this.log.warn("User: '" + userRoleMapping.getUserId() + "' of source: '"
            + userRoleMapping.getSource() + "' could not be found.");

        this.log.debug("Most likely caused by a user role mapping that is invalid.", e);
      }
      catch (NoSuchUserManagerException e) {
        this.log.warn("User: '" + userRoleMapping.getUserId() + "' of source: '"
            + userRoleMapping.getSource() + "' could not be found.", e);
      }
    }

    return users;
  }

  public Set<String> listUserIds() {
    Set<String> userIds = new HashSet<String>();

    Set<User> users = new HashSet<User>();

    for (User user : users) {
      userIds.add(user.getUserId());
    }

    List<CUserRoleMapping> userRoleMappings = this.configuration.listUserRoleMappings();
    for (CUserRoleMapping userRoleMapping : userRoleMappings) {
      String userId = userRoleMapping.getUserId();
      if (StringUtils.isNotEmpty(userId)) {
        userIds.add(userId);
      }
    }

    return userIds;
  }

  public User getUser(String userId) {
    // this resource will only list the users
    return null;
  }

  public Set<User> searchUsers(UserSearchCriteria criteria) {
    // we only want to do this if the criteria is set to the source
    if (this.getSource().equals(criteria.getSource())) {
      return this.filterListInMemeory(this.listUsers(), criteria);
    }
    else {
      return new HashSet<User>();
    }
  }

  private SecuritySystem getSecuritySystem() {
    return this.securitySystem;
  }

  /*
   * (non-Javadoc)
   * @see org.sonatype.security.usermanagement.AbstractUserManager#matchesCriteria(java.lang.String, java.lang.String,
   * java.util.Collection, org.sonatype.security.usermanagement.UserSearchCriteria)
   */
  protected boolean matchesCriteria(String userId, String userSource, Collection<String> usersRoles,
                                    UserSearchCriteria criteria)
  {
    // basically the same as the super, but we don't want to check the source
    if (StringUtils.isNotEmpty(criteria.getUserId())
        && !userId.toLowerCase().startsWith(criteria.getUserId().toLowerCase())) {
      return false;
    }

    if (criteria.getOneOfRoleIds() != null && !criteria.getOneOfRoleIds().isEmpty()) {
      Set<String> userRoles = new HashSet<String>();
      if (usersRoles != null) {
        userRoles.addAll(usersRoles);
      }

      // check the intersection of the roles
      if (CollectionUtils.intersection(criteria.getOneOfRoleIds(), userRoles).isEmpty()) {
        return false;
      }
    }

    return true;
  }

  public String getAuthenticationRealmName() {
    return null;
  }

}
