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
package org.sonatype.nexus.repository.apt.datastore.internal.metadata;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.TimeZone;

import org.sonatype.nexus.common.cooperation2.Cooperation2;
import org.sonatype.nexus.common.cooperation2.Cooperation2Factory;
import org.sonatype.nexus.common.hash.HashAlgorithm;
import org.sonatype.nexus.common.time.Clock;
import org.sonatype.nexus.repository.Facet.Exposed;
import org.sonatype.nexus.repository.FacetSupport;
import org.sonatype.nexus.repository.apt.datastore.AptContentFacet;
import org.sonatype.nexus.repository.apt.datastore.internal.data.AptKeyValueFacet;
import org.sonatype.nexus.repository.apt.internal.AptMimeTypes;
import org.sonatype.nexus.repository.apt.internal.debian.ControlFile;
import org.sonatype.nexus.repository.apt.internal.debian.ControlFile.Paragraph;
import org.sonatype.nexus.repository.apt.internal.gpg.AptSigningFacet;
import org.sonatype.nexus.repository.config.Configuration;
import org.sonatype.nexus.repository.content.Asset;
import org.sonatype.nexus.repository.content.AssetBlob;
import org.sonatype.nexus.repository.content.fluent.FluentAsset;
import org.sonatype.nexus.repository.content.store.InternalIds;
import org.sonatype.nexus.repository.content.utils.FormatAttributesUtils;
import org.sonatype.nexus.repository.apt.internal.hosted.CompressingTempFileStore;
import org.sonatype.nexus.repository.view.Content;
import org.sonatype.nexus.repository.view.payloads.BytesPayload;
import org.sonatype.nexus.repository.view.payloads.StreamPayload;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateFormatUtils;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.sonatype.nexus.common.hash.HashAlgorithm.MD5;
import static org.sonatype.nexus.common.hash.HashAlgorithm.SHA256;
import static org.sonatype.nexus.repository.apt.internal.AptProperties.BZ2;
import static org.sonatype.nexus.repository.apt.internal.AptProperties.GZ;
import static org.apache.http.protocol.HttpDateGenerator.PATTERN_RFC1123;
import static org.sonatype.nexus.repository.apt.internal.ReleaseName.INRELEASE;
import static org.sonatype.nexus.repository.apt.internal.ReleaseName.RELEASE;
import static org.sonatype.nexus.repository.apt.internal.ReleaseName.RELEASE_GPG;

/**
 * Base class for APT metadata facets providing common functionality for hosted and proxy repositories.
 * <p>
 * This base class extracts common patterns including:
 * - Package metadata serialization/deserialization
 * - Path generation for metadata files
 * - Release file building
 * - Signature handling
 * - Cooperation coordination for metadata rebuilds
 * - Metadata cleanup operations
 */
