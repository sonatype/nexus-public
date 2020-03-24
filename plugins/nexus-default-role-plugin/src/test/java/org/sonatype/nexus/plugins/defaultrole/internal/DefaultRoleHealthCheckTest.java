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
package org.sonatype.nexus.plugins.defaultrole.internal;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.plugins.defaultrole.DefaultRoleRealm;
import org.sonatype.nexus.security.SecuritySystem;
import org.sonatype.nexus.security.realm.RealmManager;
import org.sonatype.nexus.security.role.Role;

import com.codahale.metrics.health.HealthCheck.Result;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static java.util.Collections.singleton;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonatype.nexus.security.user.UserManager.DEFAULT_SOURCE;

public class DefaultRoleHealthCheckTest
    extends TestSupport
{
  @Mock
  private RealmManager realmManager;

  @Mock
  private DefaultRoleRealm defaultRoleRealm;

  @Mock
  private SecuritySystem securitySystem;

  private DefaultRoleHealthCheck underTest;

  @Before
  public void setup() {
    underTest = new DefaultRoleHealthCheck(realmManager, defaultRoleRealm, securitySystem);
  }

  @Test
  public void testCheck_notConfiguredNotEnabled() throws Exception {
    Result result = underTest.check();
    assertThat(result.isHealthy(), is(true));
  }

  @Test
  public void testCheck_notConfiguredIsEnabled() throws Exception {
    when(realmManager.isRealmEnabled(DefaultRoleRealm.NAME)).thenReturn(true);

    Result result = underTest.check();
    assertThat(result.isHealthy(), is(false));
  }

  @Test
  public void testCheck_isConfiguredIsEnabledRoleMissing() throws Exception {
    when(realmManager.isRealmEnabled(DefaultRoleRealm.NAME)).thenReturn(true);
    when(defaultRoleRealm.getRole()).thenReturn("test-role");

    Result result = underTest.check();
    assertThat(result.isHealthy(), is(false));
  }

  @Test
  public void testCheck_isConfiguredIsEnabledRoleAvailable() throws Exception {
    when(realmManager.isRealmEnabled(DefaultRoleRealm.NAME)).thenReturn(true);
    when(defaultRoleRealm.getRole()).thenReturn("test-role");

    Role role = mock(Role.class);
    when(role.getRoleId()).thenReturn("test-role");
    when(securitySystem.listRoles(DEFAULT_SOURCE)).thenReturn(singleton(role));

    Result result = underTest.check();
    assertThat(result.isHealthy(), is(true));
  }
}
