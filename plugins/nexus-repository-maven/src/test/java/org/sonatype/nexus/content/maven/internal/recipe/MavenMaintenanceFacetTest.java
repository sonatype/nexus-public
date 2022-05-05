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

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.content.maven.MavenContentFacet;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.content.Asset;
import org.sonatype.nexus.repository.content.Component;
import org.sonatype.nexus.repository.content.facet.ContentFacet;
import org.sonatype.nexus.repository.content.fluent.FluentAsset;
import org.sonatype.nexus.repository.content.fluent.FluentAssets;
import org.sonatype.nexus.repository.content.fluent.FluentComponent;
import org.sonatype.nexus.repository.content.fluent.FluentComponents;
import org.sonatype.nexus.repository.maven.MavenPath;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static com.google.common.collect.Lists.newArrayList;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class MavenMaintenanceFacetTest
    extends TestSupport
{
  @Mock
  private Repository repository;

  @Mock
  private ContentFacet contentFacet;

  @Mock
  private MavenContentFacet mavenContentFacet;

  @Mock
  private FluentAssets fluentAssets;

  @Mock
  private FluentAsset fluentAsset;

  @Mock
  private Asset asset;

  @Mock
  private FluentComponents fluentComponents;

  @Mock
  private FluentComponent fluentComponent;

  @Mock
  private Component component;

  private MavenMaintenanceFacet underTest;

  @Before
  public void setUp() throws Exception {
    when(repository.facet(ContentFacet.class)).thenReturn(contentFacet);
    when(repository.facet(MavenContentFacet.class)).thenReturn(mavenContentFacet);

    when(contentFacet.assets()).thenReturn(fluentAssets);
    when(contentFacet.components()).thenReturn(fluentComponents);

    when(fluentAssets.with(fluentAsset)).thenReturn(fluentAsset);
    when(fluentAssets.with(asset)).thenReturn(fluentAsset);

    when(fluentComponents.with(component)).thenReturn(fluentComponent);
    when(fluentComponents.with(fluentComponent)).thenReturn(fluentComponent);

    when(mavenContentFacet.delete(any(MavenPath.class))).thenReturn(true);

    underTest = new MavenMaintenanceFacet();
    underTest.attach(repository);
  }

  @Test
  public void deleteAsset_should_deleteTheAsset() {
    when(asset.component()).thenReturn(empty());

    underTest.deleteAsset(asset);

    verify(fluentAsset).delete();
  }

  @Test
  public void deleteAsset_should_deleteAssociatedComponentIfNoAssetsLeft() {
    when(asset.component()).thenReturn(of(component));
    when(fluentComponent.assets()).thenReturn(newArrayList(fluentAsset));

    underTest.deleteAsset(asset);

    verify(fluentComponent).delete();
    verify(fluentAsset).delete();
  }

  @Test
  public void deleteAsset_should_notDeleteAssociatedComponentWhenOtherAssetsArePresent() {
    FluentAsset otherFluentAsset = mock(FluentAsset.class);
    when(asset.component()).thenReturn(of(component));
    when(fluentComponent.assets()).thenReturn(newArrayList(fluentAsset, otherFluentAsset));

    underTest.deleteAsset(asset);

    verify(fluentAsset).delete();
    verify(otherFluentAsset, times(0)).delete();
    verify(fluentComponent, times(0)).delete();
    verify(mavenContentFacet, times(0)).deleteMetadataOrFlagForRebuild(component);
    verify(mavenContentFacet, times(0)).deleteMetadataOrFlagForRebuild(fluentComponent);
  }

  @Test
  public void deleteComponent_should_deleteAllAssociatedAssets() {
    FluentAsset otherFluentAsset = mock(FluentAsset.class);
    when(asset.component()).thenReturn(of(component));
    when(fluentComponent.assets()).thenReturn(newArrayList(fluentAsset, otherFluentAsset));

    underTest.deleteComponent(component);

    verify(fluentComponent).delete();
    verify(fluentAsset).delete();
    verify(otherFluentAsset).delete();
  }

  @Test
  public void deleteComponent_should_deleteMetadataOrFlagForRebuild() {
    when(fluentComponent.assets()).thenReturn(newArrayList());

    underTest.deleteComponent(component);

    verify(fluentComponent).delete();
    verify(mavenContentFacet).deleteMetadataOrFlagForRebuild(component);
  }
}
