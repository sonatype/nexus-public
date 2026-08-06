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
package org.sonatype.nexus.repository.apt.datastore.internal.hosted.metadata;

import java.io.IOException;
import java.io.Writer;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;

import org.sonatype.nexus.common.entity.Continuation;
import org.sonatype.nexus.common.entity.Continuations;
import org.sonatype.nexus.common.cooperation2.Cooperation2Factory;
import org.sonatype.nexus.common.time.Clock;
import org.sonatype.nexus.repository.Facet.Exposed;
import org.sonatype.nexus.repository.apt.AptFormat;
import org.sonatype.nexus.repository.apt.datastore.AptContentFacet;
import org.sonatype.nexus.repository.apt.datastore.internal.AptContentFacetImpl;
import org.sonatype.nexus.repository.apt.datastore.internal.metadata.AptMetadataFacetSupport;
import org.sonatype.nexus.repository.apt.datastore.internal.store.AptAssetStore;
import org.sonatype.nexus.repository.apt.internal.gpg.AptSigningFacet;
import org.sonatype.nexus.repository.apt.internal.hosted.CompressingTempFileStore;
import org.sonatype.nexus.repository.apt.internal.hosted.CompressingTempFileStore.DistComponentArchKey;
import org.sonatype.nexus.repository.apt.internal.hosted.CompressingTempFileStore.FileMetadata;
import org.sonatype.nexus.repository.content.Asset;
import org.sonatype.nexus.repository.content.fluent.FluentAsset;
import org.sonatype.nexus.repository.view.Content;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import static org.sonatype.nexus.repository.apt.internal.AptFacetHelper.normalizeAssetPath;
import static org.sonatype.nexus.repository.apt.internal.AptProperties.P_ARCHITECTURE;
import static org.sonatype.nexus.repository.apt.internal.AptProperties.P_INDEX_SECTION;
import static org.sonatype.nexus.repository.apt.internal.AptProperties.P_PACKAGE_NAME;
import static org.sonatype.nexus.repository.apt.internal.AptProperties.P_DISTRIBUTION;
import static org.sonatype.nexus.repository.apt.internal.AptProperties.P_COMPONENT;
import static org.sonatype.nexus.repository.apt.internal.AptProperties.P_PACKAGE_VERSION;

/**
 * Apt metadata facet. Holds the logic for metadata recalculation.
 */
