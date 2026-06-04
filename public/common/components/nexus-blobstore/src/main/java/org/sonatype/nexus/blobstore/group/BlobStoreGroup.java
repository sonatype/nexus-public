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

import java.io.InputStream;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.stream.Stream;

import javax.annotation.Nullable;
import javax.cache.Cache;
import javax.cache.configuration.MutableConfiguration;
import javax.cache.expiry.CreatedExpiryPolicy;
import javax.cache.expiry.Duration;

import org.sonatype.nexus.common.time.Time;
import org.sonatype.nexus.blobstore.MemoryBlobSession;
import org.sonatype.nexus.blobstore.api.Blob;
import org.sonatype.nexus.blobstore.api.BlobAttributes;
import org.sonatype.nexus.blobstore.api.BlobId;
import org.sonatype.nexus.blobstore.api.BlobMetrics;
import org.sonatype.nexus.blobstore.api.BlobRef;
import org.sonatype.nexus.blobstore.api.BlobSession;
import org.sonatype.nexus.blobstore.api.BlobStore;
import org.sonatype.nexus.blobstore.api.BlobStoreConfiguration;
import org.sonatype.nexus.blobstore.api.BlobStoreException;
import org.sonatype.nexus.blobstore.api.BlobStoreManager;
import org.sonatype.nexus.blobstore.api.BlobStoreMetrics;
import org.sonatype.nexus.blobstore.api.BlobStoreUsageChecker;
import org.sonatype.nexus.blobstore.api.ExternalMetadata;
import org.sonatype.nexus.blobstore.api.OperationMetrics;
import org.sonatype.nexus.blobstore.api.OperationType;
import org.sonatype.nexus.blobstore.api.metrics.BlobStoreMetricsService;
import org.sonatype.nexus.blobstore.group.internal.BlobStoreGroupMetrics;
import org.sonatype.nexus.blobstore.group.internal.WriteToFirstMemberFillPolicy;
import org.sonatype.nexus.blobstore.metrics.MonitoringBlobStoreMetrics;
import org.sonatype.nexus.cache.CacheHelper;
import org.sonatype.nexus.common.QualifierUtil;
import org.sonatype.nexus.common.stateguard.Guarded;
import org.sonatype.nexus.common.stateguard.StateGuardLifecycleSupport;
import org.sonatype.nexus.common.stateguard.Transitions;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.google.common.hash.HashCode;
import org.springframework.beans.factory.annotation.Autowired;
import jakarta.inject.Provider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.Collections.synchronizedList;
import static java.util.Collections.unmodifiableList;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toList;
import static org.sonatype.nexus.blobstore.api.OperationType.DOWNLOAD;
import static org.sonatype.nexus.blobstore.api.OperationType.UPLOAD;
import static org.sonatype.nexus.common.stateguard.StateGuardLifecycleSupport.State.FAILED;
import static org.sonatype.nexus.common.stateguard.StateGuardLifecycleSupport.State.NEW;
import static org.sonatype.nexus.common.stateguard.StateGuardLifecycleSupport.State.SHUTDOWN;
import static org.sonatype.nexus.common.stateguard.StateGuardLifecycleSupport.State.STARTED;
import static org.sonatype.nexus.common.stateguard.StateGuardLifecycleSupport.State.STOPPED;

/**
 * A {@link BlobStore} consisting of other blob stores.
 *
 * @since 3.14
 */
