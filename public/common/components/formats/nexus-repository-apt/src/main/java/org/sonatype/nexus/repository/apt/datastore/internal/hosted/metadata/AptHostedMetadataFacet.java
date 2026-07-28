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
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import java.util.TimeZone;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.sonatype.nexus.common.entity.Continuation;
import org.sonatype.nexus.common.cooperation2.Cooperation2;
import org.sonatype.nexus.common.cooperation2.Cooperation2Factory;
import org.sonatype.nexus.common.hash.HashAlgorithm;
import org.sonatype.nexus.common.time.Clock;
import org.sonatype.nexus.repository.Facet.Exposed;
import org.sonatype.nexus.repository.FacetSupport;
import org.sonatype.nexus.repository.apt.AptFormat;
import org.sonatype.nexus.repository.apt.datastore.AptContentFacet;
import org.sonatype.nexus.repository.apt.datastore.internal.AptContentFacetImpl;
import org.sonatype.nexus.repository.apt.datastore.internal.data.AptKeyValueFacet;
import org.sonatype.nexus.repository.apt.datastore.internal.store.AptAssetStore;
import org.sonatype.nexus.repository.apt.internal.AptMimeTypes;
import org.sonatype.nexus.repository.apt.internal.debian.ControlFile;
import org.sonatype.nexus.repository.apt.internal.debian.ControlFile.Paragraph;
import org.sonatype.nexus.repository.apt.internal.gpg.AptSigningFacet;
import org.sonatype.nexus.repository.apt.internal.hosted.CompressingTempFileStore;
import org.sonatype.nexus.repository.apt.internal.hosted.CompressingTempFileStore.DistComponentArchKey;
import org.sonatype.nexus.repository.apt.internal.hosted.CompressingTempFileStore.FileMetadata;
import org.sonatype.nexus.repository.config.Configuration;
import org.sonatype.nexus.repository.content.Asset;
import org.sonatype.nexus.repository.content.AssetBlob;
import org.sonatype.nexus.repository.content.fluent.FluentAsset;
import org.sonatype.nexus.repository.content.store.InternalIds;
import org.sonatype.nexus.repository.content.utils.FormatAttributesUtils;
import org.sonatype.nexus.repository.view.Content;
import org.sonatype.nexus.repository.view.payloads.BytesPayload;
import org.sonatype.nexus.repository.view.payloads.StreamPayload;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.apache.http.protocol.HttpDateGenerator.PATTERN_RFC1123;
import static org.sonatype.nexus.common.hash.HashAlgorithm.MD5;
import static org.sonatype.nexus.common.hash.HashAlgorithm.SHA256;
import static org.sonatype.nexus.repository.apt.internal.AptFacetHelper.normalizeAssetPath;
import static org.sonatype.nexus.repository.apt.internal.AptProperties.BZ2;
import static org.sonatype.nexus.repository.apt.internal.AptProperties.GZ;
import static org.sonatype.nexus.repository.apt.internal.AptProperties.P_ARCHITECTURE;
import static org.sonatype.nexus.repository.apt.internal.AptProperties.P_INDEX_SECTION;
import static org.sonatype.nexus.repository.apt.internal.AptProperties.P_PACKAGE_NAME;
import static org.sonatype.nexus.repository.apt.internal.AptProperties.P_DISTRIBUTION;
import static org.sonatype.nexus.repository.apt.internal.AptProperties.P_COMPONENT;
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
    extends FacetSupport
{

  private final ObjectMapper mapper;

  private final Clock clock;

  private final Cooperation2Factory.Builder cooperationBuilder;

  private Cooperation2 cooperation;

  @Inject
  public AptHostedMetadataFacet(
      final ObjectMapper mapper,
      final Clock clock,
      final Cooperation2Factory cooperationFactory,
      @Value("${nexus.apt.metadata.cooperation.enabled:true}") final boolean cooperationEnabled,
      @Value("${nexus.apt.metadata.cooperation.majorTimeout:0s}") final Duration majorTimeout,
      @Value("${nexus.apt.metadata.cooperation.minorTimeout:30s}") final Duration minorTimeout,
      @Value("${nexus.apt.metadata.cooperation.threadsPerKey:100}") final int threadsPerKey)
  {
    this.mapper = checkNotNull(mapper);
    this.clock = checkNotNull(clock);
    this.cooperationBuilder = checkNotNull(cooperationFactory).configure()
        .enabled(cooperationEnabled)
        .majorTimeout(majorTimeout)
        .minorTimeout(minorTimeout)
        .threadsPerKey(threadsPerKey);
  }

  @Override
  protected void doInit(final Configuration configuration) throws Exception {
    super.doInit(configuration);
    this.cooperation = cooperationBuilder.build(getRepository().getName() + ":repomd");
  }

  public void addPackageMetadata(final FluentAsset asset) {
    checkNotNull(asset);
    log.debug("Storing metadata for repository: {} asset: {}", getRepository().getName(), asset.path());
    componentId(asset).ifPresent(
        componentId -> data().addPackageMetadata(componentId, InternalIds.internalAssetId(asset), serialize(asset)));
  }

  public void removePackageMetadata(final FluentAsset asset) {
    checkNotNull(asset);
    log.debug("Removing metadata for repository: {} asset: {}", getRepository().getName(), asset.path());
    OptionalInt compId = componentId(asset);
    if (compId.isPresent()) {
      int componentId = compId.getAsInt();
      int assetId = InternalIds.internalAssetId(asset);
      log.debug("Removing package metadata from KV store for component: {} asset: {}", componentId, assetId);
      data().removePackageMetadata(componentId, assetId);
    }
    else {
      log.warn("Cannot remove package metadata: asset {} has no component ID", asset.path());
    }
  }

  public Optional<Content> rebuildMetadata() throws IOException {
    return Optional.ofNullable(
        cooperation.on(this::doRebuildMetadata)
            .cooperate("rebuild"));
  }

  private Content doRebuildMetadata() throws IOException {
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
          
          // Clean up stale architecture metadata after successful rebuild
          removeStaleArchitectureMetadata(distribution, component, componentEntry.getValue().keySet());
          
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

        // If the store is empty, then delete all metadata
        removeStaleComponentMetadata(distribution, currentComponents);

      }

      // If the store is empty, then delete all metadata
      removeStaleDistributionMetadata(pkgIndexes.keySet());

    }

    OffsetDateTime finishTime = clock.clusterTime();
    log.debug("Completed metadata rebuild in {} ms",
        finishTime.toInstant().toEpochMilli() - rebuildStart.toInstant().toEpochMilli());
    return Optional.ofNullable(releaseFileAsset).map(FluentAsset::download).orElse(null);
  }

  private FluentAsset generateReleaseFiles(
      final String dist,
      final String releaseFile,
      final AptContentFacet aptFacet,
      final AptSigningFacet signingFacet) throws IOException
  {
    FluentAsset releaseFileAsset = null;
    if (releaseFile != null) {
      releaseFileAsset = aptFacet.put(
          releaseIndexName(dist, RELEASE),
          new BytesPayload(releaseFile.getBytes(StandardCharsets.UTF_8), AptMimeTypes.TEXT));

      aptFacet.put(
          releaseIndexName(dist, INRELEASE),
          new BytesPayload(signingFacet.signInline(releaseFile), AptMimeTypes.TEXT));

      aptFacet.put(
          releaseIndexName(dist, RELEASE_GPG),
          new BytesPayload(signingFacet.signExternal(releaseFile), AptMimeTypes.SIGNATURE));
    }
    return releaseFileAsset;
  }

  private CompressingTempFileStore buildPackageIndexes(AptContentFacet aptFacet) throws IOException {
    CompressingTempFileStore result = new CompressingTempFileStore();
    Map<DistComponentArchKey, Writer> writersMap = new HashMap<>();
    boolean ok = false;
    try {
      final List<Map<String, Object>> allPackagesMetadata = data()
          .browsePackagesMetadata()
          .map(this::deserialize)
          .toList();

      // Single-pass deduplication and grouping by architecture
      // Deduplicate by (architecture, package name) to handle KV store duplicate entries
      Map<DistComponentArchKey, Set<String>> packageNameSeen = new HashMap<>();
      for (Map<String, Object> pkg : allPackagesMetadata) {
        Object distributionObj = pkg.get(P_DISTRIBUTION);
        Object componentObj = pkg.get(P_COMPONENT);
        Object architectureObj = pkg.get(P_ARCHITECTURE);
        Object packageNameObj = pkg.get(P_PACKAGE_NAME);
        if (architectureObj == null || packageNameObj == null) {
          log.warn("Skipping package with missing architecture or name: {}", pkg);
          continue;
        }

        // Handle package in multiple distro
        String distributions = distributionObj !=null ? distributionObj.toString() : aptFacet.getDistribution();
        for(String distribution : distributions.split(",")) {
          distribution = distribution.trim();

          // When component is not defined, default to "main" for backward compatibility
          String component = componentObj !=null ? componentObj.toString() : "main";
          String architecture = architectureObj.toString();
          String packageName = packageNameObj.toString();

          // Check if duplicate
          DistComponentArchKey key = new DistComponentArchKey(distribution, component, architecture);
          if(packageNameSeen.computeIfAbsent(key, k -> new HashSet<>()).contains(packageName)){
            log.warn("Skipping duplicate package name: {}", pkg);
            continue;
          }
          packageNameSeen.get(key).add(packageName);

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
   * Removes Package index metadata files for architectures that no longer exist in the repository.
   */
  private void removeStaleArchitectureMetadata(final String distribution, final String component, final Collection<String> currentArchitectures) {
    log.debug("Checking for stale architecture metadata in repository: {}", getRepository().getName());

    String pathPattern = "/dists/" + distribution + "/" + component + "/binary-%";
    Pattern extractPattern = Pattern.compile("^/dists/" + distribution + "/" + component + "/binary-([^/]+)/");
    removeStaleMetadata(
        pathPattern,
        extractPattern,
        currentArchitectures,
        staleArch -> "dists/" + distribution + "/" + component + "/binary-" + staleArch + "/",
        "architecture"
    );
  }

  /**
   * Removes component metadata for components that no longer exist in the given distribution.
   */
  private void removeStaleComponentMetadata(final String distribution, final Collection<String> currentComponents) {
    log.debug("Checking for stale component metadata in repository: {}", getRepository().getName());

    String pathPattern = "/dists/" + distribution + "/%";
    Pattern extractPattern = Pattern.compile("^/dists/" + distribution + "/([^/]+)/");
    removeStaleMetadata(
        pathPattern,
        extractPattern,
        currentComponents,
        staleComponent -> "dists/" + distribution + "/" + staleComponent + "/",
        "component"
    );
  }

  /**
   * Removes distribution metadata for distributions that no longer exist in the repository.
   */
  private void removeStaleDistributionMetadata(final Collection<String> currentDistributions) {
    log.debug("Checking for stale distribution metadata in repository: {}", getRepository().getName());

    String pathPattern = "/dists/%";
    Pattern extractPattern = Pattern.compile("^/dists/([^/]+)/");

    removeStaleMetadata(
        pathPattern,
        extractPattern,
        currentDistributions,
        staleDist -> "dists/" + staleDist + "/",
        "distribution"
    );
  }

  /**
   * Generic stale-metadata removal: browses assets matching the given path pattern, extracts an
   * identifier from each asset path using the first capturing group of {@code extractPattern}, and
   * deletes assets under the prefix corresponding to any identifier not present in
   * {@code currentValues}.
   *
   * @param pathPattern     the asset path pattern to browse (SQL-like, e.g. "/dists/bullseye/%")
   * @param extractPattern  regex whose first capturing group extracts the relevant identifier
   *                        (architecture, component, or distribution) from an asset path
   * @param currentValues   the set of values currently expected (from KV store)
   * @param prefixResolver  function mapping a stale identifier to the asset path prefix to delete
   * @param label           human-readable label for logging
   */
  private void removeStaleMetadata(
      final String pathPattern,
      final Pattern extractPattern,
      final Collection<String> currentValues,
      final Function<String, String> prefixResolver,
      final String label) {

    AptContentFacet aptFacet = content();

    AptAssetStore aptAssetStore = (AptAssetStore) ((AptContentFacetImpl) aptFacet).stores().assetStore;
    Continuation<Asset> assets = aptAssetStore.browsePackageIndexAssets(
        aptFacet.contentRepositoryId(),
        1000,
        null,
        pathPattern);

    Set<String> storedValues = assets.stream()
        .map(asset -> extractGroup(extractPattern, asset.path()))
        .filter(Objects::nonNull)
        .collect(Collectors.toSet());

    for (String storedValue : storedValues) {
      if (!currentValues.contains(storedValue)) {
        log.info("Removing stale {} metadata for '{}' in repository: {}",
            label, storedValue, getRepository().getName());
        aptFacet.deleteAssetsByPrefix(normalizeAssetPath(prefixResolver.apply(storedValue)));
      }
    }
  }

  /**
   * Applies the given regex to the path and returns the first capturing group, or null if no match.
   */
  private static String extractGroup(final Pattern pattern, final String path) {
    Matcher matcher = pattern.matcher(path);
    return matcher.find() ? matcher.group(1) : null;
  }

  private String buildReleaseFile(
      final String distribution,
      final Iterable<String> components,
      final Collection<String> architectures,
      final String md5,
      final String sha256)
  {
    String date = DateFormatUtils.format(new Date(), PATTERN_RFC1123, TimeZone.getTimeZone("GMT"));
    Paragraph p = new Paragraph(Arrays.asList(
        new ControlFile.ControlField("Suite", distribution),
        new ControlFile.ControlField("Codename", distribution),
        new ControlFile.ControlField("Components", String.join(" ", components)),
        new ControlFile.ControlField("Date", date),
        new ControlFile.ControlField("Architectures", String.join(StringUtils.SPACE, architectures)),
        new ControlFile.ControlField("SHA256", sha256), new ControlFile.ControlField("MD5Sum", md5)));
    return p.toString();
  }

  private String releaseIndexName(final String dist, final String name) {
    return "dists/" + dist + "/" + name;
  }

  private String packageIndexName(final String dist, final String component, final String arch, final String ext) {
    return "dists/" + dist + "/" + component + "/binary-" + arch + "/Packages" + ext;
  }

  private String packageRelativeIndexName(final String component, final String arch, final String ext) {
    return component + "/binary-" + arch + "/Packages" + ext;
  }

  private void addSignatureItem(
      final StringBuilder builder,
      final HashAlgorithm algo,
      final FluentAsset asset,
      final String filename)
  {
    AssetBlob assetBlob = asset.blob()
        .orElseThrow(() -> new IllegalStateException(
            "Cannot generate signature for metadata. Blob couldn't be found for asset: " + filename));

    builder.append("\n ");
    builder.append(assetBlob.checksums().get(algo.name()));
    builder.append(StringUtils.SPACE);
    builder.append(assetBlob.blobSize());
    builder.append(StringUtils.SPACE);
    builder.append(filename);
  }

  private void storePackageIndexFiles(
      final AptContentFacet aptFacet,
      final String distribution,
      final String component,
      final String architecture,
      final CompressingTempFileStore.FileMetadata metadata,
      final StringBuilder md5Builder,
      final StringBuilder sha256Builder) throws IOException
  {
    putPackageIndexWithSignatures(aptFacet, distribution, component, architecture, StringUtils.EMPTY, metadata.plainSupplier(),
        metadata.plainSize(), AptMimeTypes.TEXT, md5Builder, sha256Builder);
    putPackageIndexWithSignatures(aptFacet, distribution, component, architecture, GZ, metadata.gzSupplier(),
        metadata.gzSize(), AptMimeTypes.GZIP, md5Builder, sha256Builder);
    putPackageIndexWithSignatures(aptFacet, distribution, component, architecture, BZ2, metadata.bzSupplier(),
        metadata.bzSize(), AptMimeTypes.BZIP, md5Builder, sha256Builder);
  }

  private void putPackageIndexWithSignatures(
      final AptContentFacet aptFacet,
      final String distro,
      final String component,
      final String arch,
      final String extension,
      final org.sonatype.nexus.common.io.InputStreamSupplier streamSupplier,
      final long size,
      final String mimeType,
      final StringBuilder md5Builder,
      final StringBuilder sha256Builder) throws IOException
  {
    FluentAsset asset = aptFacet.put(
        packageIndexName(distro, component, arch, extension),
        new StreamPayload(streamSupplier, size, mimeType));
    addSignatureItem(md5Builder, MD5, asset, packageRelativeIndexName(component, arch, extension));
    addSignatureItem(sha256Builder, SHA256, asset, packageRelativeIndexName(component, arch, extension));
  }

  private AptContentFacet content() {
    return facet(AptContentFacet.class);
  }

  private AptKeyValueFacet data() {
    return facet(AptKeyValueFacet.class);
  }

  private AptSigningFacet signing() {
    return facet(AptSigningFacet.class);
  }

  /*
   * We use Component IDs to simplify cleanup on purge events.
   */
  private static OptionalInt componentId(final Asset asset) {
    return InternalIds.internalComponentId(asset);
  }

  private String serialize(final FluentAsset asset) {
    try {
      return mapper.writeValueAsString(FormatAttributesUtils.getFormatAttributes(asset));
    }
    catch (JsonProcessingException e) {
      throw new IllegalStateException(e);
    }
  }

  private Map<String, Object> deserialize(final String value) {
    try {
      return mapper.readValue(value, new TypeReference<>()
      {
      });
    }
    catch (JsonProcessingException e) {
      throw new IllegalStateException(e);
    }
  }
}
