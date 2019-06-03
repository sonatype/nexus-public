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
package org.sonatype.nexus.repository.apt.internal.hosted;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Named;

import org.sonatype.nexus.common.hash.HashAlgorithm;
import org.sonatype.nexus.orient.entity.AttachedEntityHelper;
import org.sonatype.nexus.repository.Facet;
import org.sonatype.nexus.repository.FacetSupport;
import org.sonatype.nexus.repository.IllegalOperationException;
import org.sonatype.nexus.repository.apt.AptFacet;
import org.sonatype.nexus.repository.apt.internal.AptMimeTypes;
import org.sonatype.nexus.repository.apt.internal.AptPackageParser;
import org.sonatype.nexus.repository.apt.internal.FacetHelper;
import org.sonatype.nexus.repository.apt.internal.debian.ControlFile;
import org.sonatype.nexus.repository.apt.internal.debian.ControlFile.Paragraph;
import org.sonatype.nexus.repository.apt.internal.debian.PackageInfo;
import org.sonatype.nexus.repository.apt.internal.gpg.AptSigningFacet;
import org.sonatype.nexus.repository.storage.Asset;
import org.sonatype.nexus.repository.storage.Bucket;
import org.sonatype.nexus.repository.storage.StorageFacet;
import org.sonatype.nexus.repository.storage.StorageTx;
import org.sonatype.nexus.repository.storage.TempBlob;
import org.sonatype.nexus.repository.transaction.TransactionalStoreBlob;
import org.sonatype.nexus.repository.transaction.TransactionalStoreMetadata;
import org.sonatype.nexus.repository.view.Content;
import org.sonatype.nexus.repository.view.Payload;
import org.sonatype.nexus.repository.view.payloads.BytesPayload;
import org.sonatype.nexus.repository.view.payloads.StreamPayload;
import org.sonatype.nexus.transaction.UnitOfWork;

import com.google.common.base.Charsets;
import com.google.common.hash.HashCode;
import com.orientechnologies.orient.core.record.impl.ODocument;
import org.apache.commons.io.IOUtils;
import org.apache.http.client.utils.DateUtils;

import static org.sonatype.nexus.common.hash.HashAlgorithm.MD5;
import static org.sonatype.nexus.common.hash.HashAlgorithm.SHA1;
import static org.sonatype.nexus.common.hash.HashAlgorithm.SHA256;
import static org.sonatype.nexus.repository.apt.internal.ReleaseName.INRELEASE;
import static org.sonatype.nexus.repository.apt.internal.ReleaseName.RELEASE;
import static org.sonatype.nexus.repository.apt.internal.ReleaseName.RELEASE_GPG;
import static org.sonatype.nexus.repository.storage.AssetEntityAdapter.P_ASSET_KIND;
import static org.sonatype.nexus.repository.storage.MetadataNodeEntityAdapter.P_BUCKET;
import static org.sonatype.nexus.repository.storage.MetadataNodeEntityAdapter.P_NAME;

/**
 * @since 3.next
 */
