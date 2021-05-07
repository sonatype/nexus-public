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
package org.sonatype.nexus.repository.pypi.datastore.internal;

import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Named;

import org.sonatype.nexus.common.hash.HashAlgorithm;
import org.sonatype.nexus.repository.content.AssetBlob;
import org.sonatype.nexus.repository.content.facet.ContentFacetSupport;
import org.sonatype.nexus.repository.content.fluent.FluentAsset;
import org.sonatype.nexus.repository.content.fluent.FluentComponent;
import org.sonatype.nexus.repository.content.fluent.FluentQuery;
import org.sonatype.nexus.repository.content.store.FormatStoreManager;
import org.sonatype.nexus.repository.pypi.AssetKind;
import org.sonatype.nexus.repository.pypi.PyPiFormat;
import org.sonatype.nexus.repository.pypi.datastore.PypiContentFacet;
import org.sonatype.nexus.repository.view.Content;
import org.sonatype.nexus.repository.view.Payload;
import org.sonatype.nexus.repository.view.payloads.TempBlob;
import org.sonatype.nexus.repository.view.payloads.TempBlobPartPayload;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.sonatype.nexus.common.entity.Continuations.iterableOf;
import static org.sonatype.nexus.common.entity.Continuations.streamOf;
import static org.sonatype.nexus.common.hash.HashAlgorithm.MD5;
import static org.sonatype.nexus.common.hash.HashAlgorithm.SHA1;
import static org.sonatype.nexus.repository.pypi.AssetKind.PACKAGE_SIGNATURE;
import static org.sonatype.nexus.repository.pypi.AssetKind.ROOT_INDEX;
import static org.sonatype.nexus.repository.pypi.PyPiAttributes.P_SUMMARY;
import static org.sonatype.nexus.repository.pypi.PyPiAttributes.P_VERSION;
import static org.sonatype.nexus.repository.pypi.PyPiPathUtils.normalizeName;
import static org.sonatype.nexus.repository.pypi.datastore.PyPiDataUtils.copyFormatAttributes;
import static org.sonatype.nexus.repository.pypi.datastore.PyPiDataUtils.setFormatAttribute;
import static org.sonatype.nexus.repository.pypi.datastore.internal.ContentPypiPathUtils.indexPath;
import static org.sonatype.nexus.repository.pypi.datastore.internal.ContentPypiPathUtils.packagesPath;
import static org.sonatype.nexus.repository.pypi.internal.PyPiStorageUtils.getNameAttributes;
import static org.sonatype.nexus.repository.pypi.internal.PyPiStorageUtils.mayAddEtag;
import static org.sonatype.nexus.repository.storage.AssetEntityAdapter.P_ASSET_KIND;

