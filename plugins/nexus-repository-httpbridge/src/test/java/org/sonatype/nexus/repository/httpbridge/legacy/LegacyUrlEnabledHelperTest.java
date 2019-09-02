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
package org.sonatype.nexus.repository.httpbridge.legacy;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.capability.CapabilityContext;
import org.sonatype.nexus.capability.CapabilityReference;
import org.sonatype.nexus.capability.CapabilityReferenceFilterBuilder.CapabilityReferenceFilter;
import org.sonatype.nexus.capability.CapabilityRegistry;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

public class LegacyUrlEnabledHelperTest
    extends TestSupport
{
  private static final String LEGACY_ENABLED =
      "org.sonatype.nexus.repository.httpbridge.internal.HttpBridgeModule.legacy";

  @Mock
  private CapabilityRegistry capabilityRegistry;

  @Mock
  private CapabilityReference capabilityReference;

  @Mock
  private CapabilityContext capabilityContext;

  private LegacyUrlEnabledHelper undertTest;

  @Before
  public void setup() throws Exception {
    System.setProperty(LEGACY_ENABLED, "false");

    doReturn(singletonList(capabilityReference)).when(capabilityRegistry).get(any(CapabilityReferenceFilter.class));
    when(capabilityReference.context()).thenReturn(capabilityContext);
    when(capabilityContext.isActive()).thenReturn(false);

    undertTest = new LegacyUrlEnabledHelper(capabilityRegistry);
  }


  @Test
  public void enabledOnProperty() throws Exception {
    System.setProperty(LEGACY_ENABLED, "true");

    undertTest = new LegacyUrlEnabledHelper(capabilityRegistry);

    assertThat(undertTest.isEnabled(), is(equalTo(true)));
  }

  @Test
  public void enabledWhenCapabilityActive() throws Exception {
    when(capabilityContext.isActive()).thenReturn(true);

    assertThat(undertTest.isEnabled(), is(equalTo(true)));
  }

  @Test
  public void disabledWhenPropertyFalseAndCapabilityNotActive() throws Exception {
    assertThat(undertTest.isEnabled(), is(equalTo(false)));
  }

  @Test
  public void disabledWhenPropertyFalseAndCapabilityNotFound() throws Exception {
    when(capabilityContext.isActive()).thenReturn(true);
    when(capabilityRegistry.get(any(CapabilityReferenceFilter.class))).thenReturn(emptyList());

    assertThat(undertTest.isEnabled(), is(equalTo(false)));
  }
}
