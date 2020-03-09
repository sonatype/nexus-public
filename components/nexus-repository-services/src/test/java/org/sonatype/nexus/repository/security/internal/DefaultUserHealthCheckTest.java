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

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.security.internal.AuthenticatingRealmImpl;
import org.sonatype.nexus.security.realm.RealmManager;

import com.codahale.metrics.health.HealthCheck.Result;
import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.AuthenticationInfo;
import org.apache.shiro.authc.UsernamePasswordToken;
import org.apache.shiro.mgt.RealmSecurityManager;
import org.apache.shiro.realm.Realm;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import static java.util.Collections.singleton;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class DefaultUserHealthCheckTest
    extends TestSupport
{
  @Mock
  private RealmManager realmManager;

  @Mock
  private RealmSecurityManager realmSecurityManager;

  @InjectMocks
  private DefaultUserHealthCheck defaultUserHealthCheck;

  @Test
  public void checkIsHealthyWhenRealmIsDisabled() {
    when(realmManager.isRealmEnabled(AuthenticatingRealmImpl.NAME)).thenReturn(false);

    Result result = defaultUserHealthCheck.check();

    assertThat(result.isHealthy(), is(true));
  }

  @Test
  public void checkIsHealthyWhenRealmIsEnabled() {
    Realm realm = mock(Realm.class);
    when(realmManager.isRealmEnabled(AuthenticatingRealmImpl.NAME)).thenReturn(true);
    when(realmSecurityManager.getRealms()).thenReturn(singleton(realm));
    when(realm.getName()).thenReturn(AuthenticatingRealmImpl.NAME);
    when(realm.getAuthenticationInfo(any(UsernamePasswordToken.class))).thenThrow(AuthenticationException.class);

    Result result = defaultUserHealthCheck.check();

    assertThat(result.isHealthy(), is(true));
  }

  @Test
  public void checkIsUnhealthy() {
    Realm realm = mock(Realm.class);
    when(realmManager.isRealmEnabled(AuthenticatingRealmImpl.NAME)).thenReturn(true);
    when(realmSecurityManager.getRealms()).thenReturn(singleton(realm));
    when(realm.getName()).thenReturn(AuthenticatingRealmImpl.NAME);
    when(realm.getAuthenticationInfo(any(UsernamePasswordToken.class))).thenReturn(mock(AuthenticationInfo.class));

    Result result = defaultUserHealthCheck.check();

    assertThat(result.isHealthy(), is(false));
    assertThat(result.getMessage(), is(DefaultUserHealthCheck.ERROR_MESSAGE));
  }
}
