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
package org.sonatype.nexus.content.pypi.internal;

import java.util.Optional;

import javax.inject.Inject;
import javax.inject.Named;

import org.sonatype.nexus.common.hash.HashAlgorithm;
import org.sonatype.nexus.content.pypi.PypiContentFacet;
import org.sonatype.nexus.repository.content.facet.ContentFacetSupport;
import org.sonatype.nexus.repository.content.fluent.FluentAsset;
import org.sonatype.nexus.repository.content.fluent.FluentComponent;
import org.sonatype.nexus.repository.content.store.FormatStoreManager;
import org.sonatype.nexus.repository.pypi.internal.PyPiFormat;
import org.sonatype.nexus.repository.view.Payload;
import org.sonatype.nexus.repository.view.payloads.TempBlob;

import com.google.common.collect.ImmutableList;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.sonatype.nexus.common.hash.HashAlgorithm.MD5;
import static org.sonatype.nexus.common.hash.HashAlgorithm.SHA1;
import static org.sonatype.nexus.content.pypi.internal.PyPiDataUtils.copyFormatAttributes;
import static org.sonatype.nexus.repository.pypi.internal.PyPiStorageUtils.getNameAttributes;

/**
 * @since 3.next
 */
@Named(PyPiFormat.NAME)
public class PypiContentFacetImpl
    extends ContentFacetSupport
    implements PypiContentFacet
{
  private static final Iterable<HashAlgorithm> HASHING = ImmutableList.of(MD5, SHA1);

  @Inject
  public PypiContentFacetImpl(
      @Named(PyPiFormat.NAME) final FormatStoreManager formatStoreManager)
  {
    super(formatStoreManager);
  }

  @Override
  public Iterable<FluentAsset> browseAssets() {
    return assets().browse(Integer.MAX_VALUE, null);
  }

  @Override
  public Optional<FluentAsset> getAsset(final String path) {
    return assets().path(path).find();
  }

  @Override
  public boolean delete(final String path) {
    return assets().path(path).find().map(FluentAsset::delete).orElse(false);
  }

  @Override
  public FluentAsset findOrCreateAsset(
      final String packagePath,
      final FluentComponent component, final String assetKind)
  {
    return assets()
        .path(packagePath)
        .kind(assetKind)
        .component(component)
        .getOrCreate();
  }

  @Override
  public FluentComponent findOrCreateComponent(
      final String name,
      final String version,
      final String normalizedName)
  {
    FluentComponent component = components().name(normalizedName).version(version).getOrCreate();
    copyFormatAttributes(component, getNameAttributes(name));
    return component;
  }

  @Override
  public TempBlob getTempBlob(final Payload payload) {
    checkNotNull(payload);
    return blobs().ingest(payload, HASHING);
  }
}
