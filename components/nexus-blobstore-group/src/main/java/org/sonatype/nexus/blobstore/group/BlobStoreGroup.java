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
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;

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
import org.sonatype.nexus.blobstore.group.internal.FillPolicy;
import org.sonatype.nexus.blobstore.group.internal.WriteToFirstMemberFillPolicy;
import org.sonatype.nexus.common.stateguard.Guarded;
import org.sonatype.nexus.common.stateguard.StateGuardLifecycleSupport;

import com.google.common.hash.HashCode;
import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.Cache;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.Collections.synchronizedList;
import static java.util.Collections.unmodifiableList;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toList;
import static org.sonatype.nexus.blobstore.group.internal.BlobStoreGroupConfigurationHelper.memberNames;
import static org.sonatype.nexus.blobstore.group.internal.BlobStoreGroupConfigurationHelper.fillPolicyName;
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

  private final BlobStoreManager blobStoreManager;

  private final Map<String, Provider<FillPolicy>> fillPolicyProviders;

  private Supplier<List<BlobStore>> members;

  private FillPolicy fillPolicy;

  private BlobStoreConfiguration blobStoreConfiguration;

  // cache of located blobs that have not been soft deleted
  private Cache<BlobId, BlobStore> locatedBlobs;

  @Inject
  public BlobStoreGroup(final BlobStoreManager blobStoreManager,
                        final Map<String, Provider<FillPolicy>> fillPolicyProviders) {
    this.blobStoreManager = checkNotNull(blobStoreManager);
    this.fillPolicyProviders = checkNotNull(fillPolicyProviders);
  }

  @Override
  public void init(final BlobStoreConfiguration configuration) {
    this.blobStoreConfiguration = configuration;
    this.members = Suppliers.memoize(new MembersSupplier());
    String fillPolicyName = fillPolicyName(configuration);
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
    CacheBuilder<Object, Object> builder = CacheBuilder.newBuilder();
    locatedBlobs = builder.weakValues().build();
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
    return create(headers, target -> target.create(blobData, headers));
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
    locatedBlobs.put(blob.getId(), result);
    return blob;
  }

  @Override
  @Guarded(by = STARTED)
  public Blob copy(final BlobId blobId, final Map<String, String> headers) {
    BlobStore target = locate(blobId)
        .orElseThrow(() -> new BlobStoreException("Unable to find blob", blobId));
    Blob blob = target.copy(blobId, headers);
    locatedBlobs.put(blob.getId(), target);
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
    locatedBlobs.invalidate(blobId);
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
    locatedBlobs.invalidate(blobId);
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
  public boolean isWritable() {
    return true;
  }

  @Override
  public boolean isGroupable() {
    return false;
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
      for (String name : memberNames(blobStoreConfiguration)) {
        BlobStore blobStore = blobStoreManager.get(name);
        if (blobStore == null) {
          throw new BlobStoreException("Blob Store '" + name + "' not found", null);
        }
        memberList.add(blobStore);
      }
      return synchronizedList(memberList);
    }
  }

  private Optional<BlobStore> locate(BlobId blobId) {
    BlobStore blobStore = locatedBlobs.getIfPresent(blobId);
    if (blobStore == null) {
      blobStore = search(blobId);
      if (blobStore != null) {
        locatedBlobs.put(blobId, blobStore);
      }
    }
    else {
      log.trace("{} location was cached as {}", blobId, blobStore);
    }
    return Optional.ofNullable(blobStore);
  }

  private BlobStore search(BlobId blobId) {
    log.trace("Searching for {} in {}", blobId, members);
    return members.get().stream()
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