@Named
@Facet.Exposed
public class AptHostedFacet
    extends FacetSupport
{
  private static final String P_INDEX_SECTION = "index_section";
  private static final String P_ARCHITECTURE = "architecture";
  private static final String P_PACKAGE_NAME = "package_name";
  private static final String P_PACKAGE_VERSION = "package_version";

  private static final String SELECT_HOSTED_ASSETS =
      "SELECT " +
      "name, " +
      "attributes.apt.index_section AS index_section, " +
      "attributes.apt.architecture AS architecture " +
      "FROM asset " +
      "WHERE bucket=:bucket " +
      "AND attributes.apt.asset_kind=:asset_kind";

  public Asset ingestAsset(Payload body) throws IOException {
    StorageFacet storageFacet = facet(StorageFacet.class);
    try (TempBlob tempBlob = storageFacet.createTempBlob(body, FacetHelper.hashAlgorithms)) {
      ControlFile control = AptPackageParser.parsePackage(tempBlob);
      if (control == null) {
        throw new IllegalOperationException("Invalid Debian package supplied");
      }
      return ingestAsset(control, tempBlob, body.getSize(), body.getContentType());
    }
  }

  @TransactionalStoreBlob
  protected Asset ingestAsset(ControlFile control, TempBlob body, long size, String contentType) throws IOException {
    AptFacet aptFacet = getRepository().facet(AptFacet.class);
    StorageTx tx = UnitOfWork.currentTx();
    Bucket bucket = tx.findBucket(getRepository());

    PackageInfo info = new PackageInfo(control);
    String name = info.getPackageName();
    String version = info.getVersion();
    String architecture = info.getArchitecture();

    String assetPath = FacetHelper.buildAssetPath(name, version, architecture);

    Content content = aptFacet.put(
        assetPath,
        new StreamPayload(() -> body.get(), size, contentType),
        info);

    Asset asset = Content.findAsset(tx, bucket, content);
    String indexSection =
        buildIndexSection(control, asset.size(), asset.getChecksums(FacetHelper.hashAlgorithms), assetPath);
    asset.formatAttributes().set(P_ARCHITECTURE, architecture);
    asset.formatAttributes().set(P_PACKAGE_NAME, name);
    asset.formatAttributes().set(P_PACKAGE_VERSION, version);
    asset.formatAttributes().set(P_INDEX_SECTION, indexSection);
    asset.formatAttributes().set(P_ASSET_KIND, "DEB");
    tx.saveAsset(asset);

    List<AssetChange> changes = new ArrayList<>();
    changes.add(new AssetChange(AssetAction.ADDED, asset));

    rebuildIndexesInTransaction(tx, changes);
    return asset;
  }

  void rebuildIndexes(List<AssetChange> changes) throws IOException {
    StorageTx tx = UnitOfWork.currentTx();
    rebuildIndexesInTransaction(tx, changes);
  }

  @TransactionalStoreMetadata
  private void rebuildIndexesInTransaction(StorageTx tx, List<AssetChange> changes) throws IOException {
    AptFacet aptFacet = getRepository().facet(AptFacet.class);
    AptSigningFacet signingFacet = getRepository().facet(AptSigningFacet.class);
    Bucket bucket = tx.findBucket(getRepository());

    StringBuilder sha256Builder = new StringBuilder();
    StringBuilder md5Builder = new StringBuilder();
    String releaseFile;
    try (CompressingTempFileStore store = buildPackageIndexes(tx, bucket, changes)) {
      for (Map.Entry<String, CompressingTempFileStore.FileMetadata> entry : store.getFiles().entrySet()) {
        Content plainContent = aptFacet.put(
            packageIndexName(entry.getKey(), ""),
            new StreamPayload(entry.getValue().plainSupplier(), entry.getValue().plainSize(), AptMimeTypes.TEXT));
        addSignatureItem(md5Builder, MD5, plainContent, packageRelativeIndexName(entry.getKey(), ""));
        addSignatureItem(sha256Builder, SHA256, plainContent, packageRelativeIndexName(entry.getKey(), ""));

        Content gzContent = aptFacet.put(
            packageIndexName(entry.getKey(), ".gz"),
            new StreamPayload(entry.getValue().gzSupplier(), entry.getValue().bzSize(), AptMimeTypes.GZIP));
        addSignatureItem(md5Builder, MD5, gzContent, packageRelativeIndexName(entry.getKey(), ".gz"));
        addSignatureItem(sha256Builder, SHA256, gzContent, packageRelativeIndexName(entry.getKey(), ".gz"));

        Content bzContent = aptFacet.put(
            packageIndexName(entry.getKey(), ".bz2"),
            new StreamPayload(entry.getValue().bzSupplier(), entry.getValue().bzSize(), AptMimeTypes.BZIP));
        addSignatureItem(md5Builder, MD5, bzContent, packageRelativeIndexName(entry.getKey(), ".bz2"));
        addSignatureItem(sha256Builder, SHA256, bzContent, packageRelativeIndexName(entry.getKey(), ".bz2"));
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

  private String buildReleaseFile(String distribution, Collection<String> architectures, String md5, String sha256) {
    Paragraph p = new Paragraph(Arrays.asList(
        new ControlFile.ControlField("Suite", distribution),
        new ControlFile.ControlField("Codename", distribution), new ControlFile.ControlField("Components", "main"),
        new ControlFile.ControlField("Date", DateUtils.formatDate(new Date())),
        new ControlFile.ControlField("Architectures", architectures.stream().collect(Collectors.joining(" "))),
        new ControlFile.ControlField("SHA256", sha256), new ControlFile.ControlField("MD5Sum", md5)));
    return p.toString();
  }

  private CompressingTempFileStore buildPackageIndexes(StorageTx tx, Bucket bucket, List<AssetChange> changes)
      throws IOException
  {
    CompressingTempFileStore result = new CompressingTempFileStore();
    Map<String, Writer> streams = new HashMap<>();
    boolean ok = false;
    try {
      Map<String, Object> sqlParams = new HashMap<>();
      sqlParams.put(P_BUCKET, AttachedEntityHelper.id(bucket));
      sqlParams.put(P_ASSET_KIND, "DEB");

      // NOTE:  We exclude added assets as well to account for the case where we are replacing an asset
      Set<String> excludeNames = changes.stream().map(change -> change.asset.name()).collect(Collectors.toSet());

      Iterable<ODocument> browse = tx.browse(SELECT_HOSTED_ASSETS, sqlParams);

      for (ODocument document : browse) {
        String name = document.field(P_NAME, String.class);
        String arch = document.field(P_ARCHITECTURE, String.class);
        Writer outWriter = streams.computeIfAbsent(arch, result::openOutput);
        if (!excludeNames.contains(name)) {
          String indexSection = document.field(P_INDEX_SECTION, String.class);
          outWriter.write(indexSection);
          outWriter.write("\n\n");
        }
      }

      List<Asset> addAssets = changes.stream().filter(change -> change.action == AssetAction.ADDED)
          .map(change -> change.asset).collect(Collectors.toList());

      // HACK: tx.browse won't see changes in the current transaction, so we have to manually add these in here
      for (Asset asset : addAssets) {
        String arch = asset.formatAttributes().get(P_ARCHITECTURE, String.class);
        String indexSection = asset.formatAttributes().get(P_INDEX_SECTION, String.class);
        Writer outWriter = streams.computeIfAbsent(arch, result::openOutput);
        outWriter.write(indexSection);
        outWriter.write("\n\n");
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

  private String buildIndexSection(ControlFile cf, long size, Map<HashAlgorithm, HashCode> hashes, String assetPath) {
    Paragraph modified = cf.getParagraphs().get(0)
        .withFields(Arrays.asList(
            new ControlFile.ControlField("Filename", assetPath),
            new ControlFile.ControlField("Size", Long.toString(size)),
            new ControlFile.ControlField("MD5Sum", hashes.get(MD5).toString()),
            new ControlFile.ControlField("SHA1", hashes.get(SHA1).toString()),
            new ControlFile.ControlField("SHA256", hashes.get(SHA256).toString())));
    return modified.toString();
  }

  private String releaseIndexName(String name) {
    AptFacet aptFacet = getRepository().facet(AptFacet.class);
    String dist = aptFacet.getDistribution();
    return "dists/" + dist + "/" + name;
  }

  private String packageIndexName(String arch, String ext) {
    AptFacet aptFacet = getRepository().facet(AptFacet.class);
    String dist = aptFacet.getDistribution();
    return "dists/" + dist + "/main/binary-" + arch + "/Packages" + ext;
  }

  private String packageRelativeIndexName(String arch, String ext) {
    return "main/binary-" + arch + "/Packages" + ext;
  }

  private void addSignatureItem(StringBuilder builder, HashAlgorithm algo, Content content, String filename) {
    Map<HashAlgorithm, HashCode> hashMap = content.getAttributes().get(Content.CONTENT_HASH_CODES_MAP,
        Content.T_CONTENT_HASH_CODES_MAP);
    builder.append("\n ");
    builder.append(hashMap.get(algo).toString());
    builder.append(" ");
    builder.append(content.getSize());
    builder.append(" ");
    builder.append(filename);
  }

  public enum AssetAction
  {
    ADDED, REMOVED
  }

  public static class AssetChange
  {
    final AssetAction action;

    public final Asset asset;

    AssetChange(AssetAction action, Asset asset) {
      super();
      this.action = action;
      this.asset = asset;
    }
  }
}