/**
 * @since 3.29
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
    return iterableOf(assets()::browse);
  }

  @Override
  public Optional<FluentAsset> getAsset(final String path) {
    checkNotNull(path);
    return assets().path(path).find();
  }

  @Override
  public boolean delete(final String path) {
    checkNotNull(path);
    return assets().path(path).find().map(FluentAsset::delete).orElse(false);
  }

  @Override
  public FluentAsset saveAsset(
      final String packagePath,
      final FluentComponent component,
      final String assetKind,
      final TempBlob tempBlob)
  {
    checkNotNull(packagePath);
    checkNotNull(component);
    checkNotNull(assetKind);
    checkNotNull(tempBlob);

    return assets()
        .path(packagePath)
        .kind(assetKind)
        .component(component)
        .blob(tempBlob)
        .save();
  }

  @Override
  public FluentAsset saveAsset(final String packagePath, final String assetKind, final TempBlob tempBlob) {
    checkNotNull(packagePath);
    checkNotNull(assetKind);
    checkNotNull(tempBlob);

    return assets()
        .path(packagePath)
        .kind(assetKind)
        .blob(tempBlob)
        .save();
  }

  @Override
  public boolean isComponentExists(final String name) {
    return componentsByName(name).count() > 0;
  }

  @Override
  public List<FluentAsset> assetsByComponentName(String name) {
    return streamOf(componentsByName(name)::browse)
        .flatMap(component -> component.assets().stream())
        .collect(Collectors.toList());
  }

  @Override
  public FluentComponent findOrCreateComponent(
      final String name,
      final String version,
      final String normalizedName)
  {
    checkNotNull(name);
    checkNotNull(version);
    checkNotNull(normalizedName);

    FluentComponent component = components().name(normalizedName).version(version).getOrCreate();
    copyFormatAttributes(component, getNameAttributes(name));
    return component;
  }

  @Override
  public TempBlob getTempBlob(final Payload payload) {
    checkNotNull(payload);
    return blobs().ingest(payload, HASHING);
  }

  @Override
  public TempBlob getTempBlob(final InputStream content, @Nullable final String contentType) {
    return blobs().ingest(content, contentType, HASHING);
  }

  @Override
  public FluentAsset putWheel(
      final String filename, final Map<String, String> attributes, final TempBlob tempBlob, final String name)
  {
    checkNotNull(filename);
    checkNotNull(attributes);
    checkNotNull(tempBlob);
    checkNotNull(name);

    String version = checkNotNull(attributes.get(P_VERSION));
    String normalizedName = normalizeName(name);
    String packagePath = createPackagePath(name, version, filename);

    FluentComponent component = findOrCreateComponent(name, version, normalizedName);
    setFormatAttribute(component, P_SUMMARY, attributes.get(P_SUMMARY));
    setFormatAttribute(component, P_VERSION, version);
    //TODO If null, delete root metadata. Need to use IndexFacet

    FluentAsset asset = saveAsset(packagePath, component, AssetKind.PACKAGE.name(), tempBlob);
    setFormatAttribute(asset, P_ASSET_KIND, AssetKind.PACKAGE.name());
    copyFormatAttributes(asset, attributes);

    return asset;
  }

  @Override
  public Content getPackage(final String packagePath) {
    checkNotNull(packagePath);

    FluentAsset asset = getAsset(packagePath).orElse(null);
    if (asset == null) {
      return null;
    }
    AssetBlob blob = asset.blob().orElse(null);
    if (blob == null) {
      return null;
    }

    Content content = asset.download();
    mayAddEtag(content.getAttributes(), blob.checksums().get(HashAlgorithm.SHA1.name()));
    return content;
  }

  @Override
  public Optional<Content> getIndex(final String name) {
    return assets().path(indexPath(name)).find().map(FluentAsset::download);
  }

  @Override
  public Content putIndex(final String name, final Payload index) {
    try (TempBlob blob = getTempBlob(index)) {
      return assets().path(indexPath(name)).kind(AssetKind.INDEX.name()).blob(blob).save().download();
    }
  }

  @Override
  public Content putRootIndex(final Payload rootIndex) {
    try (TempBlob tempBlob = getTempBlob(rootIndex)) {
      return saveRootIndex(tempBlob).download();
    }
  }

  @Override
  public Content putWheelSignature(
      final String name,
      final String version,
      final TempBlobPartPayload gpgPayload)
  {
    checkNotNull(name);
    checkNotNull(version);
    checkNotNull(gpgPayload);

    FluentComponent component = components().name(name).version(version).find()
        .orElseGet(() -> findOrCreateComponent(name, version, normalizeName(name)));

    return saveAsset(
        createPackagePath(name, version, gpgPayload.getName()),
        component,
        PACKAGE_SIGNATURE.name(),
        gpgPayload.getTempBlob()).markAsCached(gpgPayload).download();
  }

  private FluentQuery<FluentComponent>  componentsByName(final String name) {
    String filter = "name = #{filterParams.nameParam}";
    Map<String, Object> params = ImmutableMap.of("nameParam", name);
    return components().byFilter(filter, params);
  }

  private FluentAsset saveRootIndex(final TempBlob tempBlob) {
    return assets()
        .path(indexPath())
        .kind(ROOT_INDEX.name())
        .blob(tempBlob)
        .save();
  }

  private String createPackagePath(final String name, final String version, final String filename) {
    final String normalizedName = normalizeName(name);
    return packagesPath(normalizedName, version, filename);
  }
}
