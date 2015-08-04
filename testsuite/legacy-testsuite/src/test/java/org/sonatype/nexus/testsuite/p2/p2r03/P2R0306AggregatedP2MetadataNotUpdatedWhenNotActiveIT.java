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
package org.sonatype.nexus.testsuite.p2.p2r03;

import java.io.File;

import org.sonatype.nexus.testsuite.p2.AbstractNexusP2GeneratorIT;

import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.sonatype.sisu.litmus.testsupport.hamcrest.FileMatchers.exists;

public class P2R0306AggregatedP2MetadataNotUpdatedWhenNotActiveIT
    extends AbstractNexusP2GeneratorIT
{

  public P2R0306AggregatedP2MetadataNotUpdatedWhenNotActiveIT() {
    super("p2r03");
  }

  /**
   * [NXCM-4487]
   * When P2 Aggregator capability is not active, p2 metadata is not updated.
   */
  @Test
  public void test()
      throws Exception
  {
    final File artifactsXML = storageP2RepositoryArtifactsXML();
    final File contentXML = storageP2RepositoryContentXML();

    createP2RepositoryAggregatorCapability();
    createP2MetadataGeneratorCapability();

    assertThat("P2 artifacts.xml does exist", artifactsXML, exists());
    assertThat("P2 content.xml does exist", contentXML, exists());

    final long artifactsXMLTimestamp = artifactsXML.lastModified();
    final long contentXMLTimestamp = contentXML.lastModified();

    passivateP2RepositoryAggregatorCapability();

    deployArtifacts(getTestResourceAsFile("artifacts/jars"));

    assertThat("P2 artifacts.xml does exist", artifactsXML, exists());
    assertThat("P2 content.xml does exist", contentXML, exists());

    assertThat("P2 artifacts last modified", artifactsXML.lastModified(), is(equalTo(artifactsXMLTimestamp)));
    assertThat("P2 content last modified", contentXML.lastModified(), is(equalTo(contentXMLTimestamp)));
  }

}
