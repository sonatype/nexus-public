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
package org.sonatype.nexus.coreui.internal.branding;

import java.util.HashMap;
import java.util.Map;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.capability.Condition;
import org.sonatype.nexus.capability.condition.CapabilityConditions;
import org.sonatype.nexus.capability.condition.Conditions;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link BrandingCapability}.
 */
public class BrandingCapabilityTest
    extends TestSupport
{
  @Mock
  private Branding branding;

  @Mock
  private Conditions conditions;

  @Mock
  private CapabilityConditions capabilityConditions;

  private BrandingCapability underTest;

  @Before
  public void setUp() {
    underTest = new BrandingCapability(branding);
    when(conditions.capabilities()).thenReturn(capabilityConditions);
  }

  @Test
  public void testConstructor_nullBranding() {
    assertThrows(NullPointerException.class, () -> new BrandingCapability(null));
  }

  @Test
  public void testCreateConfig() {
    Map<String, String> properties = new HashMap<>();
    properties.put(BrandingCapabilityConfiguration.HEADER_ENABLED, "true");
    properties.put(BrandingCapabilityConfiguration.HEADER_HTML, "<h1>Test</h1>");
    properties.put(BrandingCapabilityConfiguration.FOOTER_ENABLED, "false");
    properties.put(BrandingCapabilityConfiguration.FOOTER_HTML, "<p>Footer</p>");

    BrandingCapabilityConfiguration config = underTest.createConfig(properties);
    assertThat(config, is(notNullValue()));
    assertThat(config.isHeaderEnabled(), is(true));
    assertThat(config.getHeaderHtml(), is("<h1>Test</h1>"));
    assertThat(config.isFooterEnabled(), is(false));
    assertThat(config.getFooterHtml(), is("<p>Footer</p>"));
  }

  @Test
  public void testOnActivate_setsBranding() throws Exception {
    Map<String, String> properties = new HashMap<>();
    properties.put(BrandingCapabilityConfiguration.HEADER_ENABLED, "true");
    properties.put(BrandingCapabilityConfiguration.HEADER_HTML, "<h1>Test</h1>");
    properties.put(BrandingCapabilityConfiguration.FOOTER_ENABLED, "true");
    properties.put(BrandingCapabilityConfiguration.FOOTER_HTML, "<p>Footer</p>");

    BrandingCapabilityConfiguration config = new BrandingCapabilityConfiguration(properties);
    underTest.onActivate(config);

    verify(branding).set(config);
  }

  @Test
  public void testOnPassivate_resetsBranding() throws Exception {
    Map<String, String> properties = new HashMap<>();
    BrandingCapabilityConfiguration config = new BrandingCapabilityConfiguration(properties);
    underTest.onPassivate(config);

    verify(branding).reset();
  }

  @Test
  public void testOnRemove_resetsBranding() throws Exception {
    Map<String, String> properties = new HashMap<>();
    BrandingCapabilityConfiguration config = new BrandingCapabilityConfiguration(properties);
    underTest.onRemove(config);

    verify(branding).reset();
  }

  @Test
  public void testActivationCondition() {
    Condition expectedCondition = mock(Condition.class);
    when(capabilityConditions.passivateCapabilityDuringUpdate()).thenReturn(expectedCondition);

    underTest.installConditionComponents(conditions);
    Condition condition = underTest.activationCondition();

    assertThat(condition, is(expectedCondition));
    verify(conditions).capabilities();
    verify(capabilityConditions).passivateCapabilityDuringUpdate();
  }
}
