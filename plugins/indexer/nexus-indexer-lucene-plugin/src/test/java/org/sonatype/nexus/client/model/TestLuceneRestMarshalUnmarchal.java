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

import org.sonatype.nexus.rest.model.AliasingListConverter;
import org.sonatype.nexus.rest.model.NexusArtifact;
import org.sonatype.nexus.rest.model.SearchResponse;

import org.junit.Test;

public class TestLuceneRestMarshalUnmarchal
    extends TestMarshalUnmarchal
{
  @Override
  public void setUp()
      throws Exception
  {
    super.setUp();

    Class[] searchTypes = new Class[]{SearchResponse.class};
    getJsonXStream().allowTypes(searchTypes);
    getJsonXStream().processAnnotations(searchTypes);
    getXmlXStream().allowTypes(searchTypes);
    getXmlXStream().processAnnotations(searchTypes);

    getJsonXStream().registerLocalConverter(SearchResponse.class, "data", new AliasingListConverter(NexusArtifact.class,
        "artifact"));

    getXmlXStream().registerLocalConverter(SearchResponse.class, "data", new AliasingListConverter(NexusArtifact.class,
        "artifact"));
  }

  @Test
  public void testSearchResponse() {
    SearchResponse response = new SearchResponse();
    response.setCount(10);
    response.setFrom(50);
    response.setTotalCount(8);
    response.setTooManyResults(true);

    NexusArtifact artifact1 = new NexusArtifact();
    artifact1.setArtifactId("artifactId1");
    artifact1.setClassifier("classifier1");
    artifact1.setContextId("contextId1");
    artifact1.setGroupId("groupId1");
    artifact1.setPackaging("packaging1");
    artifact1.setRepoId("repoId1");
    artifact1.setResourceURI("resourceURI1");
    artifact1.setVersion("version1");
    artifact1.setArtifactLink("artifactLink");
    artifact1.setExtension("extension");
    artifact1.setPomLink("pomLink");
    response.addData(artifact1);

    NexusArtifact artifact2 = new NexusArtifact();
    artifact2.setArtifactId("artifactId1");
    artifact2.setClassifier("classifier1");
    artifact2.setContextId("contextId1");
    artifact2.setGroupId("groupId1");
    artifact2.setPackaging("packaging1");
    artifact2.setRepoId("repoId1");
    artifact2.setResourceURI("resourceURI1");
    artifact2.setVersion("version1");
    artifact2.setArtifactLink("artifactLink2");
    artifact2.setExtension("extension2");
    artifact2.setPomLink("pomLink2");
    response.addData(artifact2);

    this.marshalUnmarchalThenCompare(response);
    this.validateXmlHasNoPackageNames(response);
  }
}
