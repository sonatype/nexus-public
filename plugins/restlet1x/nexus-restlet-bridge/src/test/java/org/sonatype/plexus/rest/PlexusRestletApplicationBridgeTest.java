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
package org.sonatype.plexus.rest;

import org.codehaus.plexus.ContainerConfiguration;
import org.codehaus.plexus.PlexusConstants;
import org.codehaus.plexus.PlexusTestCase;
import org.restlet.Application;
import org.restlet.Component;
import org.restlet.data.Form;
import org.restlet.data.Parameter;
import org.restlet.data.Protocol;
import org.restlet.data.Response;

public class PlexusRestletApplicationBridgeTest
    extends PlexusTestCase
{
  private Component component;

  @Override
  protected void customizeContainerConfiguration(ContainerConfiguration configuration) {
    super.customizeContainerConfiguration(configuration);
    configuration.setAutoWiring(true);
    configuration.setClassPathScanning(PlexusConstants.SCANNING_CACHE);
  }

  public void testRest()
      throws Exception
  {

    TestClient client = new TestClient();

    assertEquals("tokenA", client.request("http://localhost:8182/tokenA"));

    assertEquals("tokenB", client.request("http://localhost:8182/tokenB"));

    assertEquals("manual", client.request("http://localhost:8182/manual"));

    // test that for tokenC an custom header is added to response
    assertEquals("tokenC", client.request("http://localhost:8182/tokenC"));
    final Response lastResponse = client.getLastResponse();
    final Form form = (Form) lastResponse.getAttributes().get("org.restlet.http.headers");
    assertNotNull(form);
    final Parameter xCustomHeader = form.getFirst("X-Custom");
    assertNotNull(xCustomHeader);
    assertEquals("foo", xCustomHeader.getValue());
  }

  @Override
  protected void setUp()
      throws Exception
  {
    super.setUp();

    component = new Component();

    component.getServers().add(Protocol.HTTP, 8182);

    TestApplication app = (TestApplication) getContainer().lookup(Application.class, "test");

    component.getDefaultHost().attach(app);

    component.start();
  }

  @Override
  protected void tearDown()
      throws Exception
  {
    super.tearDown();

    if (component != null) {
      component.stop();
    }
  }

}
