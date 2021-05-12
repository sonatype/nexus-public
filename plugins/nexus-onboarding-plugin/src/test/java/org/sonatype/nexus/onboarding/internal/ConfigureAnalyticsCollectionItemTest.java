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
package org.sonatype.nexus.onboarding.internal;

import java.util.List;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.capability.CapabilityReference;
import org.sonatype.nexus.capability.CapabilityRegistry;
import org.sonatype.nexus.common.app.ApplicationVersion;

import com.google.common.base.Predicate;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Matchers;
import org.mockito.Mock;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;
import static org.sonatype.nexus.onboarding.internal.ConfigureAnalyticsCollectionItem.OSS;

public class ConfigureAnalyticsCollectionItemTest
    extends TestSupport
{
  @Mock
  private CapabilityReference capabilityReference;

  @Mock
  private ApplicationVersion applicationVersion;

  @Mock
  private CapabilityRegistry capabilityRegistry;

  @InjectMocks
  private ConfigureAnalyticsCollectionItem underTest;

  @Test
  public void shouldBeFalseWhenEditionIsPro() {
    when(applicationVersion.getEdition()).thenReturn("PRO");

    assertThat(underTest.applies(), is(false));
  }

  @Test
  public void shouldBeFalseWhenAnalyticsCapabilityIsPresent() {
    when(applicationVersion.getEdition()).thenReturn(OSS);

    mockCapability(singletonList(capabilityReference));

    assertThat(underTest.applies(), is(false));
  }

  @Test
  public void shouldBeTrueWhenOssAndAnalyticsCapabilityIsAbsent() {
    when(applicationVersion.getEdition()).thenReturn(OSS);

    mockCapability(emptyList());

    assertThat(underTest.applies(), is(true));
  }

  private void mockCapability(final List<CapabilityReference> capabilities) {
    doReturn(capabilities)
        .when(capabilityRegistry)
        .get(Matchers.<Predicate<CapabilityReference>>any());
  }
}
