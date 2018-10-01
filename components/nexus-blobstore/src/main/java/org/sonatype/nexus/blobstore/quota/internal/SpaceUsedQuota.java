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
package org.sonatype.nexus.blobstore.quota.internal;

import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.blobstore.api.BlobStore;
import org.sonatype.nexus.blobstore.quota.BlobStoreQuota;
import org.sonatype.nexus.blobstore.quota.BlobStoreQuotaResult;
import org.sonatype.nexus.blobstore.quota.BlobStoreQuotaSupport;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.lang.String.format;

/**
 * A {@link BlobStoreQuota} which checks that a blob store isn't using more space the limit.
 *
 * @since 3.14
 */
@Named(SpaceUsedQuota.ID)
@Singleton
public class SpaceUsedQuota
    extends BlobStoreQuotaSupport
{
  public static final String ID = "spaceUsedQuota";

  private static final String DISPLAY_NAME = "Space Used";

  @Override
  public BlobStoreQuotaResult check(final BlobStore blobStore) {
    checkNotNull(blobStore);

    long usedSpace = blobStore.getMetrics().getTotalSize();
    Number limitObj = blobStore.getBlobStoreConfiguration().attributes(ROOT_KEY).get(LIMIT_KEY, Number.class);
    long limit = checkNotNull(limitObj).longValue();

    String name = blobStore.getBlobStoreConfiguration().getName();
    String msg = format("Blob store %s is using %s space and has a limit of %s", name,
        convertBytesToSI(usedSpace),
        convertBytesToSI(limit));

    return new BlobStoreQuotaResult(usedSpace > limit, name, msg);
  }

  @Override
  public String getDisplayName() {
    return DISPLAY_NAME;
  }

  @Override
  public String getId() {
    return ID;
  }
}
