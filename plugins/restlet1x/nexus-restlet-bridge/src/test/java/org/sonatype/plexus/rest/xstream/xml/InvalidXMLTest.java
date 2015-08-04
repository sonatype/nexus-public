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
package org.sonatype.plexus.rest.xstream.xml;

import org.sonatype.plexus.rest.TestApplication;

import org.codehaus.plexus.ContainerConfiguration;
import org.codehaus.plexus.PlexusConstants;
import org.codehaus.plexus.PlexusTestCase;
import org.restlet.Application;
import org.restlet.Client;
import org.restlet.Component;
import org.restlet.Context;
import org.restlet.data.MediaType;
import org.restlet.data.Method;
import org.restlet.data.Protocol;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.data.Status;

public class InvalidXMLTest
    extends PlexusTestCase
{

  private static final String URL = "http://localhost:8182/XStreamPlexusResource";

  private static final String VALID = "<simple><data>something</data></simple>";

  private static final String INVALID = "<simple><invalid/><data>something</data></simple>";

  @Override
  protected void customizeContainerConfiguration(ContainerConfiguration configuration) {
    super.customizeContainerConfiguration(configuration);
    configuration.setAutoWiring(true);
    configuration.setClassPathScanning(PlexusConstants.SCANNING_CACHE);
  }

  public void testXML()
      throws Exception
  {
    Component component = new Component();

    component.getServers().add(Protocol.HTTP, 8182);

    TestApplication app = (TestApplication) getContainer().lookup(Application.class, "test");

    component.getDefaultHost().attach(app);

    component.start();

    Status status = post(URL, VALID).getStatus();
    assertTrue(status.toString(), status.isSuccess());
    status = post(URL, INVALID).getStatus();
    assertEquals(status.toString(), 400, status.getCode());

    component.stop();
  }

  private Response post(String url, String content) {
    Request request = new Request();
    request.setResourceRef(url);
    request.setMethod(Method.POST);
    request.setEntity(content, MediaType.APPLICATION_XML);
    Context ctx = new Context();

    Client client = new Client(ctx, Protocol.HTTP);

    return client.handle(request);
  }
}
