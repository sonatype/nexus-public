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

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.SharedMetricRegistries;
import org.eclipse.jetty.ee10.servlet.ServletContextHandler;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.sonatype.nexus.common.app.FeatureFlags.METRICS_INTERNAL_ENABLED;
import static org.sonatype.nexus.common.metrics.MetricsConstants.NEXUS_METRICS_REGISTRY_NAME;

/**
 * Extension of {@link io.dropwizard.metrics.jetty12.AbstractInstrumentedHandler} that restores the delegate
 * constructor.
 */
public final class InstrumentedHandler // NOSONAR
    extends io.dropwizard.metrics.jetty12.AbstractInstrumentedHandler
{
  private static final MetricRegistry NOOP_REGISTRY = new MetricRegistry();

  private final ServletContextHandler delegate;

  public InstrumentedHandler(final ServletContextHandler delegate) {
    super(resolveRegistry());
    setHandler(delegate);
    this.delegate = checkNotNull(delegate);
  }

  private static MetricRegistry resolveRegistry() {
    if (Boolean.parseBoolean(System.getProperty(METRICS_INTERNAL_ENABLED, "true"))) {
      return SharedMetricRegistries.getOrCreate(NEXUS_METRICS_REGISTRY_NAME);
    }
    return NOOP_REGISTRY;
  }

  public ServletContextHandler getDelegate() {
    return delegate;
  }

  @Override
  protected void setupServletListeners(final Request request, final Response response) {
    // no-op
  }

  @Override
  protected boolean isSuspended(final Request request, final Response response) {
    return false;
  }
}
