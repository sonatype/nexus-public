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
package org.sonatype.nexus.repository.npm.internal;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Predicate;

import javax.cache.Cache;
import javax.cache.configuration.Factory;
import javax.cache.configuration.MutableConfiguration;
import javax.cache.expiry.CreatedExpiryPolicy;
import javax.cache.expiry.Duration;
import javax.cache.expiry.ExpiryPolicy;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.goodies.packageurl.PackageUrl;
import org.sonatype.nexus.blobstore.api.Blob;
import org.sonatype.nexus.blobstore.api.BlobRef;
import org.sonatype.nexus.cache.CacheHelper;
import org.sonatype.nexus.capability.CapabilityReference;
import org.sonatype.nexus.capability.CapabilityReferenceFilterBuilder;
import org.sonatype.nexus.capability.CapabilityReferenceFilterBuilder.CapabilityReferenceFilter;
import org.sonatype.nexus.capability.CapabilityRegistry;
import org.sonatype.nexus.capability.CapabilityType;
import org.sonatype.nexus.common.app.ManagedLifecycle;
import org.sonatype.nexus.common.event.EventManager;
import org.sonatype.nexus.common.stateguard.Guarded;
import org.sonatype.nexus.common.stateguard.StateGuardLifecycleSupport;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.firewall.event.ComponentVersionsRequest;
import org.sonatype.nexus.repository.storage.Asset;
import org.sonatype.nexus.repository.storage.StorageFacet;

import com.google.common.annotations.VisibleForTesting;

import static com.google.common.base.Preconditions.checkArgument;
import static java.lang.Boolean.TRUE;
import static java.util.Collections.emptyList;
import static java.util.concurrent.TimeUnit.SECONDS;
import static java.util.stream.Collectors.toList;
import static org.sonatype.nexus.common.app.ManagedLifecycle.Phase.SERVICES;
import static org.sonatype.nexus.common.stateguard.StateGuardLifecycleSupport.State.STARTED;
import static org.sonatype.nexus.repository.npm.internal.NpmFieldFactory.removeObjectFieldMatcher;
import static org.sonatype.nexus.repository.npm.internal.NpmFieldFactory.rewriteLatest;
import static org.sonatype.nexus.repository.npm.internal.NpmFormat.NAME;

/**
 *
 * @since 3.29
 */
