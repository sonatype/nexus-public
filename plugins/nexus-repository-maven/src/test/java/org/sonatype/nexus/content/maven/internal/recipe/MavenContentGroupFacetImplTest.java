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

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.content.maven.MavenContentFacet;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.Type;
import org.sonatype.nexus.repository.cache.RepositoryCacheInvalidationService;
import org.sonatype.nexus.repository.content.Asset;
import org.sonatype.nexus.repository.content.event.asset.AssetUploadedEvent;
import org.sonatype.nexus.repository.content.facet.ContentFacet;
import org.sonatype.nexus.repository.content.fluent.FluentAssetBuilder;
import org.sonatype.nexus.repository.content.fluent.FluentAssets;
import org.sonatype.nexus.repository.manager.RepositoryManager;
import org.sonatype.nexus.repository.maven.internal.Maven2MavenPathParser;
import org.sonatype.nexus.validation.ConstraintViolationFactory;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class MavenContentGroupFacetImplTest
    extends TestSupport
{
  private MavenContentGroupFacetImpl underTest;

  @Mock
  private RepositoryManager repositoryManager;

  @Mock
  private ConstraintViolationFactory constraintViolationFactory;

  @Mock
  private Type groupType;

  @Mock
  private RepositoryCacheInvalidationService repositoryCacheInvalidationService;

  @Before
  public void setup() {
    underTest = spy(new MavenContentGroupFacetImpl(repositoryManager, constraintViolationFactory, groupType,
        repositoryCacheInvalidationService));
  }

  /**
   * Ensure that the path used to find assets in the handler uses a path that is
   * prefixed with a "/"
   */
  @Test
  public void testHandleAssetEvent_path_find_with_slash() throws Exception {
    FluentAssetBuilder assetBuilder = mock(FluentAssetBuilder.class);
    when(assetBuilder.find()).thenReturn(Optional.empty());
    FluentAssets assets = mock(FluentAssets.class);
    when(assets.path(any())).thenReturn(assetBuilder);
    MavenContentFacet contentFacet = mock(MavenContentFacet.class);
    when(contentFacet.getMavenPathParser()).thenReturn(new Maven2MavenPathParser());
    when(contentFacet.assets()).thenReturn(assets);
    Repository repository = mock(Repository.class);
    when(repository.getName()).thenReturn("repo1");
    when(repository.facet(MavenContentFacet.class)).thenReturn(contentFacet);
    when(repository.facet(ContentFacet.class)).thenReturn(contentFacet);
    doReturn(true).when(underTest).member(repository);
    underTest.attach(repository);
    Asset asset = mock(Asset.class);
    when(asset.path()).thenReturn("/com/example/foo/1.0-SNAPSHOT/maven-metadata.xml");
    when(asset.component()).thenReturn(Optional.empty());
    AssetUploadedEvent event = mock(AssetUploadedEvent.class);
    when(event.getAsset()).thenReturn(asset);
    when(event.getRepository()).thenReturn(Optional.of(repository));
    underTest.onAssetUploadedEvent(event);
    verify(assets).path("/com/example/foo/1.0-SNAPSHOT/maven-metadata.xml");
  }
}
