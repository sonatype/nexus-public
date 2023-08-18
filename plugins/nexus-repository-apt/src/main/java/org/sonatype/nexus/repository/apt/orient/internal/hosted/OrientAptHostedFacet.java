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
package org.sonatype.nexus.repository.apt.orient.internal.hosted;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import javax.inject.Named;

import org.sonatype.nexus.common.hash.HashAlgorithm;
import org.sonatype.nexus.repository.Facet;
import org.sonatype.nexus.repository.FacetSupport;
import org.sonatype.nexus.repository.IllegalOperationException;
import org.sonatype.nexus.repository.apt.internal.AptFacetHelper;
import org.sonatype.nexus.repository.apt.internal.AptPackageParser;
import org.sonatype.nexus.repository.apt.internal.debian.ControlFile;
import org.sonatype.nexus.repository.apt.internal.debian.ControlFile.Paragraph;
import org.sonatype.nexus.repository.apt.internal.debian.PackageInfo;
import org.sonatype.nexus.repository.apt.internal.hosted.AssetAction;
import org.sonatype.nexus.repository.apt.orient.OrientAptFacet;
import org.sonatype.nexus.repository.apt.orient.internal.hosted.metadata.OrientAptHostedMetadataFacet;
import org.sonatype.nexus.repository.storage.Asset;
import org.sonatype.nexus.repository.storage.Bucket;
import org.sonatype.nexus.repository.storage.StorageFacet;
import org.sonatype.nexus.repository.storage.StorageTx;
import org.sonatype.nexus.repository.transaction.TransactionalStoreBlob;
import org.sonatype.nexus.repository.view.Content;
import org.sonatype.nexus.repository.view.Payload;
import org.sonatype.nexus.repository.view.payloads.StreamPayload;
import org.sonatype.nexus.repository.view.payloads.TempBlob;
import org.sonatype.nexus.transaction.UnitOfWork;

import com.google.common.hash.HashCode;

import static org.sonatype.nexus.common.hash.HashAlgorithm.MD5;
import static org.sonatype.nexus.common.hash.HashAlgorithm.SHA1;
import static org.sonatype.nexus.common.hash.HashAlgorithm.SHA256;
import static org.sonatype.nexus.repository.storage.AssetEntityAdapter.P_ASSET_KIND;

/**
 * @since 3.17
 */
@Named
@Facet.Exposed
public class OrientAptHostedFacet
    extends FacetSupport
{
  private static final String P_INDEX_SECTION = "index_section";

  private static final String P_ARCHITECTURE = "architecture";

  private static final String P_PACKAGE_NAME = "package_name";

  private static final String P_PACKAGE_VERSION = "package_version";

  public Asset ingestAsset(final Payload body) throws IOException {
    StorageFacet storageFacet = facet(StorageFacet.class);
    try (TempBlob tempBlob = storageFacet.createTempBlob(body, AptFacetHelper.hashAlgorithms)) {
      ControlFile control = AptPackageParser
          .parsePackageInfo(tempBlob)
          .getControlFile();
      if (control == null) {
        throw new IllegalOperationException("Invalid Debian package supplied");
      }
      return ingestAsset(control, tempBlob, body.getSize(), body.getContentType());
    }
  }

  @TransactionalStoreBlob
  protected Asset ingestAsset(final ControlFile control, final TempBlob body, final long size, final String contentType)
      throws IOException
  {
    OrientAptFacet aptFacet = getRepository().facet(OrientAptFacet.class);
    StorageTx tx = UnitOfWork.currentTx();
    Bucket bucket = tx.findBucket(getRepository());

    PackageInfo info = new PackageInfo(control);
    String name = info.getPackageName();
    String version = info.getVersion();
    String architecture = info.getArchitecture();

    String assetPath = AptFacetHelper.buildAssetPath(name, version, architecture);

    Content content = aptFacet.put(
        assetPath,
        new StreamPayload(body, size, contentType),
        info);

    Asset asset = Content.findAsset(tx, bucket, content);
    String indexSection =
        buildIndexSection(control, asset.size(), asset.getChecksums(AptFacetHelper.hashAlgorithms), assetPath);
    asset.formatAttributes().set(P_ARCHITECTURE, architecture);
    asset.formatAttributes().set(P_PACKAGE_NAME, name);
    asset.formatAttributes().set(P_PACKAGE_VERSION, version);
    asset.formatAttributes().set(P_INDEX_SECTION, indexSection);
    asset.formatAttributes().set(P_ASSET_KIND, "DEB");
    tx.saveAsset(asset);

    return asset;
  }

  public void rebuildMetadata() throws IOException {
    metadata().rebuildMetadata(Collections.emptyList());
  }

  public void invalidateMetadata() {
    metadata().invalidateMetadata();
  }

  private String buildIndexSection(
      final ControlFile cf,
      final long size,
      final Map<HashAlgorithm, HashCode> hashes,
      final String assetPath)
  {
    Paragraph modified = cf.getParagraphs().get(0)
        .withFields(Arrays.asList(
            new ControlFile.ControlField("Filename", assetPath),
            new ControlFile.ControlField("Size", Long.toString(size)),
            new ControlFile.ControlField("MD5Sum", hashes.get(MD5).toString()),
            new ControlFile.ControlField("SHA1", hashes.get(SHA1).toString()),
            new ControlFile.ControlField("SHA256", hashes.get(SHA256).toString())));
    return modified.toString();
  }

  private OrientAptHostedMetadataFacet metadata() {
    return facet(OrientAptHostedMetadataFacet.class);
  }

  public static class AssetChange
  {
    public final AssetAction action;

    public final Asset asset;

    public AssetChange(final AssetAction action, final Asset asset) {
      super();
      this.action = action;
      this.asset = asset;
    }
  }
}
