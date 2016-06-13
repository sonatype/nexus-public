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
package org.sonatype.nexus.webapp.jetty;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.bootstrap.jetty.ConnectorConfiguration;
import org.sonatype.nexus.bootstrap.jetty.ConnectorRegistrar;
import org.sonatype.nexus.bootstrap.jetty.JettyServer;
import org.sonatype.nexus.bootstrap.jetty.UnsupportedHttpSchemeException;
import org.sonatype.sisu.goodies.common.ComponentSupport;

import com.google.common.collect.Maps;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Connector registrar component.
 */
@Singleton
@Named
public class ConnectorRegistrarImpl
    extends ComponentSupport
    implements ConnectorRegistrar
{
  private final JettyServer jettyServer;

  private final HashMap<Integer, ConnectorConfiguration> managedConfigurations;

  @Inject
  public ConnectorRegistrarImpl(final JettyServer jettyServer) {
    this.jettyServer = checkNotNull(jettyServer);
    this.managedConfigurations = Maps.newHashMap();
  }

  @Override
  public List<ConnectorConfiguration.Scheme> availableSchemes() {
    final List<ConnectorConfiguration.Scheme> result = new ArrayList<>();
    for (ConnectorConfiguration defaultConnector : jettyServer.defaultConnectors()) {
      result.add(defaultConnector.getScheme());
    }
    return result;
  }

  @Override
  public List<Integer> unavailablePorts() {
    final List<Integer> result = new ArrayList<>();
    for (ConnectorConfiguration defaultConnector : jettyServer.defaultConnectors()) {
      result.add(defaultConnector.getPort());
    }
    for (Integer port : managedConfigurations.keySet()) {
      result.add(port);
    }
    return result;
  }

  @Override
  public void addConnector(final ConnectorConfiguration connectorConfiguration) {
    checkNotNull(connectorConfiguration);
    validate(connectorConfiguration);

    jettyServer.addCustomConnector(connectorConfiguration);

    log.info("Adding connector configuration {}", connectorConfiguration);
    managedConfigurations.put(connectorConfiguration.getPort(), connectorConfiguration);
  }

  @Override
  public void removeConnector(final ConnectorConfiguration connectorConfiguration) {
    checkNotNull(connectorConfiguration);
    if (managedConfigurations.containsKey(connectorConfiguration.getPort())) {
      log.info("Removing connector configuration {}", connectorConfiguration);
      jettyServer.removeCustomConnector(connectorConfiguration);
      managedConfigurations.remove(connectorConfiguration.getPort());
    }
  }

  private void validate(final ConnectorConfiguration connectorConfiguration) {
    // connector is not already added
    checkArgument(!managedConfigurations.containsKey(connectorConfiguration.getPort()));

    // schema is not null and is available
    final ConnectorConfiguration.Scheme httpScheme = connectorConfiguration.getScheme();
    checkNotNull(httpScheme);
    if (!availableSchemes().contains(httpScheme)) {
      throw new UnsupportedHttpSchemeException(httpScheme);
    }

    // port is ok and free
    final int port = connectorConfiguration.getPort();
    checkArgument(port > 0);
    checkArgument(port < 65536);
    checkArgument(!unavailablePorts().contains(port));
  }
}
