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
package org.sonatype.security.ldap.usermanagement;

import java.io.FileOutputStream;

import org.sonatype.security.ldap.LdapTestSupport;
import org.sonatype.security.usermanagement.UserManager;
import org.sonatype.security.usermanagement.UserNotFoundTransientException;

import junit.framework.Assert;
import org.apache.commons.io.IOUtils;
import org.junit.Test;

public class LdapUserManagerNotConfiguredIT
    extends LdapTestSupport
{
  @Override
  public void setUp()
      throws Exception
  {
    super.setUp();

    IOUtils.copy(getClass().getResourceAsStream("/test-conf/conf/security-configuration-no-ldap.xml"),
        new FileOutputStream(getNexusSecurityConfiguration()));

    IOUtils.copy(getClass().getResourceAsStream("/test-conf/conf/security-configuration.xml"),
        new FileOutputStream(getSecurityConfiguration()));

    getLdapRealmConfig().delete();

    // IOUtil.copy(
    // getClass().getResourceAsStream( "/test-conf/conf/ldap.xml" ),
    // new FileOutputStream( new File( CONF_HOME, "ldap.xml" ) ) );
  }

  @Test
  public void testNotConfigured()
      throws Exception
  {
    UserManager userManager = this.lookup(UserManager.class, "LDAP");
    try {
      userManager.getUser("cstamas");

      Assert.fail("Expected UserNotFoundTransientException");
    }
    catch (UserNotFoundTransientException e) {
      // expect transient error due to misconfiguration
    }
  }
}
