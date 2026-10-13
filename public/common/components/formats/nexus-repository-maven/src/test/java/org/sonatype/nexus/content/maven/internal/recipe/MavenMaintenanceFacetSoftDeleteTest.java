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
package org.sonatype.nexus.content.maven.internal.recipe;

import java.util.Optional;

import org.sonatype.nexus.content.maven.MavenContentFacet;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.content.Component;
import org.sonatype.nexus.repository.content.browse.BrowseFacet;
import org.sonatype.nexus.repository.content.browse.store.BrowseNodeData;
import org.sonatype.nexus.repository.content.facet.ContentFacet;
import org.sonatype.nexus.repository.content.fluent.FluentAssetBuilder;
import org.sonatype.nexus.repository.content.fluent.FluentAssets;
import org.sonatype.nexus.repository.maven.MavenMetadataRebuildFacet;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.Silent.class)
public class MavenMaintenanceFacetSoftDeleteTest
{
  private static final String GROUP = "org.example";

  private static final String ARTIFACT = "my-artifact";

  private static final String BASE_VERSION = "2.0.0-SNAPSHOT";

  @Mock
  private Repository repository;

  @Mock
  private ContentFacet contentFacet;

  @Mock
  private MavenContentFacet mavenContentFacet;

  @Mock
  private MavenMetadataRebuildFacet mavenMetadataRebuildFacet;

  @Mock
  private FluentAssets fluentAssets;

  @Mock
  private Component component;

  @Mock
  private org.sonatype.nexus.common.collect.NestedAttributesMap mavenAttributes;

  private MavenMaintenanceFacet underTest;

  @Before
  public void setUp() throws Exception {
    when(repository.facet(ContentFacet.class)).thenReturn(contentFacet);
    when(repository.facet(MavenContentFacet.class)).thenReturn(mavenContentFacet);
    when(repository.facet(MavenMetadataRebuildFacet.class)).thenReturn(mavenMetadataRebuildFacet);

    when(contentFacet.assets()).thenReturn(fluentAssets);

    when(component.namespace()).thenReturn(GROUP);
    when(component.name()).thenReturn(ARTIFACT);

    underTest = new MavenMaintenanceFacet();
    underTest.attach(repository);
  }

  @Test
  public void afterComponentSoftDeleted_deletesMetadataOrFlagsForRebuild() {
    underTest.afterComponentSoftDeleted(component);

    verify(mavenContentFacet).deleteMetadataOrFlagForRebuild(component);
  }

  @Test
  public void afterComponentRestored_rebuildsMetadata() {
    underTest.afterComponentRestored(component);

    verify(mavenMetadataRebuildFacet).rebuildMetadata(GROUP, ARTIFACT, null, false, false);
  }

  /**
   * The artifact-level metadata assets are hard-deleted on soft-delete, which leaves asset-less browse-node
   * rows holding the unique (repository_id, request_path) slot and blocks re-creation of the node for the
   * rebuilt asset. On restore we must free that slot for every artifact-level metadata path
   * (maven-metadata.xml plus its four checksums) before re-adding the browse nodes.
   */
  @Test
  public void afterComponentRestored_freesStaleMetadataBrowseNodeSlots() {
    BrowseFacet browseFacet = mock(BrowseFacet.class);
    when(repository.optionalFacet(BrowseFacet.class)).thenReturn(Optional.of(browseFacet));

    // No live metadata asset is looked up in this path; keep addPathsToAssets out of scope and focus on
    // the stale-slot freeing that is the crux of the fix.
    FluentAssetBuilder assetBuilder = mock(FluentAssetBuilder.class);
    when(mavenContentFacet.assets()).thenReturn(fluentAssets);
    when(fluentAssets.path(anyString())).thenReturn(assetBuilder);
    when(assetBuilder.find()).thenReturn(Optional.empty());

    // A stale/orphaned node exists at each artifact-level metadata request path.
    BrowseNodeData staleNode = mock(BrowseNodeData.class);
    when(staleNode.getNodeId()).thenReturn(42L);
    when(browseFacet.getByRequestPath(anyString())).thenReturn(Optional.of(staleNode));

    underTest.afterComponentRestored(component);

    verify(mavenMetadataRebuildFacet).rebuildMetadata(GROUP, ARTIFACT, null, false, false);
    // group-level (5) + GAV-level (5) => 10 request paths freed for a release (non-snapshot) component.
    verify(browseFacet, times(10)).getByRequestPath(anyString());
    verify(browseFacet, times(10)).deleteByNodeId(42L);
  }

