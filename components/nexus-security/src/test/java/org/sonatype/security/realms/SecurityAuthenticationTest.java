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
package org.sonatype.security.realms;

import org.sonatype.security.AbstractSecurityTest;
import org.sonatype.security.SecuritySystem;
import org.sonatype.security.authentication.AuthenticationException;

import org.apache.shiro.authc.UsernamePasswordToken;
import org.apache.shiro.subject.SimplePrincipalCollection;
import org.apache.shiro.subject.Subject;
import org.junit.Ignore;

public class SecurityAuthenticationTest
    extends AbstractSecurityTest
{
  private SecuritySystem security;

  protected void setUp()
      throws Exception
  {
    super.setUp();

    security = (SecuritySystem) lookup(SecuritySystem.class); // started in parent class
  }

  /**
   * Security system is not restartable since the use of Shiro Guice Module, the Shiro Guice support. Test ignored.
   */
  @Ignore
  public void INGNOREtestAuthcAndAuthzAfterRestart()
      throws Exception
  {
    testSuccessfulAuthentication();
    testAuthorization();
    security.stop();
    security.start();
    testSuccessfulAuthentication();
    testAuthorization();
  }

  public void testSuccessfulAuthentication()
      throws Exception
  {
    UsernamePasswordToken upToken = new UsernamePasswordToken("username", "password");

    // this.setupLoginContext( "test" );

    Subject ai = security.login(upToken);

    assertEquals("username", ai.getPrincipal().toString());
  }

  public void testFailedAuthentication()
      throws Exception
  {
    UsernamePasswordToken upToken = new UsernamePasswordToken("username", "badpassword");

    try {
      security.login(upToken);

      fail("Authentication should have failed");
    }
    catch (AuthenticationException e) {
      // good
    }
  }

  public void testAuthorization()
      throws Exception
  {
    assertTrue(security.isPermitted(new SimplePrincipalCollection("username", FakeRealm1.class.getName()),
        "test:perm"));

    assertTrue(security.isPermitted(new SimplePrincipalCollection("username", FakeRealm1.class.getName()),
        "other:perm"));

    assertTrue(security.isPermitted(new SimplePrincipalCollection("username", FakeRealm2.class.getName()),
        "other:perm"));

    assertTrue(security.isPermitted(new SimplePrincipalCollection("username", FakeRealm2.class.getName()),
        "test:perm"));
  }

}
