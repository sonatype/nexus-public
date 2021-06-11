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
package org.sonatype.nexus.repository.apt.datastore.internal.hosted;

import java.io.IOException;
import java.io.Writer;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TimeZone;
import java.util.stream.Collectors;

import javax.inject.Named;

import org.sonatype.nexus.common.entity.Continuation;
import org.sonatype.nexus.common.hash.HashAlgorithm;
import org.sonatype.nexus.repository.Facet;
import org.sonatype.nexus.repository.FacetSupport;
import org.sonatype.nexus.repository.apt.datastore.AptContentFacet;
import org.sonatype.nexus.repository.apt.internal.AptMimeTypes;
import org.sonatype.nexus.repository.apt.internal.debian.ControlFile;
import org.sonatype.nexus.repository.apt.internal.debian.ControlFile.Paragraph;
import org.sonatype.nexus.repository.apt.internal.debian.PackageInfo;
import org.sonatype.nexus.repository.apt.internal.gpg.AptSigningFacet;
import org.sonatype.nexus.repository.apt.internal.hosted.AssetAction;
import org.sonatype.nexus.repository.apt.internal.hosted.CompressingTempFileStore;
import org.sonatype.nexus.repository.content.AssetBlob;
import org.sonatype.nexus.repository.content.facet.ContentFacet;
import org.sonatype.nexus.repository.content.fluent.FluentAsset;
import org.sonatype.nexus.repository.content.utils.FormatAttributesUtils;
import org.sonatype.nexus.repository.view.Payload;
import org.sonatype.nexus.repository.view.payloads.BytesPayload;
import org.sonatype.nexus.repository.view.payloads.StreamPayload;

import com.google.common.base.Charsets;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateFormatUtils;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.apache.http.protocol.HttpDateGenerator.PATTERN_RFC1123;
import static org.sonatype.nexus.common.hash.HashAlgorithm.MD5;
import static org.sonatype.nexus.common.hash.HashAlgorithm.SHA256;
import static org.sonatype.nexus.repository.apt.internal.AptProperties.BZ2;
import static org.sonatype.nexus.repository.apt.internal.AptProperties.DEB;
import static org.sonatype.nexus.repository.apt.internal.AptProperties.GZ;
import static org.sonatype.nexus.repository.apt.internal.AptProperties.P_ARCHITECTURE;
import static org.sonatype.nexus.repository.apt.internal.AptProperties.P_INDEX_SECTION;
import static org.sonatype.nexus.repository.apt.internal.ReleaseName.INRELEASE;
import static org.sonatype.nexus.repository.apt.internal.ReleaseName.RELEASE;
import static org.sonatype.nexus.repository.apt.internal.ReleaseName.RELEASE_GPG;

/**
 * Apt hosted facet. Holds the logic for recalculation of metadata.
 *
 * @since 3.31
 */
