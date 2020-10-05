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

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import javax.inject.Named;

import org.sonatype.nexus.common.collect.AttributesMap;
import org.sonatype.nexus.common.hash.HashAlgorithm;
import org.sonatype.nexus.content.pypi.PypiContentFacet;
import org.sonatype.nexus.repository.Facet.Exposed;
import org.sonatype.nexus.repository.FacetSupport;
import org.sonatype.nexus.repository.content.AssetBlob;
import org.sonatype.nexus.repository.content.fluent.FluentAsset;
import org.sonatype.nexus.repository.content.fluent.FluentComponent;
import org.sonatype.nexus.repository.pypi.internal.AssetKind;
import org.sonatype.nexus.repository.pypi.internal.PyPiInfoUtils;
import org.sonatype.nexus.repository.view.Content;
import org.sonatype.nexus.repository.view.payloads.TempBlob;
import org.sonatype.nexus.repository.view.payloads.TempBlobPartPayload;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.sonatype.nexus.content.pypi.internal.ContentPypiPathUtils.packagesPath;
import static org.sonatype.nexus.content.pypi.internal.PyPiDataUtils.setFormatAttribute;
import static org.sonatype.nexus.repository.pypi.internal.PyPiAttributes.P_NAME;
import static org.sonatype.nexus.repository.pypi.internal.PyPiAttributes.P_SUMMARY;
import static org.sonatype.nexus.repository.pypi.internal.PyPiAttributes.P_VERSION;
import static org.sonatype.nexus.repository.pypi.internal.PyPiPathUtils.normalizeName;
import static org.sonatype.nexus.repository.pypi.internal.PyPiStorageUtils.validateMd5Hash;
import static org.sonatype.nexus.repository.storage.AssetEntityAdapter.P_ASSET_KIND;
import static org.sonatype.nexus.repository.view.Content.CONTENT_ETAG;

/**
 * @since 3.next
 */
@Named
@Exposed
public class PyPiHostedFacet
    extends FacetSupport
{
  public FluentAsset upload(final String filename,
                            final Map<String, String> attributes,
                            final TempBlobPartPayload payload)
  {
    return savePyPiWheelPayload(filename, attributes, payload);
  }

  public Content uploadSignature(
      final String name,
      final String version,
      final TempBlobPartPayload payload)
  {
    return storeGpgSignaturePayload(payload, name, version);
  }

  public String createPackagePath(final String name, final String version, final String filename) {
    final String normalizedName = normalizeName(name);
    return packagesPath(normalizedName, version, filename);
  }

  public Content getPackage(final String packagePath) {
    checkNotNull(packagePath);

    FluentAsset asset = facet(PypiContentFacet.class).getAsset(packagePath).orElse(null);
    if (asset == null) {
      return null;
    }
    AssetBlob blob = asset.blob().orElse(null);
    if (blob == null) {
      return null;
    }

    Content content = asset.download();
    mayAddEtag(content.getAttributes(), blob.checksums().get(HashAlgorithm.SHA1.toString()));
    return content;
  }

  public Map<String, String> extractMetadata(final TempBlob tempBlob) throws IOException {
    try (InputStream in = tempBlob.get()) {
      return PyPiInfoUtils.extractMetadata(in);
    }
  }

  private FluentAsset savePyPiWheelPayload(
      final String filename,
      final Map<String, String> attributes,
      final TempBlobPartPayload wheelPayload)
  {
    checkNotNull(filename);

    TempBlob tempBlob = wheelPayload.getTempBlob();
    String name = checkNotNull(attributes.get(P_NAME));
    String version = checkNotNull(attributes.get(P_VERSION));
    String normalizedName = normalizeName(name);
    String packagePath = createPackagePath(name, version, filename);

    validateMd5Hash(attributes, tempBlob);

    PypiContentFacet contentFacet = facet(PypiContentFacet.class);

    FluentComponent component = contentFacet.findOrCreateComponent(name, version, normalizedName);
    PyPiDataUtils.setFormatAttribute(component, P_SUMMARY, attributes.get(P_SUMMARY));
    PyPiDataUtils.setFormatAttribute(component, P_VERSION, version);
    //TODO If null, delete root metadata. Need to use IndexFacet

    FluentAsset asset = contentFacet.findOrCreateAsset(packagePath, component, AssetKind.PACKAGE.name());
    setFormatAttribute(asset, P_ASSET_KIND, AssetKind.PACKAGE.name());
    PyPiDataUtils.copyFormatAttributes(asset, attributes);

    return asset.attach(tempBlob).markAsCached(wheelPayload);
  }

  private Content storeGpgSignaturePayload(final TempBlobPartPayload gpgPayload,
                                           final String name,
                                           final String version)
  {
    PypiContentFacet contentFacet = facet(PypiContentFacet.class);

    FluentComponent component = contentFacet.findOrCreateComponent(name, version, normalizeName(name));
    //TODO If null, delete root metadata. Need to use IndexFacet

    FluentAsset asset = contentFacet.findOrCreateAsset(
        createPackagePath(name, version, gpgPayload.getName()),
        component,
        AssetKind.PACKAGE_SIGNATURE.name());
    asset.attach(gpgPayload.getTempBlob()).markAsCached(gpgPayload);
    return asset.download();
  }

  private void mayAddEtag(final AttributesMap attributesMap, final String hashCode) {
    if (attributesMap.contains(CONTENT_ETAG)) {
      return;
    }

    if (hashCode != null) {
      attributesMap.set(CONTENT_ETAG, "{SHA1{" + hashCode + "}}");
    }
  }
}
