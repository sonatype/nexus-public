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
package org.sonatype.nexus;

import org.sonatype.security.SecuritySystem;
import org.sonatype.security.authentication.AuthenticationException;
import org.sonatype.security.ldap.LdapConstants;

import org.apache.shiro.authc.UsernamePasswordToken;
import org.apache.shiro.subject.SimplePrincipalCollection;
import org.junit.Assert;
import org.junit.Test;

public class LdapNexusIT
    extends NexusLdapTestSupport
{

  @Test
  public void testAuthentication()
      throws Exception
  {
    SecuritySystem security = lookup(SecuritySystem.class);
    security.start();

    Assert.assertNotNull(security.authenticate(new UsernamePasswordToken("cstamas", "cstamas123")));
  }

  @Test
  public void testAuthenticationFailure()
      throws Exception
  {
    SecuritySystem security = lookup(SecuritySystem.class);
    security.start();

    try {
      Assert.assertNull(security.authenticate(new UsernamePasswordToken("cstamas", "INVALID")));
    }
    catch (AuthenticationException e) {
      // expected
    }
  }

  @Test
  public void testAuthorization()
      throws Exception
  {
    SecuritySystem security = lookup(SecuritySystem.class);
    security.start();

    SimplePrincipalCollection principals = new SimplePrincipalCollection();
    principals.add("cstamas", LdapConstants.REALM_NAME);

    Assert.assertTrue(security.hasRole(principals, "developer"));
    Assert.assertFalse(security.hasRole(principals, "JUNK"));
  }

  @Test
  public void testAuthorizationPriv()
      throws Exception
  {
    SecuritySystem security = lookup(SecuritySystem.class);
    security.start();

    SimplePrincipalCollection principals = new SimplePrincipalCollection();
    principals.add("cstamas", LdapConstants.REALM_NAME);

    Assert.assertTrue(security.isPermitted(principals, "security:usersforgotpw:create"));
    Assert.assertFalse(security.isPermitted(principals, "security:usersforgotpw:delete"));
  }
}
