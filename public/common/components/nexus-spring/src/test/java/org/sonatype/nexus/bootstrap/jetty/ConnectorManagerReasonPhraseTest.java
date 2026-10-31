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

import java.util.HashMap;
import java.util.Map;

import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

/**
 * STL-476: the reason-phrase customizer must be present on the shared {@code httpConfig} the main HTTP connector
 * uses, even when a booted {@code jetty.xml} omits the declarative {@code <Call name="addCustomizer">} block.
 *
 * <p>
 * The main connectors are built at startup via {@code buildDefaultConnectors()} (from the constructor), not via
 * {@code addConnector(...)} - which is only reached by dynamic Docker/OCI connector registration. This test
 * constructs a {@code ConnectorManager} with an HTTP-only bean set whose {@code httpConfig} has no customizer
 * (mirroring the common install with a jetty.xml missing the block) and asserts the guard registered it anyway.
 */
class ConnectorManagerReasonPhraseTest
{
  private Server server;

  @BeforeEach
  void setUp() {
    server = new Server();
  }

  @AfterEach
  void tearDown() throws Exception {
    server.stop();
  }

  @Test
  void startupRegistersCustomizerOnMainHttpConfigWhenJettyXmlOmitsIt() {
    // Bare httpConfig == a jetty.xml that never called addCustomizer.
    HttpConfiguration httpConfig = new HttpConfiguration();

    new ConnectorManager(server, httpOnlyBeans(httpConfig));

    assertThat(hasReasonPhraseCustomizer(httpConfig), is(true));
  }

  @Test
  void startupRegistrationIsIdempotentWhenJettyXmlAlreadyAddedIt() {
    HttpConfiguration httpConfig = new HttpConfiguration();
    httpConfig.addCustomizer(new NexusReasonPhraseCustomizer()); // already wired declaratively

    new ConnectorManager(server, httpOnlyBeans(httpConfig));

    long count = httpConfig.getCustomizers()
        .stream()
        .filter(NexusReasonPhraseCustomizer.class::isInstance)
        .count();
    assertThat(count, is(1L));
  }

  /**
   * Minimal HTTP-only bean set for {@code buildDefaultConnectors()}. No HTTPS beans, so the HTTPS branch is
   * skipped exactly as on an HTTP-only install.
   */
  private Map<String, Object> httpOnlyBeans(final HttpConfiguration httpConfig) {
    ServerConnector httpConnector = new ServerConnector(server);
    httpConnector.setPort(8081);
    Map<String, Object> beans = new HashMap<>();
    beans.put("httpConfig", httpConfig);
    beans.put("httpConnector", httpConnector);
    return beans;
  }

  private static boolean hasReasonPhraseCustomizer(final HttpConfiguration config) {
    return config.getCustomizers().stream().anyMatch(NexusReasonPhraseCustomizer.class::isInstance);
  }
}
