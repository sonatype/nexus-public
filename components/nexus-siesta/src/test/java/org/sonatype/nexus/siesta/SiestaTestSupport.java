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

import java.util.EnumSet;

import javax.servlet.DispatcherType;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;

import org.sonatype.goodies.testsupport.TestSupport;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.servlet.GuiceFilter;
import com.google.inject.servlet.GuiceServletContextListener;
import org.eclipse.jetty.servlet.ServletTester;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.ExpectedException;

/**
 * Support for Siesta tests.
 */
public class SiestaTestSupport
    extends TestSupport
{
  private ServletTester servletTester;

  private String url;

  private Client client;

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @Before
  public void startJetty() throws Exception {
    servletTester = new ServletTester();
    servletTester.getContext().addEventListener(new GuiceServletContextListener()
    {
      final Injector injector = Guice.createInjector(new TestModule());

      @Override
      protected Injector getInjector() {
        return injector;
      }
    });

    url = servletTester.createConnector(true) + TestModule.MOUNT_POINT;
    servletTester.addFilter(GuiceFilter.class, "/*", EnumSet.of(DispatcherType.REQUEST));
    servletTester.addServlet(DummyServlet.class, "/*");
    servletTester.start();

    client = ClientBuilder.newClient();
  }

  @After
  public void stopJetty() throws Exception {
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