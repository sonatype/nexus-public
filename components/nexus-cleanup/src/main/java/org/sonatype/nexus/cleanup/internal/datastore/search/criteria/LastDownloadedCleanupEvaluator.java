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
package org.sonatype.nexus.cleanup.internal.datastore.search.criteria;

import java.time.OffsetDateTime;
import java.util.function.Predicate;

import javax.inject.Inject;
import javax.inject.Named;

import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.content.Asset;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.sonatype.nexus.repository.search.DefaultComponentMetadataProducer.LAST_DOWNLOADED_KEY;

/**
 * Tests whether the asset was last downloaded before the specified offset, or if the asset has never been downloaded
 * whether its asset was created before the offset.
 *
 * @since 3.next
 */
@Named(LAST_DOWNLOADED_KEY)
public class LastDownloadedCleanupEvaluator
    implements AssetCleanupEvaluator
{
  private final LastBlobUpdatedCleanupEvaluator lastBlobUpdatedCleanupCriteria;

  @Inject
  public LastDownloadedCleanupEvaluator(final LastBlobUpdatedCleanupEvaluator lastBlobUpdatedCleanupCriteria) {
    this.lastBlobUpdatedCleanupCriteria = checkNotNull(lastBlobUpdatedCleanupCriteria);
  }

  /*
   * Value is expected to be an offset from the current time specified in seconds.
   */
  @Override
  public Predicate<Asset> getPredicate(final Repository repository, final String value) {
    OffsetDateTime cutTime = OffsetDateTime.now().minusSeconds(Long.valueOf(value));
    Predicate<Asset> lastBlobUpdated = lastBlobUpdatedCleanupCriteria.getPredicate(repository, value);

    return asset -> asset.lastDownloaded()
          .map(lastDownloaded -> lastDownloaded.isBefore(cutTime) || lastDownloaded.isEqual(cutTime))
          .orElseGet(() -> lastBlobUpdated.test(asset));
  }
}
