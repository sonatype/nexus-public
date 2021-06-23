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

package org.sonatype.repository.conan.internal.datastore;

import java.util.Optional;

import javax.inject.Inject;
import javax.inject.Named;

import org.sonatype.nexus.repository.content.Component;
import org.sonatype.nexus.repository.content.facet.ContentFacet;
import org.sonatype.nexus.repository.content.facet.ContentFacetSupport;
import org.sonatype.nexus.repository.content.fluent.FluentAsset;
import org.sonatype.nexus.repository.content.store.FormatStoreManager;
import org.sonatype.nexus.repository.view.payloads.TempBlob;
import org.sonatype.repository.conan.ConanContentFacet;
import org.sonatype.repository.conan.internal.AssetKind;
import org.sonatype.repository.conan.internal.ConanFormat;
import org.sonatype.repository.conan.internal.metadata.ConanCoords;

import static org.sonatype.repository.conan.internal.AssetKind.CONAN_PACKAGE;

/**
 * @since 3.next
 */
@Named(ConanFormat.NAME)
public class ConanContentFacetImpl
    extends ContentFacetSupport
    implements ConanContentFacet
{
  @Inject
  protected ConanContentFacetImpl(@Named(ConanFormat.NAME) final FormatStoreManager formatStoreManager) {
    super(formatStoreManager);
  }

  @Override
  public Optional<FluentAsset> getAsset(final ConanCoords coords, final AssetKind assetKind) {
    return facet(ContentFacet.class)
        .assets()
        .path(ConanProxyHelper.getProxyAssetPath(coords, assetKind))
        .find();
  }

  @Override
  public FluentAsset putPackage(final TempBlob tempBlob, final ConanCoords coords, final AssetKind assetKind) {
    return facet(ConanContentFacet.class)
        .assets()
        .path(ConanProxyHelper.getProxyAssetPath(coords, assetKind))
        .component(getOrCreateComponent(coords))
        .kind(CONAN_PACKAGE.name())
        .blob(tempBlob)
        .save();
  }

  @Override
  public FluentAsset putMetadata(final TempBlob tempBlob, final ConanCoords coords, final AssetKind assetKind) {
    return facet(ConanContentFacet.class)
        .assets()
        .path(ConanProxyHelper.getProxyAssetPath(coords, assetKind))
        .component(getOrCreateComponent(coords))
        .kind(assetKind.name())
        .blob(tempBlob)
        .save();
  }

  private Component getOrCreateComponent(final ConanCoords coords) {
    return facet(ContentFacet.class)
        .components()
        .name(coords.getProject())
        .namespace(coords.getGroup())
        .version(ConanProxyHelper.getComponentVersion(coords))
        .getOrCreate();
  }
}
