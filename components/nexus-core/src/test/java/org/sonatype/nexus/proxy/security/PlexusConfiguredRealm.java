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
package org.sonatype.nexus.proxy.security;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.AuthenticationInfo;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.authc.SimpleAuthenticationInfo;
import org.apache.shiro.authc.credential.AllowAllCredentialsMatcher;
import org.apache.shiro.authc.credential.CredentialsMatcher;
import org.apache.shiro.authz.AuthorizationInfo;
import org.apache.shiro.authz.Permission;
import org.apache.shiro.authz.SimpleAuthorizationInfo;
import org.apache.shiro.authz.permission.WildcardPermission;
import org.apache.shiro.realm.AuthorizingRealm;
import org.apache.shiro.subject.PrincipalCollection;
import org.codehaus.plexus.util.StringUtils;

public class PlexusConfiguredRealm
    extends AuthorizingRealm
{

  private Map<String, String> userPrivilageMap;

  @Override
  protected AuthorizationInfo doGetAuthorizationInfo(PrincipalCollection principals) {
    String username = (String) principals.iterator().next();

    // check the userPrivilageMap key set for the user

    if (StringUtils.isNotEmpty(username) && this.userPrivilageMap.containsKey(username)) {
      SimpleAuthorizationInfo info = new SimpleAuthorizationInfo();

      // "nexus:target:" + <targetId> + ":" + <repoId> + ":" + <action>
      // String priv = "nexus:target:" + "*" + ":" + "repo1" + ":" + "*";
      info.addObjectPermissions(this.buildPermissions(this.userPrivilageMap.get(username)));
      return info;
    }

    return null;
  }

  private List<Permission> buildPermissions(String commaSeperatedList) {
    String[] privs = commaSeperatedList.split(",");
    List<Permission> permissions = new ArrayList<Permission>();

    for (int ii = 0; ii < privs.length; ii++) {
      permissions.add(new WildcardPermission(privs[ii].trim()));
    }
    return permissions;
  }

  @Override
  protected AuthenticationInfo doGetAuthenticationInfo(AuthenticationToken token)
      throws AuthenticationException
  {
    if (this.userPrivilageMap.containsKey(token.getPrincipal().toString())) {
      return new SimpleAuthenticationInfo(token.getPrincipal().toString(), token.getCredentials(), this.getName());
    }

    return null;
  }

  @Override
  public CredentialsMatcher getCredentialsMatcher() {
    return new AllowAllCredentialsMatcher();
  }
}
