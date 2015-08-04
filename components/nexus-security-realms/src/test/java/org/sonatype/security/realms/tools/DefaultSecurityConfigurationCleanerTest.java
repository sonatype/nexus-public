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
package org.sonatype.security.realms.tools;

import java.util.List;

import org.sonatype.security.AbstractSecurityTestCase;
import org.sonatype.security.model.CPrivilege;
import org.sonatype.security.model.CRole;
import org.sonatype.security.model.CUserRoleMapping;
import org.sonatype.security.model.Configuration;

public class DefaultSecurityConfigurationCleanerTest
    extends AbstractSecurityTestCase
{
  private DefaultSecurityConfigurationCleaner cleaner;

  @Override
  protected void setUp()
      throws Exception
  {
    super.setUp();

    cleaner = (DefaultSecurityConfigurationCleaner) lookup(SecurityConfigurationCleaner.class);
  }

  public void testRemovePrivilege()
      throws Exception
  {
    Configuration configuration =
        getConfigurationFromStream(getClass().getResourceAsStream(
            "/org/sonatype/security/realms/tools/cleaner-security.xml"));

    CPrivilege priv = (CPrivilege) configuration.getPrivileges().get(0);

    configuration.removePrivilege(priv);

    cleaner.privilegeRemoved(new EnhancedConfiguration(configuration), priv.getId());

    for (CRole role : (List<CRole>) configuration.getRoles()) {
      assertFalse(role.getPrivileges().contains(priv.getId()));
    }
  }

  public void testRemoveRole()
      throws Exception
  {
    Configuration configuration =
        getConfigurationFromStream(getClass().getResourceAsStream(
            "/org/sonatype/security/realms/tools/cleaner-security.xml"));

    CRole role = (CRole) configuration.getRoles().get(0);

    configuration.removeRole(role);

    cleaner.roleRemoved(new EnhancedConfiguration(configuration), role.getId());

    for (CRole crole : (List<CRole>) configuration.getRoles()) {
      assertFalse(crole.getPrivileges().contains(role.getId()));
    }

    for (CUserRoleMapping mapping : (List<CUserRoleMapping>) configuration.getUserRoleMappings()) {
      assertFalse(mapping.getRoles().contains(role.getId()));
    }
  }
}
