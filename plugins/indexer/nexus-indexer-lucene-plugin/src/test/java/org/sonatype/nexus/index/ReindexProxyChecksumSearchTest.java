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
package org.sonatype.nexus.index;

import java.util.Collection;

import org.sonatype.nexus.proxy.ResourceStoreRequest;

import org.apache.maven.index.ArtifactInfo;
import org.apache.maven.index.MAVEN;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;

/**
 * UT for NEXUS-6246: Checksum search fails after repair index task is run
 */
public class ReindexProxyChecksumSearchTest
    extends AbstractIndexerManagerTest
{
  @Test
  public void testProxyReindex()
      throws Exception
  {
    // just fetch it to have index "primed" with it's SHA1
    central.retrieveItem(new ResourceStoreRequest("/log4j/log4j/1.2.12/log4j-1.2.12.jar"));
    wairForAsyncEventsToCalmDown(); // indexer is async event subscriber

    // now search for it
    Collection<ArtifactInfo> candidates = indexerManager
        .identifyArtifact(MAVEN.SHA1, "057b8740427ee6d7b0b60792751356cad17dc0d9");
    assertThat(candidates, notNullValue());
    assertThat(candidates, hasSize(1));
    assertThat(candidates.iterator().next().groupId, equalTo("log4j"));
    assertThat(candidates.iterator().next().artifactId, equalTo("log4j"));
    assertThat(candidates.iterator().next().version, equalTo("1.2.12"));

    indexerManager.reindexRepository("/", central.getId(), true);

    // now search for it again
    candidates = indexerManager
        .identifyArtifact(MAVEN.SHA1, "057b8740427ee6d7b0b60792751356cad17dc0d9");
    assertThat(candidates, notNullValue());
    assertThat(candidates, hasSize(1));
    assertThat(candidates.iterator().next().groupId, equalTo("log4j"));
    assertThat(candidates.iterator().next().artifactId, equalTo("log4j"));
    assertThat(candidates.iterator().next().version, equalTo("1.2.12"));
  }
}
