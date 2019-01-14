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
package org.sonatype.nexus.repository.repair;

import java.util.function.BiFunction;

import javax.annotation.Nullable;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.repository.Format;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.Type;
import org.sonatype.nexus.repository.manager.RepositoryManager;
import org.sonatype.nexus.repository.storage.Asset;
import org.sonatype.nexus.repository.storage.AssetEntityAdapter;
import org.sonatype.nexus.repository.storage.StorageTx;
import org.sonatype.nexus.repository.types.HostedType;
import org.sonatype.nexus.repository.types.ProxyType;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class RepairMetadataComponentTest
    extends TestSupport
{
  @Mock
  private RepositoryManager repositoryManager;

  @Mock
  private AssetEntityAdapter assetEntityAdapter;

  private Type type;

  private Format format;

  private Repository repository;

  private RepairMetadataComponentForTest underTest;

  class RepairMetadataComponentForTest
      extends RepairMetadataComponent
  {

    public int called;

    public RepairMetadataComponentForTest(final RepositoryManager repositoryManager,
                                          final AssetEntityAdapter assetEntityAdapter,
                                          final Type type,
                                          final Format format)
    {
      super(repositoryManager, assetEntityAdapter, type, format);
    }

    @Override
    protected void updateAsset(final Repository repository, final StorageTx tx, final Asset asset) { }
  }

  @Before
  public void setUp() throws Exception {
    type = new HostedType();
    format = new Format("value") { };

    repository = mock(Repository.class);
    when(repository.getFormat()).thenReturn(format);
    when(repository.getType()).thenReturn(type);
  }

  @Test(expected = UnsupportedOperationException.class)
  public void whenFormatAndTypeMatchShouldRepairRepository() {
    underTest = new RepairMetadataComponentForTest(repositoryManager, assetEntityAdapter, type, format)
    {
      @Override
      protected void doRepairRepository(final Repository repository) {
        throw new UnsupportedOperationException();
      }
    };

    underTest.repairRepository(repository);
  }

  @Test
  public void whenFormatDoesNotMatchShouldNotRepair() {
    underTest = new RepairMetadataComponentForTest(repositoryManager, assetEntityAdapter, type, format)
    {
      @Override
      protected void doRepairRepository(final Repository repository) {
        throw new UnsupportedOperationException();
      }
    };

    when(repository.getFormat()).thenReturn(new Format("wrong") { });

    underTest.repairRepository(repository);
  }

  @Test
  public void whenTypeDoesNotMatchShouldNotRepair() {
    underTest = new RepairMetadataComponentForTest(repositoryManager, assetEntityAdapter, type, format)
    {
      @Override
      protected void doRepairRepository(final Repository repository) {
        throw new UnsupportedOperationException();
      }
    };

    when(repository.getType()).thenReturn(new ProxyType());

    underTest.repairRepository(repository);
  }

  @Test(expected = UnsupportedOperationException.class)
  public void whenRepairingBeforeCalledOnce() {
    underTest = new RepairMetadataComponentForTest(repositoryManager, assetEntityAdapter, type, format)
    {
      @Override
      protected void beforeRepair(final Repository repository) {
        throw new UnsupportedOperationException();
      }
    };

    underTest.repairRepository(repository);
  }


  @Test(expected = UnsupportedOperationException.class)
  public void whenRepairingAfterCalledOnce() {
    underTest = new RepairMetadataComponentForTest(repositoryManager, assetEntityAdapter, type, format)
    {
      @Override
      protected void afterRepair(final Repository repository) {
        throw new UnsupportedOperationException();
      }

      @Nullable
      @Override
      protected String processBatchWith(final Repository repository,
                                        final String lastId,
                                        final BiFunction<Repository, Iterable<Asset>, String> function) throws Exception
      {
        return null;
      }
    };

    underTest.repairRepository(repository);
  }

  @Test
  public void processBatchIteratesUntilNullReturned() {
    int timesToIterate = 2;

    underTest = new RepairMetadataComponentForTest(repositoryManager, assetEntityAdapter, type, format)
    {
      private int iterated = 0;

      @Nullable
      @Override
      protected String processBatchWith(final Repository repository,
                                        final String lastId,
                                        final BiFunction<Repository, Iterable<Asset>, String> function) throws Exception
      {
        this.called++;
        if (iterated++ == timesToIterate) {
          return null;
        }
        else {
          return "";
        }
      }
    };

    underTest.doRepairRepository(repository);

    assertThat(underTest.called, is(3));
  }
}
