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

import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebFilter;

import org.sonatype.nexus.common.app.WebFilterPriority;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.SharedMetricRegistries;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import static org.sonatype.nexus.common.metrics.MetricsConstants.NEXUS_METRICS_REGISTRY_NAME;

/**
 * Servlet-level instrumentation filter producing per-status-code response meters and request
 * timing metrics. Registers under the same {@link MetricRegistry} that backs
 * {@code /service/metrics/data}.
 *
 * <p>
 * NEXUS-46395: pre-migration this class extended
 * {@code com.codahale.metrics.servlet.InstrumentedFilter} (Dropwizard's javax.servlet-bound
 * filter). After the EE 8 -> EE 10 migration that base class is incompatible with our
 * jakarta.servlet container. The Dropwizard project ships the same filter under new
 * coordinates -
 * {@link io.dropwizard.metrics.servlet.InstrumentedFilter} from
 * {@code io.dropwizard.metrics:metrics-jakarta-servlet} - which implements
 * {@link jakarta.servlet.Filter}. This class delegates the per-status-code meter wiring to that
 * parent, restoring the metric names that customer dashboards depend on:
 *
 * <ul>
 * <li>{@code org.sonatype.nexus.metrics.InstrumentedFilter.responseCodes.ok}</li>
 * <li>{@code …responseCodes.created}</li>
 * <li>{@code …responseCodes.noContent}</li>
 * <li>{@code …responseCodes.badRequest}</li>
 * <li>{@code …responseCodes.notFound}</li>
 * <li>{@code …responseCodes.serverError}</li>
 * <li>{@code …responseCodes.other} (catch-all for codes outside the set above)</li>
 * <li>{@code …requests} (timer, per-request duration)</li>
 * <li>{@code …activeRequests} (counter, currently in-flight)</li>
 * <li>{@code …errors} (meter, uncaught exceptions during filter processing)</li>
 * <li>{@code …timeouts} (meter, async-context timeouts)</li>
 * </ul>
 *
 * <p>
 * Note the prefix is {@code getClass().getName()} per Dropwizard's {@code AbstractInstrumentedFilter},
 * which resolves to this subclass (NOT the parent). The metric names are therefore identical to
 * what was published before the EE 10 migration, so existing dashboards and alerts keyed on
 * {@code org.sonatype.nexus.metrics.InstrumentedFilter.responseCodes.*} keep working.
 *
 * <p>
 * The Dropwizard parent's {@code init()} looks up its registry via a servlet-context attribute
 * (see {@link io.dropwizard.metrics.servlet.InstrumentedFilter#REGISTRY_ATTRIBUTE}). We populate
 * that attribute with the same {@link SharedMetricRegistries} singleton that
 * {@code org.sonatype.nexus.bootstrap.jetty.InstrumentedHandler} and
 * {@code MetricsResource} use, so all sources land in one registry exposed by
 * {@code /service/metrics/data}.
 */
@Component
@Order(WebFilterPriority.WEB)
@WebFilter("/*")
public class InstrumentedFilter
    extends io.dropwizard.metrics.servlet.InstrumentedFilter
{
  @Override
  public void init(final FilterConfig filterConfig) throws ServletException {
    // Make sure the Dropwizard parent finds OUR shared MetricRegistry rather than creating a
    // detached one. The parent reads filterConfig.getServletContext().getAttribute(REGISTRY_ATTRIBUTE)
    // and falls back to a fresh MetricRegistry if absent - that fresh one would never reach
    // /service/metrics/data, which is the bug we're avoiding here.
    MetricRegistry registry = SharedMetricRegistries.getOrCreate(NEXUS_METRICS_REGISTRY_NAME);
    filterConfig.getServletContext().setAttribute(REGISTRY_ATTRIBUTE, registry);
    super.init(filterConfig);
  }
}
