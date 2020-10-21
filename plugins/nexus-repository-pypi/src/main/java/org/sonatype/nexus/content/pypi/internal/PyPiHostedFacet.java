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
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.stream.Collectors;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Named;

import org.sonatype.nexus.common.collect.AttributesMap;
import org.sonatype.nexus.common.hash.HashAlgorithm;
import org.sonatype.nexus.common.template.TemplateHelper;
import org.sonatype.nexus.content.pypi.PypiContentFacet;
import org.sonatype.nexus.repository.Facet.Exposed;
import org.sonatype.nexus.repository.FacetSupport;
import org.sonatype.nexus.repository.content.AssetBlob;
import org.sonatype.nexus.repository.content.fluent.FluentAsset;
import org.sonatype.nexus.repository.content.fluent.FluentComponent;
import org.sonatype.nexus.repository.content.fluent.FluentComponents;
import org.sonatype.nexus.repository.pypi.internal.AssetKind;
import org.sonatype.nexus.repository.pypi.internal.PyPiAttributes;
import org.sonatype.nexus.repository.pypi.internal.PyPiFormat;
import org.sonatype.nexus.repository.pypi.internal.PyPiIndexFacet;
import org.sonatype.nexus.repository.pypi.internal.PyPiInfoUtils;
import org.sonatype.nexus.repository.pypi.internal.PyPiLink;
import org.sonatype.nexus.repository.pypi.internal.SignablePyPiPackage;
import org.sonatype.nexus.repository.view.Content;
import org.sonatype.nexus.repository.view.Payload;
import org.sonatype.nexus.repository.view.payloads.BytesPayload;
import org.sonatype.nexus.repository.view.payloads.StringPayload;
import org.sonatype.nexus.repository.view.payloads.TempBlob;
import org.sonatype.nexus.repository.view.payloads.TempBlobPartPayload;

import org.apache.commons.lang.StringUtils;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.sonatype.nexus.content.pypi.internal.ContentPypiPathUtils.indexPath;
import static org.sonatype.nexus.content.pypi.internal.ContentPypiPathUtils.packagesPath;
import static org.sonatype.nexus.content.pypi.internal.PyPiDataUtils.copyFormatAttributes;
import static org.sonatype.nexus.content.pypi.internal.PyPiDataUtils.getMd5;
import static org.sonatype.nexus.content.pypi.internal.PyPiDataUtils.setFormatAttribute;
import static org.sonatype.nexus.repository.pypi.internal.AssetKind.ROOT_INDEX;
import static org.sonatype.nexus.repository.pypi.internal.PyPiAttributes.P_NAME;
import static org.sonatype.nexus.repository.pypi.internal.PyPiAttributes.P_SUMMARY;
import static org.sonatype.nexus.repository.pypi.internal.PyPiAttributes.P_VERSION;
import static org.sonatype.nexus.repository.pypi.internal.PyPiIndexUtils.buildIndexPage;
import static org.sonatype.nexus.repository.pypi.internal.PyPiIndexUtils.buildRootIndexPage;
import static org.sonatype.nexus.repository.pypi.internal.PyPiPathUtils.normalizeName;
import static org.sonatype.nexus.repository.pypi.internal.PyPiStorageUtils.mayAddEtag;
import static org.sonatype.nexus.repository.pypi.internal.PyPiStorageUtils.validateMd5Hash;
import static org.sonatype.nexus.repository.storage.AssetEntityAdapter.P_ASSET_KIND;
import static org.sonatype.nexus.repository.view.ContentTypes.TEXT_HTML;

/**
 * @since 3.next
 */