@Named
@Facet.Exposed
public class AptHostedFacet
    extends FacetSupport
{
  /**
   * Saves asset to the database and triggers metadata recalculation
   *
   * @param assetPath   - Asset path
   * @param payload     - Request  payload
   * @param packageInfo - Package info
   * @return - Returns Fluent asset after successful save.
   */
  public FluentAsset put(final String assetPath,
                         final Payload payload,
                         final PackageInfo packageInfo) throws IOException
  {
    checkNotNull(assetPath);
    checkNotNull(payload);
    checkNotNull(packageInfo);

    AptContentFacet contentFacet = getRepository().facet(AptContentFacet.class);
    FluentAsset asset = contentFacet.put(assetPath, payload, packageInfo);
    rebuildMetadata(Collections.singletonList(new AssetChange(AssetAction.ADDED, asset)));
    return asset;
  }

  /**
   * Method for triggering Apt metadata recalculation.
   */
  public void rebuildMetadata() throws IOException {
    rebuildMetadata(Collections.emptyList());
  }

  /**
   * Method for triggering Apt metadata recalculation with possibility to specify what actually asset was changed
   */
  public void rebuildMetadata(List<AssetChange> changeList) throws IOException {
    AptContentFacet aptFacet = getRepository().facet(AptContentFacet.class);
    AptSigningFacet signingFacet = getRepository().facet(AptSigningFacet.class);

    StringBuilder sha256Builder = new StringBuilder();
    StringBuilder md5Builder = new StringBuilder();
    String releaseFile;
    try (CompressingTempFileStore store = buildPackageIndexes(changeList)) {
      for (Map.Entry<String, CompressingTempFileStore.FileMetadata> entry : store.getFiles().entrySet()) {
        FluentAsset metadataAsset = aptFacet.put(
            packageIndexName(entry.getKey(), StringUtils.EMPTY),
            new StreamPayload(entry.getValue().plainSupplier(), entry.getValue().plainSize(), AptMimeTypes.TEXT));
        addSignatureItem(md5Builder, MD5, metadataAsset, packageRelativeIndexName(entry.getKey(), StringUtils.EMPTY));
        addSignatureItem(sha256Builder, SHA256, metadataAsset,
            packageRelativeIndexName(entry.getKey(), StringUtils.EMPTY));

        FluentAsset gzMetadataAsset = aptFacet.put(
            packageIndexName(entry.getKey(), GZ),
            new StreamPayload(entry.getValue().gzSupplier(), entry.getValue().bzSize(), AptMimeTypes.GZIP));
        addSignatureItem(md5Builder, MD5, gzMetadataAsset, packageRelativeIndexName(entry.getKey(), GZ));
        addSignatureItem(sha256Builder, SHA256, gzMetadataAsset, packageRelativeIndexName(entry.getKey(), GZ));

        FluentAsset bzMetadataAsset = aptFacet.put(
            packageIndexName(entry.getKey(), BZ2),
            new StreamPayload(entry.getValue().bzSupplier(), entry.getValue().bzSize(), AptMimeTypes.BZIP));
        addSignatureItem(md5Builder, MD5, bzMetadataAsset, packageRelativeIndexName(entry.getKey(), BZ2));
        addSignatureItem(sha256Builder, SHA256, bzMetadataAsset, packageRelativeIndexName(entry.getKey(), BZ2));
      }

      releaseFile = buildReleaseFile(aptFacet.getDistribution(), store.getFiles().keySet(), md5Builder.toString(),
          sha256Builder.toString());
    }

    aptFacet.put(releaseIndexName(RELEASE), new BytesPayload(releaseFile.getBytes(Charsets.UTF_8), AptMimeTypes.TEXT));
    byte[] inRelease = signingFacet.signInline(releaseFile);
    aptFacet.put(releaseIndexName(INRELEASE), new BytesPayload(inRelease, AptMimeTypes.TEXT));
    byte[] releaseGpg = signingFacet.signExternal(releaseFile);
    aptFacet.put(releaseIndexName(RELEASE_GPG), new BytesPayload(releaseGpg, AptMimeTypes.SIGNATURE));
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

      final Continuation<FluentAsset> allAssets = getRepository().facet(ContentFacet.class)
          .assets()
          .byKind(DEB)
          .browse(Integer.MAX_VALUE, null);

      allAssets.stream().map(this::getArchitecture).collect(Collectors.toCollection(() -> architectures));

      Map<String, List<FluentAsset>> assetsPerArch = new HashMap<>();
      for (String architecture : architectures) {
        List<FluentAsset> assets = allAssets.stream()
            .filter(asset -> getArchitecture(asset).equals(architecture))
            .collect(Collectors.toList());
        assetsPerArch.put(architecture, assets);
      }

      for (List<FluentAsset> assets : assetsPerArch.values()) {
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
        IOUtils.closeQuietly(writer);
      }

      if (!ok) {
        result.close();
      }
    }
    return result;
  }

  private String getArchitecture(final FluentAsset asset) {
    return (String) FormatAttributesUtils.getFormatAttributes(asset).get(P_ARCHITECTURE);
  }

  private void createEmptyMetadataFile(final CompressingTempFileStore result,
                                       final Map<String, Writer> streams,
                                       final AssetChange removeAssetChange)
  {
    String arch = (String) FormatAttributesUtils.getFormatAttributes(removeAssetChange.getAsset())
        .get(P_ARCHITECTURE);
    streams.computeIfAbsent(arch, result::openOutput);
  }

  private void createMetadataFileWithData(final List<AssetChange> changes,
                                          final CompressingTempFileStore result,
                                          final Map<String, Writer> streams,
                                          final List<FluentAsset> assets) throws IOException
  {
    // NOTE:  We exclude added assets as well to account for the case where we are replacing an asset
    Set<String> excludeNames = changes.stream().map(c -> c.getAsset().path()).collect(Collectors.toSet());

    for (FluentAsset asset : assets) {
      final String name = asset.path();
      final String arch = (String) FormatAttributesUtils.getFormatAttributes(asset).get(P_ARCHITECTURE);
      Writer outWriter = streams.computeIfAbsent(arch, result::openOutput);
      if (!excludeNames.contains(name)) {
        final String indexSection = (String) FormatAttributesUtils.getFormatAttributes(asset).get(P_INDEX_SECTION);
        outWriter.write(indexSection);
        outWriter.write("\n\n");
      }
    }

    List<FluentAsset> addAssets = changes.stream().filter(c -> c.getAction() == AssetAction.ADDED)
        .map(AssetChange::getAsset).collect(Collectors.toList());

    // HACK: tx.browse won't see changes in the current transaction, so we have to manually add these in here
    for (FluentAsset asset : addAssets) {
      String arch = (String) FormatAttributesUtils.getFormatAttributes(asset).get(P_ARCHITECTURE);
      String indexSection = (String) FormatAttributesUtils.getFormatAttributes(asset).get(P_INDEX_SECTION);
      Writer outWriter = streams.computeIfAbsent(arch, result::openOutput);
      outWriter.write(indexSection);
      outWriter.write("\n\n");
    }
  }

  private String buildReleaseFile(
      final String distribution,
      final Collection<String> architectures,
      final String md5,
      final String sha256)
  {
    String date = DateFormatUtils.format(new Date(), PATTERN_RFC1123, TimeZone.getTimeZone("GMT"));
    Paragraph p = new Paragraph(Arrays.asList(
        new ControlFile.ControlField("Suite", distribution),
        new ControlFile.ControlField("Codename", distribution), new ControlFile.ControlField("Components", "main"),
        new ControlFile.ControlField("Date", date),
        new ControlFile.ControlField("Architectures", String.join(StringUtils.SPACE, architectures)),
        new ControlFile.ControlField("SHA256", sha256), new ControlFile.ControlField("MD5Sum", md5)));
    return p.toString();
  }

  private String releaseIndexName(final String name) {
    AptContentFacet aptFacet = getRepository().facet(AptContentFacet.class);
    String dist = aptFacet.getDistribution();
    return "dists/" + dist + "/" + name;
  }

  private String packageIndexName(final String arch, final String ext) {
    AptContentFacet aptFacet = getRepository().facet(AptContentFacet.class);
    String dist = aptFacet.getDistribution();
    return "dists/" + dist + "/main/binary-" + arch + "/Packages" + ext;
  }

  private String packageRelativeIndexName(final String arch, final String ext) {
    return "main/binary-" + arch + "/Packages" + ext;
  }

  private void addSignatureItem(final StringBuilder builder,
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
}
