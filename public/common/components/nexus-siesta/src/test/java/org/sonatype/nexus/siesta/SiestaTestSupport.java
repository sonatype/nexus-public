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
package org.sonatype.nexus.siesta;

import java.util.Map;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;

import org.sonatype.nexus.bootstrap.siesta.SiestaConfiguration;
import org.sonatype.nexus.siesta.SiestaTestSupport.SiestaTestSupportConfiguration;

import org.eclipse.jetty.ee8.servlet.ServletHolder;
import org.eclipse.jetty.ee8.servlet.ServletTester;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.context.ContextConfiguration;

/**
 * Support for Siesta tests.
 */
@SpringBootTest
@ContextConfiguration(classes = {SiestaTestSupportConfiguration.class, SiestaConfiguration.class})
public abstract class SiestaTestSupport
{
  public static final String MOUNT_POINT = "/siesta";

  private ServletTester servletTester;

  private String url;

  private Client client;

  @Autowired
  protected ApplicationContext context;

  @ComponentScan({"org.sonatype.nexus.siesta"})
  static class SiestaTestSupportConfiguration
  {
  }

  @BeforeEach
  void startJetty() throws Exception {
    servletTester = new ServletTester();
    servletTester.getContext();

    url = servletTester.createConnector(true) + MOUNT_POINT;

    ServletHolder holder = new ServletHolder(context.getBean(SiestaServlet.class));
    holder.setInitParameters(Map.of("resteasy.servlet.mapping.prefix", MOUNT_POINT));
    servletTester.addServlet(holder, MOUNT_POINT + "/*");

    servletTester.addServlet(DummyServlet.class, "/*");
    servletTester.start();

    client = ClientBuilder.newClient();
  }

  @AfterEach
  void stopJetty() throws Exception {
    if (servletTester != null) {
      servletTester.stop();
    }
  }

  protected Client client() {
    return client;
  }

  protected String url() {
    return url;
  }

  protected String url(final String path) {
    return url + "/" + path;
  }
}
