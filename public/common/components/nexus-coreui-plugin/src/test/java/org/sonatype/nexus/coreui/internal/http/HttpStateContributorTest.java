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
package org.sonatype.nexus.coreui.internal.http;

import java.util.Map;

import org.sonatype.nexus.common.time.Time;
import org.sonatype.nexus.httpclient.HttpDefaultsCustomizer;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.Silent.class)
public class HttpStateContributorTest
{
  @Mock
  private HttpDefaultsCustomizer customizer;

  private HttpStateContributor underTest;

  @Before
  public void setUp() {
    when(customizer.getRequestTimeout()).thenReturn(Time.seconds(30));
    when(customizer.getRetryCount()).thenReturn(2);
    underTest = new HttpStateContributor(true, "/", customizer);
  }

  @Test
  public void shouldReturnFeatureFlagEnabled() {
    Map<String, Object> state = underTest.getState();

    assertThat(state.get("nexus.react.httpSettings"), is(true));
  }

  @Test
  public void shouldReturnFeatureFlagDisabled() {
    underTest = new HttpStateContributor(false, "/", customizer);

    Map<String, Object> state = underTest.getState();

    assertThat(state.get("nexus.react.httpSettings"), is(false));
  }

  @Test
  public void shouldReturnRequestTimeout() {
    Map<String, Object> state = underTest.getState();

    assertThat(state.get("requestTimeout"), is(Time.seconds(30)));
  }

  @Test
  public void shouldReturnRetryCount() {
    Map<String, Object> state = underTest.getState();

    assertThat(state.get("retryCount"), is(2));
  }

  @Test
  public void shouldReturnContextPath() {
    Map<String, Object> state = underTest.getState();

    assertThat(state.get("nexus-context-path"), is("/"));
  }

  @Test
  public void shouldReturnCustomContextPath() {
    underTest = new HttpStateContributor(true, "/nexus", customizer);

    Map<String, Object> state = underTest.getState();

    assertThat(state.get("nexus-context-path"), is("/nexus"));
  }
}
