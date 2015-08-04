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

import java.io.StringWriter;
import java.io.Writer;

import org.restlet.Client;
import org.restlet.data.Method;
import org.restlet.data.Protocol;
import org.restlet.data.Request;
import org.restlet.data.Response;

public class TestClient
{
  private Response response;

  public String request(String uri)
      throws Exception
  {
    Request request = new Request();

    request.setResourceRef(uri);

    request.setMethod(Method.GET);

    Client client = new Client(Protocol.HTTP);

    response = client.handle(request);

    Writer writer = new StringWriter();

    if (response.getStatus().isSuccess()) {
      response.getEntity().write(writer);

      return writer.toString();
    }
    else {
      return null;
    }
  }

  public Response getLastResponse() {
    return response;
  }
}
