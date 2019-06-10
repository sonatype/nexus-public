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
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

import javax.annotation.Nullable;
import javax.cache.Cache;
import javax.cache.configuration.MutableConfiguration;
import javax.cache.expiry.CreatedExpiryPolicy;
import javax.cache.expiry.Duration;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;

import org.sonatype.goodies.common.Time;
import org.sonatype.nexus.blobstore.api.Blob;
import org.sonatype.nexus.blobstore.api.BlobAttributes;
import org.sonatype.nexus.blobstore.api.BlobId;
import org.sonatype.nexus.blobstore.api.BlobStore;
import org.sonatype.nexus.blobstore.api.BlobStoreConfiguration;
import org.sonatype.nexus.blobstore.api.BlobStoreException;
import org.sonatype.nexus.blobstore.api.BlobStoreManager;
import org.sonatype.nexus.blobstore.api.BlobStoreMetrics;
import org.sonatype.nexus.blobstore.api.BlobStoreUsageChecker;
import org.sonatype.nexus.blobstore.group.internal.BlobStoreGroupMetrics;
import org.sonatype.nexus.blobstore.group.internal.WriteToFirstMemberFillPolicy;
import org.sonatype.nexus.cache.CacheHelper;
import org.sonatype.nexus.common.stateguard.Guarded;
import org.sonatype.nexus.common.stateguard.StateGuardLifecycleSupport;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.google.common.hash.HashCode;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.Collections.synchronizedList;
import static java.util.Collections.unmodifiableList;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toList;
import static org.sonatype.nexus.common.stateguard.StateGuardLifecycleSupport.State.FAILED;
import static org.sonatype.nexus.common.stateguard.StateGuardLifecycleSupport.State.NEW;
import static org.sonatype.nexus.common.stateguard.StateGuardLifecycleSupport.State.STARTED;
import static org.sonatype.nexus.common.stateguard.StateGuardLifecycleSupport.State.STOPPED;

/**
 * A {@link BlobStore} consisting of other blob stores.
 *
 * @since 3.14
 */
