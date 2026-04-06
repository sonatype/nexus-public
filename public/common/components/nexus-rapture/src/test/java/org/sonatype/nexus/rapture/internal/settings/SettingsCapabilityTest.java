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
package org.sonatype.nexus.rapture.internal.settings;

import java.util.HashMap;
import java.util.Map;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.rapture.UiSettingsManager;
import org.sonatype.nexus.rapture.settings.RaptureSettings;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.mockito.Mockito.verify;

/**
 * Tests for {@link SettingsCapability}.
 */
@RunWith(MockitoJUnitRunner.class)
public class SettingsCapabilityTest
    extends TestSupport
{
  @Mock
  private UiSettingsManager uiSettingsManager;

  @Mock
  private SessionTimeoutManager sessionTimeoutManager;

  private SettingsCapability underTest;

  @Before
  public void setup() {
    underTest = new SettingsCapability(uiSettingsManager, sessionTimeoutManager);
  }

  @Test
  public void testOnActivate_updatesSessionTimeout() throws Exception {
    SettingsCapabilityConfiguration config = createConfig(720);

    underTest.onActivate(config);

    verify(uiSettingsManager).setSettings(config);
    verify(sessionTimeoutManager).updateTimeout(720);
  }

  @Test
  public void testOnActivate_defaultTimeout() throws Exception {
    SettingsCapabilityConfiguration config = createConfig(RaptureSettings.DEFAULT_SESSION_TIMEOUT);

    underTest.onActivate(config);

    verify(uiSettingsManager).setSettings(config);
    verify(sessionTimeoutManager).updateTimeout(RaptureSettings.DEFAULT_SESSION_TIMEOUT);
  }

  @Test
  public void testOnUpdate_updatesSessionTimeout() throws Exception {
    SettingsCapabilityConfiguration config = createConfig(360);

    underTest.onUpdate(config);

    verify(sessionTimeoutManager).updateTimeout(360);
  }

  @Test
  public void testOnUpdate_zeroTimeout() throws Exception {
    SettingsCapabilityConfiguration config = createConfig(0);

    underTest.onUpdate(config);

    verify(sessionTimeoutManager).updateTimeout(0);
  }

  private SettingsCapabilityConfiguration createConfig(int sessionTimeout) {
    Map<String, String> properties = new HashMap<>();
    properties.put(SettingsCapabilityConfiguration.SESSION_TIMEOUT, String.valueOf(sessionTimeout));
    properties.put(SettingsCapabilityConfiguration.TITLE, "Test Title");
    return new SettingsCapabilityConfiguration(properties);
  }
}
