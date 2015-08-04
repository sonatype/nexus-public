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
package org.sonatype.security.realms.kenai;

import org.sonatype.nexus.apachehttpclient.Hc4Provider;

import junit.framework.Assert;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.shiro.authc.AccountException;
import org.apache.shiro.authc.AuthenticationInfo;
import org.apache.shiro.authc.UsernamePasswordToken;
import org.apache.shiro.subject.SimplePrincipalCollection;
import org.junit.Test;
import org.mockito.Mockito;

public class KenaiRealmTest
    extends AbstractKenaiRealmTest
{
  @Override
  protected boolean runWithSecurityDisabled() {
    return false;
  }

  private KenaiRealm getRealm()
      throws Exception
  {
    final Hc4Provider mockHc4Provider = Mockito.mock(Hc4Provider.class);
    Mockito.when(mockHc4Provider.createHttpClient()).thenReturn(new DefaultHttpClient());
    return new KenaiRealm(getKenaiRealmConfiguration(), mockHc4Provider);
  }

  @Test
  public void testAuthenticate()
      throws Exception
  {
    KenaiRealm kenaiRealm = this.getRealm();

    AuthenticationInfo info = kenaiRealm.getAuthenticationInfo(new UsernamePasswordToken(username, password));
    Assert.assertNotNull(info);
  }

  @Test
  public void testAuthorize()
      throws Exception
  {
    KenaiRealm kenaiRealm = this.getRealm();
    kenaiRealm.checkRole(new SimplePrincipalCollection(username, kenaiRealm.getName()), DEFAULT_ROLE);
  }

  @Test
  public void testAuthFail()
      throws Exception
  {
    KenaiRealm kenaiRealm = this.getRealm();

    try {
      kenaiRealm.getAuthenticationInfo(new UsernamePasswordToken("random", "JUNK-PASS"));
      Assert.fail("Expected: AccountException to be thrown");
    }
    catch (AccountException e) {
      // expected
    }
  }

  @Test
  public void testAuthFailAuthFail()
      throws Exception
  {
    KenaiRealm kenaiRealm = this.getRealm();

    try {
      Assert.assertNotNull(kenaiRealm.getAuthenticationInfo(new UsernamePasswordToken("unknown-user-foo-bar",
          "invalid")));
      Assert.fail("Expected: AccountException to be thrown");
    }
    catch (AccountException e) {
      // expected
    }

    try {
      kenaiRealm.getAuthenticationInfo(new UsernamePasswordToken("random", "JUNK-PASS"));
      Assert.fail("Expected: AccountException to be thrown");
    }
    catch (AccountException e) {
      // expected
    }

    Assert.assertNotNull(kenaiRealm.getAuthenticationInfo(new UsernamePasswordToken(username, password)));

    try {
      kenaiRealm.getAuthenticationInfo(new UsernamePasswordToken("random", "JUNK-PASS"));
      Assert.fail("Expected: AccountException to be thrown");
    }
    catch (AccountException e) {
      // expected
    }
  }
}
