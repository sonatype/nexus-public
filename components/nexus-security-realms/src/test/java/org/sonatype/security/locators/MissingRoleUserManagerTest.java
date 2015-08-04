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
package org.sonatype.security.locators;

import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

import org.sonatype.security.AbstractSecurityTestCase;
import org.sonatype.security.SecuritySystem;
import org.sonatype.security.usermanagement.RoleIdentifier;
import org.sonatype.security.usermanagement.User;

import junit.framework.Assert;

public class MissingRoleUserManagerTest
    extends AbstractSecurityTestCase
{

  public static final String PLEXUS_SECURITY_XML_FILE = "security-xml-file";

  private final String SECURITY_CONFIG_FILE_PATH = getBasedir()
      + "/target/test-classes/org/sonatype/jsecurity/locators/missingRoleTest-security.xml";

  @Override
  public void configure(Properties properties) {
    properties.put(PLEXUS_SECURITY_XML_FILE, SECURITY_CONFIG_FILE_PATH);
    super.configure(properties);
  }

  // private Set<String> getXMLRoles() throws Exception
  // {
  // PlexusRoleLocator locator = (PlexusRoleLocator) this.lookup( PlexusRoleLocator.class );
  // return locator.listRoleIds();
  // }

  private SecuritySystem getSecuritySystem()
      throws Exception
  {
    return (SecuritySystem) this.lookup(SecuritySystem.class);
  }

  public void testInvalidRoleMapping()
      throws Exception
  {
    SecuritySystem userManager = this.getSecuritySystem();

    User user = userManager.getUser("jcoder");
    Assert.assertNotNull(user);

    Set<String> roleIds = new HashSet<String>();
    for (RoleIdentifier role : user.getRoles()) {
      Assert.assertNotNull("User has null role.", role);
      roleIds.add(role.getRoleId());
    }
    Assert.assertFalse(roleIds.contains("INVALID-ROLE-BLA-BLA"));
  }

}
