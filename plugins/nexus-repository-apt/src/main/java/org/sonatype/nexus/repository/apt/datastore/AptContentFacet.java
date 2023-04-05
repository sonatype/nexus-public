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
import java.io.InputStream;
import java.util.Optional;

import javax.annotation.Nullable;

import org.sonatype.nexus.repository.Facet;
import org.sonatype.nexus.repository.apt.internal.debian.PackageInfo;
import org.sonatype.nexus.repository.content.facet.ContentFacet;
import org.sonatype.nexus.repository.content.fluent.FluentAsset;
import org.sonatype.nexus.repository.view.Content;
import org.sonatype.nexus.repository.view.Payload;
import org.sonatype.nexus.repository.view.payloads.TempBlob;

/**
 * Apt content facet
 *
 * @since 3.31
 */
@Facet.Exposed
public interface AptContentFacet
    extends ContentFacet
{
  String getDistribution();

  boolean isFlat();

  Optional<FluentAsset> getAsset(String path);

  Optional<Content> get(String assetPath);

  FluentAsset put(String path, Payload content) throws IOException;

  FluentAsset put(String path, Payload payload, @Nullable PackageInfo packageInfo) throws IOException;

  FluentAsset findOrCreateMetadataAsset(TempBlob tempBlob, String path);

  TempBlob getTempBlob(Payload payload);

  TempBlob getTempBlob(InputStream in, @Nullable String contentType);

  void deleteAssetsByPrefix(String pathPrefix);

  Iterable<FluentAsset> getAptPackageAssets();
}
