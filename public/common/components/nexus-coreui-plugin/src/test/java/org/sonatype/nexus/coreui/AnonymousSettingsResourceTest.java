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
package org.sonatype.nexus.coreui;

import org.sonatype.nexus.security.anonymous.AnonymousConfiguration;
import org.sonatype.nexus.security.anonymous.AnonymousManager;
import org.sonatype.nexus.testcommon.extensions.AuthenticationExtension;
import org.sonatype.nexus.testcommon.extensions.AuthenticationExtension.WithUser;
import org.sonatype.nexus.testcommon.validation.ValidationExtension;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@ExtendWith(ValidationExtension.class)
@ExtendWith(AuthenticationExtension.class)
@WithUser
class AnonymousSettingsResourceTest
{
  @Mock
  private AnonymousManager anonymousManager;

  @Mock
  private AnonymousConfiguration anonymousConfiguration;

  @InjectMocks
  private AnonymousSettingsResource underTest;

  @Test
  void readReturnsCurrentConfiguration() {
    when(anonymousManager.getConfiguration()).thenReturn(anonymousConfiguration);
    when(anonymousConfiguration.isEnabled()).thenReturn(true);
    when(anonymousConfiguration.getUserId()).thenReturn("anonymous");
    when(anonymousConfiguration.getRealmName()).thenReturn("NexusAuthorizingRealm");

    AnonymousSettingsXO result = underTest.read();

    assertThat(result, is(notNullValue()));
    assertThat(result.getEnabled(), is(true));
    assertThat(result.getUserId(), is("anonymous"));
    assertThat(result.getRealmName(), is("NexusAuthorizingRealm"));
  }

  @Test
  void readReturnsDisabledConfiguration() {
    when(anonymousManager.getConfiguration()).thenReturn(anonymousConfiguration);
    when(anonymousConfiguration.isEnabled()).thenReturn(false);
    when(anonymousConfiguration.getUserId()).thenReturn("anon-user");
    when(anonymousConfiguration.getRealmName()).thenReturn("TestRealm");

    AnonymousSettingsXO result = underTest.read();

    assertThat(result, is(notNullValue()));
    assertThat(result.getEnabled(), is(false));
    assertThat(result.getUserId(), is("anon-user"));
    assertThat(result.getRealmName(), is("TestRealm"));
  }

  @Test
  void updateSetsConfigurationOnManager() {
    AnonymousConfiguration newConfig = setupMockNewConfiguration();

    AnonymousSettingsXO xo = new AnonymousSettingsXO();
    xo.setEnabled(true);
    xo.setUserId("anonymous");
    xo.setRealmName("NexusAuthorizingRealm");

    underTest.update(xo);

    verify(anonymousManager).newConfiguration();
    verify(newConfig).setEnabled(true);
    verify(newConfig).setUserId("anonymous");
    verify(newConfig).setRealmName("NexusAuthorizingRealm");
    verify(anonymousManager).setConfiguration(newConfig);
  }

  @Test
  void updateWithDisabledConfiguration() {
    AnonymousConfiguration newConfig = setupMockNewConfiguration();

    AnonymousSettingsXO xo = new AnonymousSettingsXO();
    xo.setEnabled(false);
    xo.setUserId("test-user");
    xo.setRealmName("TestRealm");

    underTest.update(xo);

    verify(newConfig).setEnabled(false);
    verify(newConfig).setUserId("test-user");
    verify(newConfig).setRealmName("TestRealm");
    verify(anonymousManager).setConfiguration(newConfig);
  }

  private AnonymousConfiguration setupMockNewConfiguration() {
    AnonymousConfiguration newConfig = org.mockito.Mockito.mock(AnonymousConfiguration.class);
    when(anonymousManager.newConfiguration()).thenReturn(newConfig);
    return newConfig;
  }
}
