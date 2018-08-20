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
import java.util.concurrent.TimeUnit;

import javax.cache.Cache;
import javax.cache.expiry.CreatedExpiryPolicy;
import javax.cache.expiry.Duration;
import javax.cache.processor.EntryProcessor;
import javax.cache.processor.MutableEntry;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.sonatype.goodies.lifecycle.Lifecycle;
import org.sonatype.nexus.cache.CacheHelper;
import org.sonatype.nexus.common.app.ManagedLifecycle;
import org.sonatype.nexus.common.stateguard.Guarded;
import org.sonatype.nexus.common.stateguard.StateGuardLifecycleSupport;
import org.sonatype.nexus.orient.DatabaseInstance;
import org.sonatype.nexus.repository.assetdownloadcount.AssetDownloadCountStore;
import org.sonatype.nexus.repository.assetdownloadcount.CacheEntryKey;
import org.sonatype.nexus.repository.assetdownloadcount.DateType;
import org.sonatype.nexus.repository.storage.ComponentDatabase;

import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
import org.joda.time.DateTime;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.sonatype.nexus.common.app.ManagedLifecycle.Phase.SCHEMAS;
import static org.sonatype.nexus.common.stateguard.StateGuardLifecycleSupport.State.STARTED;
import static org.sonatype.nexus.orient.transaction.OrientTransactional.inTx;
import static org.sonatype.nexus.orient.transaction.OrientTransactional.inTxRetry;

/**
 * Manages shuffling counts data to from the database, as well as a cache of counts to not overwhelm the request
 * handling with these db updates
 *
 * @since 3.4
 */
