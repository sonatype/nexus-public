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

package org.sonatype.nexus.repository.maven;

import java.util.Collections;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.capability.CapabilityContext;
import org.sonatype.nexus.capability.CapabilityReference;
import org.sonatype.nexus.capability.CapabilityReferenceFilterBuilder.CapabilityReferenceFilter;
import org.sonatype.nexus.capability.CapabilityRegistry;
import org.sonatype.nexus.common.app.ApplicationVersion;
import org.sonatype.nexus.utils.httpclient.UserAgentGenerator;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class MavenProxyRequestHeaderSupportTest
    extends TestSupport
{
  @Mock
  private CapabilityRegistry capabilityRegistry;

  @Mock
  private ApplicationVersion applicationVersion;

  private UserAgentGenerator userAgentGenerator;

  private MavenProxyRequestHeaderSupport underTest;

  @Before
  public void setUp() {
    userAgentGenerator = new UserAgentGenerator(applicationVersion);
    this.underTest = new MavenProxyRequestHeaderSupport(capabilityRegistry, userAgentGenerator);
    when(applicationVersion.getEdition()).thenReturn("edition");
  }

  @Test
  public void testUserAgentWithAnalyticsNotConfigured() {
    String userAgentForAnalytics = underTest.getUserAgentForAnalytics();
    String expectedUserAgent = userAgentGenerator.generate().replace(")","; pau)");
    assertEquals(expectedUserAgent, userAgentForAnalytics);
  }

  @Test
  public void testUserAgentWithAnalyticsEnabled() {
    CapabilityReference capabilityReference = mockCapabilityReference();
    when(capabilityReference.context().isEnabled()).thenReturn(true);
    String userAgentForAnalytics = underTest.getUserAgentForAnalytics();
    String expectedUserAgent = userAgentGenerator.generate().replace(")","; pae)");
    assertEquals(expectedUserAgent, userAgentForAnalytics);
  }

  @Test
  public void testUserAgentWithAnalyticsDisabled() {
    CapabilityReference capabilityReference = mockCapabilityReference();
    when(capabilityReference.context().isEnabled()).thenReturn(false);
    String userAgentForAnalytics = underTest.getUserAgentForAnalytics();
    String expectedUserAgent = userAgentGenerator.generate().replace(")","; pad)");
    assertEquals(expectedUserAgent, userAgentForAnalytics);
  }

  private CapabilityReference mockCapabilityReference() {
    CapabilityReference capabilityReference = mock(CapabilityReference.class);
    CapabilityContext capabilityContext = mock(CapabilityContext.class);
    when(capabilityRegistry.get(any(CapabilityReferenceFilter.class))).then(
        i -> Collections.singleton(capabilityReference));
    when(capabilityReference.context()).thenReturn(capabilityContext);
    return capabilityReference;
  }
}
