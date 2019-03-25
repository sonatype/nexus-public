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
package org.sonatype.nexus.repository.storage;


import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.BooleanSupplier;

import javax.annotation.Nullable;
import javax.inject.Named;

import org.sonatype.nexus.common.entity.EntityId;
import org.sonatype.nexus.common.stateguard.Guarded;
import org.sonatype.nexus.repository.FacetSupport;
import org.sonatype.nexus.repository.transaction.TransactionalDeleteBlob;
import org.sonatype.nexus.transaction.UnitOfWork;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.Iterables.partition;
import static org.sonatype.nexus.common.stateguard.StateGuardLifecycleSupport.State.STARTED;

/**
 * A default implementation of {@link ComponentMaintenance} for repository format that don't need
 * additional bookkeeping.
 *
 * @since 3.0
 */
@Named
public class DefaultComponentMaintenanceImpl
    extends FacetSupport
    implements ComponentMaintenance
{
  /**
   * Deletes the component directly, with no additional bookkeeping.
   */
  @Override
  public Set<String> deleteComponent(final EntityId componentId) {
    return deleteComponent(componentId, true);
  }

  /**
   * Deletes the component directly, with no additional bookkeeping.
   */
  @Override
  public Set<String> deleteComponent(final EntityId componentId, final boolean deleteBlobs) {
    checkNotNull(componentId);
    UnitOfWork.begin(getRepository().facet(StorageFacet.class).txSupplier());
    try {
      return deleteComponentTx(componentId, deleteBlobs).getAssets();
    }
    finally {
      UnitOfWork.end();
    }
  }

  @TransactionalDeleteBlob
  protected DeletionResult deleteComponentTx(final EntityId componentId, final boolean deleteBlobs) {
    StorageTx tx = UnitOfWork.currentTx();
    Component component = tx.findComponentInBucket(componentId, tx.findBucket(getRepository()));
    if (component == null) {
      return new DeletionResult(null, Collections.emptySet());
    }
    log.debug("Deleting component: {}", component.toStringExternal());
    return new DeletionResult(component, tx.deleteComponent(component, deleteBlobs));
  }

  /**
   * Deletes the asset directly, with no additional bookkeeping.
   */
  @Override
  @Guarded(by = STARTED)
  public Set<String> deleteAsset(final EntityId assetId) {
    return deleteAsset(assetId, true);
  }

  @Override
  @Guarded(by = STARTED)
  public Set<String> deleteAsset(final EntityId assetId, final boolean deleteBlob) {
    checkNotNull(assetId);
    UnitOfWork.begin(getRepository().facet(StorageFacet.class).txSupplier());
    try {
      return deleteAssetTx(assetId, deleteBlob);
    }
    finally {
      UnitOfWork.end();
    }
  }

  @Override
  public DeletionProgress deleteComponents(final Iterable<EntityId> components,
                                           final BooleanSupplier cancelledCheck,
                                           final int batchSize)
  {
    checkNotNull(components);
    checkNotNull(cancelledCheck);

    UnitOfWork.beginBatch(getRepository().facet(StorageFacet.class).txSupplier());

    DeletionProgress deletionProgress = new DeletionProgress();

    try {
      Iterable<List<EntityId>> split = partition(components, batchSize);
      for (List<EntityId> entityIds : split) {
        
        if (cancelledCheck.getAsBoolean()) {
          break;
        }

        DeletionProgress batchDeletion = doBatchDelete(entityIds, cancelledCheck);
        deletionProgress.addCount(batchDeletion.getCount());
        if (batchDeletion.isFailed()) {
          deletionProgress.setFailed(true);
          break;
        }
      }
    }
    finally {
      UnitOfWork.end();
    }

    tryAfter();

    return deletionProgress;
  }

  private void tryAfter() {
    try {
      after();
    }
    catch (Exception e) {
      log.debug("Unable to run post-cleanup tasks. This could result in missing or incorrect metadata", e);
    }
  }

  @TransactionalDeleteBlob
  protected DeletionProgress deleteComponentBatch(final Iterable<EntityId> components, final BooleanSupplier cancelledCheck) {
    DeletionProgress deletionProgress = new DeletionProgress();

    try {
      for (EntityId component : components) {
        if (!cancelledCheck.getAsBoolean()) {
          try {
            DeletionResult deletionResult = deleteComponentTx(component, true);

            if (deletionResult.getComponent() != null) {
              log.info("Deleted component with ID '{}', Attributes '{}' and Assets '{}' from repository {}", component,
                  deletionResult.getComponent().toStringExternal(), deletionResult.getAssets(), getRepository());
            }

            deletionProgress.addCount(1);
          }
          catch (Exception e) {
            log.debug("Unable to delete component with ID {}", component, e);
          }
        }
      }
    }
    catch (Exception e) {
      log.warn("Unable to delete current batch", e);
      deletionProgress.setFailed(true);
    }
    return deletionProgress;
  }

  @TransactionalDeleteBlob
  protected Set<String> deleteAssetTx(final EntityId assetId, final boolean deleteBlob) {
    StorageTx tx = UnitOfWork.currentTx();
    Asset asset = tx.findAsset(assetId, tx.findBucket(getRepository()));
    if (asset == null) {
      return Collections.emptySet();
    }
    log.info("Deleting asset: {}", asset);
    tx.deleteAsset(asset, deleteBlob);
    return Collections.singleton(asset.name());
  }

  protected DeletionProgress doBatchDelete(final List<EntityId> entityIds, final BooleanSupplier cancelledCheck) {
    return deleteComponentBatch(entityIds, cancelledCheck);
  }

  @Override
  public void after() {
    //no op
  }

  protected static class DeletionResult
  {
    @Nullable
    private final Component component;

    private final Set<String> assets;

    public DeletionResult(@Nullable final Component component, final Set<String> assets) {
      this.component = component;
      this.assets = checkNotNull(assets);
    }

    @Nullable
    public Component getComponent() {
      return component;
    }

    public Set<String> getAssets() {
      return assets;
    }
  }

  public static class DeletionProgress {
    private long count = 0L;

    private boolean failed;

    private int attempts = 0;

    private int retryLimit = 0;

    public DeletionProgress() {
    }

    public DeletionProgress(final int retryLimit) {
      this.retryLimit = retryLimit;
    }

    public long getCount() {
      return count;
    }

    public void addCount(final long count) {
      this.count += count;
    }

    public boolean isFailed() {
      return failed;
    }

    public void setFailed(final boolean completed) {
      this.failed = completed;
    }

    public int getAttempts() {
      return attempts;
    }

    public void setAttempts(final int attempts) {
      this.attempts = attempts;
    }

    public void update(final DeletionProgress progress) {
      failed = progress.isFailed();
      count += progress.getCount();
      if (progress.isFailed()) {
        attempts++;
      }
    }

    public boolean isFinished() {
      return !isFailed() || getAttempts() >= retryLimit;
    }
  }
}
