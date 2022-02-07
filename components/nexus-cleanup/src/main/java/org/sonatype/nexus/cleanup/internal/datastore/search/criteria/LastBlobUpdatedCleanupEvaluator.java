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

import javax.inject.Named;

import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.content.Asset;
import org.sonatype.nexus.repository.content.AssetBlob;

import static org.sonatype.nexus.repository.search.DefaultComponentMetadataProducer.LAST_BLOB_UPDATED_KEY;

/**
 * Creates a test which evaluates whether an asset's blob was created (i.e. attached) to the asset on or before
 * the time determined by the offset.
 *
 * @since 3.next
 */
@Named(LAST_BLOB_UPDATED_KEY)
public class LastBlobUpdatedCleanupEvaluator
    implements AssetCleanupEvaluator
{
  /*
   * Value is expected to be an offset from the current time specified in seconds.
   */
  @Override
  public Predicate<Asset> getPredicate(final Repository repository, final String value) {
    OffsetDateTime cutTime = OffsetDateTime.now().minusSeconds(Long.valueOf(value));

    return asset -> {
      OffsetDateTime blobCreated = asset.blob().map(AssetBlob::blobCreated).orElse(null);

      return blobCreated != null && (blobCreated.isBefore(cutTime) || blobCreated.isEqual(cutTime));
    };
  }
}
