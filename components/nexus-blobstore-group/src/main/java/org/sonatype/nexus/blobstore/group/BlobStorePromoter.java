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
package org.sonatype.nexus.blobstore.group;

import java.util.Collections;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.blobstore.api.BlobStore;
import org.sonatype.nexus.blobstore.api.BlobStoreConfiguration;
import org.sonatype.nexus.blobstore.api.BlobStoreException;
import org.sonatype.nexus.blobstore.api.BlobStoreManager;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.lang.String.format;
import static org.sonatype.nexus.blobstore.group.BlobStoreGroup.CONFIG_KEY;
import static org.sonatype.nexus.blobstore.group.BlobStoreGroup.FALLBACK_FILL_POLICY_TYPE;
import static org.sonatype.nexus.blobstore.group.BlobStoreGroup.FILL_POLICY_KEY;
import static org.sonatype.nexus.blobstore.group.BlobStoreGroup.MEMBERS_KEY;

/**
 * Promotes an existing blobstore to a group blobstore.
 *
 * The way it works is that the existing blobstore is renamed via database update. A new group is created that has the
 * existing blobstore's original name and then contains the original blobstore.
 *
 * @since 3.14
 */
@Singleton
public class BlobStorePromoter
    extends ComponentSupport
{
  private final BlobStoreManager blobStoreManager;

  @Inject
  public BlobStorePromoter(final BlobStoreManager blobStoreManager) {
    this.blobStoreManager = checkNotNull(blobStoreManager);
  }

  /**
   * Takes a {@link BlobStore} and creates a {@link BlobStoreGroup} that contains the original blobstore
   *
   * @param from a {@link BlobStore} to be "promoted"
   * @return {@link BlobStoreGroup} that contains the original blobstore
   */
  @SuppressWarnings("squid:MethodCyclomaticComplexity") // trying to keep the rollback logic isolated in this method
  public BlobStoreGroup promote(final BlobStore from) {
    BlobStoreConfiguration fromConf = from.getBlobStoreConfiguration();
    String fromOldName = fromConf.getName();
    String fromNewName = format("%s-%s", fromOldName, "promoted");

    // create the group config that includes the fbs
    BlobStoreConfiguration groupConf = fromConf.copy(fromOldName);
    groupConf.setType(BlobStoreGroup.TYPE);
    groupConf.attributes(CONFIG_KEY).set(MEMBERS_KEY, Collections.singletonList(fromNewName));
    groupConf.attributes(CONFIG_KEY).set(FILL_POLICY_KEY, FALLBACK_FILL_POLICY_TYPE);

    // rename the fbs
    fromConf.setName(fromNewName);

    try {
      blobStoreManager.forceDelete(fromOldName); // does not delete data
    }
    catch (Exception e) {
      throw new BlobStoreException(
          format("during promotion to group, failed to stop existing blob store: %s", fromOldName), e, null
      );
    }

    try {
      blobStoreManager.create(fromConf);
    }
    catch (Exception e) {
      // old blobstore was already removed, if create fails, we need to try to resurrect
      fromConf.setName(fromOldName);
      try {
        blobStoreManager.create(fromConf);
      }
      catch (Exception inner) {
        log.error("during promotion to group, existing blob: {} store was removed, but failed to be created " +
            "with new configuration and failed to be resurrected", fromOldName, inner);
      }
      throw new BlobStoreException(
          format("during promotion to group, failed to stop existing blob store: %s", fromOldName), e, null
      );
    }

    try {
      return (BlobStoreGroup) blobStoreManager.create(groupConf);
    }
    catch (Exception e) {
      try {
        blobStoreManager.forceDelete(fromNewName);
      }
      catch (Exception inner) {
        log.error("during promotion to group, existing blob: {} store was removed, but the creation of the new" +
            " group failed, recreating the original blob store", fromOldName, inner);
      }
      fromConf.setName(fromOldName);
      try {
        blobStoreManager.create(fromConf);
      }
      catch (Exception inner) {
        log.error("during promotion to group, existing blob: {} store was removed, but failed to be created " +
            "with new configuration and failed to be resurrected", fromOldName, inner);
      }
      throw new BlobStoreException("failed to create group configuration", e, null);
    }
  }
}
