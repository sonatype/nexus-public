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
package org.sonatype.nexus.coreui.internal.content;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.common.collect.NestedAttributesMap;
import org.sonatype.nexus.coreui.AssetXO;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.content.Asset;
import org.sonatype.nexus.repository.content.AssetBlob;
import org.sonatype.nexus.repository.content.fluent.internal.FluentAssetImpl;
import org.sonatype.nexus.repository.content.maintenance.MaintenanceService;
import org.sonatype.nexus.repository.content.search.ComponentFinder;
import org.sonatype.nexus.repository.content.security.AssetPermissionChecker;
import org.sonatype.nexus.repository.content.store.AssetData;
import org.sonatype.nexus.repository.manager.RepositoryManager;
import org.sonatype.nexus.repository.types.HostedType;
import org.sonatype.nexus.repository.types.ProxyType;
import org.sonatype.nexus.selector.SelectorFactory;

import org.junit.Test;
import org.mockito.Mock;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ContentComponentHelperTest
    extends TestSupport
{
  @Mock
  MaintenanceService maintenanceService;

  @Mock
  Map<String, ComponentFinder> componentFinders;

  @Mock
  ComponentFinder componentFinder;

  @Mock
  AssetPermissionChecker assetPermissionChecker;

  @Mock
  SelectorFactory selectorFactory;

  @Mock
  RepositoryManager repositoryManager;

  @Mock
  NestedAttributesMap assetAttributes;

  @Mock
  AssetBlob blob;

  @Mock
  Repository repository;

  @Test
  public void toAssetXOTestHosted() {

    when(repositoryManager.get("maven-hosted")).thenReturn(repository);
    when(repository.getType()).thenReturn(new HostedType());
    when(componentFinders.get("default")).thenReturn(componentFinder);

    ContentComponentHelper underTest = new ContentComponentHelper(
        maintenanceService,
        componentFinders,
        assetPermissionChecker,
        selectorFactory,
        repositoryManager
    );

    AssetXO assetXO = underTest.toAssetXO(
        "maven-hosted",
        "maven-hosted",
        "maven2",
        createAsset()
    );
    assertThat(((Map) assetXO.getAttributes().get("content")).containsKey("last_modified"), is(false));
  }

  @Test
  public void toAssetXOTestProxy() {

    when(repositoryManager.get("maven-hosted")).thenReturn(repository);
    when(repository.getType()).thenReturn(new ProxyType());
    when(componentFinders.get("default")).thenReturn(componentFinder);

    ContentComponentHelper underTest = new ContentComponentHelper(
        maintenanceService,
        componentFinders,
        assetPermissionChecker,
        selectorFactory,
        repositoryManager
    );

    AssetXO assetXO = underTest.toAssetXO(
        "maven-hosted",
        "maven-hosted",
        "maven2",
        createAsset()
    );
    Map<String, Object> contentMap =(Map<String, Object>) assetXO.getAttributes().get("content");
    assertThat(contentMap.containsKey("last_modified"), is(true));
    assertThat(contentMap.get("last_modified"), is("2023-11-13T16:00:20.450+02:00"));
  }

  private Asset createAsset() {

    FluentAssetImpl asset = mock(FluentAssetImpl.class);
    when(asset.path()).thenReturn("/org/apache/logging/log4j/log4j-core/maven-metadata.xml");
    Map<String, String> contentMap = new HashMap<>();
    contentMap.put("last_modified", "2023-11-13T16:00:20.450+02:00");
    Map<String, Object> backingMap = new HashMap<>();
    backingMap.put("content", contentMap);
    when(assetAttributes.backing()).thenReturn(backingMap);
    when(asset.attributes()).thenReturn(assetAttributes);
    when(asset.kind()).thenReturn("REPOSITORY_METADATA");
    OffsetDateTime blobCreated = OffsetDateTime.now();
    when(blob.blobCreated()).thenReturn(blobCreated);
    OffsetDateTime assetCreated = OffsetDateTime.now();
    when(asset.created()).thenReturn(assetCreated);
    AssetData assetData = new AssetData();
    assetData.setAssetId(1);
    when(asset.unwrap()).thenReturn(assetData);
    return asset;
  }
}
