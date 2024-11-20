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
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.inject.Named;

import org.sonatype.nexus.common.cooperation2.Cooperation2;
import org.sonatype.nexus.common.cooperation2.Cooperation2Factory;
import org.sonatype.nexus.common.hash.HashAlgorithm;
import org.sonatype.nexus.common.time.Clock;
import org.sonatype.nexus.repository.Facet.Exposed;
import org.sonatype.nexus.repository.FacetSupport;
import org.sonatype.nexus.repository.apt.AptFormat;
import org.sonatype.nexus.repository.apt.datastore.AptContentFacet;
import org.sonatype.nexus.repository.apt.datastore.internal.data.AptKeyValueFacet;
import org.sonatype.nexus.repository.apt.datastore.internal.hosted.AssetChange;
import org.sonatype.nexus.repository.apt.internal.AptMimeTypes;
import org.sonatype.nexus.repository.apt.internal.debian.ControlFile;
import org.sonatype.nexus.repository.apt.internal.debian.ControlFile.Paragraph;
import org.sonatype.nexus.repository.apt.internal.gpg.AptSigningFacet;
import org.sonatype.nexus.repository.apt.internal.hosted.AssetAction;
import org.sonatype.nexus.repository.apt.internal.hosted.CompressingTempFileStore;
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

import static com.google.common.base.Preconditions.checkNotNull;
import static org.sonatype.nexus.common.hash.HashAlgorithm.MD5;
import static org.sonatype.nexus.common.hash.HashAlgorithm.SHA256;
import static org.sonatype.nexus.repository.apt.internal.AptFacetHelper.normalizeAssetPath;
import static org.sonatype.nexus.repository.apt.internal.AptProperties.BZ2;
import static org.sonatype.nexus.repository.apt.internal.AptProperties.DEB;
import static org.sonatype.nexus.repository.apt.internal.AptProperties.GZ;
import static org.sonatype.nexus.repository.apt.internal.AptProperties.P_ARCHITECTURE;
import static org.sonatype.nexus.repository.apt.internal.AptProperties.P_INDEX_SECTION;
import static org.sonatype.nexus.repository.apt.internal.AptProperties.P_PACKAGE_NAME;
import static org.sonatype.nexus.repository.apt.internal.ReleaseName.INRELEASE;
import static org.sonatype.nexus.repository.apt.internal.ReleaseName.RELEASE;
import static org.sonatype.nexus.repository.apt.internal.ReleaseName.RELEASE_GPG;
import static org.sonatype.nexus.repository.date.DateTimeUtils.formatDateTime;

/**
 * Apt metadata facet. Holds the logic for metadata recalculation.
 */
