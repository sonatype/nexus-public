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
package org.sonatype.nexus.repository.pypi.internal;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Named;

import org.sonatype.nexus.common.collect.AttributesMap;
import org.sonatype.nexus.common.collect.NestedAttributesMap;
import org.sonatype.nexus.common.hash.HashAlgorithm;
import org.sonatype.nexus.common.template.TemplateHelper;
import org.sonatype.nexus.repository.FacetSupport;
import org.sonatype.nexus.repository.IllegalOperationException;
import org.sonatype.nexus.repository.storage.Asset;
import org.sonatype.nexus.repository.storage.Bucket;
import org.sonatype.nexus.repository.storage.Component;
import org.sonatype.nexus.repository.storage.StorageFacet;
import org.sonatype.nexus.repository.storage.StorageTx;
import org.sonatype.nexus.repository.storage.TempBlob;
import org.sonatype.nexus.repository.transaction.TransactionalStoreBlob;
import org.sonatype.nexus.repository.transaction.TransactionalStoreMetadata;
import org.sonatype.nexus.repository.transaction.TransactionalTouchBlob;
import org.sonatype.nexus.repository.view.Content;
import org.sonatype.nexus.repository.view.payloads.StringPayload;
import org.sonatype.nexus.repository.view.payloads.TempBlobPartPayload;
import org.sonatype.nexus.transaction.Transactional;
import org.sonatype.nexus.transaction.UnitOfWork;

import com.google.common.hash.HashCode;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.sonatype.nexus.common.hash.HashAlgorithm.MD5;
import static org.sonatype.nexus.repository.pypi.internal.AssetKind.ROOT_INDEX;
import static org.sonatype.nexus.repository.pypi.internal.PyPiAttributes.P_NAME;
import static org.sonatype.nexus.repository.pypi.internal.PyPiAttributes.P_SUMMARY;
import static org.sonatype.nexus.repository.pypi.internal.PyPiAttributes.P_VERSION;
import static org.sonatype.nexus.repository.pypi.internal.PyPiDataUtils.HASH_ALGORITHMS;
import static org.sonatype.nexus.repository.pypi.internal.PyPiDataUtils.findAsset;
import static org.sonatype.nexus.repository.pypi.internal.PyPiDataUtils.findAssetsByComponentName;
import static org.sonatype.nexus.repository.pypi.internal.PyPiDataUtils.findComponent;
import static org.sonatype.nexus.repository.pypi.internal.PyPiDataUtils.findComponentExists;
import static org.sonatype.nexus.repository.pypi.internal.PyPiDataUtils.saveAsset;
import static org.sonatype.nexus.repository.pypi.internal.PyPiDataUtils.toContent;
import static org.sonatype.nexus.repository.pypi.internal.PyPiPathUtils.INDEX_PATH_PREFIX;
import static org.sonatype.nexus.repository.pypi.internal.PyPiPathUtils.normalizeName;
import static org.sonatype.nexus.repository.pypi.internal.PyPiPathUtils.packagesPath;
import static org.sonatype.nexus.repository.storage.AssetEntityAdapter.P_ASSET_KIND;
import static org.sonatype.nexus.repository.view.Content.CONTENT_ETAG;
import static org.sonatype.nexus.repository.view.ContentTypes.TEXT_HTML;

/**
 * {@link PyPiHostedFacet} implementation.
 *
 * @since 3.1
 */
