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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.naming.InvalidNameException;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import javax.naming.ldap.LdapContext;
import javax.naming.ldap.LdapName;

import org.sonatype.security.ldap.LdapEncoder;
import org.sonatype.sisu.goodies.common.ComponentSupport;

import org.codehaus.plexus.util.StringUtils;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.sonatype.security.ldap.LdapEncoder.nameEncode;

/**
 * @author cstamas
 */
@Singleton
@Named
public class DefaultLdapGroupDAO
    extends ComponentSupport
    implements LdapGroupDAO
{
  private final LdapUserDAO ldapUserManager;

  @Inject
  public DefaultLdapGroupDAO(final LdapUserDAO ldapUserManager) {
    this.ldapUserManager = checkNotNull(ldapUserManager);
  }

  private static boolean isGroupsEnabled(LdapAuthConfiguration configuration) {
    return configuration.isLdapGroupsAsRoles();
  }

  public Set<String> getGroupMembership(String username, LdapContext context, LdapAuthConfiguration configuration)
      throws LdapDAOException,
             NoLdapUserRolesFoundException
  {

    boolean dynamicGroups = !StringUtils.isEmpty(configuration.getUserMemberOfAttribute());
    boolean groupsEnabled = isGroupsEnabled(configuration);

    Set<String> roleIds = new HashSet<String>();
    if (groupsEnabled) {
      if (!dynamicGroups) {
        roleIds = this.getGroupMembershipFromGroups(username, context, configuration);
      }
      else {
        try {
          roleIds = this.getGroupMembershipFromUser(username, context, configuration);
        }
        catch (NoSuchLdapUserException e) {
          throw new NoLdapUserRolesFoundException(username);
        }
      }

      if (roleIds == null | roleIds.isEmpty()) {
        throw new NoLdapUserRolesFoundException(username);
      }
    }
    else if (dynamicGroups && !groupsEnabled) {
      throw new NoLdapUserRolesFoundException(username);
    }

    return roleIds;
  }

  public Set<String> getAllGroups(LdapContext context, LdapAuthConfiguration configuration)
      throws LdapDAOException
  {
    Set<String> groups = new HashSet<String>();
    ;

    if (isGroupsEnabled(configuration)) {
      try {

        if (StringUtils.isEmpty(configuration.getUserMemberOfAttribute())) {
          // static groups
          String groupIdAttribute = configuration.getGroupIdAttribute();
          String groupBaseDn = StringUtils.defaultString(configuration.getGroupBaseDn(), "");

          String filter = "(objectClass=" + configuration.getGroupObjectClass() + ")";

          log.debug(
              "Searching for groups in group DN: " + groupBaseDn + "\nUsing filter: \'" + filter + "\'");

          SearchControls ctls = this.getBaseSearchControls(new String[]{groupIdAttribute}, configuration
              .isGroupSubtree());

          NamingEnumeration<SearchResult> results = context.search(groupBaseDn, filter, ctls);
          try {
            groups = this.getGroupIdsFromSearch(results, groupIdAttribute, configuration);
          }
          finally {
            results.close();
          }
        }
        else {
          // dynamic groups
          String memberOfAttribute = configuration.getUserMemberOfAttribute();

          String filter = "(objectClass=" + configuration.getUserObjectClass() + ")";

          SearchControls ctls = this.getBaseSearchControls(new String[]{memberOfAttribute}, true);

          String userBaseDn = StringUtils.defaultString(configuration.getUserBaseDn(), "");

          Set<String> roles = this.getGroupIdsFromSearch(
              context.search(userBaseDn, filter, ctls),
              memberOfAttribute,
              configuration);

          for (String groupDN : roles) {
            groups.add(this.getGroupFromString(groupDN));
          }
        }

      }
      catch (NamingException e) {
        String message = "Failed to get list of groups.";

        throw new LdapDAOException(message, e);
      }
    }
    return groups;
  }

  private SearchControls getBaseSearchControls(String[] returningAttributes, boolean subtree) {
    SearchControls ctls = new SearchControls();
    ctls.setReturningAttributes(returningAttributes);
    ctls.setSearchScope(subtree ? SearchControls.SUBTREE_SCOPE : SearchControls.ONELEVEL_SCOPE);
    return ctls;
  }

  private Set<String> getGroupIdsFromSearch(NamingEnumeration searchResults, String groupIdAttribute,
                                            LdapAuthConfiguration configuration)
      throws NamingException
  {
    Set<String> roles = new LinkedHashSet<String>();

    Map<String, Set<String>> mappings = configuration.getGroupReverseMappings();

    while (searchResults.hasMoreElements()) {
      SearchResult result = (SearchResult) searchResults.nextElement();
      Attribute groupIdAttr = result.getAttributes().get(groupIdAttribute);

      // some users might not have any groups, (no memberOf attribute)
      if (groupIdAttr != null) {
        // get all the attributes
        NamingEnumeration attributes = groupIdAttr.getAll();
        while (attributes.hasMoreElements()) {
          String group = String.valueOf(attributes.nextElement());

          Set<String> mappedRoles = mappings.get(group);
          if (mappedRoles == null) {
            roles.add(group);
          }
          else {
            roles.addAll(mappedRoles);
          }
        }
      }
    }

    return roles;
  }

  private Set<String> getGroupMembershipFromUser(String username, LdapContext context,
                                                 LdapAuthConfiguration configuration)
      throws LdapDAOException,
             NoSuchLdapUserException
  {
    LdapUser user = this.ldapUserManager.getUser(username, context, configuration);
    return Collections.unmodifiableSet(user.getMembership());
  }

  private Set<String> getGroupMembershipFromGroups(String username, LdapContext context,
                                                   LdapAuthConfiguration configuration)
      throws LdapDAOException
  {
    String groupIdAttribute = configuration.getGroupIdAttribute();
    String groupMemberAttribute = configuration.getGroupMemberAttribute();
    String groupBaseDn = StringUtils.defaultString(configuration.getGroupBaseDn(), "");

    String groupMemberFormat = configuration.getGroupMemberFormat();

    String filter = "(&(objectClass={0})(&({1}=*)(";
    ArrayList<String> filterValues = new ArrayList<>();
    filterValues.add(configuration.getGroupObjectClass());
    filterValues.add(groupIdAttribute);

    if (groupMemberFormat != null) {
      String member = StringUtils.replace( groupMemberFormat, "${username}", "{2}" );
      if (groupMemberFormat.contains("${username}")) {
        filterValues.add(nameEncode(username));
      }

      if (groupMemberFormat.contains("${dn}")) {
        LdapUser user;
        try {
          user = this.ldapUserManager.getUser(username, context, configuration);
        }
        catch (NoSuchLdapUserException e) {
          String message = "Failed to retrieve role information from ldap for user: " + username;
          throw new LdapDAOException(message, e);
        }
        member = StringUtils.replace(member, "${dn}", "{" + filterValues.size() + "}");
        filterValues.add(user.getDn());
      }

      filter += groupMemberAttribute + "=" + member + ")))";
    }
    else {
      filterValues.add(nameEncode(username));
      filter += groupMemberAttribute + "={2})))";
    }

    log.debug(
        "Searching for group membership of: " + username + " in group DN: " + groupBaseDn + "\nUsing filter: \'"
            + filter + "\'");

    try {
      SearchControls ctls = this.getBaseSearchControls(new String[]{groupIdAttribute}, configuration
          .isGroupSubtree());
      NamingEnumeration<SearchResult> results = context.search( groupBaseDn, filter, filterValues.toArray(), ctls );
      try {
        Set<String> roles = this.getGroupIdsFromSearch(results, groupIdAttribute, configuration);
        return roles;
      }
      finally {
        results.close();
      }
    }
    catch (NamingException e) {
      String message = "Failed to retrieve role information from ldap for user: " + username;

      throw new LdapDAOException(message, e);
    }
  }

  private String getGroupFromString(String dnString) {
    String result = dnString;
    try {
      LdapName dn = new LdapName(dnString);
      result = String.valueOf(dn.getRdn(dn.size() - 1).getValue());
    }
    catch (InvalidNameException e) {
      log.debug("Expected a Group DN but found: " + dnString);
    }
    return result;
  }

  public String getGroupName(String groupId, LdapContext context, LdapAuthConfiguration configuration)
      throws LdapDAOException,
             NoSuchLdapGroupException
  {
    if (!isGroupsEnabled(configuration)) {
      throw new NoSuchLdapGroupException(groupId, groupId);
    }

    if (StringUtils.isEmpty(configuration.getUserMemberOfAttribute())) {
      // static groups
      String groupIdAttribute = configuration.getGroupIdAttribute();
      String groupBaseDn = StringUtils.defaultString(configuration.getGroupBaseDn(), "");

      String filter = "(&(objectClass=" + configuration.getGroupObjectClass() + ") (" + groupIdAttribute
          + "=" + groupId + "))";

      SearchControls ctls = this.getBaseSearchControls(new String[]{groupIdAttribute}, configuration
          .isGroupSubtree());

      Set<String> groups;
      try {
        NamingEnumeration<SearchResult> results = context.search(groupBaseDn, filter, ctls);
        try {
          groups = this.getGroupIdsFromSearch(results, groupIdAttribute, configuration);
        }
        finally {
          results.close();
        }
      }
      catch (NamingException e) {
        throw new LdapDAOException("Failed to find group: " + groupId, e);
      }
      if (groups.size() <= 0) {
        throw new NoSuchLdapGroupException(groupId, groupId);
      }
      if (groups.size() > 1) {
        throw new NoSuchLdapGroupException(groupId, "More then one group found for group: " + groupId);
      }
      else {
        return groups.iterator().next();
      }
    }
    else {
      String memberOfAttribute = configuration.getUserMemberOfAttribute();

      String filter = "(objectClass=" + configuration.getUserObjectClass() + ")";

      SearchControls ctls = this.getBaseSearchControls(new String[]{memberOfAttribute}, true);

      String userBaseDn = StringUtils.defaultString(configuration.getUserBaseDn(), "");

      Set<String> roles;
      try {
        roles = this.getGroupIdsFromSearch(
            context.search(userBaseDn, filter, ctls),
            memberOfAttribute,
            configuration);

        for (String groupDN : roles) {
          if (groupId.equals(this.getGroupFromString(groupDN))) {
            return groupId;
          }
        }
      }
      catch (NamingException e) {
        throw new LdapDAOException("Failed to find group: " + groupId, e);
      }
    }
    throw new NoSuchLdapGroupException(groupId, groupId);
  }

}