@Named
@Exposed
public class PyPiHostedFacet
    extends FacetSupport
{
  private final TemplateHelper templateHelper;

  @Inject
  public PyPiHostedFacet(final TemplateHelper templateHelper) {
    this.templateHelper = templateHelper;
  }

  public FluentAsset upload(final SignablePyPiPackage pyPiPackage) throws IOException {
    FluentAsset savedPiPyPackage;
    Map<String, String> attributes = pyPiPackage.getAttributes();
    try (
        TempBlobPartPayload wheelPayload = pyPiPackage.getWheelPayload();
        TempBlobPartPayload gpgPayload = pyPiPackage.getGpgSignature()) {
      savedPiPyPackage = storeWheelAndSignaturePayloads(wheelPayload, gpgPayload, attributes);
    }
    catch (Exception e) {
      log.info("Unable to store wheel and gpg signature", e);
      throw e;
    }
    return savedPiPyPackage;
  }

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
    mayAddEtag(content.getAttributes(), blob.checksums().get(HashAlgorithm.SHA1.name()));
    return content;
  }

  public Content getRootIndex() {
    FluentAsset asset = facet(PypiContentFacet.class).getAsset(indexPath()).orElseGet(this::createRootIndex);
    return asset.download();
  }

  public Content getIndex(final String name) {
    checkNotNull(name);

    PypiContentFacet contentFacet = facet(PypiContentFacet.class);
    // If we don't even have a single component entry, then nothing has been uploaded yet
    if (!contentFacet.isComponentExists(name)) {
      return null;
    }

    String indexPath = indexPath(name);
    FluentAsset savedIndex = contentFacet.getAsset(indexPath).orElseGet(() -> createIndexAsset(name, indexPath));
    return savedIndex.download();
  }

  public Map<String, String> extractMetadata(final TempBlob tempBlob) throws IOException {
    try (InputStream in = tempBlob.get()) {
      return PyPiInfoUtils.extractMetadata(in);
    }
  }

  private FluentAsset createRootIndex() {
    Collection<PyPiLink> links = findAllLinks();
    FluentAsset asset = createRootIndexAsset();

    String rootIndexHtml = buildRootIndexPage(templateHelper, links);
    try (TempBlob tempBlob = facet(PypiContentFacet.class).getTempBlob(new StringPayload(rootIndexHtml, TEXT_HTML))) {
      return asset.attach(tempBlob);
    }
  }

  protected FluentAsset createRootIndexAsset() {
    return facet(PypiContentFacet.class).assets()
        .path(indexPath()).getOrCreate()
        .kind(ROOT_INDEX.name());
  }

  protected Collection<PyPiLink> findAllLinks() {
    Map<String, PyPiLink> links = new TreeMap<>();
    FluentComponents components = facet(PypiContentFacet.class).components();

    components.browse(Integer.MAX_VALUE, null).stream()
        .forEach(c -> links.put(c.name(), new PyPiLink(c.name(), c.name() + "/")));
    return links.values();
  }

  private FluentAsset createIndexAsset(final String name, final String indexPath)
  {
    String html = buildIndex(name);
    Payload indexPayload = new BytesPayload(html.getBytes(UTF_8), TEXT_HTML);
    PypiContentFacet contentFacet = facet(PypiContentFacet.class);

    try (TempBlob tempBlob = contentFacet.getTempBlob(indexPayload)) {
      return facet(PypiContentFacet.class)
          .findOrCreateAsset(indexPath, AssetKind.INDEX.name())
          .attach(tempBlob);
    }
  }

  private String buildIndex(final String name) {
    List<FluentAsset> assets = facet(PypiContentFacet.class).assetsByComponentName(name);

    List<PyPiLink> links = assets.stream()
        .map(this::buildPyPiLink)
        .filter(Optional::isPresent)
        .map(Optional::get)
        .collect(Collectors.toList());

    return buildIndexPage(templateHelper, name, links);
  }

  private Optional<PyPiLink> buildPyPiLink(FluentAsset asset) {
    AttributesMap pypiAttributes = asset.attributes().child(PyPiFormat.NAME);
    String path = asset.path();
    String file = path.substring(path.lastIndexOf('/') + 1);
    Optional<String> md5 = getMd5(asset);
    if (!md5.isPresent()) {
      return Optional.empty();
    }
    String link = String.format("../../%s#md5=%s", path, md5.get());
    String dataRequiresPython = pypiAttributes.get(PyPiAttributes.P_REQUIRES_PYTHON, String.class, StringUtils.EMPTY);

    return Optional.of(new PyPiLink(file, link, dataRequiresPython));
  }

  private FluentAsset storeWheelAndSignaturePayloads(final TempBlobPartPayload wheelPayload,
                                                     @Nullable final TempBlobPartPayload gpgPayload,
                                                     final Map<String, String> attributes)
  {
    FluentAsset wheelAsset = savePyPiWheelPayload(wheelPayload.getName(), attributes, wheelPayload);
    if (gpgPayload != null) {
      storeGpgSignaturePayload(gpgPayload, attributes.get(P_NAME), attributes.get(P_VERSION));
    }
    return wheelAsset;
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

    PyPiIndexFacet indexFacet = facet(PyPiIndexFacet.class);
    // A package has been added or redeployed and therefore the cached index is no longer relevant
    indexFacet.deleteIndex(name);

    PypiContentFacet contentFacet = facet(PypiContentFacet.class);
    if (!contentFacet.isComponentExists(name)) {
      indexFacet.deleteRootIndex();
    }

    FluentComponent component = contentFacet.findOrCreateComponent(name, version, normalizedName);
    setFormatAttribute(component, P_SUMMARY, attributes.get(P_SUMMARY));
    setFormatAttribute(component, P_VERSION, version);
    //TODO If null, delete root metadata. Need to use IndexFacet

    FluentAsset asset = contentFacet.findOrCreateAsset(packagePath, component, AssetKind.PACKAGE.name());
    setFormatAttribute(asset, P_ASSET_KIND, AssetKind.PACKAGE.name());
    copyFormatAttributes(asset, attributes);

    return asset.attach(tempBlob).markAsCached(wheelPayload);
  }

  private Content storeGpgSignaturePayload(final TempBlobPartPayload gpgPayload,
                                           final String name,
                                           final String version)
  {
    PypiContentFacet contentFacet = facet(PypiContentFacet.class);

    FluentComponent component = contentFacet.components().name(name).version(version).find().orElseGet(() -> {
      facet(PyPiIndexFacet.class).deleteRootIndex();
      return contentFacet.findOrCreateComponent(name, version, normalizeName(name));
    });

    FluentAsset asset = contentFacet.findOrCreateAsset(
        createPackagePath(name, version, gpgPayload.getName()),
        component,
        AssetKind.PACKAGE_SIGNATURE.name());
    asset.attach(gpgPayload.getTempBlob()).markAsCached(gpgPayload);
    return asset.download();
  }
}
