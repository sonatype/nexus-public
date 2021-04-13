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
package org.sonatype.nexus.repository.maven.internal.orient;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.BooleanSupplier;

import javax.annotation.Nullable;
import javax.inject.Named;

import org.sonatype.nexus.common.entity.EntityId;
import org.sonatype.nexus.orient.maven.OrientMavenFacet;
import org.sonatype.nexus.repository.maven.MavenHostedFacet;
import org.sonatype.nexus.repository.storage.Bucket;
import org.sonatype.nexus.repository.storage.Component;
import org.sonatype.nexus.repository.storage.ComponentMaintenance;
import org.sonatype.nexus.repository.storage.StorageTx;
import org.sonatype.nexus.repository.transaction.TransactionalDeleteBlob;
import org.sonatype.nexus.transaction.Transactional;
import org.sonatype.nexus.transaction.UnitOfWork;

import static java.util.stream.Collectors.toList;
import static org.sonatype.nexus.repository.maven.internal.Attributes.P_ARTIFACT_ID;
import static org.sonatype.nexus.repository.maven.internal.Attributes.P_BASE_VERSION;
import static org.sonatype.nexus.repository.maven.internal.Attributes.P_GROUP_ID;

/**
 * maven format specific hosted {@link ComponentMaintenance}.
 *
 * @since 3.0
 */
@Named
public class MavenHostedComponentMaintenanceFacet
    extends MavenComponentMaintenanceFacet
{
  @Override
  @TransactionalDeleteBlob
  protected DeletionResult deleteComponentTx(final EntityId componentId, final boolean deleteBlobs) {
    DeletionResult result = super.deleteComponentTx(componentId, deleteBlobs);

    Component component = result.getComponent();
    if (component != null) {
      String groupId = component.formatAttributes().get(P_GROUP_ID, String.class);
      String artifactId = component.formatAttributes().get(P_ARTIFACT_ID, String.class);
      String baseVersion = component.formatAttributes().get(P_BASE_VERSION, String.class);
      Set<String> deletedMetadataPaths =
          getRepository().facet(MavenHostedFacet.class).deleteMetadata(groupId, artifactId, baseVersion);
      result.getAssets().addAll(deletedMetadataPaths);
    }

    return result;
  }

  @Override
  @TransactionalDeleteBlob
  protected DeletionProgress doBatchDelete(final List<EntityId> entityIds, final BooleanSupplier cancelledCheck) {
    try {
      List<String[]> gavs = collectGavs(entityIds);
      OrientMavenFacet mavenFacet = getRepository().facet(OrientMavenFacet.class);
      final StorageTx tx = UnitOfWork.currentTx();
      Bucket bucket = tx.findBucket(getRepository());

      DeletionProgress batchProgress = deleteComponentBatch(entityIds, cancelledCheck);

      for (String[] gav : gavs) {
        mavenFacet.maybeDeleteOrFlagToRebuildMetadata(bucket, gav[0], gav[1], gav[2]);
        mavenFacet.maybeDeleteOrFlagToRebuildMetadata(bucket, gav[0], gav[1]);
        mavenFacet.maybeDeleteOrFlagToRebuildMetadata(bucket, gav[0]);
      }

      return batchProgress;
    }
    catch (Exception ex) {
      log.debug("Error encountered attempting to delete components for repository {}.", getRepository().getName(), ex);
    }
    return new DeletionProgress();
  }

  @Transactional
  protected List<String[]> collectGavs(final List<EntityId> entityIds) {
    return entityIds.stream()
        .map(this::findComponent)
        .filter(Objects::nonNull)
        .collect(toList());
  }

  @Nullable
  private String[] findComponent(final EntityId entityId) {
    final StorageTx tx = UnitOfWork.currentTx();
    Component component = tx.findComponentInBucket(entityId, tx.findBucket(getRepository()));
    if (component != null) {
      return new String[]{
          component.formatAttributes().get(P_GROUP_ID, String.class),
          component.formatAttributes().get(P_ARTIFACT_ID, String.class),
          component.formatAttributes().get(P_BASE_VERSION, String.class)
      };
    }
    return null; //NOSONAR
  }
}