@Exposed
public abstract class AptMetadataFacetSupport
    extends FacetSupport
{
  /**
   * Default component name for APT repositories.
   * Hosted repositories use a single "main" component.
   * Proxy repositories use this as fallback when upstream is unreachable.
   */
  protected static final String DEFAULT_COMPONENT = "main";

  protected final ObjectMapper mapper;

  protected final Clock clock;

  protected final Cooperation2Factory.Builder cooperationBuilder;

  protected Cooperation2 cooperation;

  /**
   * Constructs the metadata facet with required dependencies.
   *
   * @param mapper JSON mapper for package metadata serialization
   * @param clock clock for timestamps
   * @param cooperationFactory factory for creating cooperation instances
   * @param cooperationEnabled whether cooperation is enabled
   * @param majorTimeout cooperation major timeout
   * @param minorTimeout cooperation minor timeout
   * @param threadsPerKey cooperation threads per key
   */
  protected AptMetadataFacetSupport(
      final ObjectMapper mapper,
      final Clock clock,
      final Cooperation2Factory cooperationFactory,
      final boolean cooperationEnabled,
      final Duration majorTimeout,
      final Duration minorTimeout,
      final int threadsPerKey)
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
    this.cooperation = cooperationBuilder.build(getRepository().getName() + ":" + getCooperationKey());
  }

  /**
   * Adds package metadata to the key-value store.
   * Stores the asset's format attributes as JSON, keyed by component and asset IDs.
   *
   * @param asset the asset containing package metadata
   */
  public void addPackageMetadata(final FluentAsset asset) {
    checkNotNull(asset);
    log.debug("Storing metadata for repository: {} asset: {}", getRepository().getName(), asset.path());
    componentId(asset).ifPresent(
        componentId -> keyValue().addPackageMetadata(componentId, InternalIds.internalAssetId(asset),
            serialize(asset)));
  }

  /**
   * Removes package metadata from the key-value store.
   *
   * @param asset the asset whose metadata should be removed
   */
  public void removePackageMetadata(final FluentAsset asset) {
    checkNotNull(asset);
    log.debug("Removing metadata for repository: {} asset: {}", getRepository().getName(), asset.path());
    OptionalInt compId = componentId(asset);
    if (compId.isPresent()) {
      int componentId = compId.getAsInt();
      int assetId = InternalIds.internalAssetId(asset);
      log.debug("Removing package metadata from KV store for component: {} asset: {}", componentId, assetId);
      keyValue().removePackageMetadata(componentId, assetId);
    }
    else {
      log.warn("Cannot remove package metadata: asset {} has no component ID", asset.path());
    }
  }

  /**
   * Rebuilds repository metadata using cooperation to ensure single execution.
   * Delegates to {@link #doRebuildMetadata()} for the actual rebuild logic.
   * Subclasses may override to change return type or coordination behavior.
   *
   * @return optional content result
   * @throws IOException if metadata rebuild fails
   */
  public Optional<Content> rebuildMetadata() throws IOException {
    return Optional.ofNullable(
        cooperation.on(this::doRebuildMetadata).cooperate(getRepository().getName()));
  }

  /**
   * Returns the key-value facet.
   */
  protected AptKeyValueFacet keyValue() {
    return getRepository().facet(AptKeyValueFacet.class);
  }

  /**
   * Returns the content facet.
   */
  protected AptContentFacet content() {
    return facet(AptContentFacet.class);
  }

  /**
   * Returns the cooperation key suffix for metadata rebuild coordination.
   * Both hosted and proxy use "apt-metadata" for unified thread pool management.
   *
   * @return cooperation key suffix
   */
  protected String getCooperationKey() {
    return "apt-metadata";
  }

  /**
   * Performs the actual metadata rebuild logic.
   * Hosted: Generates metadata from KV store only.
   * Proxy: Merges upstream metadata with cached package metadata.
   *
   * @return the result of the rebuild operation
   * @throws IOException if rebuild fails
   */
  protected abstract Content doRebuildMetadata() throws IOException;

  /**
   * Stores a package index file.
   * Both hosted and proxy use content().put() for storing package indices.
   *
   * @param path asset path
   * @param payload content payload
   * @return the stored asset
   * @throws IOException if storage fails
   */
  protected FluentAsset storePackageIndex(final String path, final StreamPayload payload) throws IOException {
    return content().put(path, payload);
  }

  /**
   * Adds a signature item (checksum + size + filename) to the Release file builder.
   * Used to populate MD5Sum and SHA256 sections of the Release file.
   *
   * @param builder string builder for Release file content
   * @param algo hash algorithm (MD5 or SHA256)
   * @param asset the asset to extract checksum and size from
   * @param filename relative path to include in Release file
   */
  protected void addSignatureItem(
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

  /**
   * Builds a Release file content with the standard Debian Release file format.
   *
   * @param distribution distribution name (e.g., "jammy", "bookworm")
   * @param components collection of component names (e.g., ["main"], ["main", "contrib"])
   * @param architectures collection of architecture names (e.g., ["amd64", "arm64"])
   * @param md5 MD5Sum section content (checksums of Packages files)
   * @param sha256 SHA256 section content (checksums of Packages files)
   * @return Release file content as string
   */
  protected String buildReleaseFile(
      final String distribution,
      final Collection<String> components,
      final Collection<String> architectures,
      final String md5,
      final String sha256)
  {
    String date = DateFormatUtils.format(new Date(), PATTERN_RFC1123, TimeZone.getTimeZone("GMT"));
    Paragraph p = new Paragraph(Arrays.asList(
        new ControlFile.ControlField("Suite", distribution),
        new ControlFile.ControlField("Codename", distribution),
        new ControlFile.ControlField("Components", String.join(StringUtils.SPACE, components)),
        new ControlFile.ControlField("Date", date),
        new ControlFile.ControlField("Architectures", String.join(StringUtils.SPACE, architectures)),
        new ControlFile.ControlField("SHA256", sha256),
        new ControlFile.ControlField("MD5Sum", md5)));
    return p.toString();
  }

  /**
   * Generates and stores Release, InRelease, and Release.gpg files for a distribution.
   *
   * @param distribution distribution name (e.g., "jammy", "bookworm")
   * @param releaseFile Release file content to sign and store
   * @param aptFacet content facet for storing files
   * @param signingFacet signing facet for GPG signatures
   * @return the stored Release file asset
   * @throws IOException if file storage fails
   */
  protected FluentAsset generateReleaseFiles(
      final String distribution,
      final String releaseFile,
      final AptContentFacet aptFacet,
      final AptSigningFacet signingFacet) throws IOException
  {
    if (releaseFile == null) {
      return null;
    }

    boolean isFlat = aptFacet.isFlat();
    String basePath = isFlat ? "/" : "/dists/" + distribution + "/";

    FluentAsset releaseFileAsset = aptFacet.put(
        basePath + RELEASE,
        new BytesPayload(releaseFile.getBytes(StandardCharsets.UTF_8), AptMimeTypes.TEXT));

    aptFacet.put(
        basePath + INRELEASE,
        new BytesPayload(signingFacet.signInline(releaseFile), AptMimeTypes.TEXT));

    aptFacet.put(
        basePath + RELEASE_GPG,
        new BytesPayload(signingFacet.signExternal(releaseFile), AptMimeTypes.SIGNATURE));

    return releaseFileAsset;
  }

  /**
   * Stores package index files (plain, gzip, bzip2) for a distribution/component/architecture.
   * Both hosted and proxy repositories use this to write Packages files.
   *
   * @param aptFacet content facet for storing files
   * @param distribution distribution name (e.g., "jammy")
   * @param component component name (e.g., "main")
   * @param arch architecture name (e.g., "amd64")
   * @param metadata compressed file metadata from CompressingTempFileStore
   * @param md5Builder builder for MD5Sum section of Release file
   * @param sha256Builder builder for SHA256 section of Release file
   */
  protected void storePackageIndexFiles(
      final AptContentFacet aptFacet,
      final String distribution,
      final String component,
      final String arch,
      final CompressingTempFileStore.FileMetadata metadata,
      final StringBuilder md5Builder,
      final StringBuilder sha256Builder) throws IOException
  {
    putPackageIndexWithSignatures(aptFacet, distribution, component, arch, StringUtils.EMPTY, metadata.plainSupplier(),
        metadata.plainSize(), AptMimeTypes.TEXT, md5Builder, sha256Builder);
    putPackageIndexWithSignatures(aptFacet, distribution, component, arch, GZ, metadata.gzSupplier(),
        metadata.gzSize(), AptMimeTypes.GZIP, md5Builder, sha256Builder);
    putPackageIndexWithSignatures(aptFacet, distribution, component, arch, BZ2, metadata.bzSupplier(),
        metadata.bzSize(), AptMimeTypes.BZIP, md5Builder, sha256Builder);
  }

  /**
   * Stores a single package index file and adds its checksum to the Release file builders.
   */
  private void putPackageIndexWithSignatures(
      final AptContentFacet aptFacet,
      final String distribution,
      final String component,
      final String arch,
      final String extension,
      final org.sonatype.nexus.common.io.InputStreamSupplier streamSupplier,
      final long size,
      final String mimeType,
      final StringBuilder md5Builder,
      final StringBuilder sha256Builder) throws IOException
  {
    boolean isFlat = aptFacet.isFlat();
    String path;
    String relativePath;

    if (isFlat) {
      path = "/Packages" + extension;
      relativePath = "Packages" + extension;
    }
    else {
      path = "/dists/" + distribution + "/" + component + "/binary-" + arch + "/Packages" + extension;
      relativePath = component + "/binary-" + arch + "/Packages" + extension;
    }

    FluentAsset asset = aptFacet.put(path, new StreamPayload(streamSupplier, size, mimeType));
    addSignatureItem(md5Builder, MD5, asset, relativePath);
    addSignatureItem(sha256Builder, SHA256, asset, relativePath);
  }

  /**
   * Gets the internal component ID for an asset.
   * Returns empty if the asset has no component or cannot be cast to the expected type.
   *
   * @param asset the asset to get component ID from
   * @return optional component ID
   */
  protected static OptionalInt componentId(final Asset asset) {
    try {
      return InternalIds.internalComponentId(asset);
    }
    catch (ClassCastException e) {
      // This can happen in unit tests with mocked assets
      return OptionalInt.empty();
    }
  }

  /**
   * Serializes asset format attributes to JSON for KV storage.
   *
   * @param asset the asset to serialize
   * @return JSON string of format attributes
   */
  protected String serialize(final FluentAsset asset) {
    try {
      return mapper.writeValueAsString(FormatAttributesUtils.getFormatAttributes(asset));
    }
    catch (JsonProcessingException e) {
      throw new IllegalStateException(e);
    }
  }

  /**
   * Deserializes JSON from KV storage to format attributes map.
   *
   * @param value JSON string to deserialize
   * @return map of format attributes
   */
  protected Map<String, Object> deserialize(final String value) {
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