  @Test
  public void afterComponentRestored_noBrowseFacet_isNoOp() {
    when(repository.optionalFacet(BrowseFacet.class)).thenReturn(Optional.empty());

    underTest.afterComponentRestored(component);

    verify(mavenMetadataRebuildFacet).rebuildMetadata(GROUP, ARTIFACT, null, false, false);
  }

  /**
   * For a SNAPSHOT there is a second, version-level {@code maven-metadata.xml} at
   * {@code {groupId}/{artifactId}/{baseVersion}/maven-metadata.xml}. Its browse node must also be freed and
   * recreated on component restore, otherwise the file is downloadable but invisible in Browse (the reported
   * "component-level metadata does not come back" symptom).
   */
  @Test
  public void afterComponentRestored_snapshot_freesVersionLevelMetadataBrowseNodeSlots() {
    when(component.attributes(anyString())).thenReturn(mavenAttributes);
    when(mavenAttributes.get("baseVersion", String.class)).thenReturn(BASE_VERSION);

    BrowseFacet browseFacet = mock(BrowseFacet.class);
    when(repository.optionalFacet(BrowseFacet.class)).thenReturn(Optional.of(browseFacet));

    FluentAssetBuilder assetBuilder = mock(FluentAssetBuilder.class);
    when(mavenContentFacet.assets()).thenReturn(fluentAssets);
    when(fluentAssets.path(anyString())).thenReturn(assetBuilder);
    when(assetBuilder.find()).thenReturn(Optional.empty());

    BrowseNodeData staleNode = mock(BrowseNodeData.class);
    when(staleNode.getNodeId()).thenReturn(42L);
    when(browseFacet.getByRequestPath(anyString())).thenReturn(Optional.of(staleNode));

    underTest.afterComponentRestored(component);

    // group-level (5) + GAV-level (5) + version-level (5) => 15 request paths freed for a snapshot component.
    verify(browseFacet, times(15)).getByRequestPath(anyString());
    verify(browseFacet, times(15)).deleteByNodeId(42L);
    // The version-level maven-metadata.xml path must be among those freed.
    verify(browseFacet).getByRequestPath("/org/example/my-artifact/2.0.0-SNAPSHOT/maven-metadata.xml");
  }

  /**
   * A plugin registers a third, group-level {@code maven-metadata.xml} at {@code {groupId}/maven-metadata.xml}
   * (the {@code <plugins>} listing, one directory above the artifact). Its browse node must also be freed and
   * recreated on component restore, otherwise the plugin-group metadata is downloadable but invisible in Browse
   * (the reported "root folder metadata not generating" symptom for a restored SNAPSHOT plugin).
   */
  @Test
  public void afterComponentRestored_freesGroupLevelMetadataBrowseNodeSlots() {
    BrowseFacet browseFacet = mock(BrowseFacet.class);
    when(repository.optionalFacet(BrowseFacet.class)).thenReturn(Optional.of(browseFacet));

    FluentAssetBuilder assetBuilder = mock(FluentAssetBuilder.class);
    when(mavenContentFacet.assets()).thenReturn(fluentAssets);
    when(fluentAssets.path(anyString())).thenReturn(assetBuilder);
    when(assetBuilder.find()).thenReturn(Optional.empty());

    BrowseNodeData staleNode = mock(BrowseNodeData.class);
    when(staleNode.getNodeId()).thenReturn(42L);
    when(browseFacet.getByRequestPath(anyString())).thenReturn(Optional.of(staleNode));

    underTest.afterComponentRestored(component);

    // The group-level maven-metadata.xml path (one directory above the artifact) must be freed.
    verify(browseFacet).getByRequestPath("/org/example/maven-metadata.xml");
  }
}
