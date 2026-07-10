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
package org.sonatype.nexus.bootstrap.jetty;

import org.eclipse.jetty.http.HttpFields;
import org.eclipse.jetty.server.Request;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.sameInstance;

/**
 * Tests for {@link CustomForwardedRequestCustomizer}.
 */
@ExtendWith(MockitoExtension.class)
class CustomForwardedRequestCustomizerTest
{
  private CustomForwardedRequestCustomizer underTest;

  @Mock(answer = Answers.RETURNS_MOCKS)
  private Request mockRequest;

  @Mock
  private HttpFields.Mutable mockResponseHeaders;

  @BeforeEach
  void setUp() {
    underTest = new CustomForwardedRequestCustomizer();
  }

  @AfterEach
  void teardown() {
    CustomForwardedRequestCustomizer.clearInstance();
  }

  @Test
  void testEnabledByDefault() {
    assertThat(underTest.isEnabled(), is(true));
  }

  @Test
  void testSetEnabledFalse() {
    underTest.setEnabled(false);
    assertThat(underTest.isEnabled(), is(false));
  }

  @Test
  void testSetEnabledTrue() {
    underTest.setEnabled(false);
    underTest.setEnabled(true);
    assertThat(underTest.isEnabled(), is(true));
  }

  @Test
  void testCustomizeWhenEnabled_DelegatesToParent() {
    // Note: We can't easily mock the parent class behavior, so this test verifies
    // that when enabled, the method doesn't return immediately
    underTest.setEnabled(true);

    // The parent will process the request - we just verify the method completes
    // In a real integration test, we'd verify the header processing
    Request result = underTest.customize(mockRequest, mockResponseHeaders);
    // Result may be the same or a wrapped request depending on parent behavior
    assertThat(result, is(mockRequest)); // When no forwarded headers, parent returns same request
  }

  @Test
  void testCustomizeWhenDisabled_ReturnsRequestUnchanged() {
    underTest.setEnabled(false);

    Request result = underTest.customize(mockRequest, mockResponseHeaders);

    assertThat(result, sameInstance(mockRequest));
  }

  @Test
  void testCustomizeWhenDisabled_KeepsWorkingAfterMultipleCalls() {
    underTest.setEnabled(false);

    Request result1 = underTest.customize(mockRequest, mockResponseHeaders);
    Request result2 = underTest.customize(mockRequest, mockResponseHeaders);

    assertThat(result1, sameInstance(mockRequest));
    assertThat(result2, sameInstance(mockRequest));
  }

  @Test
  void testToggleEnabled() {
    // Start disabled
    underTest.setEnabled(false);
    assertThat(underTest.isEnabled(), is(false));

    // Enable
    underTest.setEnabled(true);
    assertThat(underTest.isEnabled(), is(true));

    // Disable again
    underTest.setEnabled(false);
    assertThat(underTest.isEnabled(), is(false));
  }
}
