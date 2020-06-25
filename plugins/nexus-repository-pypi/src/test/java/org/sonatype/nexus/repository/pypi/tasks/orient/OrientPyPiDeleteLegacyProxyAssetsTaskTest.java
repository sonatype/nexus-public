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
package org.sonatype.nexus.repository.pypi.tasks.orient;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.common.app.ApplicationDirectories;
import org.sonatype.nexus.repository.Format;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.Type;
import org.sonatype.nexus.repository.manager.RepositoryManager;
import org.sonatype.nexus.repository.pypi.internal.PyPiFormat;
import org.sonatype.nexus.repository.pypi.tasks.orient.OrientPyPiDeleteLegacyProxyAssetsTask;
import org.sonatype.nexus.repository.storage.Asset;
import org.sonatype.nexus.repository.storage.Bucket;
import org.sonatype.nexus.repository.storage.StorageFacet;
import org.sonatype.nexus.repository.storage.StorageTx;
import org.sonatype.nexus.repository.types.HostedType;
import org.sonatype.nexus.repository.types.ProxyType;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class OrientPyPiDeleteLegacyProxyAssetsTaskTest
    extends TestSupport
{
  @Mock
  private ApplicationDirectories directories;

  @Mock
  private RepositoryManager repositoryManager;

  private Repository pyPiProxy;

  private Repository pyPiHosted;

  private Repository otherProxy;

  private OrientPyPiDeleteLegacyProxyAssetsTask underTest;

  @Before
  public void setup() {
    underTest = new OrientPyPiDeleteLegacyProxyAssetsTask(directories, repositoryManager);

    pyPiProxy =
        mockRepository(new PyPiFormat(), new ProxyType(), mockAsset("simple/"), mockAsset("simple/foo"),
            mockAsset("packages/00/01/02/foo-123.whl"), mockAsset("packages/foo/123/foo-123.whl"));
    pyPiHosted = mockRepository(new PyPiFormat(), new HostedType());
    otherProxy = mockRepository(mock(Format.class), new ProxyType());

    when(repositoryManager.browse()).thenReturn(Arrays.asList(pyPiProxy, pyPiHosted, otherProxy));
  }

  @Test
  public void testCall() throws Exception {
    underTest.call();
    verify(otherProxy, never()).facet(StorageFacet.class);
    verify(pyPiHosted, never()).facet(StorageFacet.class);

    verify(pyPiProxy, times(1)).facet(StorageFacet.class);

    StorageTx tx = pyPiProxy.facet(StorageFacet.class).txSupplier().get();
    verify(tx).begin();

    ArgumentCaptor<Asset> assetCaptor = ArgumentCaptor.forClass(Asset.class);

    verify(tx, times(2)).deleteAsset(assetCaptor.capture());

    List<String> assetNames = assetCaptor.getAllValues().stream().map(Asset::name).collect(Collectors.toList());
    assertThat(assetNames, containsInAnyOrder("simple/foo", "packages/00/01/02/foo-123.whl"));

    verify(tx).commit();
  }

  private Asset mockAsset(final String name) {
    Asset asset = mock(Asset.class);
    when(asset.name()).thenReturn(name);
    return asset;
  }

  private Repository mockRepository(final Format format, final Type type, final Asset... assets) {
    Repository repository = mock(Repository.class);
    when(repository.getFormat()).thenReturn(format);
    when(repository.getType()).thenReturn(type);

    StorageFacet facet = mock(StorageFacet.class);
    when(repository.facet(StorageFacet.class)).thenReturn(facet);

    StorageTx storageTx = mock(StorageTx.class);
    when(facet.txSupplier()).thenReturn(() -> storageTx);

    when(storageTx.browseAssets(any(Bucket.class))).thenReturn(Arrays.asList(assets));

    return repository;
  }
}
