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
package org.sonatype.nexus.repository.apt.orient.internal.hosted.metadata;

import java.io.IOException;
import java.io.Writer;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TimeZone;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.inject.Named;

import org.sonatype.nexus.common.cooperation2.Cooperation2;
import org.sonatype.nexus.common.cooperation2.Cooperation2Factory;
import org.sonatype.nexus.common.hash.HashAlgorithm;
import org.sonatype.nexus.common.time.Clock;
import org.sonatype.nexus.orient.entity.AttachedEntityHelper;
import org.sonatype.nexus.repository.Facet.Exposed;
import org.sonatype.nexus.repository.FacetSupport;
import org.sonatype.nexus.repository.apt.AptFormat;
import org.sonatype.nexus.repository.apt.internal.AptMimeTypes;
import org.sonatype.nexus.repository.apt.internal.debian.ControlFile;
import org.sonatype.nexus.repository.apt.internal.debian.ControlFile.Paragraph;
import org.sonatype.nexus.repository.apt.internal.gpg.AptSigningFacet;
import org.sonatype.nexus.repository.apt.internal.hosted.AssetAction;
import org.sonatype.nexus.repository.apt.internal.hosted.CompressingTempFileStore;
import org.sonatype.nexus.repository.apt.orient.OrientAptFacet;
import org.sonatype.nexus.repository.apt.orient.internal.hosted.OrientAptHostedFacet.AssetChange;
import org.sonatype.nexus.repository.config.Configuration;
import org.sonatype.nexus.repository.storage.Asset;
import org.sonatype.nexus.repository.storage.Bucket;
import org.sonatype.nexus.repository.storage.StorageTx;
import org.sonatype.nexus.repository.transaction.TransactionalStoreMetadata;
import org.sonatype.nexus.repository.view.Content;
import org.sonatype.nexus.repository.view.payloads.BytesPayload;
import org.sonatype.nexus.repository.view.payloads.StreamPayload;
import org.sonatype.nexus.transaction.UnitOfWork;

import com.google.common.base.Charsets;
import com.google.common.hash.HashCode;
import com.orientechnologies.orient.core.record.impl.ODocument;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.time.DateFormatUtils;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.apache.http.protocol.HttpDateGenerator.PATTERN_RFC1123;
import static org.sonatype.nexus.common.hash.HashAlgorithm.MD5;
import static org.sonatype.nexus.common.hash.HashAlgorithm.SHA256;
import static org.sonatype.nexus.repository.apt.internal.ReleaseName.INRELEASE;
import static org.sonatype.nexus.repository.apt.internal.ReleaseName.RELEASE;
import static org.sonatype.nexus.repository.apt.internal.ReleaseName.RELEASE_GPG;
import static org.sonatype.nexus.repository.storage.AssetEntityAdapter.P_ASSET_KIND;
import static org.sonatype.nexus.repository.storage.MetadataNodeEntityAdapter.P_BUCKET;
import static org.sonatype.nexus.repository.storage.MetadataNodeEntityAdapter.P_NAME;

/**
 * Apt metadata facet. Holds the logic for metadata recalculation.
 */
