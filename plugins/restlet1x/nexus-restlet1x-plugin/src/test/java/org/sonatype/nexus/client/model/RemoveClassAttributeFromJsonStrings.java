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
package org.sonatype.nexus.client.model;

import org.sonatype.nexus.rest.NexusApplication;
import org.sonatype.nexus.rest.model.RepositoryRouteResourceResponse;
import org.sonatype.nexus.rest.model.ScheduledServiceResourceResponse;
import org.sonatype.plexus.rest.representation.XStreamRepresentation;
import org.sonatype.plexus.rest.xstream.json.JsonOrgHierarchicalStreamDriver;
import org.sonatype.sisu.goodies.testsupport.TestSupport;

import com.thoughtworks.xstream.XStream;
import org.junit.Before;
import org.junit.Test;
import org.restlet.data.MediaType;

// FIXME: Why doesn't this end with Test?

public class RemoveClassAttributeFromJsonStrings
  extends TestSupport
{
  private XStream xstreamJSON;

  @Before
  public void setUp()
      throws Exception
  {
    NexusApplication napp = new NexusApplication();

    xstreamJSON = napp.doConfigureXstream(new XStream(new JsonOrgHierarchicalStreamDriver()));
  }

  @Test
  public void testJsonStringWithClassAttribute() {
    String text = "{\"data\":{\"id\":\"11c75e9aea2\",\"ruleType\":\"exclusive\",\"groupId\":\"*\",\"pattern\":\".*\",\"repositories\":[{\"id\":\"central\",\"name\":\"Maven Central\",\"resourceURI\":\"http://localhost:8081/nexus/service/local/repositories/central\",\"@class\":\"repo-routes-member\"},{\"id\":\"thirdparty\",\"name\":\"3rd party\",\"resourceURI\":\"http://localhost:8081/nexus/service/local/repositories/thirdparty\",\"@class\":\"repo-routes-member\"},{\"id\":\"central-m1\",\"name\":\"Central M1 shadow\",\"resourceURI\":\"http://localhost:8081/nexus/service/local/repositories/central-m1\",\"@class\":\"repo-routes-member\"}]}}";

    XStreamRepresentation representation = new XStreamRepresentation(
        this.xstreamJSON,
        text,
        MediaType.APPLICATION_JSON);

    RepositoryRouteResourceResponse repoRouteResourceResponse = (RepositoryRouteResourceResponse) representation
        .getPayload(new RepositoryRouteResourceResponse());

    // System.out.println( "repoRouteResourceResponse: "+ repoRouteResourceResponse.getData().getPattern() );

  }

  @Test
  public void testJsonStringWithOutClassAttribute() {
    String text = "{\"data\":{\"id\":\"11c75e9aea2\",\"ruleType\":\"exclusive\",\"groupId\":\"*\",\"pattern\":\".*\",\"repositories\":[{\"id\":\"central\",\"name\":\"Maven Central\",\"resourceURI\":\"http://localhost:8081/nexus/service/local/repositories/central\"},{\"id\":\"thirdparty\",\"name\":\"3rd party\",\"resourceURI\":\"http://localhost:8081/nexus/service/local/repositories/thirdparty\"},{\"id\":\"central-m1\",\"name\":\"Central M1 shadow\",\"resourceURI\":\"http://localhost:8081/nexus/service/local/repositories/central-m1\"}]}}";

    XStreamRepresentation representation = new XStreamRepresentation(
        this.xstreamJSON,
        text,
        MediaType.APPLICATION_JSON);

    RepositoryRouteResourceResponse repoRouteResourceResponse = (RepositoryRouteResourceResponse) representation
        .getPayload(new RepositoryRouteResourceResponse());

    // System.out.println( "repoRouteResourceResponse: "+ repoRouteResourceResponse.getData().getPattern() );

  }

  // public void testScheduleJsonStringWithClassAttribute()
  // {
  // String text =
  // "{\"data\":{\"id\":\"11c75e9aea2\",\"ruleType\":\"exclusive\",\"groupId\":\"*\",\"pattern\":\".*\",\"repositories\":[{\"id\":\"central\",\"name\":\"Maven Central\",\"resourceURI\":\"http://localhost:8081/nexus/service/local/repositories/central\",\"@class\":\"repo-routes-member\"},{\"id\":\"thirdparty\",\"name\":\"3rd party\",\"resourceURI\":\"http://localhost:8081/nexus/service/local/repositories/thirdparty\",\"@class\":\"repo-routes-member\"},{\"id\":\"central-m1\",\"name\":\"Central M1 shadow\",\"resourceURI\":\"http://localhost:8081/nexus/service/local/repositories/central-m1\",\"@class\":\"repo-routes-member\"}]}}";
  //
  // XStreamRepresentation representation = new XStreamRepresentation( this.xstreamJSON, text,
  // MediaType.APPLICATION_JSON );
  //
  // RepositoryRouteResourceResponse repoRouteResourceResponse = (RepositoryRouteResourceResponse)
  // representation.getPayload( new RepositoryRouteResourceResponse() );
  //
  // System.out.println( "repoRouteResourceResponse: "+ repoRouteResourceResponse.getData().getPattern() );
  //
  // }

  @Test
  public void testScheduleJsonStringWithOutClassAttribute() {
    String text = "{\"data\":{\"id\":null,\"name\":\"clear repo cache\",\"enabled\":true,\"typeId\":\"ExpireCacheTask\",\"schedule\":\"manual\",\"properties\":[{\"id\":\"repositoryId\",\"value\":\"all_repo\"},{\"id\":\"resourceStorePath\",\"value\":\"\"}]}}";

    XStreamRepresentation representation = new XStreamRepresentation(
        this.xstreamJSON,
        text,
        MediaType.APPLICATION_JSON);

    ScheduledServiceResourceResponse repoRouteResourceResponse = (ScheduledServiceResourceResponse) representation
        .getPayload(new ScheduledServiceResourceResponse());

    // System.out.println( "repoRouteResourceResponse: "+ repoRouteResourceResponse.getData().getPattern() );

  }

}
