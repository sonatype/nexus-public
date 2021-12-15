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
package org.sonatype.nexus.extender.modules;

import java.util.Map;

import org.sonatype.nexus.extender.modules.internal.CachedGaugeTypeListener;

import com.codahale.metrics.SharedMetricRegistries;
import com.google.inject.AbstractModule;
import com.google.inject.matcher.Matchers;
import com.palominolabs.metrics.guice.DefaultMetricNamer;
import com.palominolabs.metrics.guice.MetricsInstrumentationModule;
import com.palominolabs.metrics.guice.annotation.MethodAnnotationResolver;

/**
 * Provides instrumentation of methods annotated with metrics annotations.
 *
 * @since 3.0
 */
public class InstrumentationModule
    extends AbstractModule
{
  private final Map<?, ?> nexusProperties;

  InstrumentationModule(Map<?, ?> nexusProperties) {
    this.nexusProperties = nexusProperties;
  }

  @Override
  protected void configure() {
    install(MetricsInstrumentationModule.builder()
        .withMetricRegistry(SharedMetricRegistries.getOrCreate("nexus"))
        .build());

    bindListener(Matchers.any(),
        new CachedGaugeTypeListener(SharedMetricRegistries.getOrCreate("nexus"), new DefaultMetricNamer(),
            new MethodAnnotationResolver(), nexusProperties));
  }
}
