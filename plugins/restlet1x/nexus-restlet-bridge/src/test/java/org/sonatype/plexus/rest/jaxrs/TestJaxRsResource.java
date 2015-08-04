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
package org.sonatype.plexus.rest.jaxrs;

import java.util.Date;

import javax.inject.Named;
import javax.inject.Singleton;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

import org.codehaus.plexus.logging.AbstractLogEnabled;

@Named("test")
@Singleton
@Path("/test")
public class TestJaxRsResource
    extends AbstractLogEnabled
{
  @GET
  @Produces({"text/xml", "application/json"})
  public TestDto get(String param) {
    getLogger().info("Got GET request with param '" + param + "'");

    TestDto result = new TestDto();

    result.setAString(param);

    result.setADate(new Date());

    result.getAStringList().add(param);

    result.getAStringList().add(param);

    TestDto child = new TestDto();

    child.setAString("child");

    result.getChildren().add(child);

    return result;
  }

  @PUT
  @Consumes({"application/xml", "application/json"})
  @Produces({"application/xml", "application/json"})
  public String put(TestDto t) {
    getLogger().info("Got TestDTO " + t.getAString());

    return "OK";
  }
}
