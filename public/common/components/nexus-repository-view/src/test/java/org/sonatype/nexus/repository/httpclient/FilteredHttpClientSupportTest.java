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
package org.sonatype.nexus.repository.httpclient;

import java.io.IOException;

import org.apache.http.HttpHost;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.Configurable;
import org.apache.http.impl.client.CloseableHttpClient;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

/**
 * Coverage for {@link FilteredHttpClientSupport}'s {@link Configurable} contract.
 *
 * <p>
 * Regression tests for NEXUS-54133: RESTEasy 7's {@code ManualClosingApacheHttpClient43Engine}
 * unconditionally casts the wrapped {@link CloseableHttpClient} to {@link Configurable} in
 * {@code getCurrentConfiguration()}. Before the fix, that cast blew up with a
 * {@link ClassCastException} against every {@code FilteredHttpClientSupport} subclass (notably
 * {@code MonitoredHttpClient} used by the Nexus 2 to 3 migration flow).
 */
public class FilteredHttpClientSupportTest
{
  /**
   * Regression guard for NEXUS-54133: the decorator base MUST be castable to
   * {@link Configurable} so RESTEasy 7+ can retrieve request configuration without failing.
   */
  @Test
  public void isConfigurable_regressionForNexus54133() {
    CloseableHttpClient delegate = mock(CloseableHttpClient.class);

    FilteredHttpClientSupport underTest = new TestFilteredHttpClient(delegate);

    assertThat(underTest, instanceOf(Configurable.class));
  }

  /**
   * When the wrapped delegate implements {@link Configurable}, {@code getConfig()} must
   * return the delegate's own {@link RequestConfig} so consumers observe the effective
   * per-client configuration (timeouts, proxy, etc.).
   */
  @Test
  public void getConfig_returnsDelegateConfig_whenDelegateIsConfigurable() {
    RequestConfig delegateConfig = RequestConfig.custom().setConnectTimeout(1234).build();
    CloseableHttpClient delegate =
        mock(CloseableHttpClient.class, withSettings().extraInterfaces(Configurable.class));
    when(((Configurable) delegate).getConfig()).thenReturn(delegateConfig);

    FilteredHttpClientSupport underTest = new TestFilteredHttpClient(delegate);

    assertThat(underTest.getConfig(), sameInstance(delegateConfig));
  }

  /**
   * When the wrapped delegate does not implement {@link Configurable} (e.g. an oddly-decorated
   * client), fall back to {@link RequestConfig#DEFAULT} rather than throw. This keeps the base
   * class safe for any {@code CloseableHttpClient} delegate.
   */
  @Test
  public void getConfig_returnsDefault_whenDelegateIsNotConfigurable() {
    CloseableHttpClient delegate = mock(CloseableHttpClient.class);

    FilteredHttpClientSupport underTest = new TestFilteredHttpClient(delegate);

    assertThat(underTest.getConfig(), sameInstance(RequestConfig.DEFAULT));
  }

  /**
   * Minimal concrete subclass used purely to instantiate the abstract base under test.
   */
  private static final class TestFilteredHttpClient
      extends FilteredHttpClientSupport
  {
    TestFilteredHttpClient(final CloseableHttpClient delegate) {
      super(delegate);
    }

    @Override
    protected CloseableHttpResponse filter(final HttpHost target, final Filterable filterable) throws IOException {
      return filterable.call();
    }
  }
}