@Named(BlobStoreGroup.TYPE)
public class BlobStoreGroup
    extends StateGuardLifecycleSupport
    implements BlobStore
{
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

  private FillPolicy fillPolicy;

  private BlobStoreConfiguration blobStoreConfiguration;

  // cache of located blobs that have not been soft deleted
  private Cache<BlobId, String> locatedBlobs;

  @Inject
  public BlobStoreGroup(final BlobStoreManager blobStoreManager,
                        final Map<String, Provider<FillPolicy>> fillPolicyProviders,
                        final Provider<CacheHelper> cacheHelperProvider,
                        @Named("${nexus.blobstore.group.blobId.cache.timeToLive:-2d}") final Time blobIdCacheTimeout) {
    this.blobStoreManager = checkNotNull(blobStoreManager);
    this.fillPolicyProviders = checkNotNull(fillPolicyProviders);
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
            CreatedExpiryPolicy.factoryOf(new Duration(blobIdCacheTimeout.unit(), blobIdCacheTimeout.value()))
        )
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
  public Blob create(final InputStream blobData, final Map<String, String> headers) {
    return create(blobData, headers, null);
  }

  @Override
  @Guarded(by = STARTED)
  public Blob create(final InputStream blobData, final Map<String, String> headers, @Nullable final BlobId blobId) {
    return create(headers, target -> target.create(blobData, headers, blobId));
  }

  @Override
  @Guarded(by = STARTED)
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
  @Guarded(by = STARTED)
  public Blob copy(final BlobId blobId, final Map<String, String> headers) {
    BlobStore target = locate(blobId)
        .orElseThrow(() -> new BlobStoreException("Unable to find blob", blobId));
    Blob blob = target.copy(blobId, headers);
    locatedBlobs.put(blob.getId(), target.getBlobStoreConfiguration().getName());
    return blob;
  }

  @Nullable
  @Override
  @Guarded(by = STARTED)
  public Blob get(final BlobId blobId) {
    return locate(blobId)
        .map((BlobStore target) -> target.get(blobId))
        .orElse(null);
  }

  @Nullable
  @Override
  @Guarded(by = STARTED)
  public Blob get(final BlobId blobId, final boolean includeDeleted) {
    if (includeDeleted) {
      // check directly without using cache
      return members.get().stream()
          .map((BlobStore member) -> member.get(blobId, true))
          .filter(Objects::nonNull)
          .findAny()
          .orElse(null);
    }
    else {
      return locate(blobId)
        .map((BlobStore target) -> target.get(blobId, false))
        .orElse(null);
    }
  }

  @Override
  @Guarded(by = STARTED)
  public boolean delete(final BlobId blobId, final String reason) {
    locatedBlobs.remove(blobId);
    List<BlobStore> locations = members.get().stream()
        .filter((BlobStore member) -> member.exists(blobId))
        .collect(toList());

    if (!locations.isEmpty()) {
      return locations.stream()
          .allMatch((BlobStore member) -> member.delete(blobId, reason));
    }
    else {
      return false;
    }
  }

  @Override
  @Guarded(by = STARTED)
  public boolean deleteHard(final BlobId blobId) {
    locatedBlobs.remove(blobId);
    List<BlobStore> locations = members.get().stream()
        .filter((BlobStore member) -> member.exists(blobId))
        .collect(toList());

    if (!locations.isEmpty()) {
      return locations.stream()
          .allMatch((BlobStore member) -> member.deleteHard(blobId));
    }
    else {
      return false;
    }
  }

  @Override
  @Guarded(by = STARTED)
  public BlobStoreMetrics getMetrics() {
    Iterable<BlobStoreMetrics> membersMetrics = (Iterable<BlobStoreMetrics>) members.get().stream()
      .map((BlobStore member) -> member.getMetrics())
      ::iterator;
    return new BlobStoreGroupMetrics(membersMetrics);
  }

  @Override
  @Guarded(by = STARTED)
  public synchronized void compact() {
    members.get().stream().forEach((BlobStore member) -> member.compact());
  }

  @Override
  @Guarded(by = STARTED)
  public synchronized void compact(@Nullable final BlobStoreUsageChecker inUseChecker) {
    members.get().stream().forEach((BlobStore member) -> member.compact(inUseChecker));
  }

  @Override
  public boolean undelete(@Nullable final BlobStoreUsageChecker inUseChecker,
                          final BlobId blobId,
                          final BlobAttributes attributes,
                          final boolean isDryRun)
  {
    return members.get().stream()
        .map((BlobStore member) -> member.undelete(inUseChecker, blobId, attributes, isDryRun))
        .anyMatch((Boolean deleted) -> deleted);
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

  @Override
  public boolean exists(final BlobId blobId) {
    return members.get().stream()
        .anyMatch((BlobStore member) -> member.exists(blobId));
  }

  @Override
  @Guarded(by = {NEW, STOPPED, FAILED})
  public void remove() {
    // no-op
  }

  @Override
  public Stream<BlobId> getBlobIdStream() {
    return members.get().stream()
        .map((BlobStore member) -> member.getBlobIdStream())
        .flatMap(identity());
  }

  @Override
  public Stream<BlobId> getDirectPathBlobIdStream(final String prefix) {
    return members.get().stream()
        .map((BlobStore member) -> member.getDirectPathBlobIdStream(prefix))
        .flatMap(identity());
  }

  @Nullable
  @Override
  public BlobAttributes getBlobAttributes(final BlobId blobId) {
    return locate(blobId)
        .map((BlobStore target) -> target.getBlobAttributes(blobId))
        .orElse(null);
  }

  @Override
  public void setBlobAttributes(BlobId blobId, BlobAttributes blobAttributes) {
    locate(blobId)
        .ifPresent((BlobStore target) -> target.setBlobAttributes(blobId, blobAttributes));
  }

  public List<BlobStore> getMembers() {
    return unmodifiableList(members.get());
  }

  /**
   * Supplier for thread-safe lazy initialization of members.
   */
  private class MembersSupplier implements Supplier<List<BlobStore>> {
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

  @VisibleForTesting
  Optional<BlobStore> locate(final BlobId blobId) {
    String blobStoreName = locatedBlobs.get(blobId);
    if (blobStoreName != null) {
      log.trace("{} location was cached as {}", blobId, blobStoreName);
      return Optional.ofNullable(blobStoreManager.get(blobStoreName));
    }

    BlobStore blobStore = search(blobId);
    if (blobStore != null && blobStore.isWritable()) {
      String memberName = blobStore.getBlobStoreConfiguration().getName();
      log.trace("Caching {} in member {}", blobId, memberName);
      locatedBlobs.put(blobId, memberName);
    }

    return Optional.ofNullable(blobStore);
  }

  private BlobStore search(BlobId blobId) {
    log.trace("Searching for {} in {}", blobId, members);
    return members.get().stream()
      .sorted(Comparator.comparing(BlobStore::isWritable).reversed())
      .filter((BlobStore member) -> member.exists(blobId))
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

  /**
   * Functional interface for caller delegation of BlobStore creation
   *
   * @since 3.14
   */
  @FunctionalInterface
  private interface CreateBlobFunction {
    Blob create(BlobStore blobStore);
  }
}
