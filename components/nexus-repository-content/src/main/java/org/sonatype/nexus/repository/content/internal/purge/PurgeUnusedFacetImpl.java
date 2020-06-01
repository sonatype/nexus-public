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
package org.sonatype.nexus.repository.content.internal.purge;

import javax.inject.Named;

import org.sonatype.nexus.common.app.FeatureFlag;
import org.sonatype.nexus.common.stateguard.Guarded;
import org.sonatype.nexus.repository.FacetSupport;
import org.sonatype.nexus.repository.content.facet.ContentFacet;
import org.sonatype.nexus.repository.content.facet.ContentFacetSupport;
import org.sonatype.nexus.repository.content.store.AssetStore;
import org.sonatype.nexus.repository.content.store.ComponentStore;
import org.sonatype.nexus.repository.purge.PurgeUnusedFacet;

import static com.google.common.base.Preconditions.checkArgument;
import static org.sonatype.nexus.repository.FacetSupport.State.STARTED;
import static org.sonatype.nexus.scheduling.CancelableHelper.checkCancellation;

/**
 * @since 3.24
 */
@FeatureFlag(name = "nexus.datastore.enabled")
@Named
public class PurgeUnusedFacetImpl
    extends FacetSupport
    implements PurgeUnusedFacet
{
  private static final int LIMIT = 1000;

  @Override
  @Guarded(by = STARTED)
  public void purgeUnused(final int numberOfDays) {
    checkArgument(numberOfDays > 0, "Number of days must be greater then zero");
    log.info("Purging unused components from repository {}", getRepository().getName());
    ContentFacetSupport contentFacetSupport = (ContentFacetSupport) getRepository().facet(ContentFacet.class);

    deleteComponentsOlderThan(numberOfDays, contentFacetSupport);
    deleteAssetsOlderThan(numberOfDays, contentFacetSupport);
  }

  private void deleteAssetsOlderThan(final int numberOfDays, final ContentFacetSupport contentFacetSupport) {
    Integer contentRepositoryId = contentFacetSupport.contentRepositoryId();
    AssetStore<?> assetStore = contentFacetSupport.stores().assetStore;
    while (checkCancellation()) {
      int deleted = assetStore.purgeNotRecentlyDownloaded(contentRepositoryId, numberOfDays, LIMIT);
      log.debug("Deleted {} unused assets without components", deleted);
      if (deleted == 0) {
        return;
      }
    }
  }

  private void deleteComponentsOlderThan(final int numberOfDays, final ContentFacetSupport contentFacetSupport) {
    Integer contentRepositoryId = contentFacetSupport.contentRepositoryId();
    ComponentStore<?> componentStore = contentFacetSupport.stores().componentStore;
    while (checkCancellation()) {
      int deleted = componentStore.purgeNotRecentlyDownloaded(contentRepositoryId, numberOfDays, LIMIT);
      log.debug("Deleted {} unused components with associated assets", deleted);
      if (deleted == 0) {
        return;
      }
    }
  }
}
