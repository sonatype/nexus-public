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
import org.sonatype.nexus.security.internal.SecurityConfigurationCleanerImpl;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertFalse;

/**
 * Tests for {@link SecurityConfigurationCleanerImpl}.
 */
public class SecuritySettingsCleanerImplTest
    extends TestSupport
{
  private SecurityConfigurationCleanerImpl underTest;

  private MemorySecurityConfiguration configuration;

  @Before
  public void setUp() throws Exception {
    underTest = new SecurityConfigurationCleanerImpl();
    configuration = DefaultSecurityConfigurationCleanerTestSecurity.securityModel();
  }

  @Test
  public void testRemovePrivilege() throws Exception {
    String privilegeId = configuration.getPrivileges().get(0).getId();
    configuration.removePrivilege(privilegeId);

    underTest.privilegeRemoved(configuration, privilegeId);

    for (CRole role : configuration.getRoles()) {
      assertFalse(role.getPrivileges().contains(privilegeId));
    }
  }

  @Test
  public void testRemoveRole() throws Exception {
    String roleId = configuration.getRoles().get(0).getId();
    configuration.removeRole(roleId);

    underTest.roleRemoved(configuration, roleId);

    for (CRole crole : configuration.getRoles()) {
      assertFalse(crole.getPrivileges().contains(roleId));
    }

    for (CUserRoleMapping mapping : configuration.getUserRoleMappings()) {
      assertFalse(mapping.getRoles().contains(roleId));
    }
  }
}
