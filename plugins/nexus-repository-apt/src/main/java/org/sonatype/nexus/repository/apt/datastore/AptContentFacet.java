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
package org.sonatype.nexus.repository.apt.datastore;

import java.io.IOException;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Named;

import org.sonatype.nexus.repository.Facet;
import org.sonatype.nexus.repository.apt.datastore.internal.AptFacetHelper;
import org.sonatype.nexus.repository.apt.internal.AptFormat;
import org.sonatype.nexus.repository.apt.internal.AptPackageParser;
import org.sonatype.nexus.repository.apt.internal.debian.PackageInfo;
import org.sonatype.nexus.repository.content.facet.ContentFacetSupport;
import org.sonatype.nexus.repository.content.fluent.FluentAsset;
import org.sonatype.nexus.repository.content.fluent.FluentComponent;
import org.sonatype.nexus.repository.content.store.FormatStoreManager;
import org.sonatype.nexus.repository.view.Content;
import org.sonatype.nexus.repository.view.Payload;
import org.sonatype.nexus.repository.view.payloads.TempBlob;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.sonatype.nexus.repository.apt.internal.debian.Utils.isDebPackageContentType;

/**
 * Apt content facet
 *
 * @since 3.next
 */
@Facet.Exposed
@Named(AptFormat.NAME)
public class AptContentFacet
    extends ContentFacetSupport
{
  @Inject
  public AptContentFacet(
      @Named(AptFormat.NAME) final FormatStoreManager formatStoreManager)
  {
    super(formatStoreManager);
  }

  public Content put(final String path, final Payload content) throws IOException {
    return put(path, content, null);
  }

  public Content put(final String path,
                     final Payload content,
                     @Nullable final PackageInfo packageInfo) throws IOException
  {
    String normalizedPath = AptFacetHelper.normalizeAssetPath(path);

    try (TempBlob tempBlob = blobs().ingest(content, AptFacetHelper.hashAlgorithms)) {
      PackageInfo info = packageInfo != null
          ? packageInfo
          : new PackageInfo(AptPackageParser.getDebControlFile(tempBlob.getBlob()));

      FluentAsset asset = isDebPackageContentType(normalizedPath)
          ? findOrCreateDebAsset(normalizedPath, tempBlob, info)
          : findOrCreateMetadataAsset(tempBlob, normalizedPath);

      return asset
          .markAsCached(content)
          .download();
    }
  }

  public FluentAsset findOrCreateDebAsset(final String path, final TempBlob tempBlob, final PackageInfo packageInfo)
  {
    return assets()
        .path(AptFacetHelper.normalizeAssetPath(path))
        .component(findOrCreateComponent(packageInfo))
        .blob(tempBlob)
        .save();
  }

  public FluentAsset findOrCreateMetadataAsset(final TempBlob tempBlob, final String path) {
    return assets()
        .path(AptFacetHelper.normalizeAssetPath(path))
        .blob(tempBlob)
        .save();
  }

  private FluentComponent findOrCreateComponent(final PackageInfo info) {
    String name = info.getPackageName();
    String version = info.getVersion();
    String architecture = info.getArchitecture();

    return components()
        .name(name)
        .version(version)
        .namespace(architecture)
        .getOrCreate();
  }

  public TempBlob getTempBlob(final Payload payload) {
    checkNotNull(payload);
    return blobs().ingest(payload, AptFacetHelper.hashAlgorithms);
  }
}
