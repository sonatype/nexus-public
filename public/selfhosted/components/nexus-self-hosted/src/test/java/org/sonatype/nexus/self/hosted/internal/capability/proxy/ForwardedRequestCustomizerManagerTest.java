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

import org.sonatype.nexus.bootstrap.jetty.CustomForwardedRequestCustomizer;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

/**
 * Tests for {@link ForwardedRequestCustomizerManager}.
 */
@ExtendWith(MockitoExtension.class)
class ForwardedRequestCustomizerManagerTest
{
  private ForwardedRequestCustomizerManager underTest;

  @BeforeEach
  void setUp() {
    underTest = new ForwardedRequestCustomizerManager();
  }

  @AfterEach
  void teardown() {
    CustomForwardedRequestCustomizer.clearInstance();
  }

  @Test
  void testSetEnabled_WhenCustomizerPresent() {
    CustomForwardedRequestCustomizer customizer = new CustomForwardedRequestCustomizer();

    underTest.setEnabled(false);

    assertThat(customizer.isEnabled(), is(false));
    assertThat(underTest.isEnabled(), is(false));
  }

  @Test
  void testSetEnabled_TogglesValue() {
    // This triggers creation
    new CustomForwardedRequestCustomizer();

    underTest.setEnabled(false);
    assertThat(underTest.isEnabled(), is(false));

    underTest.setEnabled(true);
    assertThat(underTest.isEnabled(), is(true));
  }
}
