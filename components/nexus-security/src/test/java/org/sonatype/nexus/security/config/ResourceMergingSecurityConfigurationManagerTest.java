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

import java.util.ArrayList;
import java.util.List;

import org.sonatype.nexus.security.AbstractSecurityTest;

import com.google.inject.AbstractModule;
import com.google.inject.Module;
import com.google.inject.name.Names;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class ResourceMergingSecurityConfigurationManagerTest
    extends AbstractSecurityTest
{
  private SecurityConfigurationManager manager;

  @Override
  protected MemorySecurityConfiguration initialSecurityConfiguration() {
    return ResourceMergingConfigurationManagerTestSecurity.securityModel();
  }

  @Override
  protected void customizeModules(List<Module> modules) {
    super.customizeModules(modules);
    modules.add(new AbstractModule()
    {
      @Override
      protected void configure() {
        bind(StaticSecurityConfigurationResource.class).annotatedWith(Names.named("s2")).to(StaticSecurityConfigurationResource2.class);
        bind(StaticSecurityConfigurationResource.class).annotatedWith(Names.named("s1")).to(StaticSecurityConfigurationResource1.class);
      }
    });
  }

  @Override
  protected void setUp() throws Exception {
    super.setUp();

    manager = lookup(SecurityConfigurationManager.class);
  }

  @Test
  public void testRoleMerging() throws Exception {
    List<CRole> roles = manager.listRoles();

    CRole anon = manager.readRole("anon");
    assertTrue("roles: " + anon.getRoles(), anon.getRoles().contains("other"));
    assertTrue("roles: " + anon.getRoles(), anon.getRoles().contains("role2"));
    assertEquals("roles: " + anon.getRoles(), 2, anon.getRoles().size());

    assertTrue(anon.getPrivileges().contains("priv1"));
    assertTrue(anon.getPrivileges().contains("4-test"));
    assertEquals("privs: " + anon.getPrivileges(), 2, anon.getPrivileges().size());

    assertEquals("Test Anon Role", anon.getName());
    assertEquals("Test Anon Role Description", anon.getDescription());

    CRole other = manager.readRole("other");
    assertTrue(other.getRoles().contains("role2"));
    assertEquals("roles: " + other.getRoles(), 1, other.getRoles().size());

    assertTrue(other.getPrivileges().contains("6-test"));
    assertTrue(other.getPrivileges().contains("priv2"));
    assertEquals("privs: " + other.getPrivileges(), 2, other.getPrivileges().size());

    assertEquals("Other Role", other.getName());
    assertEquals("Other Role Description", other.getDescription());

    // all roles
    assertEquals(8, roles.size());

  }

  @Test
  public void testPrivsMerging() throws Exception {
    List<CPrivilege> privs = manager.listPrivileges();

    CPrivilege priv = manager.readPrivilege("1-test");
    assertTrue(priv != null);

    priv = manager.readPrivilege("2-test");
    assertTrue(priv != null);

    priv = manager.readPrivilege("4-test");
    assertTrue(priv != null);

    priv = manager.readPrivilege("5-test");
    assertTrue(priv != null);

    priv = manager.readPrivilege("6-test");
    assertTrue(priv != null);

    assertNotNull(manager.readPrivilege("priv1"));
    assertNotNull(manager.readPrivilege("priv2"));
    assertNotNull(manager.readPrivilege("priv3"));
    assertNotNull(manager.readPrivilege("priv4"));
    assertNotNull(manager.readPrivilege("priv5"));

    assertEquals("privs: " + this.privilegeListToStringList(privs), 10, privs.size());
  }

  private List<String> privilegeListToStringList(List<CPrivilege> privs) {
    List<String> ids = new ArrayList<String>();

    for (CPrivilege priv : privs) {
      ids.add(priv.getId());
    }

    return ids;
  }
}