@Component
@Qualifier(BlobStoreGroup.TYPE)
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class BlobStoreGroup
    extends StateGuardLifecycleSupport
    implements BlobStore
{
  private static final String RAW_OBJECTS_NOT_SUPPORTED = "Group BlobStore does not support raw objects";

  public static final String TYPE = "Group";

  public static final String CONFIG_KEY = "group";

  public static final String MEMBERS_KEY = "members";

  public static final String FILL_POLICY_KEY = "fillPolicy";

  public static final String FALLBACK_FILL_POLICY_TYPE = WriteToFirstMemberFillPolicy.TYPE;

  public static final String CACHE_NAME = "blobstore-group-blobIds";

  private final BlobStoreManager blobStoreManager;

  private final Map<String, Provider<FillPolicy>> fillPolicyProviders;

  private Provider<CacheHelper> cacheHelperProvider;

  private Time blobIdCacheTimeout;

  private Supplier<List<BlobStore>> members;

  @VisibleForTesting
  FillPolicy fillPolicy;

  private BlobStoreConfiguration blobStoreConfiguration;

  // cache of located blobs that have not been soft deleted
  private Cache<BlobId, String> locatedBlobs;

  @Autowired
  public BlobStoreGroup(
      final BlobStoreManager blobStoreManager,
      final List<Provider<FillPolicy>> fillPolicyProvidersList,
      final Provider<CacheHelper> cacheHelperProvider,
      @Value("${nexus.blobstore.group.blobId.cache.timeToLive:2d}") final Time blobIdCacheTimeout)
  {
    this.blobStoreManager = checkNotNull(blobStoreManager);
    this.fillPolicyProviders = checkNotNull(QualifierUtil.buildQualifierBeanMap(fillPolicyProvidersList));
    this.cacheHelperProvider = checkNotNull(cacheHelperProvider);
    this.blobIdCacheTimeout = checkNotNull(blobIdCacheTimeout);
  }

  @Override
  public void init(final BlobStoreConfiguration configuration) {
    this.blobStoreConfiguration = configuration;
    this.members = Suppliers.memoize(new MembersSupplier());
    String fillPolicyName = BlobStoreGroupConfigurationHelper.fillPolicyName(configuration);
    if (fillPolicyProviders.containsKey(fillPolicyName)) {
      this.fillPolicy = fillPolicyProviders.get(fillPolicyName).get();
    }
    else {
      log.warn("Unable to find fill policy {} for Blob Store Group {}, using fill policy {}",
          fillPolicyName, configuration.getName(), FALLBACK_FILL_POLICY_TYPE);
      this.fillPolicy = fillPolicyProviders.get(FALLBACK_FILL_POLICY_TYPE).get();
    }
  }

  @Override
  protected void doStart() throws Exception {
    locatedBlobs = cacheHelperProvider.get().maybeCreateCache(CACHE_NAME, getCacheConfiguration());
  }

  private MutableConfiguration<BlobId, String> getCacheConfiguration() {
    return new MutableConfiguration<BlobId, String>()
        .setStoreByValue(false)
        .setExpiryPolicyFactory(
            CreatedExpiryPolicy.factoryOf(new Duration(blobIdCacheTimeout.unit(), blobIdCacheTimeout.value())))
        .setManagementEnabled(true)
        .setStatisticsEnabled(true);
  }

  @Override
  protected void doStop() throws Exception {
    locatedBlobs = null;
  }

  @Override
  public BlobStoreConfiguration getBlobStoreConfiguration() {
    return this.blobStoreConfiguration;
  }

  @Override
  @Guarded(by = STARTED)
  public BlobSession<?> openSession() {
    return new MemoryBlobSession(this);
  }

  @Override
  @Guarded(by = STARTED)
  public Blob create(final InputStream blobData, final Map<String, String> headers) {
    return create(blobData, headers, null);
  }

  @Override
  @Guarded(by = STARTED)
  @MonitoringBlobStoreMetrics(operationType = UPLOAD)
  public Blob create(final InputStream blobData, final Map<String, String> headers, @Nullable final BlobId blobId) {
    return create(headers, target -> target.create(blobData, headers, blobId));
  }

  @Override
  @Guarded(by = STARTED)
  @MonitoringBlobStoreMetrics(operationType = UPLOAD)
  public Blob create(final Path sourceFile, final Map<String, String> headers, final long size, final HashCode sha1) {
    return create(headers, target -> target.create(sourceFile, headers, size, sha1));
  }

  private Blob create(final Map<String, String> headers, final CreateBlobFunction createBlobFunction) {
    BlobStore result = fillPolicy.chooseBlobStore(this, headers);
    if (result == null) {
      throw new BlobStoreException("Unable to find a member Blob Store of '" + this + "' for create", null);
    }
    Blob blob = createBlobFunction.create(result);
    locatedBlobs.put(blob.getId(), result.getBlobStoreConfiguration().getName());
    return blob;
  }

  @Override
  public void createBlobAttributes(final BlobId blobId, final Map<String, String> headers, final BlobMetrics metrics) {
    locateBlobStore(blobId)
        .orElseThrow(() -> new BlobStoreException(
            "Unable to find a member Blob Store of '" + this + "' for create properties", null))
        .createBlobAttributes(blobId, headers, metrics);
  }

  @Override
  public BlobAttributes createBlobAttributesInstance(
      final BlobId blobId,
      final Map<String, String> headers,
      final BlobMetrics metrics)
  {
    BlobStore result = fillPolicy.chooseBlobStore(this, headers);
    if (result == null) {
      throw new BlobStoreException("Unable to find a member Blob Store of '" + this + "' for create properties", null);
    }
    return result.createBlobAttributesInstance(blobId, headers, metrics);
  }

  @Override
  @Guarded(by = STARTED)
  public Blob copy(final BlobId blobId, final Map<String, String> headers) {
    BlobStore target = locateBlobStore(blobId)
        .orElseThrow(() -> new BlobStoreException("Unable to find blob", blobId));
    Blob blob = target.copy(blobId, headers);
    locatedBlobs.put(blob.getId(), target.getBlobStoreConfiguration().getName());
    return blob;
  }

  @Override
  public boolean isInternalMoveSupported(final BlobStore destBlobStore) {
    return false;
  }

  @Guarded(by = STARTED)
  @Override
  public Blob makeBlobPermanent(final Blob blob, final Map<String, String> headers) {
    return locate(blob)
        .orElseThrow(() -> new BlobStoreException("Blob is not owned by a group member", blob.getId()))
        .makeBlobPermanent(blob, headers);
  }

  @Override
  @Guarded(by = STARTED)
  public boolean deleteIfTemp(final Blob blob) {
    return members.get()
        .stream()
        .anyMatch(blobstore -> blobstore.deleteIfTemp(blob));
  }

  @Override
  public Blob moveInternal(final BlobStore destBlobStore, final BlobId blobId, final Map<String, String> headers) {
    return null;
  }

  @Nullable
  @Override
  @Guarded(by = STARTED)
  @MonitoringBlobStoreMetrics(operationType = DOWNLOAD)
  public Blob get(final BlobId blobId) {
    return locateBlob(blobId, false).orElse(null);
  }

  @Nullable
  @Override
  @Guarded(by = STARTED)
  @MonitoringBlobStoreMetrics(operationType = DOWNLOAD)
  public Blob get(final BlobId blobId, final boolean includeDeleted) {
    return locateBlob(blobId, includeDeleted).orElse(null);
  }

  @Override
  @Guarded(by = STARTED)
  public boolean delete(final BlobId blobId, final String reason) {
    locatedBlobs.remove(blobId);
    List<BlobStore> locations = members.get()
        .stream()
        .filter(member -> member.exists(blobId))
        .collect(toList());

    if (!locations.isEmpty()) {
      return locations.stream()
          .allMatch(member -> member.delete(blobId, reason));
    }
    else {
      return false;
    }
  }

  @Override
  @Guarded(by = STARTED)
  public boolean deleteHard(final BlobId blobId) {
    locatedBlobs.remove(blobId);
    List<BlobStore> locations = members.get()
        .stream()
        .filter(member -> member.exists(blobId))
        .collect(toList());

    if (!locations.isEmpty()) {
      return locations.stream()
          .allMatch(member -> member.deleteHard(blobId));
    }
    else {
      return false;
    }
  }

  @Override
  @Guarded(by = STARTED)
  public BlobStoreMetricsService<BlobStoreGroup> getMetricsService() {
    throw new UnsupportedOperationException("metrics service is not available at a group level");
  }

  @Override
  @Guarded(by = STARTED)
  public BlobStoreMetrics getMetrics() {
    Iterable<BlobStoreMetrics> membersMetrics = members.get()
        .stream()
        .filter(BlobStore::isStarted)
        .map(BlobStore::getMetrics)::iterator;
    return new BlobStoreGroupMetrics(membersMetrics);
  }

  @Override
  public Map<OperationType, OperationMetrics> getOperationMetricsByType() {
    Map<OperationType, OperationMetrics> result = new EnumMap<>(OperationType.class);
    Iterable<Map<OperationType, OperationMetrics>> metrics = members.get()
        .stream()
        .map(BlobStore::getOperationMetricsByType)::iterator;
    for (Map<OperationType, OperationMetrics> metric : metrics) {
      for (Entry<OperationType, OperationMetrics> metricsEntry : metric.entrySet()) {
        OperationType type = metricsEntry.getKey();
        OperationMetrics operationMetrics = metricsEntry.getValue();
        OperationMetrics existingMetrics = result.get(type);
        if (existingMetrics != null) {
          OperationMetrics aggregatedMetrics = existingMetrics.add(operationMetrics);
          result.put(type, aggregatedMetrics);
        }
        else {
          result.put(type, operationMetrics);
        }
      }
    }
    return result;
  }

  @Override
  public Map<OperationType, OperationMetrics> getOperationMetricsDelta() {
    Map<OperationType, OperationMetrics> result = new EnumMap<>(OperationType.class);
    Iterable<Map<OperationType, OperationMetrics>> metrics = members.get()
        .stream()
        .map(BlobStore::getOperationMetricsDelta)::iterator;
    for (Map<OperationType, OperationMetrics> metric : metrics) {
      for (Entry<OperationType, OperationMetrics> metricsEntry : metric.entrySet()) {
        OperationType type = metricsEntry.getKey();
        OperationMetrics operationMetrics = metricsEntry.getValue();
        OperationMetrics existingMetrics = result.get(type);
        if (existingMetrics != null) {
          OperationMetrics aggregatedMetrics = existingMetrics.add(operationMetrics);
          result.put(type, aggregatedMetrics);
        }
        else {
          result.put(type, operationMetrics);
        }
      }
    }
    return result;
  }

  @Override
  public void clearOperationMetrics() {
    // noop invoke the method on the members
  }

  @Override
  @Guarded(by = STARTED)
  public synchronized void compact(
      @Nullable final BlobStoreUsageChecker inUseChecker,
      final java.time.Duration blobsBefore)
  {
    members.get().stream().forEach(member -> member.compact(inUseChecker, blobsBefore));
  }

  @Override
  @Guarded(by = STARTED)
  public synchronized void deleteTempFiles(@Nullable final Integer daysOlderThan) {
    members.get().forEach(member -> deleteTempFiles(daysOlderThan));
  }

  @Override
  public boolean undelete(
      @Nullable final BlobStoreUsageChecker inUseChecker,
      final BlobId blobId,
      final BlobAttributes attributes,
      final boolean isDryRun)
  {
    return members.get()
        .stream()
        .map(member -> member.undelete(inUseChecker, blobId, attributes, isDryRun))
        .anyMatch((final Boolean deleted) -> deleted);
  }

  @Override
  public boolean isStorageAvailable() {
    return true;
  }

  @Override
  public boolean isGroupable() {
    return false;
  }

  @Override
  public boolean isWritable() {
    return false;
  }

  @Override
  public boolean isEmpty() {
    return members.get().stream().map(BlobStore::isEmpty).reduce(true, Boolean::logicalAnd);
  }

  /**
   * Permanently stops this blob store regardless of the current state, disallowing restarts.
   */
  @Override
  @Transitions(to = SHUTDOWN)
  public void shutdown() throws Exception {
    if (isStarted()) {
      doStop();
    }
  }

  @Override
  public boolean exists(final BlobId blobId) {
    return members.get()
        .stream()
        .anyMatch(member -> member.exists(blobId));
  }

  @Override
  public boolean bytesExists(final BlobId blobId) {
    return members.get()
        .stream()
        .anyMatch(member -> member.bytesExists(blobId));
  }

  @Override
  @Guarded(by = {NEW, STOPPED, FAILED, SHUTDOWN})
  public void remove() {
    // no-op
  }

  @Override
  public Stream<BlobId> getBlobIdStream() {
    return members.get()
        .stream()
        .flatMap(BlobStore::getBlobIdStream);
  }

  @Override
  public Stream<BlobId> getBlobIdUpdatedSinceStream(
      final String prefix,
      final OffsetDateTime fromDateTime,
      final OffsetDateTime toDateTime)
  {
    throw new UnsupportedOperationException();
  }

  @Override
  public Stream<BlobId> getDirectPathBlobIdStream(final String prefix) {
    return members.get()
        .stream()
        .map(member -> member.getDirectPathBlobIdStream(prefix))
        .flatMap(identity());
  }

  @Nullable
  @Override
  public BlobAttributes getBlobAttributes(final BlobId blobId) {
    return locateBlobStore(blobId)
        .map(target -> target.getBlobAttributes(blobId))
        .orElse(null);
  }

  @Override
  public void setBlobAttributes(final BlobId blobId, final BlobAttributes blobAttributes) {
    locateBlobStore(blobId)
        .ifPresent(target -> target.setBlobAttributes(blobId, blobAttributes));
  }

  public List<BlobStore> getMembers() {
    return unmodifiableList(members.get());
  }

  /**
   * Supplier for thread-safe lazy initialization of members.
   */
  private class MembersSupplier
      implements Supplier<List<BlobStore>>
  {
    @Override
    public List<BlobStore> get() {
      List<BlobStore> memberList = new ArrayList<>();
      for (String name : BlobStoreGroupConfigurationHelper.memberNames(blobStoreConfiguration)) {
        BlobStore blobStore = blobStoreManager.get(name);
        if (blobStore == null) {
          throw new BlobStoreException("Blob Store '" + name + "' not found", null);
        }
        memberList.add(blobStore);
      }
      return synchronizedList(memberList);
    }
  }

  private Optional<BlobStore> locate(final BlobId blobId) {
    String blobStoreName = locatedBlobs.get(blobId);
    if (blobStoreName == null) {
      return Optional.empty();
    }
    BlobStore cached = blobStoreManager.get(blobStoreName);
    if (cached == null) {
      log.debug("Cached member {} for {} is no longer available, evicting", blobStoreName, blobId);
      locatedBlobs.remove(blobId);
      return Optional.empty();
    }
    return Optional.of(cached);
  }

  /**
   * Returns the cached member {@link BlobStore} for the given blob without any existence check.
   * Falls back to searching all members when there is no valid cache entry.
   * The result is cached if the owning member is writable.
   *
   * @return the owning member, or empty if not found in any member
   */
  @VisibleForTesting
  Optional<BlobStore> locateBlobStore(final BlobId blobId) {
    Optional<BlobStore> located = locate(blobId);
    if (located.isPresent()) {
      log.trace("{} location was cached as {}", blobId,
          located.get().getBlobStoreConfiguration().getName());
      return located;
    }
    BlobStore found = search(blobId);
    if (found != null && found.isWritable()) {
      log.trace("Caching {} in member {}", blobId, found.getBlobStoreConfiguration().getName());
      locatedBlobs.put(blobId, found.getBlobStoreConfiguration().getName());
    }
    return Optional.ofNullable(found);
  }

  /**
   * Retrieves the {@link Blob} for the given id, handling stale cache entries.
   * <p>
   * Uses the cache for a fast lookup first. If the cached member returns null for the blob
   * (e.g. after a move to another member), the stale entry is evicted, the remaining members
   * are searched, the cache is updated, and the blob is fetched from the new location.
   *
   * @param blobId the blob to retrieve
   * @param includeDeleted whether to include soft-deleted blobs
   * @return the blob, or empty if not found in any member
   */
  @VisibleForTesting
  Optional<Blob> locateBlob(final BlobId blobId, final boolean includeDeleted) {
    Optional<BlobStore> located = locate(blobId);
    if (located.isPresent()) {
      Blob blob = located.get().get(blobId, includeDeleted);
      if (blob != null) {
        log.trace("{} retrieved from cached member {}", blobId,
            located.get().getBlobStoreConfiguration().getName());
        return Optional.of(blob);
      }
      // get() returning null could mean soft-deleted; only evict if the blob is truly absent
      if (!located.get().exists(blobId)) {
        log.debug("Stale cache entry for {} in member {}, evicting and searching members", blobId,
            located.get().getBlobStoreConfiguration().getName());
        locatedBlobs.remove(blobId);
      }
      else {
        log.debug("Blob {} is soft-deleted in cached member {}, returning empty without cache eviction", blobId,
            located.get().getBlobStoreConfiguration().getName());
        return Optional.empty();
      }
    }
    BlobStore found = search(blobId);
    if (found == null) {
      return Optional.empty();
    }
    Blob blob = found.get(blobId, includeDeleted);
    if (blob != null && found.isWritable()) {
      log.trace("Caching {} in member {}", blobId, found.getBlobStoreConfiguration().getName());
      locatedBlobs.put(blobId, found.getBlobStoreConfiguration().getName());
    }
    return Optional.ofNullable(blob);
  }

  public void invalidateCachedLocation(final BlobId blobId) {
    if (locatedBlobs != null) {
      locatedBlobs.remove(blobId);
    }
  }

  @VisibleForTesting
  Optional<BlobStore> locate(final Blob blob) {
    return members.get()
        .stream()
        .filter(candidate -> candidate.isOwner(blob))
        .findAny();
  }

  private BlobStore search(final BlobId blobId) {
    log.trace("Searching for {} in {}", blobId, members);
    return members.get()
        .stream()
        .sorted(Comparator.comparing(BlobStore::isWritable).reversed())
        .filter(member -> member.exists(blobId))
        .findAny()
        .orElse(null);
  }

  @Override
  public String toString() {
    String name = blobStoreConfiguration != null ? blobStoreConfiguration.getName() : null;
    return getClass().getSimpleName() + "{" +
        "name='" + name + "'," +
        "members='" + members.get() + '\'' +
        '}';
  }

  @Override
  public Optional<ExternalMetadata> getExternalMetadata(final BlobRef blobRef) {
    return members.get()
        .stream()
        .filter(candidate -> candidate.getBlobStoreConfiguration().getName().equals(blobRef.getStore()))
        .findAny()
        .flatMap(store -> store.getExternalMetadata(blobRef));
  }

  /**
   * Functional interface for caller delegation of BlobStore creation
   *
   * @since 3.14
   */
  @FunctionalInterface
  private interface CreateBlobFunction
  {
    Blob create(BlobStore blobStore);
  }

  @Nullable
  @Override
  public BlobAttributes getBlobAttributesWithException(final BlobId blobId) throws BlobStoreException {
    try {
      return getBlobAttributes(blobId);
    }
    catch (Exception e) {
      log.error("Error occurred getting attributes for group blobstore", e);
      throw new BlobStoreException(e, blobId);
    }
  }

  @Override
  public boolean isOwner(final Blob blob) {
    return members.get()
        .stream()
        .anyMatch(member -> member.isOwner(blob));
  }
}
