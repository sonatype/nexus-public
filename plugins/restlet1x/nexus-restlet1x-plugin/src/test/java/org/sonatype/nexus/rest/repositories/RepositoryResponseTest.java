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
package org.sonatype.nexus.rest.repositories;

import org.sonatype.nexus.proxy.repository.RepositoryWritePolicy;
import org.sonatype.nexus.rest.AbstractRestTestCase;
import org.sonatype.nexus.rest.model.RepositoryResourceResponse;
import org.sonatype.plexus.rest.representation.XStreamRepresentation;

import org.junit.Test;
import org.restlet.data.MediaType;

public class RepositoryResponseTest
    extends AbstractRestTestCase
{

  @Test
  public void testRepo()
      throws Exception
  {
    String jsonString =
        "{\"data\" : {\"writePolicy\":\""
            + RepositoryWritePolicy.ALLOW_WRITE.name()
            +
            "\", \"browseable\":true,\"defaultLocalStorageUrl\":null,\"id\":\"test1\", \"indexable\":false,\"name\":\"test1\",\"notFoundCacheTTL\":1440,\"overrideLocalStorageUrl\":null,\"repoPolicy\":\"release\", \"repoType\":\"hosted\"}}";

    XStreamRepresentation representation =
        new XStreamRepresentation(xstream, jsonString, MediaType.APPLICATION_JSON);

    RepositoryResourceResponse response =
        (RepositoryResourceResponse) representation.getPayload(new RepositoryResourceResponse());

  }

  @Test
  public void testProxyRepo()
      throws Exception
  {
    String jsonString =
        "{\"data\" : {\"writePolicy\":\""
            + RepositoryWritePolicy.ALLOW_WRITE.name()
            +
            "\", \"artifactMaxAge\":1440,\"browseable\":true,\"defaultLocalStorageUrl\":null,\"id\":\"test1\", \"indexable\":false,\"metadataMaxAge\":1440,\"name\":\"test1\",\"notFoundCacheTTL\":1440,\"overrideLocalStorageUrl\":null,\"repoPolicy\":\"release\", \"repoType\":\"proxy\"}}";

    XStreamRepresentation representation =
        new XStreamRepresentation(xstream, jsonString, MediaType.APPLICATION_JSON);

    RepositoryResourceResponse response =
        (RepositoryResourceResponse) representation.getPayload(new RepositoryResourceResponse());

  }

}
