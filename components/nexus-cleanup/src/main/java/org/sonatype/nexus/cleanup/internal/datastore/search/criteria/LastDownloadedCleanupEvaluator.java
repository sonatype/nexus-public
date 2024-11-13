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
import java.util.Objects;
import java.util.function.BiPredicate;
import java.util.stream.StreamSupport;

import javax.inject.Named;

import org.sonatype.nexus.cleanup.datastore.search.criteria.ComponentCleanupEvaluator;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.content.Asset;
import org.sonatype.nexus.repository.content.AssetBlob;
import org.sonatype.nexus.repository.content.Component;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.sonatype.nexus.cleanup.config.CleanupPolicyConstants.LAST_DOWNLOADED_KEY;

/**
 * Tests whether all assets under a component were last downloaded before the specified offset. If an asset has never
 * been downloaded then it's blob created date is used instead of last downloaded.
 *
 */
@Named(LAST_DOWNLOADED_KEY)
public class LastDownloadedCleanupEvaluator
    implements ComponentCleanupEvaluator
{
  private static final Logger log = LoggerFactory.getLogger(LastDownloadedCleanupEvaluator.class);

  /*
   * Value is expected to be an offset from the current time specified in seconds.
   */
  @Override
  public BiPredicate<Component, Iterable<Asset>> getPredicate(final Repository repository, final String value) {
    OffsetDateTime cutTime = OffsetDateTime.now().minusSeconds(Long.valueOf(value));

    return (component, assets) -> {
      OffsetDateTime max = StreamSupport.stream(assets.spliterator(), false)
          .map(asset -> asset.lastDownloaded().orElse(blobCreated(asset)))
          .filter(Objects::nonNull)
          .max(OffsetDateTime::compareTo).orElse(null);
      if (max != null) {
        boolean shouldCleanup = max.isBefore(cutTime);
        log.debug("{} cleanup component (assuming other criteria pass) with max last downloaded timestamp {} < {}",
            shouldCleanup ? "Should" : "Should not", max, cutTime);
        return shouldCleanup;
      }
      return false;
    };
  }

  private OffsetDateTime blobCreated(final Asset asset) {
    return asset.blob().map(AssetBlob::blobCreated).orElse(null);
  }
}
