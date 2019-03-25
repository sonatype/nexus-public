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
package org.sonatype.nexus.repository.npm.repair;

import java.util.ArrayList;
import java.util.List;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.repository.Format;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.Type;
import org.sonatype.nexus.repository.manager.RepositoryManager;
import org.sonatype.nexus.repository.npm.internal.NpmFormat;
import org.sonatype.nexus.repository.npm.internal.NpmPackageParser;
import org.sonatype.nexus.repository.storage.AssetEntityAdapter;
import org.sonatype.nexus.repository.storage.StorageFacet;
import org.sonatype.nexus.repository.storage.StorageTx;
import org.sonatype.nexus.repository.types.GroupType;
import org.sonatype.nexus.repository.types.HostedType;
import org.sonatype.nexus.repository.types.ProxyType;

import com.google.common.collect.ImmutableList;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static java.util.Collections.emptyList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsCollectionContaining.hasItem;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

public class NpmRepairPackageRootComponentTest
    extends TestSupport
{
  @Mock
  private RepositoryManager repositoryManager;

  @Mock
  private AssetEntityAdapter assetEntityAdapter;

  @Mock
  private NpmPackageParser npmPackageParser;

  @Mock
  private Repository npmHosted, npmProxy, npmGroup, nonNpmFormat;

  @Mock
  private StorageFacet storageFacet;

  @Mock
  private StorageTx tx;

  @Before
  public void setup() throws Exception {
    initialiseRepository(npmHosted, new HostedType(), new NpmFormat());
    initialiseRepository(npmProxy, new ProxyType(), new NpmFormat());
    initialiseRepository(npmGroup, new GroupType(), new NpmFormat());
    initialiseRepository(nonNpmFormat, new HostedType(), new Format("non-npm") { });
  }

  @Test
  public void onlyRepairNpmHostedRepositories() {
    when(repositoryManager.browse()).thenReturn(ImmutableList.of(npmHosted, npmProxy, npmGroup, nonNpmFormat));

    List<Repository> repairedRepositories = new ArrayList<>();

    NpmRepairPackageRootComponent repairComponent = new NpmRepairPackageRootComponent(repositoryManager,
        assetEntityAdapter, npmPackageParser, new HostedType(), new NpmFormat())
    {
      @Override
      protected void beforeRepair(final Repository repository) {
        repairedRepositories.add(repository);
      }
    };

    repairComponent.repair();

    assertThat(repairedRepositories, is(equalTo(ImmutableList.of(npmHosted))));
  }

  private void initialiseRepository(final Repository repository,
                                    final Type type,
                                    final Format format)
  {
    when(repository.getType()).thenReturn(type);
    when(repository.getFormat()).thenReturn(format);
    when(repository.facet(StorageFacet.class)).thenReturn(storageFacet);
    when(storageFacet.txSupplier()).thenReturn(() -> tx);
    when(tx.findAssets(any(), any(), any(), any())).thenReturn(emptyList());
  }
}
