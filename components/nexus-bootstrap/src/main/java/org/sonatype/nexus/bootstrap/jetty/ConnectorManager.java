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

import org.eclipse.jetty.http.HttpScheme;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.SslConnectionFactory;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.util.Collections.unmodifiableList;

/**
 * {@link JettyServer}'s internal connector manager to manage custom {@link ConnectorConfiguration}s.
 * <p/>
 * Note: This class relies on "id" properties in Jetty XML configuration! Hence, the existing beans in Jetty XML
 * should NOT be renamed, while adding new (with unique, non-clashing) IDs is okay thing to do.
 */
public class ConnectorManager
{
  private static final Logger log = LoggerFactory.getLogger(ConnectorManager.class);

  private static final String HTTP_CONFIG_ID = "httpConfig";

  private static final String HTTP_CONNECTOR_ID = "httpConnector";

  private static final String SSL_CONTEXT_FACTORY_ID = "sslContextFactory";

  private static final String HTTPS_CONFIG_ID = "httpsConfig";

  private static final String HTTPS_CONNECTOR_ID = "httpsConnector";

  private final Server server;

  private final Map<String, Object> namedBeans;

  private final IdentityHashMap<ConnectorConfiguration, ServerConnector> managedConnectors;

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
  public ServerConnector addConnector(final ConnectorConfiguration connectorConfiguration)
  {
    final HttpScheme httpScheme = connectorConfiguration.getScheme();
    verifyConfiguration(httpScheme);

    final HttpConfiguration httpConfiguration =
        connectorConfiguration.customize(defaultHttpConfiguration(httpScheme));
    final ServerConnector connectorPrototype =
        defaultConnector(httpScheme);
    final ServerConnector serverConnector;
    if (HttpScheme.HTTP == httpScheme) {
      serverConnector = new ServerConnector(
          server,
          connectorPrototype.getAcceptors(),
          connectorPrototype.getSelectorManager().getSelectorCount(),
          new InstrumentedConnectionFactory(
              new HttpConnectionFactory(httpConfiguration)
          )
      );
    }
    else if (HttpScheme.HTTPS == httpScheme) {
      final SslContextFactory sslContextFactory = bean(SSL_CONTEXT_FACTORY_ID, SslContextFactory.class);
      serverConnector = new ServerConnector(
          server,
          connectorPrototype.getAcceptors(),
          connectorPrototype.getSelectorManager().getSelectorCount(),
          new InstrumentedConnectionFactory(
              new SslConnectionFactory(sslContextFactory, "http/1.1")
          ),
          new HttpConnectionFactory(httpConfiguration)
      );
    }
    else {
      throw new UnsupportedHttpSchemeException(httpScheme);
    }
    serverConnector.setHost(connectorPrototype.getHost());
    serverConnector.setPort(connectorConfiguration.getPort());
    serverConnector.setIdleTimeout(connectorPrototype.getIdleTimeout());
    serverConnector.setSoLingerTime(connectorPrototype.getSoLingerTime());
    serverConnector.setAcceptorPriorityDelta(connectorPrototype.getAcceptorPriorityDelta());
    serverConnector.setSelectorPriorityDelta(connectorPrototype.getSelectorPriorityDelta());
    serverConnector.setAcceptQueueSize(connectorPrototype.getAcceptQueueSize());

    managedConnectors.put(connectorConfiguration, serverConnector);
    server.addConnector(serverConnector);
    try {
      serverConnector.start();
    }
    catch (Exception e) {
      log.warn("Could not start connector: {}", connectorConfiguration, e);
      throw new RuntimeException(e);
    }

    return serverConnector;
  }

  /**
   * Stops and removes the connector configuration from Jetty.
   */
  public void removeConnector(final ConnectorConfiguration connectorConfiguration) {
    final ServerConnector serverConnector = managedConnectors.remove(connectorConfiguration);
    if (serverConnector != null) {
      try {
        serverConnector.stop();
      }
      catch (Exception e) {
        log.warn("Could not stop connector: {}", connectorConfiguration, e);
        throw new RuntimeException(e);
      }
      server.removeConnector(serverConnector);
    }
  }

  // ==

  /**
   * Verifies all the needed bits are present in Jetty XML configuration (as HTTPS must be enabled by users).
   */
  private void verifyConfiguration(final HttpScheme httpScheme) {
    try {
      if (HttpScheme.HTTP == httpScheme) {
        bean(HTTP_CONFIG_ID, HttpConfiguration.class);
        bean(HTTP_CONNECTOR_ID, ServerConnector.class);
      }
      else if (HttpScheme.HTTPS == httpScheme) {
        bean(SSL_CONTEXT_FACTORY_ID, SslContextFactory.class);
        bean(HTTPS_CONFIG_ID, HttpConfiguration.class);
        bean(HTTPS_CONNECTOR_ID, ServerConnector.class);
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
      verifyConfiguration(HttpScheme.HTTP);
      final int port = defaultConnector(HttpScheme.HTTP).getPort();
      result.add(new ConnectorConfiguration()
      {
        @Override
        public HttpScheme getScheme() {
          return HttpScheme.HTTP;
        }

        @Override
        public int getPort() {
          return port;
        }

        @Override
        public HttpConfiguration customize(final HttpConfiguration configuration) {
          return configuration;
        }
      });
    }
    catch (IllegalStateException e) {
      log.debug("No HTTP configuration present", e);
    }
    try {
      verifyConfiguration(HttpScheme.HTTPS);
      final int port = defaultConnector(HttpScheme.HTTPS).getPort();
      result.add(new ConnectorConfiguration()
      {
        @Override
        public HttpScheme getScheme() {
          return HttpScheme.HTTPS;
        }

        @Override
        public int getPort() {
          return port;
        }

        @Override
        public HttpConfiguration customize(final HttpConfiguration configuration) {
          return configuration;
        }
      });
    }
    catch (IllegalStateException e) {
      log.debug("No HTTPS configuration present", e);
    }
    return result;
  }

  /**
   * Returns the OOTB defined configuration for given HTTP scheme.
   */
  private HttpConfiguration defaultHttpConfiguration(final HttpScheme httpScheme) {
    if (HttpScheme.HTTP == httpScheme) {
      return bean(HTTP_CONFIG_ID, HttpConfiguration.class);
    }
    else if (HttpScheme.HTTPS == httpScheme) {
      return bean(HTTPS_CONFIG_ID, HttpConfiguration.class);
    }
    else {
      throw new UnsupportedHttpSchemeException(httpScheme);
    }
  }

  /**
   * Returns the OOTB defined connector for given HTTP scheme.
   */
  private ServerConnector defaultConnector(final HttpScheme httpScheme) {
    if (HttpScheme.HTTP == httpScheme) {
      return bean(HTTP_CONNECTOR_ID, ServerConnector.class);
    }
    else if (HttpScheme.HTTPS == httpScheme) {
      return bean(HTTPS_CONNECTOR_ID, ServerConnector.class);
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
