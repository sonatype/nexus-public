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
package org.sonatype.nexus.security.role;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.sonatype.nexus.security.AbstractSecurityTest;
import org.sonatype.nexus.security.AbstractSecurityTest.BaseSecurityConfiguration;
import org.sonatype.nexus.security.SecuritySystem;
import org.sonatype.nexus.security.internal.AuthorizingRealmImpl;
import org.sonatype.nexus.security.privilege.Privilege;
import org.sonatype.nexus.security.privilege.WildcardPrivilegeDescriptor;
import org.sonatype.nexus.security.realm.MockRealm;
import org.sonatype.nexus.security.realm.RealmManager;
import org.sonatype.nexus.security.role.ExternalRoleMappedTest.ExternalRoleMappedTestConfiguration;
import org.sonatype.nexus.security.user.MockUserManager;
import org.sonatype.nexus.security.user.NoOpEventManager;
import org.sonatype.nexus.security.user.UserManager;

import org.apache.shiro.authz.AuthorizationException;
import org.apache.shiro.subject.PrincipalCollection;
import org.apache.shiro.subject.SimplePrincipalCollection;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.TestPropertySource;

import static org.junit.jupiter.api.Assertions.fail;

@Import({BaseSecurityConfiguration.class, ExternalRoleMappedTestConfiguration.class})
@TestPropertySource(properties = {"nexus.security.principal.permissions.cache.enabled=false"})
public class ExternalRoleMappedTest
    extends AbstractSecurityTest
{

  static class ExternalRoleMappedTestConfiguration
  {
    @Bean
    public UserManager userManager() {
      return new MockUserManager(new NoOpEventManager());
    }

    @Primary
    @Bean
    public MockRealm mockRealm(@Qualifier("Mock") final UserManager userManager) {
      return new MockRealm(userManager);
    }
  }

  @Test
  void testUserHasPermissionFromExternalRole() throws Exception {
    SecuritySystem securitySystem = this.lookup(SecuritySystem.class);

    Map<String, String> properties = new HashMap<String, String>();
    properties.put(WildcardPrivilegeDescriptor.P_PATTERN, "permissionOne:read");

    securitySystem.getAuthorizationManager("default")
        .addPrivilege(new Privilege(
            "randomId",
            "permissionOne",
            "permissionOne",
            WildcardPrivilegeDescriptor.TYPE,
            properties,
            false));

    securitySystem.getAuthorizationManager("default")
        .addRole(new Role("mockrole1", "mockrole1", "mockrole1",
            "default", false, null,
            Collections.singleton("randomId")));

    // add MockRealm to config
    RealmManager realmManager = lookup(RealmManager.class);
    realmManager.setConfiguredRealmIds(List.of("Mock", AuthorizingRealmImpl.NAME));

    // jcohen has the role mockrole1, there is also test role with the same ID, which means jcohen automaticly has
    // this test role

    PrincipalCollection jcohen = new SimplePrincipalCollection("jcohen", MockRealm.NAME);

    try {
      securitySystem.checkPermission(jcohen, "permissionOne:invalid");
      fail("Expected AuthorizationException");
    }
    catch (AuthorizationException e) {
      // expected
    }

    securitySystem.checkPermission(jcohen, "permissionOne:read"); // throws on error, so this is all we need to do
  }
}