@Named
@Singleton
@ManagedLifecycle(phase = SERVICES)
public class NonCatalogedVersionHelper
    extends StateGuardLifecycleSupport
{
  public static final String REMOVE_NON_CATALOGED_KEY = "removeNonCataloged";

  public static final String CLM_CAPABILITY_TYPE_ID = "firewall.audit";

  public static final String REPOSITORY_KEY = "repository";

  public static final String CACHE_NAME = "NPM_CATALOGED_VERSIONS";

  private static final Class<? extends ArrayList> LIST_CLASS = new ArrayList<String>().getClass();

  private final int limit;

  private final EventManager eventManager;

  private final int componentVersionsTimeout;

  private final CapabilityRegistry capabilityRegistry;

  private final CacheHelper cacheHelper;

  private Cache<String, List<String>> npmCatalogedVersions;

  private final long cacheDuration;

  @Inject
  public NonCatalogedVersionHelper(final EventManager eventManager,
                                   final CapabilityRegistry capabilityRegistry,
                                   final CacheHelper cacheHelper,
                                   @Named("${nexus.npm.firewall.check_last_limit:-5}") final int checkLastLimit,
                                   @Named("${nexus.npm.firewall.component_versions_timeout:-10}")
                                   final int componentVersionsTimeout,
                                   @Named("${nexus.npm.firewall.component_versions_cache_duration:-72}")
                                   final long cacheDuration)
  {
    this.eventManager = eventManager;
    this.capabilityRegistry = capabilityRegistry;
    this.cacheHelper = cacheHelper;
    checkArgument(cacheDuration > 0, "nexus.npm.firewall.component_versions_cache_duration must be > 0. Value was %s", checkLastLimit);
    this.cacheDuration = cacheDuration;
    checkArgument(checkLastLimit > 0, "nexus.npm.firewall.check_last_limit must be > 0. Value was %s", checkLastLimit);
    this.limit = checkLastLimit;
    checkArgument(componentVersionsTimeout > 0, "nexus.npm.firewall.component_versions_timeout must be > 0. Value was %s", componentVersionsTimeout);
    this.componentVersionsTimeout = componentVersionsTimeout;
  }

  @Override
  protected void doStart() {
    maybeCreateCache();
  }

  private void maybeCreateCache() {
    Duration duration = new Duration(TimeUnit.HOURS, cacheDuration);
    Factory<ExpiryPolicy> expiryPolicyFactory = CreatedExpiryPolicy.factoryOf(duration);
    MutableConfiguration<String, List<String>> config = new MutableConfiguration<String, List<String>>()
        .setStoreByValue(false)
        .setExpiryPolicyFactory(expiryPolicyFactory)
        .setManagementEnabled(true)
        .setStatisticsEnabled(true);
    npmCatalogedVersions = cacheHelper.maybeCreateCache(CACHE_NAME, config);
  }

  @Override
  protected void doStop() {
    maybeDestroyCache();
  }

  private void maybeDestroyCache() {
    if(npmCatalogedVersions != null) {
      cacheHelper.maybeDestroyCache(CACHE_NAME);
      npmCatalogedVersions = null;
    }
  }

  public void clearCache() {
    maybeDestroyCache();
    maybeCreateCache();
  }

  @Guarded(by = STARTED)
  public void maybeAddExcludedVersionsFieldMatchers(final List<NpmFieldMatcher> fieldMatchers,
                                                    final Asset packageRootAsset,
                                                    final Repository repository)
  {
    Boolean removeNonCatalogedVersions = repository.getConfiguration()
        .attributes(NAME)
        .get(REMOVE_NON_CATALOGED_KEY, Boolean.class);
    if (!TRUE.equals(removeNonCatalogedVersions) || !isFirewallEnabled(repository)) {
      return;
    }
    try {
      BlobRef blobRef = packageRootAsset.blobRef();
      if (blobRef != null) {
        Blob blob = repository.facet(StorageFacet.class).blobStore().get(blobRef.getBlobId());
        if (blob != null) {
          List<String> versions = MetadataVersionParser.readVersions(blob.getInputStream());
          List<String> nonCatalogedVersions = nonCatalogedVersions(packageRootAsset.name(), last(limit, versions));
          List<NpmFieldMatcher> excludeVersions = nonCatalogedVersions.stream()
              .map(v -> removeObjectFieldMatcher(v, "/versions/" + v))
              .collect(toList());
          maybeRewriteLatest(excludeVersions, nonCatalogedVersions, versions);
          fieldMatchers.addAll(excludeVersions);
        }
      }
    }
    catch (Exception e) {
      log.warn("Error removing non-cataloged versions from metadata");
      log.debug("Error removing non-cataloged versions from metadata", e);
    }
  }

  private boolean isFirewallEnabled(final Repository repository) {
    CapabilityReferenceFilter capabilityReferenceFilter = CapabilityReferenceFilterBuilder.capabilities()
        .withType(new CapabilityType(CLM_CAPABILITY_TYPE_ID))
        .withProperty(REPOSITORY_KEY, repository.getName())
        .enabled();
    Collection<? extends CapabilityReference> capabilityReferences = capabilityRegistry.get(capabilityReferenceFilter);
    return !capabilityReferences.isEmpty();
  }

  @VisibleForTesting
  static List<String> last(final int n, final List<String> versions) {
    if (versions.isEmpty()) {
      return emptyList();
    }
    return versions.subList(versions.size() > n ? versions.size() - n : 0, versions.size());
  }

  private void maybeRewriteLatest(final List<NpmFieldMatcher> excludeVersions,
                                  final List<String> nonCatalogedVersions,
                                  final List<String> allVersions)
  {
    if (excludeVersions.size() > 0) {
      excludeVersions.add(rewriteLatest(nonCatalogedVersions, allVersions));
    }
  }

  private List<String> nonCatalogedVersions(final String packageName, final List<String> versions) {
    try {
      PackageNameParser parser = new PackageNameParser(packageName);
      PackageUrl packageUrl = PackageUrl.builder().type(NAME).namespace(parser.namespace).name(parser.name).build();
      List<String> cachedVersions = npmCatalogedVersions.get(packageUrl.toString());
      if (cachedVersions != null && versions.stream().noneMatch(notCataloged(cachedVersions))) {
        return emptyList();
      }
      final ComponentVersionsRequest componentVersionsRequest = new ComponentVersionsRequest(packageUrl);
      eventManager.post(componentVersionsRequest);
      final List<String> catalogedVersions = componentVersionsRequest.getResult().get(componentVersionsTimeout, SECONDS);
      npmCatalogedVersions.put(packageUrl.toString(), catalogedVersions);
      List<String> nonCatalogedVersions = versions.stream().filter(notCataloged(catalogedVersions)).collect(toList());
      if (noneAreCataloged(versions, nonCatalogedVersions)) {
        return emptyList();
      }
      return nonCatalogedVersions;
    }
    catch (InterruptedException | TimeoutException | ExecutionException e) {
      log.error("Error getting non cataloged versions for {}", packageName, e);
      throw new RuntimeException(e);
    }
  }

  private boolean noneAreCataloged(final List<String> versions, final List<String> nonCatalogedVersions) {
    return versions.size() == nonCatalogedVersions.size();
  }

  private Predicate<String> notCataloged(final List<String> catalogedVersions) {
    return version -> !catalogedVersions.contains(version);
  }

  private static class PackageNameParser
  {
    final String namespace;

    final String name;

    public PackageNameParser(final String packageName) {
      String[] namespaceAndName = packageName.split("/", 2);
      namespace = namespaceAndName.length == 2 ? namespaceAndName[0] : null;
      name = namespaceAndName.length == 2 ? namespaceAndName[1] : namespaceAndName[0];
    }
  }
}