@Component
@Qualifier(AptFormat.NAME)
@Exposed
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class AptHostedMetadataFacet
    extends AptMetadataFacetSupport
{
  @Autowired
  public AptHostedMetadataFacet(
      final ObjectMapper mapper,
      final Clock clock,
      final Cooperation2Factory cooperationFactory,
      @Value("${nexus.apt.metadata.cooperation.enabled:true}") final boolean cooperationEnabled,
      @Value("${nexus.apt.metadata.cooperation.majorTimeout:0s}") final Duration majorTimeout,
      @Value("${nexus.apt.metadata.cooperation.minorTimeout:30s}") final Duration minorTimeout,
      @Value("${nexus.apt.metadata.cooperation.threadsPerKey:100}") final int threadsPerKey)
  {
    super(mapper, clock, cooperationFactory, cooperationEnabled, majorTimeout, minorTimeout, threadsPerKey);
  }

  @Override
  protected Content doRebuildMetadata() throws IOException {
    log.debug("Starting rebuilding metadata at {}", getRepository().getName());
    OffsetDateTime rebuildStart = clock.clusterTime();

    AptContentFacet aptFacet = content();
    AptSigningFacet signingFacet = signing();

    String releaseFile = null;
    FluentAsset releaseFileAsset = null;
    try (CompressingTempFileStore store = buildPackageIndexes(aptFacet)) {

      // Loop on each discovered distributions, components & architectures.
      Map<String, Map<String, Map<String, FileMetadata>>> pkgIndexes = store.getFiles();
      for (Entry<String, Map<String, Map<String, FileMetadata>>> distEntry : pkgIndexes.entrySet()) {
        final String distribution = distEntry.getKey();
        Set<String> currentArchitectures = new HashSet<>();

        // Create package index per architecture.
        StringBuilder sha256Builder = new StringBuilder();
        StringBuilder md5Builder = new StringBuilder();
        for (Entry<String, Map<String, FileMetadata>> componentEntry : distEntry.getValue().entrySet()) {

          String component = componentEntry.getKey();
          for (Entry<String, FileMetadata> archEntry : componentEntry.getValue().entrySet()) {
            storePackageIndexFiles(aptFacet, distribution, component, archEntry.getKey(), archEntry.getValue(), md5Builder, sha256Builder);
          }

          // Collect valid architectures
          currentArchitectures.addAll(componentEntry.getValue().keySet());
          
        }
        
        Set<String> currentComponents = distEntry.getValue().keySet();
        if (!currentArchitectures.isEmpty()) {
          releaseFile = buildReleaseFile(
              distribution,
              currentComponents,
              currentArchitectures,
              md5Builder.toString(),
              sha256Builder.toString());
          releaseFileAsset = generateReleaseFiles(distribution, releaseFile, aptFacet, signingFacet);
        } 

      }

      // Single cleanup pass: anything under dists/ untouched by this rebuild is stale.
      removeStaleMetadata(rebuildStart);

    }

    OffsetDateTime finishTime = clock.clusterTime();
    log.debug("Completed metadata rebuild in {} ms",
        finishTime.toInstant().toEpochMilli() - rebuildStart.toInstant().toEpochMilli());
    return Optional.ofNullable(releaseFileAsset).map(FluentAsset::download).orElse(null);
  }

  private CompressingTempFileStore buildPackageIndexes(AptContentFacet aptFacet) throws IOException {
    CompressingTempFileStore result = new CompressingTempFileStore();
    Map<DistComponentArchKey, Writer> writersMap = new HashMap<>();
    boolean ok = false;
    try {
      final List<Map<String, Object>> allPackagesMetadata = keyValue()
          .browsePackagesMetadata()
          .map(this::deserialize)
          .toList();

      // Single-pass deduplication and grouping by architecture
      // Deduplicate by (distribution, component, architecture, package name, version)
      // to handle KV store duplicate entries
      Map<DistComponentArchKey, Set<String>> packageNameSeen = new HashMap<>();
      for (Map<String, Object> pkg : allPackagesMetadata) {
        Object distributionObj = pkg.get(P_DISTRIBUTION);
        Object componentObj = pkg.get(P_COMPONENT);
        Object packageNameObj = pkg.get(P_PACKAGE_NAME);
        Object architectureObj = pkg.get(P_ARCHITECTURE);
        Object versionObj = pkg.get(P_PACKAGE_VERSION);
        if (architectureObj == null) {
          log.warn("Skipping package with missing architecture: {}", pkg);
          continue;
        }

        // Handle package in multiple distro
        String distributions = distributionObj != null ? distributionObj.toString() : aptFacet.getDistribution();
        for (String distribution : distributions.split(",")) {
          distribution = distribution.trim();

          // When component is not defined, default to "main" for backward compatibility
          String component = componentObj != null ? componentObj.toString() : "main";
          String architecture = architectureObj.toString();
          String packageName = packageNameObj.toString();
          String version = versionObj != null ? versionObj.toString() : "";

          // Check if duplicate — now based on name + version, not just name
          DistComponentArchKey key = new DistComponentArchKey(distribution, component, architecture);
          String nameVersionKey = packageName + "_" + version;
          if (packageNameSeen.computeIfAbsent(key, k -> new HashSet<>()).contains(nameVersionKey)) {
            log.warn("Skipping duplicate package name/version: {}", pkg);
            continue;
          }
          packageNameSeen.get(key).add(nameVersionKey);

          // Write package details to Package Indexes
          Writer outWriter = writersMap.computeIfAbsent(key, result::openOutput);
          final String indexSection = (String) pkg.get(P_INDEX_SECTION);
          outWriter.write(indexSection);
          outWriter.write("\n\n");
        }
      }
      ok = true;
    }
    finally {
      for (Writer writer : writersMap.values()) {
        IOUtils.closeQuietly(writer, null);
      }

      if (!ok) {
        result.close();
      }
    }
    return result;
  }

  /**
   * Removes any metadata assets under dists/ that were not touched during this rebuild
   * (i.e. their lastUpdated timestamp is older than the rebuild start time).
   */
  private void removeStaleMetadata(final OffsetDateTime removeOlderThan) {
    log.debug("Checking for stale metadata older than {} in repository: {}",
        removeOlderThan, getRepository().getName());

    AptContentFacet aptFacet = content();
    AptAssetStore aptAssetStore = (AptAssetStore) ((AptContentFacetImpl) aptFacet).stores().assetStore;

    int deletedCount = 0;
    int totalAssets = 0;

    Continuation<Asset> assets = aptAssetStore.browsePackageIndexAssets(
        aptFacet.contentRepositoryId(),
        Continuations.BROWSE_LIMIT,
        null,
        "/dists/%");

    while (!assets.isEmpty()) {
      for (Asset asset : assets) {
        totalAssets++;
        if (asset.lastUpdated().isBefore(removeOlderThan)) {
          log.info("Removing stale metadata asset '{}' (last updated {}) in repository: {}",
              asset.path(), asset.lastUpdated(), getRepository().getName());
          aptAssetStore.deleteAsset(asset);
          deletedCount++;
        }
        else {
          log.debug("Asset '{}' is current (last updated {}) - skipping", asset.path(), asset.lastUpdated());
        }
      }

      assets = aptAssetStore.browsePackageIndexAssets(
          aptFacet.contentRepositoryId(),
          Continuations.BROWSE_LIMIT,
          assets.nextContinuationToken(),
          "/dists/%");
    }

    log.debug("Stale metadata cleanup complete: {} of {} assets removed in repository: {}",
        deletedCount, totalAssets, getRepository().getName());
  }

  private AptSigningFacet signing() {
    return facet(AptSigningFacet.class);
  }
}
