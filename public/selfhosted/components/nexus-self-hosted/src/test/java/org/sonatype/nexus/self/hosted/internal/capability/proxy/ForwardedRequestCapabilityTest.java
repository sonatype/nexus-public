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
package org.sonatype.nexus.self.hosted.internal.capability.proxy;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.verify;

/**
 * Tests for {@link ForwardedRequestCapability}.
 */
@ExtendWith(MockitoExtension.class)
class ForwardedRequestCapabilityTest
{
  @Mock
  private ForwardedRequestCustomizerManager manager;

  private ForwardedRequestCapability underTest;

  @BeforeEach
  void setUp() {
    underTest = new ForwardedRequestCapability(manager);
  }

  @Test
  void testTypeId() {
    assertThat(ForwardedRequestCapability.TYPE_ID, is("http.forwarded"));
  }

  @Test
  void testType() {
    assertThat(ForwardedRequestCapability.TYPE.toString(), is("http.forwarded"));
  }

  @Test
  void testOnActivate_EnablesManager() {
    Map<String, String> properties = new HashMap<>();
    properties.put("enabled", "true");
    ForwardedRequestCapabilityConfiguration config = new ForwardedRequestCapabilityConfiguration(properties);

    underTest.onActivate(config);

    verify(manager).setEnabled(true);
  }

  @Test
  void testOnActivate_Disabled() {
    Map<String, String> properties = new HashMap<>();
    properties.put("enabled", "false");
    ForwardedRequestCapabilityConfiguration config = new ForwardedRequestCapabilityConfiguration(properties);

    underTest.onActivate(config);

    verify(manager).setEnabled(false);
  }

  @Test
  void testOnPassivate_DisablesManager() {
    Map<String, String> properties = new HashMap<>();
    ForwardedRequestCapabilityConfiguration config = new ForwardedRequestCapabilityConfiguration(properties);

    underTest.onPassivate(config);

    verify(manager).setEnabled(false);
  }

  @Test
  void testConfiguration_DefaultEnabled() {
    Map<String, String> properties = new HashMap<>();
    ForwardedRequestCapabilityConfiguration config = new ForwardedRequestCapabilityConfiguration(properties);

    assertThat(config.isEnabled(), is(true));
  }

  @Test
  void testConfiguration_ExplicitEnabled() {
    Map<String, String> properties = new HashMap<>();
    properties.put("enabled", "true");
    ForwardedRequestCapabilityConfiguration config = new ForwardedRequestCapabilityConfiguration(properties);

    assertThat(config.isEnabled(), is(true));
  }

  @Test
  void testConfiguration_ExplicitDisabled() {
    Map<String, String> properties = new HashMap<>();
    properties.put("enabled", "false");
    ForwardedRequestCapabilityConfiguration config = new ForwardedRequestCapabilityConfiguration(properties);

    assertThat(config.isEnabled(), is(false));
  }
}
