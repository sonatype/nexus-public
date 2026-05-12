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
package org.sonatype.nexus.cleanup.internal.content.method;

import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.stream.Stream;
import jakarta.inject.Inject;

import org.sonatype.nexus.cleanup.internal.method.CleanupMethod;
import org.sonatype.nexus.common.db.DatabaseCheck;
import org.sonatype.nexus.common.entity.Continuations;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.content.browse.BrowseFacet;
import org.sonatype.nexus.repository.content.fluent.FluentComponent;
import org.sonatype.nexus.repository.content.maintenance.ContentMaintenanceFacet;
import org.sonatype.nexus.repository.content.store.InternalIds;
import org.sonatype.nexus.repository.task.DeletionProgress;
import org.sonatype.nexus.scheduling.TaskInterruptedException;

import com.google.common.collect.Iterators;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Preconditions.checkNotNull;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * Provides a delete mechanism for cleanup
 *
 * @since 3.29
 */
@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class DeleteCleanupMethod
    implements CleanupMethod
{
  protected final Logger log = LoggerFactory.getLogger(getClass());

  private final DatabaseCheck databaseCheck;

  @Inject
  public DeleteCleanupMethod(final DatabaseCheck databaseCheck) {
    this.databaseCheck = checkNotNull(databaseCheck);
  }

  @Override
  public DeletionProgress run(
      final Repository repository,
      final Stream<FluentComponent> components,
      final BooleanSupplier cancelledCheck)
  {
    ContentMaintenanceFacet maintenance = repository.facet(ContentMaintenanceFacet.class);
    DeletionProgress progress = new DeletionProgress();

    Iterators.partition(components.iterator(), Continuations.BROWSE_LIMIT)
        .forEachRemaining((batch) -> deleteBatch(maintenance, batch.stream(), progress, cancelledCheck));

    return progress;
  }

  private void deleteBatch(
      final ContentMaintenanceFacet maintenance,
      final Stream<FluentComponent> batch,
      final DeletionProgress progress,
      final BooleanSupplier cancelledCheck)
  {

    if (cancelledCheck.getAsBoolean()) {
      throw new TaskInterruptedException(
          String.format("Thread '%s' is canceled", Thread.currentThread().getName()),
          true);
    }

    // Collect the stream into a list to allow reuse
    List<FluentComponent> components = batch.toList();

    if (isPostgresql()) {
      deleteBrowseNodes(components);
    }

    logAssetsBeingDeleted(components);

    progress.addComponentCount(maintenance.deleteComponents(components.stream()));
  }

  private void logAssetsBeingDeleted(final List<FluentComponent> components) {
    if (log.isDebugEnabled()) {
      components.forEach(component -> component.assets().forEach(asset -> {
        String repositoryName = asset.repository().getName();
        String assetPath = asset.path();

        asset.blob().ifPresent(blob -> {
          log.debug("Deleting asset - repository: {}, path: {}, blobRef: {}, size: {} bytes",
              repositoryName, assetPath, blob.blobRef(), blob.blobSize());
        });
      }));
    }
  }

  private void deleteBrowseNodes(final List<FluentComponent> components) {
    components.forEach(component -> component.assets().forEach(asset -> {
      Integer internalAssetId = InternalIds.internalAssetId(asset);
      asset.repository()
          .optionalFacet(BrowseFacet.class)
          .ifPresent(facet -> facet.deleteByAssetIdAndPath(internalAssetId, asset.path()));
    }));
  }

  private boolean isPostgresql() {
    return this.databaseCheck.isPostgresql();
  }
}
