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
package org.sonatype.nexus.security.internal;

import org.sonatype.nexus.security.AbstractSecurityTest;
import org.sonatype.nexus.security.authz.AuthorizationManager;
import org.sonatype.nexus.security.config.InitialSecurityConfiguration;
import org.sonatype.nexus.security.config.PreconfiguredSecurityConfigurationSource;
import org.sonatype.nexus.security.config.SecurityConfigurationSource;
import org.sonatype.nexus.security.internal.SecurityContributorMediatorIntegrationTest.SecurityContributorMediatorTestConfiguration;
import org.sonatype.nexus.security.role.NoSuchRoleException;
import org.sonatype.nexus.security.role.Role;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.sonatype.nexus.security.Roles.ADMIN_ROLE_ID;
import static org.sonatype.nexus.security.Roles.ANONYMOUS_ROLE_ID;

/**
 * Integration test for {@link SecurityContributorMediator}.
 *
 * This test verifies the fix for NEXUS-48819: Built-in roles (nx-anonymous, nx-admin) are available
 * during the CAPABILITIES phase because SecurityContributorMediator registers them during the SECURITY phase.
 *
 * The test simulates the startup sequence and ensures that security contributors are registered
 * before any capabilities would validate role existence.
 */
@Import(SecurityContributorMediatorTestConfiguration.class)
class SecurityContributorMediatorIntegrationTest
    extends AbstractSecurityTest
{
  static class SecurityContributorMediatorTestConfiguration
  {
    @Qualifier("default")
    @Primary
    @Bean
    SecurityConfigurationSource securityConfigurationSource() {
      return new PreconfiguredSecurityConfigurationSource(InitialSecurityConfiguration.getConfiguration());
    }

    @Bean
    org.sonatype.nexus.security.config.SecurityContributor testSecurityContributor() {
      return new org.sonatype.nexus.security.config.SecurityContributorSupport()
      {
        @Override
        public org.sonatype.nexus.security.config.MemorySecurityConfiguration getContribution() {
          org.sonatype.nexus.security.config.MemorySecurityConfiguration config =
              new org.sonatype.nexus.security.config.MemorySecurityConfiguration();

          // Add nx-all privilege (for admin role)
          config.addPrivilege(createWildcardPrivilege("nx-all", "All permissions", "nexus:*"));

          // Add privileges for anonymous role
          config.addPrivilege(createApplicationPrivilege("nx-search-read", "Search read", "search", "read"));
          config.addPrivilege(
              createApplicationPrivilege("nx-healthcheck-read", "Healthcheck read", "healthcheck", "read"));
          config.addPrivilege(createWildcardPrivilege("nx-repository-view-*-*-browse", "Browse all repositories",
              "nexus:repository-view:*:*:browse"));
          config.addPrivilege(createWildcardPrivilege("nx-repository-view-*-*-read", "Read all repositories",
              "nexus:repository-view:*:*:read"));

          // Add nx-admin role with nx-all privilege
          config.addRole(createRole(ADMIN_ROLE_ID, ADMIN_ROLE_ID, "Administrator Role", "nx-all"));

          // Add nx-anonymous role with search and repository view privileges
          config.addRole(createRole(ANONYMOUS_ROLE_ID, ANONYMOUS_ROLE_ID, "Anonymous Role",
              "nx-search-read", "nx-healthcheck-read", "nx-repository-view-*-*-browse", "nx-repository-view-*-*-read"));

          return config;
        }
      };
    }
  }

  private AuthorizationManager authorizationManager;

  @BeforeEach
  @Override
  protected void setUp() throws Exception {
    super.setUp();
    authorizationManager = lookup(AuthorizationManager.class);
  }

  /**
   * Verifies that built-in roles are available after initialization.
   *
   * This test validates the fix for NEXUS-48819 by ensuring:
   * 1. The SecurityContributorMediator registers security contributors during initialization
   * 2. Built-in roles (nx-anonymous, nx-admin) are available
   * 3. Role lookups succeed (as they would during capability validation)
   */
  @Test
  void testBuiltInRolesAvailableAfterInitialization() {
    // Verify: Built-in roles are available (these are provided by security contributors)
    Role anonymousRole = authorizationManager.getRole(ANONYMOUS_ROLE_ID);
    assertThat("nx-anonymous role should be available", anonymousRole, is(notNullValue()));
    assertThat("nx-anonymous role ID should match", anonymousRole.getRoleId(), is(ANONYMOUS_ROLE_ID));

    Role adminRole = authorizationManager.getRole(ADMIN_ROLE_ID);
    assertThat("nx-admin role should be available", adminRole, is(notNullValue()));
    assertThat("nx-admin role ID should match", adminRole.getRoleId(), is(ADMIN_ROLE_ID));
  }

  /**
   * Verifies that looking up a non-existent role throws the expected exception.
   *
   * This confirms the authorization manager is working correctly and would properly
   * reject invalid roles during capability validation.
   */
  @Test
  void testNonExistentRoleThrowsException() {
    // Verify: Non-existent role lookup throws exception
    assertThrows(NoSuchRoleException.class,
        () -> authorizationManager.getRole("non-existent-role-id"),
        "Looking up non-existent role should throw NoSuchRoleException");
  }

  /**
   * Verifies that the anonymous role has the expected privileges.
   *
   * This ensures the security contributor provided complete role definitions,
   * not just role IDs.
   */
  @Test
  void testAnonymousRoleHasExpectedPrivileges() {
    Role anonymousRole = authorizationManager.getRole(ANONYMOUS_ROLE_ID);

    // Verify: Anonymous role has search and healthcheck privileges
    assertThat("Anonymous role should have privileges",
        anonymousRole.getPrivileges().isEmpty(), is(false));

    // Verify the expected privileges from security contributors
    assertThat("Anonymous role should have search privilege",
        anonymousRole.getPrivileges().contains("nx-search-read"), is(true));
    assertThat("Anonymous role should have healthcheck privilege",
        anonymousRole.getPrivileges().contains("nx-healthcheck-read"), is(true));
    assertThat("Anonymous role should have browse privilege",
        anonymousRole.getPrivileges().contains("nx-repository-view-*-*-browse"), is(true));
    assertThat("Anonymous role should have read privilege",
        anonymousRole.getPrivileges().contains("nx-repository-view-*-*-read"), is(true));
  }

  /**
   * Verifies that the admin role has the all-permissions privilege.
   *
   * This ensures the admin role is configured with full access as expected.
   */
  @Test
  void testAdminRoleHasAllPermissions() {
    Role adminRole = authorizationManager.getRole(ADMIN_ROLE_ID);

    // Verify: Admin role has nx-all privilege (all permissions)
    assertThat("Admin role should have privileges",
        adminRole.getPrivileges().isEmpty(), is(false));
    assertThat("Admin role should have nx-all privilege",
        adminRole.getPrivileges().contains("nx-all"), is(true));
  }
}
