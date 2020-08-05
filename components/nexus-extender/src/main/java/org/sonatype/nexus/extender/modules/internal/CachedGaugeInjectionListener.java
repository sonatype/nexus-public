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
package org.sonatype.nexus.extender.modules.internal;

import java.lang.reflect.Method;
import java.util.concurrent.TimeUnit;

import com.codahale.metrics.CachedGauge;
import com.codahale.metrics.MetricRegistry;
import com.google.inject.spi.InjectionListener;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Registers the supplied cache gauage to the metrics registry
 * @since 3.26
 */
public class CachedGaugeInjectionListener<T>
    implements InjectionListener<T>
{
  private final MetricRegistry metricRegistry;

  private final String metricName;

  private final long timeout;

  private final TimeUnit timeUnit;

  private final Method method;

  public CachedGaugeInjectionListener(
      final MetricRegistry metricRegistry,
      final String metricName,
      final Method method,
      final long timeout,
      final TimeUnit timeUnit)
  {
    checkArgument(timeout > 0);
    this.metricRegistry = checkNotNull(metricRegistry);
    this.metricName = checkNotNull(metricName);
    this.timeout = timeout;
    this.timeUnit = checkNotNull(timeUnit);
    this.method = checkNotNull(method);
  }

  @Override
  public void afterInjection(final T t) {
    metricRegistry.register(metricName, new CachedGauge<Object>(timeout, timeUnit)
    {
      @Override
      protected Object loadValue() {
        try {
          return method.invoke(t);
        }
        catch (Exception e) {
          return new RuntimeException(e);
        }
      }
    });
  }
}