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

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.nio.SelectChannelConnector;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.util.Collections.unmodifiableList;

/**
 * {@link JettyServer}'s internal connector manager to manage custom {@link ConnectorConfiguration}s.
 * <p/>
 * Note: This class relies on "id" properties in Jetty XML configuration! Hence, the existing beans in Jetty XML
 * should NOT be renamed, while adding new (with unique, non-clashing) IDs is okay thing to do.
 *
 * @since 2.13.1
 */
public class ConnectorManager
{
  private static final Logger log = LoggerFactory.getLogger(ConnectorManager.class);

  private static final String HTTP_CONNECTOR_ID = "HTTPConnector";

  private static final String SSL_CONTEXT_FACTORY_ID = "SSLContextFactory";

  private static final String HTTPS_CONNECTOR_ID = "HTTPSConnector";

  private final Server server;

  private final Map<String, Object> namedBeans;

  private final IdentityHashMap<ConnectorConfiguration, SelectChannelConnector> managedConnectors;

  private final List<ConnectorConfiguration> defaultConnectors;

  public ConnectorManager(final Server server,
                          final Map<String, Object> namedBeans)
  {
    this.server = server;
    this.namedBeans = namedBeans;
    this.managedConnectors = new IdentityHashMap<>();
    this.defaultConnectors = unmodifiableList(buildDefaultConnectors());
  }

  public List<ConnectorConfiguration> defaultConnectors() {
    return defaultConnectors;
  }

  /**
   * Adds and starts connector to Jetty based on passed in configuration.
   */
  public SelectChannelConnector addConnector(final ConnectorConfiguration connectorConfiguration)
  {
    final ConnectorConfiguration.Scheme httpScheme = connectorConfiguration.getScheme();
    verifyConfiguration(httpScheme);

    final SelectChannelConnector connectorPrototype =
        defaultConnector(httpScheme);
    final SelectChannelConnector serverConnector;
    if (ConnectorConfiguration.Scheme.HTTP == httpScheme) {
      InstrumentedSelectChannelConnector instrumentedConnector = new InstrumentedSelectChannelConnector();
      final RequestCustomizer requestCustomizer = connectorConfiguration.getRequestCustomizer();
      if (requestCustomizer != null) {
        instrumentedConnector.setRequestCustomizer(requestCustomizer);
      }
      serverConnector = instrumentedConnector;
    }
    else if (ConnectorConfiguration.Scheme.HTTPS == httpScheme) {
      final SslContextFactory sslContextFactory = bean(SSL_CONTEXT_FACTORY_ID, SslContextFactory.class);
      InstrumentedSslSelectChannelConnector instrumentedConnector =
          new InstrumentedSslSelectChannelConnector(sslContextFactory);
      final RequestCustomizer requestCustomizer = connectorConfiguration.getRequestCustomizer();
      if (requestCustomizer != null) {
        instrumentedConnector.setRequestCustomizer(requestCustomizer);
      }
      serverConnector = instrumentedConnector;
    }
    else {
      throw new UnsupportedHttpSchemeException(httpScheme);
    }

    serverConnector.setServer(server);
    serverConnector.setHost(connectorPrototype.getHost());
    serverConnector.setPort(connectorConfiguration.getPort());
    serverConnector.setMaxIdleTime(connectorPrototype.getMaxIdleTime());
    serverConnector.setSoLingerTime(connectorPrototype.getSoLingerTime());
    serverConnector.setLowResourcesConnections(connectorPrototype.getLowResourcesConnections());
    //serverConnector.setAcceptorPriorityDelta(connectorPrototype.getAcceptorPriorityDelta());
    //serverConnector.setSelectorPriorityDelta(connectorPrototype.getSelectorPriorityDelta());
    serverConnector.setAcceptQueueSize(connectorPrototype.getAcceptQueueSize());

    managedConnectors.put(connectorConfiguration, serverConnector);
    server.addConnector(serverConnector);
    if (server.isStarted()) {
      try {
        serverConnector.start();
      }
      catch (Exception e) {
        log.warn("Could not start connector: {}", connectorConfiguration, e);
        throw new RuntimeException(e);
      }
    }

    return serverConnector;
  }

  /**
   * Stops and removes the connector configuration from Jetty.
   */
  public void removeConnector(final ConnectorConfiguration connectorConfiguration) {
    final SelectChannelConnector serverConnector = managedConnectors.remove(connectorConfiguration);
    if (serverConnector != null) {
      if (server.isStarted()) {
        try {
          serverConnector.stop();
        }
        catch (Exception e) {
          log.warn("Could not stop connector: {}", connectorConfiguration, e);
          throw new RuntimeException(e);
        }
      }
      server.removeConnector(serverConnector);
    }
  }

  // ==

  /**
   * Verifies all the needed bits are present in Jetty XML configuration (as HTTPS must be enabled by users).
   */
  private void verifyConfiguration(final ConnectorConfiguration.Scheme httpScheme) {
    try {
      if (ConnectorConfiguration.Scheme.HTTP == httpScheme) {
        bean(HTTP_CONNECTOR_ID, SelectChannelConnector.class);
      }
      else if (ConnectorConfiguration.Scheme.HTTPS == httpScheme) {
        bean(SSL_CONTEXT_FACTORY_ID, SslContextFactory.class);
        bean(HTTPS_CONNECTOR_ID, SelectChannelConnector.class);
      }
      else {
        throw new UnsupportedHttpSchemeException(httpScheme);
      }
    }
    catch (IllegalStateException e) {
      throw new IllegalStateException("Jetty HTTPS is not enabled in Nexus", e);
    }
  }

  private List<ConnectorConfiguration> buildDefaultConnectors() {
    final List<ConnectorConfiguration> result = new ArrayList<>();
    try {
      verifyConfiguration(ConnectorConfiguration.Scheme.HTTP);
      final int port = defaultConnector(ConnectorConfiguration.Scheme.HTTP).getPort();
      result.add(new ConnectorConfiguration(ConnectorConfiguration.Scheme.HTTP, port, null));
    }
    catch (IllegalStateException e) {
      log.debug("No HTTP configuration present", e);
    }
    try {
      verifyConfiguration(ConnectorConfiguration.Scheme.HTTPS);
      final int port = defaultConnector(ConnectorConfiguration.Scheme.HTTPS).getPort();
      result.add(new ConnectorConfiguration(ConnectorConfiguration.Scheme.HTTPS, port, null));
    }
    catch (IllegalStateException e) {
      log.debug("No HTTPS configuration present", e);
    }
    return result;
  }

  /**
   * Returns the OOTB defined connector for given HTTP scheme.
   */
  private SelectChannelConnector defaultConnector(final ConnectorConfiguration.Scheme httpScheme) {
    if (ConnectorConfiguration.Scheme.HTTP == httpScheme) {
      return bean(HTTP_CONNECTOR_ID, SelectChannelConnector.class);
    }
    else if (ConnectorConfiguration.Scheme.HTTPS == httpScheme) {
      return bean(HTTPS_CONNECTOR_ID, SelectChannelConnector.class);
    }
    else {
      throw new UnsupportedHttpSchemeException(httpScheme);
    }
  }

  /**
   * Gets a bean for Jetty XML configuration by name, casted to given type.
   */
  private <T> T bean(final String name, final Class<T> clazz) {
    final Object bean = namedBeans.get(name);
    if (bean == null) {
      throw new IllegalStateException(
          "Jetty XML configuration does not contain bean with name: " + name + ", type=" + clazz.getName());
    }
    return clazz.cast(bean);
  }
}
