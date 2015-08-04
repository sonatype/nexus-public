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

import java.io.File;
import java.io.FileInputStream;

import org.sonatype.nexus.proxy.ResourceStoreRequest;
import org.sonatype.nexus.proxy.maven.MavenHostedRepository;
import org.sonatype.nexus.proxy.maven.packaging.ArtifactPackagingMapper;
import org.sonatype.nexus.proxy.registry.RepositoryRegistry;

import org.apache.maven.index.ArtifactInfo;
import org.apache.maven.index.IteratorSearchResponse;
import org.apache.maven.index.SearchType;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

/**
 * Custom packaging test.
 *
 * @author cstamas
 */
public class Nexus5525IndexerManagerTest
    extends AbstractIndexerManagerTest
{
  @Test
  public void testDeployOfCustomPackaging()
      throws Exception
  {
    final File jarFile = getTestFile("src/test/resources/nexus-5525/bundle-1.0-20130318.131408-1.jar");
    final File pomFile = getTestFile("src/test/resources/nexus-5525/bundle-1.0-20130318.131408-1.pom");
    final MavenHostedRepository snapshots =
        lookup(RepositoryRegistry.class).getRepositoryWithFacet("snapshots", MavenHostedRepository.class);
    lookup(ArtifactPackagingMapper.class).setPropertiesFile(
        getTestFile("src/test/resources/nexus-5525/packaging2extension-mapping.properties"));

    // simulate Maven3 deploy: it happens JAR then POM
    snapshots.storeItem(
        new ResourceStoreRequest("/org/sonatype/nexus5525/bundle/1.0-SNAPSHOT/" + jarFile.getName()),
        new FileInputStream(jarFile), null);
    snapshots.storeItem(
        new ResourceStoreRequest("/org/sonatype/nexus5525/bundle/1.0-SNAPSHOT/" + pomFile.getName()),
        new FileInputStream(pomFile), null);

    wairForAsyncEventsToCalmDown();

    IteratorSearchResponse response = null;
    try {
      // check index
      response =
          lookup(IndexerManager.class).searchArtifactIterator("org.sonatype.nexus5525", "bundle", null,
              "bundle", null, snapshots.getId(), null, null, null, false, SearchType.EXACT, null);

      assertThat(response.getTotalHitsCount(), equalTo(1));
      final ArtifactInfo ai = response.getResults().next();

      assertThat(ai.packaging, equalTo("bundle"));
      assertThat(ai.fextension, equalTo("jar"));
    }
    finally {
      if (response != null) {
        response.close();
      }
    }
  }
}
