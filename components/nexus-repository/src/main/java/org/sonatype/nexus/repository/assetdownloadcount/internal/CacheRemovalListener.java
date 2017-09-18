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

import java.util.concurrent.atomic.AtomicLong;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.common.event.EventAware;
import org.sonatype.nexus.orient.DatabaseInstance;
import org.sonatype.nexus.orient.freeze.DatabaseFreezeChangeEvent;
import org.sonatype.nexus.repository.storage.ComponentDatabase;

import com.google.common.cache.RemovalListener;
import com.google.common.cache.RemovalNotification;
import com.google.common.eventbus.Subscribe;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.sonatype.nexus.orient.transaction.OrientTransactional.inTxRetry;

/**
 * Google cache removal listener that will stuff the data into the database
 *
 * @since 3.4
 */
@Named
@Singleton
public class CacheRemovalListener
    extends ComponentSupport
    implements RemovalListener<CacheEntryKey, AtomicLong>, EventAware, EventAware.Asynchronous
{
  private final AssetDownloadCountEntityAdapter entityAdapter;

  private final Provider<DatabaseInstance> databaseInstance;

  private volatile boolean frozen;

  @Inject
  public CacheRemovalListener(@Named(ComponentDatabase.NAME) final Provider<DatabaseInstance> databaseInstance,
                              final AssetDownloadCountEntityAdapter entityAdapter)
  {
    this.entityAdapter = checkNotNull(entityAdapter);
    this.databaseInstance = checkNotNull(databaseInstance);
  }

  @Override
  public void onRemoval(final RemovalNotification<CacheEntryKey, AtomicLong> removalNotification) {
    if (!frozen) {
      inTxRetry(databaseInstance).run(
          db -> {
            final CacheEntryKey key = removalNotification.getKey();
            entityAdapter.incrementCount(db, key.getRepositoryName(), key.getAssetName(), removalNotification
                .getValue().get());
            log.debug("Incremented count(DB) {} {} by {}", key.getRepositoryName(), key.getAssetName(),
                removalNotification.getValue());
          });
    }
  }

  @Subscribe
  public void onDatabaseFreezeChangeEvent(final DatabaseFreezeChangeEvent databaseFreezeChangeEvent) {
    frozen = databaseFreezeChangeEvent.isFrozen();
  }
}
