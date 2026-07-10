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
package org.sonatype.nexus.metrics;

import java.io.IOException;

import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletResponse;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.SharedMetricRegistries;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.sonatype.nexus.common.metrics.MetricsConstants.NEXUS_METRICS_REGISTRY_NAME;

/**
 * Locks in the per-status-code meter names that the legacy
 * {@code com.codahale.metrics.servlet.InstrumentedFilter} produced before the Jakarta EE 10
 * migration. Customer dashboards and Sonatype-internal alerts key off these exact names; if the
 * names shift again the test fails and forces an explicit decision.
 *
 * <p>
 * NEXUS-46395 regression check: the migration briefly stubbed {@link InstrumentedFilter} to a
 * pass-through, eliminating all of these metrics. After restoring the Dropwizard
 * {@code metrics-jakarta-servlet} flavor, this test asserts the meter names are present in the
 * shared {@link MetricRegistry} that {@code /service/metrics/data} serializes.
 */
@RunWith(MockitoJUnitRunner.class)
public class InstrumentedFilterTest
{
  @Mock
  private FilterConfig filterConfig;

  @Mock
  private ServletContext servletContext;

  @Mock
  private FilterChain chain;

  @Mock
  private HttpServletResponse response;

  private MetricRegistry registry;

  private InstrumentedFilter underTest;

  @Before
  public void setUp() throws Exception {
    registry = SharedMetricRegistries.getOrCreate(NEXUS_METRICS_REGISTRY_NAME);
    when(filterConfig.getServletContext()).thenReturn(servletContext);
    // Capture/return the registry attribute the way a real ServletContext would.
    doAnswer(invocation -> {
      Object value = invocation.getArgument(1);
      when(servletContext.getAttribute((String) invocation.getArgument(0))).thenReturn(value);
      return null;
    }).when(servletContext).setAttribute(any(), any());

    underTest = new InstrumentedFilter();
    underTest.init(filterConfig);
  }

  @After
  public void tearDown() {
    SharedMetricRegistries.clear();
  }

  @Test
  public void initRegistersExpectedMetricNames() {
    String prefix = InstrumentedFilter.class.getName();

    // Per-status-code meters - these names are what customer dashboards depend on.
    assertThat(registry.getMeters().keySet(), hasItems(
        prefix + ".responseCodes.ok",
        prefix + ".responseCodes.created",
        prefix + ".responseCodes.noContent",
        prefix + ".responseCodes.badRequest",
        prefix + ".responseCodes.notFound",
        prefix + ".responseCodes.serverError",
        prefix + ".responseCodes.other",
        prefix + ".errors",
        prefix + ".timeouts"));

    // Counter for in-flight requests + timer for request duration.
    assertThat(registry.getCounters().keySet(), hasItems(prefix + ".activeRequests"));
    assertThat(registry.getTimers().keySet(), hasItems(prefix + ".requests"));
  }

  @Test
  public void doFilter200IncrementsResponseCodesOk() throws IOException, ServletException {
    runWithStatus(200);
    assertOnlyMeterIncremented(".responseCodes.ok");
  }

  @Test
  public void doFilter201IncrementsResponseCodesCreated() throws IOException, ServletException {
    runWithStatus(201);
    assertOnlyMeterIncremented(".responseCodes.created");
  }

  @Test
  public void doFilter204IncrementsResponseCodesNoContent() throws IOException, ServletException {
    runWithStatus(204);
    assertOnlyMeterIncremented(".responseCodes.noContent");
  }

  @Test
  public void doFilter400IncrementsResponseCodesBadRequest() throws IOException, ServletException {
    runWithStatus(400);
    assertOnlyMeterIncremented(".responseCodes.badRequest");
  }

  @Test
  public void doFilter404IncrementsResponseCodesNotFound() throws IOException, ServletException {
    runWithStatus(404);
    assertOnlyMeterIncremented(".responseCodes.notFound");
  }

  @Test
  public void doFilter500IncrementsResponseCodesServerError() throws IOException, ServletException {
    runWithStatus(500);
    assertOnlyMeterIncremented(".responseCodes.serverError");
  }

  @Test
  public void doFilterUnknownStatusIncrementsResponseCodesOther() throws IOException, ServletException {
    // 401 is not in the explicit set (200/201/204/400/404/500) - falls into 'other'.
    runWithStatus(401);
    assertOnlyMeterIncremented(".responseCodes.other");
  }

  @Test
  public void registryAttributeIsSharedNotPrivate() {
    // Make sure init() seeded the servlet context with our shared registry, not a fresh one
    // (a fresh one would mean metrics never reach /service/metrics/data).
    verify(servletContext).setAttribute(io.dropwizard.metrics.servlet.InstrumentedFilter.REGISTRY_ATTRIBUTE,
        registry);
  }

  /**
   * Drive a single request through the filter, with the chain calling {@code setStatus(code)} on
   * the dropwizard {@code StatusExposingServletResponse} wrapper - that's the only path the
   * wrapper updates its captured status, since it doesn't delegate {@code getStatus()} to the
   * wrapped response.
   */
  private void runWithStatus(final int code) throws IOException, ServletException {
    ServletRequest request = mock(ServletRequest.class);
    doAnswer(invocation -> {
      ServletResponse wrapped = invocation.getArgument(1, ServletResponse.class);
      ((HttpServletResponse) wrapped).setStatus(code);
      return null;
    }).when(chain).doFilter(any(), any());
    underTest.doFilter(request, response, chain);
  }

  /**
   * Assert that exactly the given meter (by suffix) saw an increment from this single request.
   * Catches the bug where a status code falls through to 'other' instead of its dedicated meter,
   * or vice versa.
   */
  private void assertOnlyMeterIncremented(final String suffix) {
    String prefix = InstrumentedFilter.class.getName();
    String[] allCodeMeters = {
        ".responseCodes.ok", ".responseCodes.created", ".responseCodes.noContent",
        ".responseCodes.badRequest", ".responseCodes.notFound", ".responseCodes.serverError",
        ".responseCodes.other"
    };
    for (String s : allCodeMeters) {
      long count = registry.meter(prefix + s).getCount();
      if (s.equals(suffix)) {
        assertThat("meter " + s + " should have incremented", count, is(greaterThanOrEqualTo(1L)));
      }
      else {
        assertThat("meter " + s + " should NOT have incremented", count, is(0L));
      }
    }
  }
}
