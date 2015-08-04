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
package org.sonatype.nexus.plugins.p2.repository;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

import org.sonatype.nexus.plugins.p2.repository.metadata.Artifacts;
import org.sonatype.nexus.plugins.p2.repository.metadata.ArtifactsMerge;
import org.sonatype.nexus.plugins.p2.repository.metadata.Content;
import org.sonatype.nexus.plugins.p2.repository.metadata.P2MetadataMergeException;
import org.sonatype.nexus.proxy.item.StorageFileItem;

import org.codehaus.plexus.util.xml.XmlStreamReader;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.codehaus.plexus.util.xml.Xpp3DomBuilder;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Mockito;

public class MetadataMergeTest
{
  @Test
  public void mergeArtifactsMetadata()
      throws Exception
  {
    final ArrayList<Artifacts> repos =
        loadArtifactsMetadata(new String[]{"metadata/merge/artifact1.xml", "metadata/merge/artifact2.xml"});

    final StorageFileItem item1 = Mockito.mock(StorageFileItem.class);
    Mockito.when(item1.getName()).thenReturn("artifact.xml");
    Mockito.when(item1.getInputStream()).thenReturn(
        new FileInputStream(new File("src/test/resources", "metadata/merge/artifact1.xml")));

    final StorageFileItem item2 = Mockito.mock(StorageFileItem.class);
    Mockito.when(item2.getName()).thenReturn("artifact.xml");
    Mockito.when(item2.getInputStream()).thenReturn(
        new FileInputStream(new File("src/test/resources", "metadata/merge/artifact2.xml")));

    // sanity check
    Assert.assertEquals(2, repos.get(0).getArtifacts().size());
    Assert.assertEquals(1, repos.get(1).getArtifacts().size());

    final ArtifactsMerge m = new ArtifactsMerge();

    final Artifacts merged = m.mergeArtifactsMetadata("test", Arrays.asList(item1, item2));

    Assert.assertEquals(3, merged.getArtifacts().size());
    Assert.assertEquals(5, merged.getMappings().size());
  }

  @Test
  @Ignore
  public void incompatibleRepositoryProperties()
      throws Exception
  {
    final ArrayList<Artifacts> repos =
        loadArtifactsMetadata(new String[]{"metadata/merge/artifact1.xml", "metadata/merge/artifact2props.xml"});

    final StorageFileItem item1 = Mockito.mock(StorageFileItem.class);
    Mockito.when(item1.getName()).thenReturn("artifact.xml");
    Mockito.when(item1.getInputStream()).thenReturn(
        new FileInputStream(new File("src/test/resources", "metadata/merge/artifact1.xml")));

    final StorageFileItem item2 = Mockito.mock(StorageFileItem.class);
    Mockito.when(item2.getName()).thenReturn("artifact.xml");
    Mockito.when(item2.getInputStream()).thenReturn(
        new FileInputStream(new File("src/test/resources", "metadata/merge/artifact2props.xml")));

    // sanity check
    Assert.assertEquals(2, repos.get(0).getArtifacts().size());
    Assert.assertEquals(1, repos.get(1).getArtifacts().size());

    final ArtifactsMerge m = new ArtifactsMerge();

    try {
      m.mergeArtifactsMetadata("test", Arrays.asList(item1, item2));
      Assert.fail("RepositoryMetadataMergeException expected");
    }
    catch (final P2MetadataMergeException e) {
      // expected
    }
  }

  @Test
  public void incompatibleMappingsRules()
      throws Exception
  {
    final ArrayList<Artifacts> repos =
        loadArtifactsMetadata(new String[]{"metadata/merge/artifact1.xml", "metadata/merge/artifact2mappins.xml"});

    final StorageFileItem item1 = Mockito.mock(StorageFileItem.class);
    Mockito.when(item1.getName()).thenReturn("artifact.xml");
    Mockito.when(item1.getInputStream()).thenReturn(
        new FileInputStream(new File("src/test/resources", "metadata/merge/artifact1.xml")));

    final StorageFileItem item2 = Mockito.mock(StorageFileItem.class);
    Mockito.when(item2.getName()).thenReturn("artifact.xml");
    Mockito.when(item2.getInputStream()).thenReturn(
        new FileInputStream(new File("src/test/resources", "metadata/merge/artifact2mappins.xml")));

    // sanity check
    Assert.assertEquals(2, repos.get(0).getArtifacts().size());
    Assert.assertEquals(1, repos.get(1).getArtifacts().size());

    final ArtifactsMerge m = new ArtifactsMerge();

    try {
      m.mergeArtifactsMetadata("test", Arrays.asList(item1, item2));
      Assert.fail("P2MetadataMergeException expected");
    }
    catch (final P2MetadataMergeException e) {
      if (!e.getMessage().startsWith("Incompatible artifact repository mapping rules: filter=")) {
        throw e;
      }
    }
  }

  @Test
  public void mergeContentMetadata()
      throws Exception
  {
    final StorageFileItem item1 = Mockito.mock(StorageFileItem.class);
    Mockito.when(item1.getName()).thenReturn("content.xml");
    Mockito.when(item1.getInputStream()).thenReturn(
        new FileInputStream(new File("src/test/resources", "metadata/merge/content1.xml")));

    final StorageFileItem item2 = Mockito.mock(StorageFileItem.class);
    Mockito.when(item2.getName()).thenReturn("content.xml");
    Mockito.when(item2.getInputStream()).thenReturn(
        new FileInputStream(new File("src/test/resources", "metadata/merge/content2.xml")));

    final ArtifactsMerge m = new ArtifactsMerge();

    final Content merged = m.mergeContentMetadata("test", Arrays.asList(item1, item2));

    // repo1: bundle, featureJar, featureGroup, jre, jreConfig
    // repo2: feature2Jar, feature2Group (jre and jreConfig ignored)
    Assert.assertEquals(7, merged.getUnits().size());
  }

  private ArrayList<Artifacts> loadArtifactsMetadata(final String[] files)
      throws IOException, XmlPullParserException
  {
    final ArrayList<Artifacts> repos = new ArrayList<Artifacts>();
    for (final String file : files) {
      repos.add(new Artifacts(loadXpp3Dom(file)));
    }
    return repos;
  }

  private Xpp3Dom loadXpp3Dom(final String filepath)
      throws IOException, XmlPullParserException
  {
    try (FileInputStream is = new FileInputStream(new File("src/test/resources", filepath))) {
      return Xpp3DomBuilder.build(new XmlStreamReader(is));
    }
  }
}