@Named
@Singleton
@ManagedLifecycle(phase = SCHEMAS)
public class AssetDownloadCountStoreImpl
    extends StateGuardLifecycleSupport
    implements AssetDownloadCountStore, Lifecycle
{
  private final Provider<DatabaseInstance> databaseInstance;

  private final AssetDownloadCountEntityAdapter entityAdapter;

  private final boolean enabled;

  private final AssetDownloadHistoricDataCleaner historicDataCleaner;

  private Cache<CacheEntryKey, Long> cache = null;

  private final EntryIncrementProcessor entryIncrementProcessor;

  private final CacheHelper cacheHelper;

  private final int cacheSize;

  private final int cacheDuration;

  private final CacheRemovalListener cacheRemovalListener;

  @Inject
  public AssetDownloadCountStoreImpl(@Named(ComponentDatabase.NAME) final Provider<DatabaseInstance> databaseInstance,
                                     @Named("${nexus.assetdownloads.enabled:-true}") final boolean enabled,
                                     @Named("${nexus.assetdownloads.cache.size:-10000}") final int cacheSize,
                                     @Named("${nexus.assetdownloads.cache.duration:-3600}") final int cacheDuration,
                                     final AssetDownloadCountEntityAdapter entityAdapter,
                                     final AssetDownloadHistoricDataCleaner historicDataCleaner,
                                     final CacheRemovalListener cacheRemovalListener,
                                     final CacheHelper cacheHelper)
  {
    this.databaseInstance = checkNotNull(databaseInstance);
    this.entityAdapter = checkNotNull(entityAdapter);
    this.historicDataCleaner = checkNotNull(historicDataCleaner);
    this.cacheHelper = checkNotNull(cacheHelper);
    this.enabled = enabled;
    this.cacheSize = cacheSize;
    this.cacheDuration = cacheDuration;

    this.cacheRemovalListener = checkNotNull(cacheRemovalListener);

    entryIncrementProcessor = new EntryIncrementProcessor();
  }

  @Override
  protected void doStart() throws Exception {
    try (ODatabaseDocumentTx db = databaseInstance.get().connect()) {
      entityAdapter.register(db);
    }

    if (cache == null) {
      this.cache = cacheHelper.getOrCreate(cacheHelper.<CacheEntryKey, Long>builder()
          .name(CACHE_NAME)
          .cacheSize(cacheSize)
          .expiryFactory(CreatedExpiryPolicy.factoryOf(new Duration(TimeUnit.SECONDS, cacheDuration)))
          .keyType(CacheEntryKey.class)
          .valueType(Long.class)
          .persister(cacheRemovalListener));
    }
  }

  @Override
  protected void doStop() {
    historicDataCleaner.stop();
    if (cache != null) {
      cache.removeAll();
    }
    cache = null;
  }

  @Override
  @Guarded(by = STARTED)
  public long getDailyCount(final String repositoryName,
                            final String assetName,
                            final DateTime date)
  {
    long count = inTx(databaseInstance)
        .call(db -> entityAdapter.getCount(db, repositoryName, assetName, DateType.DAY, date));
    log.debug("Get daily count for {} {} {} return {}", repositoryName, assetName, date, count);
    return count;
  }

  @Override
  @Guarded(by = STARTED)
  public long getMonthlyCount(final String repositoryName,
                              final String assetName,
                              final DateTime date)
  {
    long count = inTx(databaseInstance)
        .call(db -> entityAdapter.getCount(db, repositoryName, assetName, DateType.MONTH, date));
    log.debug("Get monthly count for {} {} {} return {}", repositoryName, assetName, date, count);
    return count;
  }

  @Override
  @Guarded(by = STARTED)
  public long[] getDailyCounts(final String repositoryName,
                               final String assetName)
  {
    long[] counts = inTx(databaseInstance)
        .call(db -> entityAdapter.getCounts(db, repositoryName, assetName, DateType.DAY));
    log.debug("Get daily counts for {} {} {}", repositoryName, assetName, counts);
    return counts;
  }

  @Override
  @Guarded(by = STARTED)
  public long[] getMonthlyCounts(final String repositoryName,
                                 final String assetName)
  {
    long[] counts = inTx(databaseInstance)
        .call(db -> entityAdapter.getCounts(db, repositoryName, assetName, DateType.MONTH));
    log.debug("Get monthly counts for {} {} {}", repositoryName, assetName, counts);
    return counts;
  }

  private static class EntryIncrementProcessor
      implements EntryProcessor<CacheEntryKey, Long, Long>, Serializable
  {

    @Override
    public Long process(MutableEntry<CacheEntryKey, Long> mutableEntry, Object... objects) {
      Long originalValue = mutableEntry.getValue();
      long newValue = originalValue == null ? 1 : originalValue + 1;
      mutableEntry.setValue(newValue);
      return newValue;
    }
  }

  @Override
  @Guarded(by = STARTED)
  public void incrementCount(final String repositoryName, final String assetName) {
    if (cache != null) {
      long newValue = cache.invoke(new CacheEntryKey(repositoryName, assetName), entryIncrementProcessor);
      log.debug("Incremented cached count for {} {} by 1 to {}", repositoryName, assetName, newValue);
    }

    historicDataCleaner.start();
  }

  @Override
  @Guarded(by = STARTED)
  public void setMonthlyVulnerableCount(final String repositoryName,
                                        final DateTime date,
                                        final long count)
  {
    inTxRetry(databaseInstance).run(db -> {
      entityAdapter.setCount(db, repositoryName, DateType.MONTH_WHOLE_REPO_VULNERABLE, date, count);
      log.debug("Setting monthly vulnerability count {} {} {}", repositoryName, date, count);
    });
  }

  @Override
  @Guarded(by = STARTED)
  public void setMonthlyCount(final String repositoryName,
                              final DateTime date,
                              final long count)
  {
    inTxRetry(databaseInstance).run(db -> {
      entityAdapter.setCount(db, repositoryName, DateType.MONTH_WHOLE_REPO, date, count);
      log.debug("Setting monthly count {} {} {}", repositoryName, date, count);
    });
  }

  @Override
  @Guarded(by = STARTED)
  public long[] getMonthlyCounts(final String repositoryName) {
    return inTx(databaseInstance).call(db -> {
      long[] counts = entityAdapter.getCounts(db, repositoryName, DateType.MONTH_WHOLE_REPO);
      log.debug("Get monthly counts for  {} {}", repositoryName, counts);
      return counts;
    });
  }

  @Override
  @Guarded(by = STARTED)
  public long[] getMonthlyVulnerableCounts(final String repositoryName) {
    return inTx(databaseInstance).call(db -> {
      long[] counts = entityAdapter.getCounts(db, repositoryName, DateType.MONTH_WHOLE_REPO_VULNERABLE);
      log.debug("Get monthly vulnerable counts for  {} {}", repositoryName, counts);
      return counts;
    });
  }

  @Override
  public boolean isEnabled() {
    return enabled;
  }

  @Override
  @Guarded(by = STARTED)
  public long getLastThirtyDays(final String repositoryName, final String assetName) {
    long lastThirty = 0;
    for (long day : getDailyCounts(repositoryName, assetName)) {
      lastThirty += day;
    }

    if (cache != null) {
      Long cachedDownloadCount = cache.get(new CacheEntryKey(repositoryName, assetName));
      lastThirty += cachedDownloadCount == null ? 0L : cachedDownloadCount;
    }

    log.debug("Last thirty days downloads for {} {} is {}", repositoryName, assetName, lastThirty);

    return lastThirty;
  }
}