@Named(AptFormat.NAME)
@Exposed
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
      @Named("${nexus.apt.metadata.cooperation.enabled:-true}") final boolean cooperationEnabled,
      @Named("${nexus.apt.metadata.cooperation.majorTimeout:-0s}") final Duration majorTimeout,
      @Named("${nexus.apt.metadata.cooperation.minorTimeout:-30s}") final Duration minorTimeout,
      @Named("${nexus.apt.metadata.cooperation.threadsPerKey:-100}") final int threadsPerKey)
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
    componentId(asset).ifPresent(componentId ->
        data().addPackageMetadata(componentId, InternalIds.internalAssetId(asset), serialize(asset))
    );
  }

  public void removePackageMetadata(final FluentAsset asset) {
    checkNotNull(asset);
    log.debug("Removing metadata for repository: {} asset: {}", getRepository().getName(), asset.path());
    componentId(asset).ifPresent(componentId ->
        data().removePackageMetadata(componentId, InternalIds.internalAssetId(asset))
    );
  }

  public void removeInReleaseIndex() {
    content().deleteAssetsByPrefix(normalizeAssetPath(releaseIndexName(INRELEASE)));
  }

  public Optional<Content> rebuildMetadata(final List<AssetChange> changeList) throws IOException {
    return Optional.ofNullable(
        cooperation.on(() -> doRebuildMetadata(changeList))
            .cooperate(changeList.toString())
    );
  }

  /**
   * Removes metadata per architecture
   */
  private void removeMetadataPerArchitecture() {
    log.debug("Removing metadata per architecture: {}", getRepository().getName());
    content().deleteAssetsByPrefix(normalizeAssetPath(mainBinaryPrefix()));
  }

  private Content doRebuildMetadata(final List<AssetChange> changeList) throws IOException {
    log.debug("Starting rebuilding metadata at {}", getRepository().getName());
    OffsetDateTime rebuildStart = clock.clusterTime();

    AptContentFacet aptFacet = content();
    AptSigningFacet signingFacet = signing();

    removeMetadataPerArchitecture();

    StringBuilder sha256Builder = new StringBuilder();
    StringBuilder md5Builder = new StringBuilder();
    String releaseFile;
    try (CompressingTempFileStore store = buildPackageIndexes(changeList)) {
      for (Map.Entry<String, CompressingTempFileStore.FileMetadata> entry : store.getFiles().entrySet()) {
        FluentAsset metadataAsset = aptFacet.put(
            packageIndexName(entry.getKey(), StringUtils.EMPTY),
            new StreamPayload(entry.getValue().plainSupplier(), entry.getValue().plainSize(), AptMimeTypes.TEXT)
        );
        addSignatureItem(md5Builder, MD5, metadataAsset, packageRelativeIndexName(entry.getKey(), StringUtils.EMPTY));
        addSignatureItem(sha256Builder, SHA256, metadataAsset,
            packageRelativeIndexName(entry.getKey(), StringUtils.EMPTY));

        FluentAsset gzMetadataAsset = aptFacet.put(
            packageIndexName(entry.getKey(), GZ),
            new StreamPayload(entry.getValue().gzSupplier(), entry.getValue().bzSize(), AptMimeTypes.GZIP)
        );
        addSignatureItem(md5Builder, MD5, gzMetadataAsset, packageRelativeIndexName(entry.getKey(), GZ));
        addSignatureItem(sha256Builder, SHA256, gzMetadataAsset, packageRelativeIndexName(entry.getKey(), GZ));

        FluentAsset bzMetadataAsset = aptFacet.put(
            packageIndexName(entry.getKey(), BZ2),
            new StreamPayload(entry.getValue().bzSupplier(), entry.getValue().bzSize(), AptMimeTypes.BZIP)
        );
        addSignatureItem(md5Builder, MD5, bzMetadataAsset, packageRelativeIndexName(entry.getKey(), BZ2));
        addSignatureItem(sha256Builder, SHA256, bzMetadataAsset, packageRelativeIndexName(entry.getKey(), BZ2));
      }

      releaseFile = buildReleaseFile(
          aptFacet.getDistribution(),
          store.getFiles().keySet(),
          md5Builder.toString(),
          sha256Builder.toString()
      );
    }

    FluentAsset releaseFileAsset = aptFacet.put(
        releaseIndexName(RELEASE),
        new BytesPayload(releaseFile.getBytes(StandardCharsets.UTF_8), AptMimeTypes.TEXT)
    );
    aptFacet.put(
        releaseIndexName(INRELEASE),
        new BytesPayload(signingFacet.signInline(releaseFile), AptMimeTypes.TEXT)
    );
    aptFacet.put(
        releaseIndexName(RELEASE_GPG),
        new BytesPayload(signingFacet.signExternal(releaseFile), AptMimeTypes.SIGNATURE)
    );

    if (log.isDebugEnabled()) {
      long finishTime = System.currentTimeMillis();
      log.debug("Completed metadata rebuild in {}", finishTime - rebuildStart.toInstant().toEpochMilli());
    }

    return releaseFileAsset.download();
  }

  private CompressingTempFileStore buildPackageIndexes(final List<AssetChange> changes)
      throws IOException
  {
    CompressingTempFileStore result = new CompressingTempFileStore();
    Map<String, Writer> streams = new HashMap<>();
    boolean ok = false;
    try {
      Set<String> architectures =
          changes.stream()
              .map(change -> getArchitecture(change.getAsset()))
              .collect(Collectors.toSet());

      final List<Map<String, Object>> packagesInfo = data()
          .browsePackagesMetadata()
          .map(this::deserialize)
          .collect(Collectors.toList());

      packagesInfo.stream().map(x -> x.get(P_ARCHITECTURE).toString())
          .collect(Collectors.toCollection(() -> architectures));

      Map<String, List<Map<String, Object>>> assetsPerArch = new HashMap<>();
      for (String architecture : architectures) {
        List<Map<String, Object>> assets = packagesInfo.stream()
            .filter(x -> x.get(P_ARCHITECTURE).toString().equals(architecture))
            .collect(Collectors.toList());
        assetsPerArch.put(architecture, assets);
      }

      for (List<Map<String, Object>> assets : assetsPerArch.values()) {
        Optional<AssetChange> removeAssetChange =
            changes.stream()
                .filter(change -> change.getAsset().kind().equals(DEB))
                .filter(change -> change.getAction() == AssetAction.REMOVED)
                .findAny();

        if (assets.isEmpty() && removeAssetChange.isPresent()) {
          createEmptyMetadataFile(result, streams, removeAssetChange.get());
        }
        else {
          createMetadataFileWithData(changes, result, streams, assets);
        }
      }
      ok = true;
    }
    finally {
      for (Writer writer : streams.values()) {
        IOUtils.closeQuietly(writer, null);
      }

      if (!ok) {
        result.close();
      }
    }
    return result;
  }

  private void createMetadataFileWithData(
      final List<AssetChange> changes,
      final CompressingTempFileStore result,
      final Map<String, Writer> streams,
      final List<Map<String, Object>> assets) throws IOException
  {
    // NOTE:  We exclude added assets as well to account for the case where we are replacing an asset
    Set<String> excludeNames = changes.stream().map(c -> c.getAsset().path()).collect(Collectors.toSet());

    for (Map<String, Object> asset : assets) {
      final String name = asset.get(P_PACKAGE_NAME).toString();
      final String arch = asset.get(P_ARCHITECTURE).toString();
      Writer outWriter = streams.computeIfAbsent(arch, result::openOutput);
      if (!excludeNames.contains(name)) {
        final String indexSection = asset.get(P_INDEX_SECTION).toString();
        outWriter.write(indexSection);
        outWriter.write("\n\n");
      }
    }
  }

  private String buildReleaseFile(
      final String distribution,
      final Collection<String> architectures,
      final String md5,
      final String sha256)
  {
    String date = DateFormatUtils.formatDateTime(new Date());
    Paragraph p = new Paragraph(Arrays.asList(
        new ControlFile.ControlField("Suite", distribution),
        new ControlFile.ControlField("Codename", distribution), new ControlFile.ControlField("Components", "main"),
        new ControlFile.ControlField("Date", date),
        new ControlFile.ControlField("Architectures", String.join(StringUtils.SPACE, architectures)),
        new ControlFile.ControlField("SHA256", sha256), new ControlFile.ControlField("MD5Sum", md5)));
    return p.toString();
  }

  private String mainBinaryPrefix() {
    String dist = content().getDistribution();
    return "dists/" + dist + "/main/binary-";
  }

  private String releaseIndexName(final String name) {
    String dist = content().getDistribution();
    return "dists/" + dist + "/" + name;
  }

  private String packageIndexName(final String arch, final String ext) {
    String dist = content().getDistribution();
    return "dists/" + dist + "/main/binary-" + arch + "/Packages" + ext;
  }

  private String packageRelativeIndexName(final String arch, final String ext) {
    return "main/binary-" + arch + "/Packages" + ext;
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

  private String getArchitecture(final FluentAsset asset) {
    return (String) FormatAttributesUtils.getFormatAttributes(asset).get(P_ARCHITECTURE);
  }

  private void createEmptyMetadataFile(
      final CompressingTempFileStore result,
      final Map<String, Writer> streams,
      final AssetChange removeAssetChange)
  {
    String arch = (String) FormatAttributesUtils.getFormatAttributes(removeAssetChange.getAsset())
        .get(P_ARCHITECTURE);
    streams.computeIfAbsent(arch, result::openOutput);
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
      return mapper.readValue(value, new TypeReference<Map<String, Object>>() { });
    }
    catch (JsonProcessingException e) {
      throw new IllegalStateException(e);
    }
  }
}
