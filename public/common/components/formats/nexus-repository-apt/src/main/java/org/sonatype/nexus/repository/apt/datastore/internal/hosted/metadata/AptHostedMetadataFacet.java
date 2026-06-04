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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;

import org.sonatype.nexus.common.entity.Continuation;
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
import static org.sonatype.nexus.repository.apt.internal.ReleaseName.INRELEASE;
import static org.sonatype.nexus.repository.apt.internal.ReleaseName.RELEASE;
import static org.sonatype.nexus.repository.apt.internal.ReleaseName.RELEASE_GPG;

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

    StringBuilder sha256Builder = new StringBuilder();
    StringBuilder md5Builder = new StringBuilder();
    String releaseFile = null;
    Set<String> currentArchitectures = null;
    String distribution = aptFacet.getDistribution();
    try (CompressingTempFileStore store = buildPackageIndexes()) {
      for (Map.Entry<String, CompressingTempFileStore.FileMetadata> entry : store.getFiles().entrySet()) {
        storePackageIndexFiles(aptFacet, distribution, DEFAULT_COMPONENT, entry.getKey(), entry.getValue(), md5Builder,
            sha256Builder);
      }
      if (!store.getFiles().isEmpty()) {
        currentArchitectures = store.getFiles().keySet();
        releaseFile = buildReleaseFile(
            distribution,
            Collections.singletonList(DEFAULT_COMPONENT),
            currentArchitectures,
            md5Builder.toString(),
            sha256Builder.toString());
      }
      else {
        // No packages in repository - remove all metadata
        log.info("No packages found in repository: {} - removing all metadata", getRepository().getName());
        removeAllMetadata();
      }

    }
    FluentAsset releaseFileAsset =
        generateReleaseFiles(distribution, releaseFile, aptFacet, signingFacet);

    // Clean up stale architecture metadata after successful rebuild
    if (currentArchitectures != null && !currentArchitectures.isEmpty()) {
      removeStaleArchitectureMetadata(currentArchitectures);
    }

    OffsetDateTime finishTime = clock.clusterTime();
    log.debug("Completed metadata rebuild in {} ms",
        finishTime.toInstant().toEpochMilli() - rebuildStart.toInstant().toEpochMilli());
    return Optional.ofNullable(releaseFileAsset).map(FluentAsset::download).orElse(null);
  }

  private CompressingTempFileStore buildPackageIndexes() throws IOException {
    CompressingTempFileStore result = new CompressingTempFileStore();
    Map<String, Writer> writersByArch = new HashMap<>();
    boolean ok = false;
    try {
      final List<Map<String, Object>> allPackagesMetadata = keyValue()
          .browsePackagesMetadata()
          .map(this::deserialize)
          .toList();

      // Group packages by architecture only - we need to preserve all versions
      Map<String, List<Map<String, Object>>> packagesByArch = new HashMap<>();
      for (Map<String, Object> pkg : allPackagesMetadata) {
        Object architectureObj = pkg.get(P_ARCHITECTURE);
        if (architectureObj == null) {
          log.warn("Skipping package with missing architecture: {}", pkg);
          continue;
        }
        String architecture = architectureObj.toString();
        packagesByArch
            .computeIfAbsent(architecture, k -> new ArrayList<>())
            .add(pkg);
      }

      int totalPackages = packagesByArch.values()
          .stream()
          .mapToInt(List::size)
          .sum();
      log.debug("Building package indexes: {} architectures, {} packages from {} KV entries",
          packagesByArch.size(), totalPackages, allPackagesMetadata.size());

      // Write package indexes for each architecture
      for (Map.Entry<String, List<Map<String, Object>>> archEntry : packagesByArch.entrySet()) {
        String architecture = archEntry.getKey();
        List<Map<String, Object>> packagesForArch = archEntry.getValue();
        Writer outWriter = writersByArch.computeIfAbsent(architecture, result::openOutput);

        for (Map<String, Object> pkg : packagesForArch) {
          final String indexSection = (String) pkg.get(P_INDEX_SECTION);
          outWriter.write(indexSection);
          outWriter.write("\n\n");
        }
      }
      ok = true;
    }
    finally {
      for (Writer writer : writersByArch.values()) {
        IOUtils.closeQuietly(writer, null);
      }

      if (!ok) {
        result.close();
      }
    }
    return result;
  }

  /**
   * Removes all metadata files (Packages indexes and Release files).
   * This is called before rebuilding to ensure stale architectures are cleaned up.
   */
  private void removeAllMetadata() {
    log.debug("Removing all metadata files for repository: {}", getRepository().getName());
    AptContentFacet aptFacet = content();

    // Remove all Package index files (binary-*/Packages*)
    try {
      aptFacet.deleteAssetsByPrefix(normalizeAssetPath(buildMainBinaryPrefix()));
    }
    catch (Exception e) {
      log.warn("Failed to delete package index files", e);
    }

    // Remove Release files
    String dist = aptFacet.getDistribution();
    deleteReleaseFile(aptFacet, dist, RELEASE);
    deleteReleaseFile(aptFacet, dist, INRELEASE);
    deleteReleaseFile(aptFacet, dist, RELEASE_GPG);
  }

  private void deleteReleaseFile(final AptContentFacet aptFacet, final String dist, final String fileName) {
    try {
      aptFacet.assets()
          .path(normalizeAssetPath("dists/" + dist + "/" + fileName))
          .find()
          .ifPresent(FluentAsset::delete);
    }
    catch (Exception e) {
      log.warn("Failed to delete release file: {}", fileName, e);
    }
  }

  /**
   * Extracts the architecture from a Package index asset path.
   * Path format: /dists/{distribution}/main/binary-{arch}/Packages{extension}
   *
   * @param path the asset path
   * @param distribution the distribution name
   * @return the architecture or null if path doesn't match expected format
   */
  private String extractArchitectureFromPath(final String path, final String distribution) {
    String pattern = "/dists/" + distribution + "/main/binary-";
    if (path.startsWith(pattern)) {
      String remainder = path.substring(pattern.length());
      int slashIndex = remainder.indexOf('/');
      if (slashIndex > 0) {
        return remainder.substring(0, slashIndex);
      }
    }
    return null;
  }

  /**
   * Builds the path prefix for main binary package index files.
   *
   * @return the path prefix (e.g., "dists/jammy/main/binary-")
   */
  private String buildMainBinaryPrefix() {
    return String.format("dists/%s/main/binary-", content().getDistribution());
  }

  private void removeStaleArchitectureMetadata(final Collection<String> currentArchitectures) {
    log.debug("Checking for stale architecture metadata in repository: {}", getRepository().getName());

    AptContentFacet aptFacet = content();
    String dist = aptFacet.getDistribution();
    String pathPattern = "/dists/" + dist + "/main/binary-%";

    // Browse all Package index assets using DAO and extract unique architectures
    // Note: We use a high limit (1000) because realistically there are only ~3-15 architectures
    // with 3 files each (Packages, Packages.gz, Packages.bz2), so ~9-45 files total
    AptAssetStore aptAssetStore = (AptAssetStore) ((AptContentFacetImpl) aptFacet).stores().assetStore;
    Continuation<Asset> assets = aptAssetStore.browsePackageIndexAssets(
        aptFacet.contentRepositoryId(),
        1000,
        null,
        pathPattern);

    Set<String> storedArchitectures = assets.stream()
        .map(asset -> extractArchitectureFromPath(asset.path(), dist))
        .filter(Objects::nonNull)
        .collect(Collectors.toSet());

    // Delete Package index files for architectures not in current set
    for (String storedArch : storedArchitectures) {
      if (!currentArchitectures.contains(storedArch)) {
        log.info("Removing stale Package index metadata for architecture '{}' in repository: {}",
            storedArch, getRepository().getName());
        aptFacet.deleteAssetsByPrefix(normalizeAssetPath("dists/" + dist + "/main/binary-" + storedArch + "/"));
      }
    }
  }

  private AptSigningFacet signing() {
    return facet(AptSigningFacet.class);
  }
}
