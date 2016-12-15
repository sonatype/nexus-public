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
package org.sonatype.nexus.pax.logging;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.UnsynchronizedAppenderBase;
import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.SharedMetricRegistries;

/**
 * Custom metrics aware appender to customize namespaces.
 * 
 * @since 3.0
 */
public final class InstrumentedAppender
  extends UnsynchronizedAppenderBase<ILoggingEvent>
{
  private final MetricRegistry registry;

  // NOTE: duplicated in part from https://github.com/dropwizard/metrics/blob/3.2-development/metrics-logback/src/main/java/com/codahale/metrics/logback/InstrumentedAppender.java
  // NOTE: ... to allow customization of metric names, which is not otherwise extensible

  // TODO: Consider layering MetricRegistry (which is a MetricSet) to organize default impl into "nexus" registry tree?

  private Meter all;
  private Meter trace;
  private Meter debug;
  private Meter info;
  private Meter warn;
  private Meter error;

  public InstrumentedAppender() {
    this.registry = SharedMetricRegistries.getOrCreate("nexus");
  }

  @Override
  public void start() {
    this.all = registry.meter(name("all"));
    this.trace = registry.meter(name("trace"));
    this.debug = registry.meter(name("debug"));
    this.info = registry.meter(name("info"));
    this.warn = registry.meter(name("warn"));
    this.error = registry.meter(name("error"));
    super.start();
  }

  /**
   * Generates a metric name including the class-name and appender name.
   */
  private String name(final String suffix) {
    return MetricRegistry.name(InstrumentedAppender.class, getName(), suffix);
  }

  @Override
  protected void append(ILoggingEvent event) {
    all.mark();
    switch (event.getLevel().toInt()) {
      case Level.TRACE_INT:
        trace.mark();
        break;
      case Level.DEBUG_INT:
        debug.mark();
        break;
      case Level.INFO_INT:
        info.mark();
        break;
      case Level.WARN_INT:
        warn.mark();
        break;
      case Level.ERROR_INT:
        error.mark();
        break;
      default:
        break;
    }
  }
}
