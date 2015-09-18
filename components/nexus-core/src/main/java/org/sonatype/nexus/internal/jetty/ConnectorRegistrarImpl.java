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
package org.sonatype.nexus.internal.jetty;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.bootstrap.jetty.ConnectorConfiguration;
import org.sonatype.nexus.bootstrap.jetty.ConnectorRegistrar;
import org.sonatype.nexus.bootstrap.jetty.JettyServerConfiguration;
import org.sonatype.nexus.bootstrap.jetty.UnsupportedHttpSchemeException;

import com.google.common.collect.Maps;
import org.eclipse.jetty.http.HttpScheme;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceRegistration;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Connector registrar component.
 *
 * @since 3.0
 */
@Singleton
@Named
public class ConnectorRegistrarImpl
    extends ComponentSupport
    implements ConnectorRegistrar
{
  private final JettyServerConfiguration serverConfiguration;

  private final IdentityHashMap<ConnectorConfiguration, ServiceRegistration<ConnectorConfiguration>> managedConfigurations;

  @Inject
  public ConnectorRegistrarImpl(final JettyServerConfiguration serverConfiguration) {
    this.serverConfiguration = checkNotNull(serverConfiguration);
    this.managedConfigurations = Maps.newIdentityHashMap();
  }

  @Override
  public List<HttpScheme> availableSchemes() {
    final List<HttpScheme> result = new ArrayList<>();
    for (ConnectorConfiguration defaultConnector : serverConfiguration.defaultConnectors()) {
      result.add(defaultConnector.getScheme());
    }
    return result;
  }

  @Override
  public List<Integer> unavailablePorts() {
    final List<Integer> result = new ArrayList<>();
    for (ConnectorConfiguration defaultConnector : serverConfiguration.defaultConnectors()) {
      result.add(defaultConnector.getPort());
    }
    for (ConnectorConfiguration defaultConnector : managedConfigurations.keySet()) {
      result.add(defaultConnector.getPort());
    }
    return result;
  }

  @Override
  public void addConnector(final ConnectorConfiguration connectorConfiguration) {
    checkNotNull(connectorConfiguration);
    validate(connectorConfiguration);

    final Bundle bundle = FrameworkUtil.getBundle(connectorConfiguration.getClass());
    if (bundle == null) {
      log.warn("No bundle found for {}, not registering connector", connectorConfiguration);
      return;
    }
    final BundleContext bundleContext = bundle.getBundleContext();
    if (bundleContext == null) {
      log.warn("No context found for bundle {}, not registering connector", bundle);
      return;
    }

    log.info("Adding connector configuration {}", connectorConfiguration);
    final ServiceRegistration<ConnectorConfiguration> serviceRegistration =
        bundleContext.registerService(ConnectorConfiguration.class, connectorConfiguration, null);
    managedConfigurations.put(connectorConfiguration, serviceRegistration);
  }

  @Override
  public void removeConnector(final ConnectorConfiguration connectorConfiguration) {
    checkNotNull(connectorConfiguration);
    final ServiceRegistration<ConnectorConfiguration> serviceRegistration =
        managedConfigurations.remove(connectorConfiguration);
    if (serviceRegistration != null) {
      log.info("Removing connector configuration {}", connectorConfiguration);
      try {
        serviceRegistration.unregister();
      }
      catch (IllegalStateException e) {
        // nop, happens on shutdown when context unregisters automatically all services
        log.debug("Could not unregister connector", e);
      }
    }
  }

  private void validate(final ConnectorConfiguration connectorConfiguration) {
    // connector is not already added
    checkArgument(!managedConfigurations.containsKey(connectorConfiguration));

    // schema is not null and is available
    final HttpScheme httpScheme = connectorConfiguration.getScheme();
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
