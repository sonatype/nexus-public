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

import java.util.Map;
import java.util.function.BiFunction;

import javax.annotation.Nullable;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.repository.Format;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.Type;
import org.sonatype.nexus.repository.manager.RepositoryManager;
import org.sonatype.nexus.repository.storage.Asset;
import org.sonatype.nexus.repository.storage.AssetEntityAdapter;
import org.sonatype.nexus.repository.storage.StorageFacet;
import org.sonatype.nexus.repository.storage.StorageTx;
import org.sonatype.nexus.repository.transaction.TransactionalStoreMetadata;
import org.sonatype.nexus.transaction.UnitOfWork;

import com.google.common.collect.ImmutableMap;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Throwables.propagateIfPossible;
import static java.util.Collections.singletonList;

/**
 * Implementations of {@link BaseRestoreBlobStrategy} run a post processing method where implementations
 * of this class should be called if any post processing is to be done on the regenerated {@link Asset}
 *
 * @since 3.15
 */
public abstract class RepairMetadataComponent
    extends ComponentSupport
{
  private static final String BEGINNING_ID = "#-1:-1";

  private static final String ASSETS_WHERE = "@rid > :rid";

  private static final String ASSETS_SUFFIX = "ORDER BY @rid LIMIT :limit";

  private static final int BATCH_SIZE = 100;

  protected final RepositoryManager repositoryManager;

  protected AssetEntityAdapter assetEntityAdapter;

  protected final Type type;

  protected final Format format;

  public RepairMetadataComponent(final RepositoryManager repositoryManager,
                                 final AssetEntityAdapter assetEntityAdapter,
                                 final Type type,
                                 final Format format)
  {
    this.repositoryManager = checkNotNull(repositoryManager);
    this.assetEntityAdapter = checkNotNull(assetEntityAdapter);
    this.type = checkNotNull(type);
    this.format = checkNotNull(format);
  }

  public void repairRepository(final Repository repository) {
    if (shouldRepairRepository(repository)) {
      doRepairRepository(repository);
    }
    else {
      log.info("Not running post processing after repairing {} repository {}", repository.getFormat(),
          repository.getName());
    }
  }

  protected void doRepairRepositoryWith(final Repository repository,
                                        final BiFunction<Repository, Iterable<Asset>, String> function)
  {
    String lastId = BEGINNING_ID;

    while (lastId != null) {
      try {
        lastId = processBatchWith(repository, lastId, function);
      }
      catch (Exception e) {
        propagateIfPossible(e, RuntimeException.class);
        throw new RuntimeException(e);
      }
    }
  }

  protected void doRepairRepository(final Repository repository) {
    beforeRepair(repository);
    doRepairRepositoryWith(repository, this::updateAssets);
    afterRepair(repository);
    log.info("Finished processing all {} packages for repair", repository.getFormat());
  }

  @Nullable
  protected String processBatchWith(final Repository repository,
                                    final String lastId,
                                    final BiFunction<Repository, Iterable<Asset>, String> function) throws Exception
  {
    return TransactionalStoreMetadata.operation
        .withDb(repository.facet(StorageFacet.class).txSupplier())
        .throwing(Exception.class)
        .call(() -> {
          Iterable<Asset> assets = readAssets(repository, lastId);
          return function.apply(repository, assets);
        });
  }

  @Nullable
  private String updateAssets(final Repository repository, final Iterable<Asset> assets) {
    String lastId = null;
    StorageTx tx = UnitOfWork.currentTx();
    for (Asset asset : assets) {
      lastId = assetEntityAdapter.recordIdentity(asset).toString();
      updateAsset(repository, tx, asset);
    }
    return lastId;
  }

  protected Iterable<Asset> readAssets(final Repository repository, final String lastId) {
    StorageTx storageTx = UnitOfWork.currentTx();
    Map<String, Object> parameters = ImmutableMap.of("rid", lastId, "limit", BATCH_SIZE);
    return storageTx.findAssets(ASSETS_WHERE, parameters, singletonList(repository), ASSETS_SUFFIX);
  }

  protected boolean shouldRepairRepository(final Repository repository) {
    return repository.getFormat().equals(format) && repository.getType().equals(type);
  }

  protected void beforeRepair(final Repository repository) {
    //no-op
  }

  protected void afterRepair(final Repository repository) {
    //no-op
  }

  protected abstract void updateAsset(final Repository repository, final StorageTx tx, final Asset asset);
}
