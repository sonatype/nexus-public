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
package org.sonatype.nexus.security.realm;

import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.AuthenticationInfo;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.authc.SimpleAuthenticationInfo;
import org.apache.shiro.authc.UsernamePasswordToken;
import org.apache.shiro.authz.AuthorizationInfo;
import org.apache.shiro.authz.SimpleAuthorizationInfo;
import org.apache.shiro.realm.AuthorizingRealm;
import org.apache.shiro.subject.PrincipalCollection;
import org.eclipse.sisu.Description;

@Singleton
@Named("MockRealmC")
@Description("MockRealmC")
public class MockRealmC extends AuthorizingRealm
{
  @Override
  protected AuthenticationInfo doGetAuthenticationInfo(AuthenticationToken token) throws AuthenticationException {
    // only allow goku/goku
    UsernamePasswordToken userpass = (UsernamePasswordToken) token;
    if ("goku".equals(userpass.getUsername()) && "goku".equals(new String(userpass.getPassword()))) {
      return new SimpleAuthenticationInfo(userpass.getUsername(), new String(userpass.getPassword()), this.getName());
    }

    return null;
  }

  @Override
  protected AuthorizationInfo doGetAuthorizationInfo(PrincipalCollection principals) {
    // make sure the user is goku, (its just for testing)

    if (principals.asList().get(0).toString().equals("goku")) {
      SimpleAuthorizationInfo info = new SimpleAuthorizationInfo();

      info.addRole("test-role1");
      info.addRole("test-role2");

      info.addStringPermission("test:*");

      return info;
    }

    return null;
  }

  @Override
  public String getName() {
    return "MockRealmC";
  }
}