@Named(AptFormat.NAME)
@Exposed
public class OrientAptHostedMetadataFacet
    extends FacetSupport
{
  private final Clock clock;

  private final Cooperation2Factory.Builder cooperationBuilder;

  private Cooperation2 cooperation;

  private static final String REBUILD_COOPERATION_KEY = "rebuild-apt-metadata";

  private static final String P_INDEX_SECTION = "index_section";

  private static final String P_ARCHITECTURE = "architecture";

  private static final String SELECT_HOSTED_ASSETS =
      "SELECT " +
          "name, " +
          "attributes.apt.index_section AS index_section, " +
          "attributes.apt.architecture AS architecture " +
          "FROM asset " +
          "WHERE bucket=:bucket " +
          "AND attributes.apt.asset_kind=:asset_kind";

  @Inject
  public OrientAptHostedMetadataFacet(
      final Clock clock,
      final Cooperation2Factory cooperationFactory,
      @Named("${nexus.apt.metadata.cooperation.enabled:-true}") final boolean cooperationEnabled,
      @Named("${nexus.apt.metadata.cooperation.majorTimeout:-0s}") final Duration majorTimeout,
      @Named("${nexus.apt.metadata.cooperation.minorTimeout:-30s}") final Duration minorTimeout,
      @Named("${nexus.apt.metadata.cooperation.threadsPerKey:-100}") final int threadsPerKey)
  {
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

  public void invalidateMetadata() {
    log.debug("Invalidating repository metadata: {}", getRepository().getName());
    try {
      getRepository().facet(OrientAptFacet.class).delete(releaseIndexName(INRELEASE));
    }
    catch (IOException e) {
      log.error("Failed to invalidate repository metadata: {}", getRepository().getName(), e);
    }
  }

  public Optional<Content> rebuildMetadata(final List<AssetChange> changeList) throws IOException {
    return Optional.ofNullable(
        cooperation
            .on(() -> doRebuildMetadata(changeList))
            .cooperate(REBUILD_COOPERATION_KEY)
    );
  }

  @TransactionalStoreMetadata
  Content doRebuildMetadata(final List<AssetChange> changes) throws IOException {
    log.debug("Starting rebuilding metadata at {}", getRepository().getName());
    OffsetDateTime rebuildStart = clock.clusterTime();

    StorageTx tx = UnitOfWork.currentTx();
    OrientAptFacet aptFacet = getRepository().facet(OrientAptFacet.class);
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

    Content releaseFileContent = aptFacet.put(releaseIndexName(RELEASE),
        new BytesPayload(releaseFile.getBytes(Charsets.UTF_8), AptMimeTypes.TEXT));
    byte[] inRelease = signingFacet.signInline(releaseFile);
    aptFacet.put(releaseIndexName(INRELEASE), new BytesPayload(inRelease, AptMimeTypes.TEXT));
    byte[] releaseGpg = signingFacet.signExternal(releaseFile);
    aptFacet.put(releaseIndexName(RELEASE_GPG), new BytesPayload(releaseGpg, AptMimeTypes.SIGNATURE));

    if (log.isDebugEnabled()) {
      long finishTime = System.currentTimeMillis();
      log.debug("Completed metadata rebuild in {}", finishTime - rebuildStart.toInstant().toEpochMilli());
    }

    return releaseFileContent;
  }

  private CompressingTempFileStore buildPackageIndexes(
      final StorageTx tx,
      final Bucket bucket,
      final List<AssetChange> changes)
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

  private void addSignatureItem(
      final StringBuilder builder,
      final HashAlgorithm algo,
      final Content content,
      final String filename)
  {
    Map<HashAlgorithm, HashCode> hashMap = content.getAttributes().get(Content.CONTENT_HASH_CODES_MAP,
        Content.T_CONTENT_HASH_CODES_MAP);
    builder.append("\n ");
    builder.append(hashMap.get(algo).toString());
    builder.append(" ");
    builder.append(content.getSize());
    builder.append(" ");
    builder.append(filename);
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
        new ControlFile.ControlField("Architectures", String.join(" ", architectures)),
        new ControlFile.ControlField("SHA256", sha256), new ControlFile.ControlField("MD5Sum", md5)));
    return p.toString();
  }

  private String packageIndexName(final String arch, final String ext) {
    OrientAptFacet aptFacet = getRepository().facet(OrientAptFacet.class);
    String dist = aptFacet.getDistribution();
    return "dists/" + dist + "/main/binary-" + arch + "/Packages" + ext;
  }

  private String packageRelativeIndexName(final String arch, final String ext) {
    return "main/binary-" + arch + "/Packages" + ext;
  }

  private String releaseIndexName(final String name) {
    OrientAptFacet aptFacet = getRepository().facet(OrientAptFacet.class);
    String dist = aptFacet.getDistribution();
    return "dists/" + dist + "/" + name;
  }
}