@Named
public class PyPiHostedFacetImpl
    extends FacetSupport
    implements PyPiHostedFacet
{
  private final TemplateHelper templateHelper;

  @Inject
  public PyPiHostedFacetImpl(final TemplateHelper templateHelper) {
    this.templateHelper = checkNotNull(templateHelper);
  }

  @Override
  @TransactionalStoreBlob
  @Nullable
  public Content getRootIndex() {
    StorageTx tx = UnitOfWork.currentTx();
    Bucket bucket = tx.findBucket(getRepository());

    Asset asset = findAsset(tx, bucket, INDEX_PATH_PREFIX);
    if (asset == null) {
      try {
        return createAndSaveRootIndex(bucket);
      }
      catch (IOException e) {
        log.error("Unable to create root index for repository: {}", getRepository().getName(), e);
        return null;
      }
    }

    return toContent(asset, tx.requireBlob(asset.requireBlobRef()));
  }

  private Content createAndSaveRootIndex(final Bucket bucket) throws IOException {
    StorageTx tx = UnitOfWork.currentTx();
    Map<String, String> links = findAllLinks();

    Asset asset = createRootIndexAsset(bucket);

    String rootIndexHtml = PyPiIndexUtils.buildRootIndexPage(templateHelper, links);
    return storeHtmlPage(tx, asset, rootIndexHtml);
  }

  @TransactionalStoreMetadata
  protected Asset createRootIndexAsset(final Bucket bucket) {
    StorageTx tx = UnitOfWork.currentTx();
    Asset asset = tx.createAsset(bucket, getRepository().getFormat());
    asset.name(INDEX_PATH_PREFIX);
    asset.formatAttributes().set(P_ASSET_KIND, ROOT_INDEX.name());
    return asset;
  }

  @Transactional
  protected Map<String, String> findAllLinks() {
    StorageTx tx = UnitOfWork.currentTx();
    Map<String, String> links = new TreeMap<>();
    Iterable<Component> components = tx.browseComponents(tx.findBucket(getRepository()));
    components.forEach((component) -> links.put(component.name(), component.name() + "/"));
    return links;
  }

  @TransactionalStoreBlob
  protected Content storeHtmlPage(final StorageTx tx, final Asset asset, final String indexPage) throws IOException
  {
    StorageFacet storageFacet = facet(StorageFacet.class);
    try (TempBlob tempBlob = storageFacet.createTempBlob(new StringPayload(indexPage, TEXT_HTML), PyPiDataUtils.HASH_ALGORITHMS)) {
      return saveAsset(tx, asset, tempBlob, TEXT_HTML, null);
    }
  }

  @Override
  @TransactionalStoreBlob
  public Content getIndex(final String name) throws IOException {
    checkNotNull(name);
    StorageTx tx = UnitOfWork.currentTx();

    // If we don't even have a single component entry, then nothing has been uploaded yet
    if (!findComponentExists(tx, getRepository(), name)) {
      return null;
    }

    String indexPath = PyPiPathUtils.indexPath(name);
    Bucket bucket = tx.findBucket(getRepository());
    Asset savedIndex = findAsset(tx, bucket, indexPath);

    if (savedIndex == null) {
      savedIndex = createIndexAsset(name, tx, indexPath, bucket);
    }

    return toContent(savedIndex, tx.requireBlob(savedIndex.requireBlobRef()));
  }

  private Asset createIndexAsset(final String name,
                                 final StorageTx tx,
                                 final String indexPath,
                                 final Bucket bucket) throws IOException
  {
    String html = buildIndex(name, tx);

    Asset savedIndex = tx.createAsset(bucket, getRepository().getFormat());
    savedIndex.name(indexPath);
    savedIndex.formatAttributes().set(P_ASSET_KIND, AssetKind.INDEX.name());

    StorageFacet storageFacet = getRepository().facet(StorageFacet.class);
    TempBlob tempBlob = storageFacet.createTempBlob(new ByteArrayInputStream(html.getBytes(UTF_8)), HASH_ALGORITHMS);

    saveAsset(tx, savedIndex, tempBlob, TEXT_HTML, new AttributesMap());

    return savedIndex;
  }

  private String buildIndex(final String name, final StorageTx tx) {
    Map<String, String> links = new LinkedHashMap<>();
    for (Asset asset : findAssetsByComponentName(tx, getRepository(), name)) {
      String path = asset.name();
      String file = path.substring(path.lastIndexOf('/') + 1);
      String link = String.format("../../%s#md5=%s", path, asset.getChecksum(MD5));
      links.put(file, link);
    }

    return PyPiIndexUtils.buildIndexPage(templateHelper, name, links);
  }

  @Override
  @TransactionalTouchBlob
  public Content getPackage(final String packagePath) {
    checkNotNull(packagePath);
    StorageTx tx = UnitOfWork.currentTx();

    Asset asset = findAsset(tx, tx.findBucket(getRepository()), packagePath);
    if (asset == null) {
      return null;
    }
    Content content = toContent(asset, tx.requireBlob(asset.requireBlobRef()));
    mayAddEtag(content.getAttributes(), asset.getChecksum(HashAlgorithm.SHA1));
    return content;
  }

  private void mayAddEtag(final AttributesMap attributesMap, final HashCode hashCode) {
    if (attributesMap.contains(CONTENT_ETAG)) {
      return;
    }

    if (hashCode != null) {
      attributesMap.set(CONTENT_ETAG, "{SHA1{" + hashCode + "}}");
    }
  }

  @Override
  @TransactionalStoreBlob
  public Asset upload(final String filename, final Map<String, String> attributes, final TempBlobPartPayload payload)
      throws IOException
  {
    checkNotNull(filename);

    TempBlob tempBlob = payload.getTempBlob();

    final String name = checkNotNull(attributes.get(P_NAME));
    final String version = checkNotNull(attributes.get(P_VERSION));
    final String normalizedName = normalizeName(name);

    if (attributes.containsKey("md5_digest")) {
      String expectedDigest = attributes.get("md5_digest");
      HashCode hashCode = tempBlob.getHashes().get(MD5);
      String hashValue = hashCode.toString();
      if (!expectedDigest.equalsIgnoreCase(hashValue)) {
        throw new IllegalOperationException(
            "Digests do not match, found: " + hashValue + ", expected: " + expectedDigest);
      }
    }

    PyPiIndexFacet indexFacet = facet(PyPiIndexFacet.class);
    // A package has been added or redeployed and therefore the cached index is no longer relevant
    indexFacet.deleteIndex(name);
    
    StorageTx tx = UnitOfWork.currentTx();
    Bucket bucket = tx.findBucket(getRepository());

    String packagePath = createPackagePath(name, version, filename);

    Component component = findComponent(tx, getRepository(), normalizedName, version);
    if (component == null) {
      //
      // The setup.py register and setup.py upload operations in Python distutils do not correctly generate CR/LF
      // sequences, which causes commons-fileupload to be unable to read the headers and throws an exception. This is
      // a known issue to the Python maintainers and a fix was made for newer distutils releases, but even though both
      // affected code paths are mentioned in the issue, they only remembered to fix the upload operation.
      //
      // Allowing creation of a new component on upload seems to be the most obvious way we can implement a workaround
      // on our side, even though it is not directly in keeping with how the tools are supposed to work. 
      //
      // See https://bugs.python.org/issue10510 for the "fixed" Python distutils issue.
      //
      component = tx.createComponent(bucket, getRepository().getFormat()).name(normalizedName).version(version);
      setComponentName(component.formatAttributes(), name);
      component.formatAttributes().set(P_VERSION, version);

      //A new component so we will need to regenerate the root index
      indexFacet.deleteRootIndex();
    }

    component.formatAttributes().set(P_SUMMARY, attributes.get(P_SUMMARY)); // use the most recent summary received?
    tx.saveComponent(component);

    Asset asset = findAsset(tx, bucket, packagePath);
    if (asset == null) {
      asset = tx.createAsset(bucket, component);
      asset.name(packagePath);
      asset.formatAttributes().set(P_ASSET_KIND, AssetKind.PACKAGE.name());
    }

    PyPiDataUtils.copyAttributes(asset, attributes);
    saveAsset(tx, asset, tempBlob, payload);

    return asset;
  }

  /**
   * We supply all variants of the name. According to pep-503 '-', '.' and '_' are to be treated equally.
   * This allows searching for this component to be found using any combination of the substituted characters.
   * When using only this technique and not parsing the actual metadata stored in the package, the original name
   * (if it contained multiple special characters) would not be accounted for in search.
   * @param attributes associated with the component
   * @param name of the component to be saved
   */
  private void setComponentName(final NestedAttributesMap attributes, final String name) {
    attributes.set(P_NAME, name);
    attributes.set("name_dash", normalizeName(name, "-"));
    attributes.set("name_dot", normalizeName(name, "."));
    attributes.set("name_underscore", normalizeName(name, "_"));
  }

  @Override
  public String createPackagePath(final String name, final String version, final String filename) {
    final String normalizedName = normalizeName(name);
    return packagesPath(normalizedName, version, filename);
  }

  @Override
  public Map<String, String> extractMetadata(final TempBlob tempBlob) throws IOException {
    try (InputStream in = tempBlob.get()) {
      return PyPiInfoUtils.extractMetadata(in);
    }
  }
}
