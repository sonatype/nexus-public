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
package org.sonatype.nexus.security.config;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.security.config.memory.MemoryCUserRoleMapping;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

@RunWith(Parameterized.class)
public class MemorySecurityConfigurationTest
    extends TestSupport
{
  @Parameters(name = "userRoleMappings for source: '{0}' read isFound: {1}")
  public static Object[][] params() {
    return new Object[][]{{"default", false}, {"ldap", true}, {"crowd", true}, {"other", false}};
  }

  @Parameter
  public String src;

  @Parameter(1)
  public boolean isFound;

  private MemorySecurityConfiguration config;

  @Before
  public void setup() {
    config = new MemorySecurityConfiguration();
  }

  @Test
  public void testGetUserRoleMapping() {
    MemoryCUserRoleMapping newUserRoleMapping =
        new MemoryCUserRoleMapping().withUserId("userid").withSource(src).withRoles("test-role");
    config.addUserRoleMapping(newUserRoleMapping);

    CUserRoleMapping roleMapping = config.getUserRoleMapping("USERID", src);

    assertThat(roleMapping != null, is(isFound));

    if (isFound) {
      roleMapping.setRoles(newUserRoleMapping.getRoles());
    }
  }
}
