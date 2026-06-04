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
import org.sonatype.nexus.security.config.SecurityContributorTest.SecurityContributorTestConfiguration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Import(SecurityContributorTestConfiguration.class)
class SecurityContributorTest
    extends AbstractSecurityTest
{
  static class SecurityContributorTestConfiguration
  {
    @Qualifier("default")
    @Primary
    @Bean
    SecurityConfigurationSource securityConfigurationSource() {
      return new PreconfiguredSecurityConfigurationSource(InitialSecurityConfiguration.getConfiguration());
    }

    @Qualifier("s1")
    @Bean
    SecurityContributor testSecurityContributor1() {
      return new TestSecurityContributor1();
    }

    @Qualifier("s2")
    @Bean
    SecurityContributor testSecurityContributor2() {
      return new TestSecurityContributor2();
    }
  }

  private SecurityConfigurationManager manager;

  @BeforeEach
  @Override
  protected void setUp() throws Exception {
    super.setUp();

    manager = lookup(SecurityConfigurationManager.class);
  }

  @Test
  void testRoleMerging() throws Exception {
    List<CRole> roles = manager.listRoles();

    CRole anon = manager.readRole("anon");
    assertTrue(anon.getRoles().contains("other"), "roles: " + anon.getRoles());
    assertTrue(anon.getRoles().contains("role2"), "roles: " + anon.getRoles());
    assertEquals(2, anon.getRoles().size(), "roles: " + anon.getRoles());

    assertTrue(anon.getPrivileges().contains("priv1"));
    assertTrue(anon.getPrivileges().contains("4-test"));
    assertThat(anon.getPrivileges(), hasSize(2));

    assertEquals("Test Anon Role", anon.getName());
    assertEquals("Test Anon Role Description", anon.getDescription());

    CRole other = manager.readRole("other");
    assertTrue(other.getRoles().contains("role2"));
    assertThat(other.getRoles(), hasSize(1));

    assertTrue(other.getPrivileges().contains("6-test"));
    assertTrue(other.getPrivileges().contains("priv2"));
    assertThat(other.getPrivileges(), hasSize(2));

    assertEquals("Other Role", other.getName());
    assertEquals("Other Role Description", other.getDescription());

    // all roles
    assertEquals(8, roles.size());

  }

  @Test
  void testPrivsMerging() throws Exception {
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

    assertThat(privs, hasSize(11));
  }

  private static List<String> privilegeListToStringList(final List<CPrivilege> privs) {
    List<String> ids = new ArrayList<String>();

    for (CPrivilege priv : privs) {
      ids.add(priv.getId());
    }

    return ids;
  }
}
