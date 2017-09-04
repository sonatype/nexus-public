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
package org.sonatype.nexus.internal.metrics;

import javax.inject.Named;

import org.sonatype.goodies.common.ComponentSupport;

import com.codahale.metrics.Metric;
import com.codahale.metrics.MetricRegistry;
import org.eclipse.sisu.BeanEntry;
import org.eclipse.sisu.Mediator;

/**
 * Manages {@link Metric} registrations via Sisu component mediation.
 *
 * @since 3.6
 */
@Named
public class MetricMediator
    extends ComponentSupport
    implements Mediator<Named, Metric, MetricRegistry>
{
  public void add(final BeanEntry<Named, Metric> entry, final MetricRegistry registry) throws Exception {
    log.debug("Registering: {}", entry);
    registry.register(entry.getKey().value(), entry.getValue());
  }

  public void remove(final BeanEntry<Named, Metric> entry, final MetricRegistry registry) throws Exception {
    log.debug("Un-registering: {}", entry);
    registry.remove(entry.getKey().value());
  }
}
