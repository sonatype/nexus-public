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

import java.io.FileOutputStream;
import java.io.IOException;

import org.sonatype.security.SecuritySystem;
import org.sonatype.security.ldap.LdapConstants;
import org.sonatype.security.realms.XmlAuthenticatingRealm;

import org.apache.commons.io.IOUtils;
import org.apache.shiro.authc.UsernamePasswordToken;
import org.apache.shiro.subject.SimplePrincipalCollection;
import org.junit.Assert;
import org.junit.Test;

public class MultipleRealmsIT
    extends NexusLdapTestSupport
{

  @Test
  public void testAuthentication()
      throws Exception
  {
    SecuritySystem security = lookup(SecuritySystem.class);
    security.start();

    security.authenticate(new UsernamePasswordToken("cstamas", "cstamas123"));

    security.authenticate(new UsernamePasswordToken("admin", "admin123"));

    security.authenticate(new UsernamePasswordToken("deployment", "deployment123"));
  }

  @Test
  public void testAuthorization()
      throws Exception
  {
    SecuritySystem security = lookup(SecuritySystem.class);
    security.start();

    // LDAP user
    SimplePrincipalCollection principals = new SimplePrincipalCollection();
    principals.add("cstamas", LdapConstants.REALM_NAME);

    Assert.assertTrue(security.hasRole(principals, "nx-developer"));
    Assert.assertFalse(security.hasRole(principals, "JUNK"));

    // xml user
    principals = new SimplePrincipalCollection();
    // users must be from the correct realm now!
    // TODO: bdemers or dbradicich, this "fix" is wrong, it relies on imple details!
    // was: principals.add( "deployment", new XmlAuthenticatingRealm().getName() );
    principals.add("deployment", XmlAuthenticatingRealm.ROLE);

    Assert.assertTrue(security.hasRole(principals, "nx-deployment"));
    Assert.assertFalse(security.hasRole(principals, "JUNK"));

  }

  @Test
  public void testAuthorizationPriv()
      throws Exception
  {
    SecuritySystem security = lookup(SecuritySystem.class);
    security.start();

    // LDAP
    SimplePrincipalCollection principals = new SimplePrincipalCollection();
    principals.add("cstamas", LdapConstants.REALM_NAME);

    Assert.assertTrue(security.isPermitted(principals, "security:usersforgotpw:create"));
    Assert.assertFalse(security.isPermitted(principals, "security:usersforgotpw:delete"));

    // XML
    principals = new SimplePrincipalCollection();
    // TODO: bdemers or dbradicich, this "fix" is wrong, it relies on imple details!
    // was: principals.add( "test-user", new XmlAuthenticatingRealm().getName() );
    principals.add("test-user", XmlAuthenticatingRealm.ROLE);

    Assert.assertTrue(security.isPermitted(principals, "security:usersforgotpw:create"));
    Assert.assertFalse(security.isPermitted(principals, "security:usersforgotpw:delete"));

    Assert.assertTrue(security.isPermitted(principals, "nexus:target:1:*:delete"));

  }

  @Override
  protected void copyDefaultConfigToPlace()
      throws IOException
  {
    IOUtils.copy(getClass().getResourceAsStream("/test-conf/security-configuration-multipleRealms.xml"),
        new FileOutputStream(getSecurityConfiguration()));
  }

}
