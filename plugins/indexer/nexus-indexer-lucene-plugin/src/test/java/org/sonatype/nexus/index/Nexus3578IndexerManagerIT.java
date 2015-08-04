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
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import org.sonatype.nexus.mime.MimeSupport;
import org.sonatype.nexus.proxy.ResourceStoreRequest;
import org.sonatype.nexus.proxy.item.DefaultStorageFileItem;
import org.sonatype.nexus.proxy.item.FileContentLocator;
import org.sonatype.nexus.proxy.item.StorageFileItem;

import org.apache.maven.index.ArtifactInfo;
import org.apache.maven.index.IteratorSearchResponse;
import org.apache.maven.index.SearchType;
import org.apache.maven.index.context.DefaultIndexingContext;
import org.apache.maven.index.context.IndexCreator;
import org.apache.maven.index.creator.JarFileContentsIndexCreator;
import org.apache.maven.index.creator.MavenArchetypeArtifactInfoIndexCreator;
import org.apache.maven.index.creator.MavenPluginArtifactInfoIndexCreator;
import org.apache.maven.index.creator.MinimalArtifactInfoIndexCreator;
import org.junit.Test;

// This is an IT just because it runs longer then 15 seconds
public class Nexus3578IndexerManagerIT
    extends AbstractIndexerManagerTest
{

  protected File pomFile;

  protected String pomPath;

  protected File jarFile;

  protected String jarPath;

  protected MimeSupport mimeSupport;

  @Override
  protected void setUp()
      throws Exception
  {
    super.setUp();

    hackContext((DefaultIndexingContext) ((DefaultIndexerManager) indexerManager).getRepositoryIndexContext(
        snapshots.getId()));

    this.mimeSupport = lookup(MimeSupport.class);

    pomFile = getTestFile("src/test/resources/nexus-3578/maven-pmd-plugin-2.6-20100607.233625-29.pom");

    pomPath = "/org/apache/maven/plugins/maven-pmd-plugin/2.6-SNAPSHOT/maven-pmd-plugin-2.6-20100607.233625-29.pom";

    jarFile = getTestFile("src/test/resources/nexus-3578/maven-pmd-plugin-2.6-20100607.233625-29.jar");

    jarPath = "/org/apache/maven/plugins/maven-pmd-plugin/2.6-SNAPSHOT/maven-pmd-plugin-2.6-20100607.233625-29.jar";
  }

  // this one fails (see NEXUS-3578)
  @Test
  public void testSnapshotJarPomOrder()
      throws Exception
  {
    fillInRepo();

    waitForTasksToStop();

    sneakyDeployAFile(jarPath, jarFile);

    sneakyDeployAFile(pomPath, pomFile);

    // validate
    validate();
  }

  // this one works (see NEXUS-3578)
  @Test
  public void testSnapshotPomJarOrder()
      throws Exception
  {
    fillInRepo();

    waitForTasksToStop();

    sneakyDeployAFile(pomPath, pomFile);

    sneakyDeployAFile(jarPath, jarFile);

    // validate
    validate();
  }

  // ==

  /**
   * "Sneaky" deploys a file, by doing it with "low level" interaction with storage, thus avoiding a repo to emit any
   * event about this and having ourselves manage the indexing to be able to control it.
   */
  protected void sneakyDeployAFile(String path, File file)
      throws Exception
  {
    ResourceStoreRequest request = new ResourceStoreRequest(path);

    FileContentLocator fc = new FileContentLocator(file, mimeSupport.guessMimeTypeFromPath(file.getName()));

    StorageFileItem item = new DefaultStorageFileItem(snapshots, request, true, true, fc);

    // deploy jar to storage
    snapshots.getLocalStorage().storeItem(snapshots, item);

    item = (StorageFileItem) snapshots.retrieveItem(request);

    // deploy jar to index
    indexerManager.addItemToIndex(snapshots, item);
  }

  /**
   * Uses the JARs checksum and GAVs to validate index content being updated.
   */
  protected void validate()
      throws Exception
  {
    // this will be EXACT search, since we gave full SHA1 checksum of 40 chars
    // BUT because of another bug https://issues.sonatype.org/browse/NEXUS-3580
    // this search wont work in this case
        /*
         * IteratorSearchResponse response = indexerManager.searchArtifactSha1ChecksumIterator(
         * "a216468fbebacabdf941ab5f1b2e4f3484103f1b", null, null, null, null, null );
         */
    IteratorSearchResponse response =
        indexerManager.searchArtifactIterator("org.apache.maven.plugins", "maven-pmd-plugin", "2.6-SNAPSHOT",
            "maven-plugin", null, snapshots.getId(), null, null, null, false,
            SearchType.EXACT, null);

    assertEquals("There should be one hit!", 1, response.getTotalHits());

    ArtifactInfo ai = response.getResults().next();

    assertEquals("Coordinates should match too!",
        "org.apache.maven.plugins:maven-pmd-plugin:2.6-SNAPSHOT:null:maven-plugin", ai.toString());
  }

  protected void hackContext(DefaultIndexingContext context)
      throws Exception
  {
    List<IndexCreator> creators = new ArrayList<IndexCreator>();

    IndexCreator min = lookup(IndexCreator.class, MinimalArtifactInfoIndexCreator.ID);
    IndexCreator mavenPlugin = lookup(IndexCreator.class, MavenPluginArtifactInfoIndexCreator.ID);
    IndexCreator mavenArchetype = lookup(IndexCreator.class, MavenArchetypeArtifactInfoIndexCreator.ID);
    IndexCreator jar = lookup(IndexCreator.class, JarFileContentsIndexCreator.ID);

    creators.add(min);
    creators.add(mavenPlugin);
    creators.add(mavenArchetype);
    creators.add(jar);

    Field indexCreatorsField = getIndexCreatorsField(context);

    if (indexCreatorsField != null) {
      indexCreatorsField.setAccessible(true);
      indexCreatorsField.set(context, creators);
    }
  }

  private Field getIndexCreatorsField(DefaultIndexingContext context)
      throws NoSuchFieldException
  {
    Class<?> type = context.getClass();
    do {
      try {
        return type.getDeclaredField("indexCreators");
      }
      catch (NoSuchFieldException e) {
        type = type.getSuperclass();
      }
    }
    while (type != null);
    throw new NoSuchFieldException();
  }
}
