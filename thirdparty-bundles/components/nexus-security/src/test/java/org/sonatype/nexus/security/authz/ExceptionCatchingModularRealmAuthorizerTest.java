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
package org.sonatype.nexus.security.authz;

import java.util.Collections;

import org.sonatype.goodies.testsupport.TestSupport;

import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.AuthenticationInfo;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.authz.AuthorizationInfo;
import org.apache.shiro.authz.Permission;
import org.apache.shiro.authz.permission.AllPermission;
import org.apache.shiro.realm.AuthorizingRealm;
import org.apache.shiro.realm.Realm;
import org.apache.shiro.subject.PrincipalCollection;
import org.junit.Assert;
import org.junit.Test;

public class ExceptionCatchingModularRealmAuthorizerTest
  extends TestSupport
{
  private static final AuthorizingRealm BROKEN_REALM = new AuthorizingRealm()
  {
    @Override
    protected AuthenticationInfo doGetAuthenticationInfo(AuthenticationToken token) throws AuthenticationException {
      throw new RuntimeException("This realm only throws exceptions");
    }

    @Override
    protected AuthorizationInfo doGetAuthorizationInfo(PrincipalCollection principals) {
      throw new RuntimeException("This realm only throws exceptions");
    }
  };

  @Test
  public void ignoreRuntimeException() throws Exception {
    ExceptionCatchingModularRealmAuthorizer subject =
        new ExceptionCatchingModularRealmAuthorizer(Collections.<Realm>singleton(BROKEN_REALM));

    Permission permission = new AllPermission();

    Assert.assertFalse(subject.isPermitted(null, ""));
    Assert.assertFalse(subject.isPermitted(null, permission));
    Assert.assertFalse(subject.isPermitted(null, new String[]{""})[0]);
    Assert.assertFalse(subject.isPermitted(null, Collections.singletonList(permission))[0]);
  }
}
