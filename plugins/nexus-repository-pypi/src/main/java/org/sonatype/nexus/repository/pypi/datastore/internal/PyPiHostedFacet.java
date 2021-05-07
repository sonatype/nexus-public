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
import org.sonatype.nexus.common.template.TemplateHelper;
import org.sonatype.nexus.repository.Facet.Exposed;
import org.sonatype.nexus.repository.FacetSupport;
import org.sonatype.nexus.repository.content.fluent.FluentAsset;
import org.sonatype.nexus.repository.content.fluent.FluentComponents;
import org.sonatype.nexus.repository.pypi.PyPiAttributes;
import org.sonatype.nexus.repository.pypi.PyPiFormat;
import org.sonatype.nexus.repository.pypi.PyPiInfoUtils;
import org.sonatype.nexus.repository.pypi.datastore.PypiContentFacet;
import org.sonatype.nexus.repository.pypi.internal.PyPiIndexFacet;
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
import static org.sonatype.nexus.common.entity.Continuations.iterableOf;
import static org.sonatype.nexus.repository.pypi.PyPiAttributes.P_NAME;
import static org.sonatype.nexus.repository.pypi.PyPiAttributes.P_VERSION;
import static org.sonatype.nexus.repository.pypi.PyPiPathUtils.normalizeName;
import static org.sonatype.nexus.repository.pypi.datastore.PyPiDataUtils.getMd5;
import static org.sonatype.nexus.repository.pypi.datastore.internal.ContentPypiPathUtils.indexPath;
import static org.sonatype.nexus.repository.pypi.datastore.internal.ContentPypiPathUtils.packagesPath;
import static org.sonatype.nexus.repository.pypi.internal.PyPiIndexUtils.buildIndexPage;
import static org.sonatype.nexus.repository.pypi.internal.PyPiIndexUtils.buildRootIndexPage;
import static org.sonatype.nexus.repository.pypi.internal.PyPiStorageUtils.validateMd5Hash;
import static org.sonatype.nexus.repository.view.ContentTypes.TEXT_HTML;

/**
 * @since 3.29
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

  public Content upload(final String filename,
                        final Map<String, String> attributes,
                        final TempBlobPartPayload payload)
  {
    return savePyPiWheelPayload(filename, attributes, payload).download();
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
    return contentFacet().getPackage(packagePath);
  }

  public Content getRootIndex() {
    PypiContentFacet contentFacet = contentFacet();
    return contentFacet.getAsset(indexPath()).map(FluentAsset::download)
        .orElseGet(() -> contentFacet.putRootIndex(createRootIndex()));
  }

  public Content getIndex(final String name) {
    checkNotNull(name);

    PypiContentFacet contentFacet = contentFacet();
    // If we don't even have a single component entry, then nothing has been uploaded yet
    if (!contentFacet.isComponentExists(name)) {
      return null;
    }
    return contentFacet.getIndex(name).orElseGet(() -> contentFacet.putIndex(name, createIndex(name)));
  }

  public Map<String, String> extractMetadata(final TempBlob tempBlob) throws IOException {
    try (InputStream in = tempBlob.get()) {
      return PyPiInfoUtils.extractMetadata(in);
    }
  }

  private PypiContentFacet contentFacet() {
    return facet(PypiContentFacet.class);
  }

  private Payload createRootIndex() {
    Collection<PyPiLink> links = findAllLinks();
    String rootIndexHtml = buildRootIndexPage(templateHelper, links);
    return new StringPayload(rootIndexHtml, TEXT_HTML);
  }

  private Collection<PyPiLink> findAllLinks() {
    Map<String, PyPiLink> links = new TreeMap<>();
    FluentComponents components = contentFacet().components();

    iterableOf(components::browse)
        .forEach(c -> links.put(c.name(), new PyPiLink(c.name(), c.name() + "/")));
    return links.values();
  }

  private Payload createIndex(final String name) {
    String html = buildIndex(name);
    return new BytesPayload(html.getBytes(UTF_8), TEXT_HTML);
  }

  private String buildIndex(final String name) {
    List<FluentAsset> assets = contentFacet().assetsByComponentName(name);

    List<PyPiLink> links = assets.stream()
        .map(this::buildPyPiLink)
        .filter(Optional::isPresent)
        .map(Optional::get)
        .collect(Collectors.toList());

    return buildIndexPage(templateHelper, name, links);
  }

  private Optional<PyPiLink> buildPyPiLink(FluentAsset asset) {
    AttributesMap pypiAttributes = asset.attributes().child(PyPiFormat.NAME);
    String uri = asset.path().startsWith("/") ? asset.path().substring(1) : asset.path();
    String file = uri.substring(uri.lastIndexOf('/') + 1);
    Optional<String> md5 = getMd5(asset);
    if (!md5.isPresent()) {
      return Optional.empty();
    }
    String link = String.format("../../%s#md5=%s", uri, md5.get());
    String dataRequiresPython = pypiAttributes.get(PyPiAttributes.P_REQUIRES_PYTHON, String.class, StringUtils.EMPTY);

    return Optional.of(new PyPiLink(file, link, dataRequiresPython));
  }

  private FluentAsset storeWheelAndSignaturePayloads(
      final TempBlobPartPayload wheelPayload,
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
    checkNotNull(attributes);
    checkNotNull(wheelPayload);

    try (TempBlob tempBlob = wheelPayload.getTempBlob()) {
      String name = checkNotNull(attributes.get(P_NAME));

      validateMd5Hash(attributes, tempBlob);

      PyPiIndexFacet indexFacet = facet(PyPiIndexFacet.class);
      // A package has been added or redeployed and therefore the cached index is no longer relevant
      indexFacet.deleteIndex(name);

      PypiContentFacet contentFacet = facet(PypiContentFacet.class);
      if (!contentFacet.isComponentExists(name)) {
        indexFacet.deleteRootIndex();
      }

      return contentFacet.putWheel(filename, attributes, tempBlob, name).markAsCached(wheelPayload);
    }
  }

  private Content storeGpgSignaturePayload(final TempBlobPartPayload gpgPayload,
                                           final String name,
                                           final String version)
  {
    PypiContentFacet contentFacet = facet(PypiContentFacet.class);
    if (!contentFacet.isComponentExists(name)) {
      facet(PyPiIndexFacet.class).deleteRootIndex();
    }
    return contentFacet.putWheelSignature(name, version, gpgPayload);
  }
}
