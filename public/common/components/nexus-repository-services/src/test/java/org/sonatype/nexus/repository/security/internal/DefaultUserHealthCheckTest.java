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
package org.sonatype.nexus.repository.security.internal;

import org.sonatype.nexus.security.config.CUser;
import org.sonatype.nexus.security.config.SecurityConfigurationManager;
import org.sonatype.nexus.security.internal.AuthenticatingRealmImpl;
import org.sonatype.nexus.security.realm.RealmManager;
import org.sonatype.nexus.security.user.UserNotFoundException;

import com.codahale.metrics.health.HealthCheck.Result;
import org.apache.shiro.authc.credential.PasswordService;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.Silent.class)
public class DefaultUserHealthCheckTest
{
  @Mock
  private RealmManager realmManager;

  @Mock
  private SecurityConfigurationManager securityConfigurationManager;

  @Mock
  private PasswordService passwordService;

  @Mock
  private CUser adminUser;

  private DefaultUserHealthCheck underTest;

  @Before
  public void setUp() throws UserNotFoundException {
    underTest = new DefaultUserHealthCheck(realmManager, securityConfigurationManager, passwordService);
    when(realmManager.isRealmEnabled(AuthenticatingRealmImpl.NAME)).thenReturn(true);
    when(securityConfigurationManager.readUser("admin")).thenReturn(adminUser);
    when(adminUser.getPassword()).thenReturn("some-password");
  }

  @Test
  public void checkIsHealthyWhenRealmIsDisabled() {
    when(realmManager.isRealmEnabled(AuthenticatingRealmImpl.NAME)).thenReturn(false);
    assertThat(underTest.check().isHealthy()).isTrue();
  }

  @Test
  public void checkIsHealthyWhenPasswordChanged() {
    when(passwordService.passwordsMatch("admin123", "some-password")).thenReturn(false);
    assertThat(underTest.check().isHealthy()).isTrue();
  }

  @Test
  public void checkIsHealthyWhenUserNotFound() throws UserNotFoundException {
    when(securityConfigurationManager.readUser("admin")).thenThrow(new UserNotFoundException("admin"));
    assertThat(underTest.check().isHealthy()).isTrue();
  }

  @Test
  public void checkIsUnhealthy() {
    when(passwordService.passwordsMatch("admin123", "some-password")).thenReturn(true);
    Result result = underTest.check();
    assertThat(result.isHealthy()).isFalse();
    assertThat(result.getMessage()).isEqualTo(DefaultUserHealthCheck.ERROR_MESSAGE);
  }
}
