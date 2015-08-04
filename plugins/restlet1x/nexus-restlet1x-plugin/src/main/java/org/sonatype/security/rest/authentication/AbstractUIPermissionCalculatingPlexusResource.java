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
package org.sonatype.security.rest.authentication;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.sonatype.security.authorization.Privilege;
import org.sonatype.security.rest.AbstractSecurityPlexusResource;
import org.sonatype.security.rest.model.AuthenticationClientPermissions;
import org.sonatype.security.rest.model.ClientPermission;
import org.sonatype.security.usermanagement.User;
import org.sonatype.security.usermanagement.UserNotFoundException;

import org.apache.commons.lang.StringUtils;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authz.Permission;
import org.apache.shiro.authz.permission.WildcardPermission;
import org.apache.shiro.subject.Subject;
import org.restlet.data.Request;
import org.restlet.resource.ResourceException;

public abstract class AbstractUIPermissionCalculatingPlexusResource
    extends AbstractSecurityPlexusResource
{
  private static final int NONE = 0;

  private static final int READ = 1;

  private static final int UPDATE = 2;

  private static final int DELETE = 4;

  private static final int CREATE = 8;

  private static final int ALL = READ | UPDATE | DELETE | CREATE;

  protected AuthenticationClientPermissions getClientPermissionsForCurrentUser(Request request)
      throws ResourceException
  {
    AuthenticationClientPermissions perms = new AuthenticationClientPermissions();

    Subject subject = SecurityUtils.getSubject();

    if (getSecuritySystem().isAnonymousAccessEnabled()) {
      perms.setLoggedIn(!getSecuritySystem().getAnonymousUsername().equals(subject.getPrincipal()));
    }
    else {
      // anon access is disabled, simply ask JSecurity about this
      perms.setLoggedIn(subject != null && subject.isAuthenticated());
    }

    if (perms.isLoggedIn()) {
      // try to set the loggedInUsername
      Object principal = subject.getPrincipal();

      if (principal != null) {
        perms.setLoggedInUsername(principal.toString());
      }
    }

    // need to set the source of the logged in user
    // The UI might need to show/hide something based on the user's source
    // i.e. like the 'Change Password' link.
    String username = perms.getLoggedInUsername();
    if (StringUtils.isNotEmpty(username)) {
      // look up the realm of the user
      try {
        User user = this.getSecuritySystem().getUser(username);
        String source = (user != null) ? user.getSource() : null;
        perms.setLoggedInUserSource(source);
      }
      catch (UserNotFoundException e) {
        if (getLogger().isDebugEnabled()) {
          getLogger().info("Failed to lookup user: {}", username, e);
        }
        else {
          getLogger().info("Failed to lookup user: {}: {}/{}", username, e.getClass().getName(), e.getMessage());
        }
      }
    }

    Map<String, Integer> privilegeMap = new HashMap<String, Integer>();

    for (Privilege priv : getSecuritySystem().listPrivileges()) {
      if (priv.getType().equals("method")) {
        String permission = priv.getPrivilegeProperty("permission");
        privilegeMap.put(permission, NONE);
      }
    }

    // this will update the privilegeMap
    this.checkSubjectsPermissions(subject, privilegeMap);

    for (Entry<String, Integer> privEntry : privilegeMap.entrySet()) {
      ClientPermission cPermission = new ClientPermission();
      cPermission.setId(privEntry.getKey());
      cPermission.setValue(privEntry.getValue());

      perms.addPermission(cPermission);
    }

    return perms;
  }

  private void checkSubjectsPermissions(Subject subject, Map<String, Integer> privilegeMap) {
    List<Permission> permissionList = new ArrayList<Permission>();
    List<String> permissionNameList = new ArrayList<String>();

    for (Entry<String, Integer> priv : privilegeMap.entrySet()) {
      permissionList.add(new WildcardPermission(priv.getKey() + ":read"));
      permissionList.add(new WildcardPermission(priv.getKey() + ":create"));
      permissionList.add(new WildcardPermission(priv.getKey() + ":update"));
      permissionList.add(new WildcardPermission(priv.getKey() + ":delete"));
      permissionNameList.add(priv.getKey() + ":read");
      permissionNameList.add(priv.getKey() + ":create");
      permissionNameList.add(priv.getKey() + ":update");
      permissionNameList.add(priv.getKey() + ":delete");
    }

    if (subject != null) {

      // get the privileges for this subject
      boolean[] boolResults = subject.isPermitted(permissionList);

      // put then in a map so we can access them easily
      Map<String, Boolean> resultMap = new HashMap<String, Boolean>();
      for (int ii = 0; ii < permissionList.size(); ii++) {
        String permissionName = permissionNameList.get(ii);
        boolean b = boolResults[ii];
        resultMap.put(permissionName, b);
      }

      // now loop through the original set and figure out the correct value
      for (Entry<String, Integer> priv : privilegeMap.entrySet()) {

        boolean readPriv = resultMap.get(priv.getKey() + ":read");
        boolean createPriv = resultMap.get(priv.getKey() + ":create");
        boolean updaetPriv = resultMap.get(priv.getKey() + ":update");
        boolean deletePriv = resultMap.get(priv.getKey() + ":delete");

        int perm = NONE;

        if (readPriv) {
          perm |= READ;
        }
        if (createPriv) {
          perm |= CREATE;
        }
        if (updaetPriv) {
          perm |= UPDATE;
        }
        if (deletePriv) {
          perm |= DELETE;
        }
        // now set the value
        priv.setValue(perm);
      }
    }
    else {// subject is null
      for (Entry<String, Integer> priv : privilegeMap.entrySet()) {
        priv.setValue(NONE);
      }
    }

  }
}
