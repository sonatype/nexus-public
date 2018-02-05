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

import java.util.List;

import com.codahale.metrics.SharedMetricRegistries;
import org.eclipse.jetty.server.ConnectionFactory;

/**
 * Extension of {@link com.codahale.metrics.jetty9.InstrumentedConnectionFactory}.
 *
 * @since 3.0
 */
public final class InstrumentedConnectionFactory
    extends com.codahale.metrics.jetty9.InstrumentedConnectionFactory
{
  private final ConnectionFactory connectionFactory;

  public InstrumentedConnectionFactory(final ConnectionFactory connectionFactory) {
    super(connectionFactory, SharedMetricRegistries.getOrCreate("nexus").timer("connection-duration"));
    this.connectionFactory = connectionFactory;
  }

  // HACK: metrics-jetty9 (presently) is based on jetty 9.2, but we have to add more api for jetty 9.3

  @Override
  public List<String> getProtocols() {
    return connectionFactory.getProtocols();
  }
}
