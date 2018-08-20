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
package org.sonatype.nexus.repository.assetdownloadcount.internal;

import java.io.Serializable;
import java.util.function.BiConsumer;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.common.node.NodeAccess;
import org.sonatype.nexus.orient.DatabaseInstance;
import org.sonatype.nexus.repository.assetdownloadcount.CacheEntryKey;
import org.sonatype.nexus.repository.storage.ComponentDatabase;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.sonatype.nexus.orient.transaction.OrientTransactional.inTxRetry;

/**
 * Cache removal listener that will stuff the data into the database
 * Required to be {@link Serializable} because some cache instances use serialization to share configuration objects
 *
 * @since 3.4
 */
@Named
@Singleton
public class CacheRemovalListener
    extends ComponentSupport
    implements Serializable, BiConsumer<CacheEntryKey, Long>
{
  private static final long serialVersionUID = 1L;

  private final transient Provider<AssetDownloadCountEntityAdapter> entityAdapter;

  private final transient Provider<DatabaseInstance> databaseInstance;

  private final transient Provider<NodeAccess> nodeAccess;

  @Inject
  public CacheRemovalListener(@Named(ComponentDatabase.NAME) final Provider<DatabaseInstance> databaseInstance,
                              final Provider<AssetDownloadCountEntityAdapter> entityAdapter,
                              final Provider<NodeAccess> nodeAccess)
  {
    this.entityAdapter = checkNotNull(entityAdapter);
    this.databaseInstance = checkNotNull(databaseInstance);
    this.nodeAccess = checkNotNull(nodeAccess);
  }

  @Override
  public void accept(final CacheEntryKey key, final Long value) {
    if (!nodeAccess.get().isClustered() || nodeAccess.get().isOldestNode()) {
      if (!databaseInstance.get().isFrozen()) {
        inTxRetry(databaseInstance).run(
            db -> {
              entityAdapter.get().incrementCount(db, key.getRepositoryName(), key.getAssetName(), value);
              log.debug("Incremented count(DB) {} {} by {}", key.getRepositoryName(), key.getAssetName(), value);
            });
      }
      else {
        log.warn("Dropping download count increment of {} for {}", value, key);
      }
    }
  }
}
