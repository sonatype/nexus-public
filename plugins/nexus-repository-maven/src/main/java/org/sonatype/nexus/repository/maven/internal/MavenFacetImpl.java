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
package org.sonatype.nexus.repository.maven.internal;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Named;
import javax.validation.constraints.NotNull;
import javax.validation.groups.Default;

import org.sonatype.nexus.blobstore.api.Blob;
import org.sonatype.nexus.common.collect.AttributesMap;
import org.sonatype.nexus.common.collect.NestedAttributesMap;
import org.sonatype.nexus.common.hash.HashAlgorithm;
import org.sonatype.nexus.repository.FacetSupport;
import org.sonatype.nexus.repository.config.Configuration;
import org.sonatype.nexus.repository.config.ConfigurationFacet;
import org.sonatype.nexus.repository.maven.LayoutPolicy;
import org.sonatype.nexus.repository.maven.MavenFacet;
import org.sonatype.nexus.repository.maven.MavenPath;
import org.sonatype.nexus.repository.maven.MavenPath.Coordinates;
import org.sonatype.nexus.repository.maven.MavenPath.HashType;
import org.sonatype.nexus.repository.maven.MavenPathParser;
import org.sonatype.nexus.repository.maven.VersionPolicy;
import org.sonatype.nexus.repository.maven.internal.validation.MavenMetadataContentValidator;
import org.sonatype.nexus.repository.storage.Asset;
import org.sonatype.nexus.repository.storage.AssetBlob;
import org.sonatype.nexus.repository.storage.Bucket;
import org.sonatype.nexus.repository.storage.Component;
import org.sonatype.nexus.repository.storage.StorageFacet;
import org.sonatype.nexus.repository.storage.StorageTx;
import org.sonatype.nexus.repository.storage.TempBlob;
import org.sonatype.nexus.repository.transaction.TransactionalDeleteBlob;
import org.sonatype.nexus.repository.transaction.TransactionalStoreBlob;
import org.sonatype.nexus.repository.transaction.TransactionalStoreMetadata;
import org.sonatype.nexus.repository.transaction.TransactionalTouchBlob;
import org.sonatype.nexus.repository.types.HostedType;
import org.sonatype.nexus.repository.types.ProxyType;
import org.sonatype.nexus.repository.view.Content;
import org.sonatype.nexus.repository.view.Payload;
import org.sonatype.nexus.repository.view.payloads.BlobPayload;
import org.sonatype.nexus.transaction.Transactional;
import org.sonatype.nexus.transaction.UnitOfWork;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.hash.HashCode;
import org.apache.maven.model.Model;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.sonatype.nexus.repository.maven.internal.Attributes.*;
import static org.sonatype.nexus.repository.maven.internal.Constants.METADATA_FILENAME;
import static org.sonatype.nexus.repository.maven.internal.MavenFacetUtils.findAsset;
import static org.sonatype.nexus.repository.maven.internal.MavenFacetUtils.findComponent;
import static org.sonatype.nexus.repository.storage.AssetEntityAdapter.P_ASSET_KIND;

/**
 * A {@link MavenFacet} that persists Maven artifacts and metadata to a {@link StorageFacet}.
 * <p/>
 * Structure for artifacts (CMA components and assets):
 * <ul>
 * <li>CMA components: keyed by groupId:artifactId:version</li>
 * <li>CMA assets: keyed by path</li>
 * </ul>
 * <p/>
 * Structure for metadata (CMA assets only):
 * <ul>
 * <li>CMA assets: keyed by path</li>
 * </ul>
 * In both cases, "external" hashes are stored as separate asset, as their path differs too.
 *
 * @since 3.0
 */
@Named
public class MavenFacetImpl
    extends FacetSupport
    implements MavenFacet
{
  private final Map<String, MavenPathParser> mavenPathParsers;

  @VisibleForTesting
  static final String CONFIG_KEY = "maven";

  @VisibleForTesting
  static class Config
  {
    @NotNull(groups = {HostedType.ValidationGroup.class, ProxyType.ValidationGroup.class})
    public VersionPolicy versionPolicy;

    @NotNull(groups = {HostedType.ValidationGroup.class, ProxyType.ValidationGroup.class})
    public LayoutPolicy layoutPolicy;

    @Override
    public String toString() {
      return getClass().getSimpleName() + "{" +
          "versionPolicy=" + versionPolicy +
          ", layoutPolicy=" + layoutPolicy +
          '}';
    }
  }

  private Config config;

  private MavenPathParser mavenPathParser;

  private StorageFacet storageFacet;

  private final MavenMetadataContentValidator metadataValidator;

  private final boolean mavenMetadataValidationEnabled;

  @Inject
  public MavenFacetImpl(final Map<String, MavenPathParser> mavenPathParsers,
                        final MavenMetadataContentValidator metadataValidator,
                        @Named("${nexus.maven.metadata.validation.enabled:-true}")
                        final boolean mavenMetadataValidationEnabled)
  {
    this.mavenPathParsers = checkNotNull(mavenPathParsers);
    this.metadataValidator = checkNotNull(metadataValidator);
    this.mavenMetadataValidationEnabled = mavenMetadataValidationEnabled;
  }

  @Override
  protected void doValidate(final Configuration configuration) throws Exception {
    facet(ConfigurationFacet.class).validateSection(configuration, CONFIG_KEY, Config.class,
        Default.class, getRepository().getType().getValidationGroup()
    );
  }

  @Override
  protected void doInit(final Configuration configuration) throws Exception {
    super.doInit(configuration);
    mavenPathParser = checkNotNull(mavenPathParsers.get(getRepository().getFormat().getValue()));
    storageFacet = getRepository().facet(StorageFacet.class);
    storageFacet.registerWritePolicySelector(new MavenWritePolicySelector());
  }

  @Override
  protected void doConfigure(final Configuration configuration) throws Exception {
    config = facet(ConfigurationFacet.class).readSection(configuration, CONFIG_KEY, Config.class);
    log.debug("Config: {}", config);
  }

  @Override
  protected void doDestroy() throws Exception {
    config = null;
  }

  @Nonnull
  @Override
  public MavenPathParser getMavenPathParser() {
    return mavenPathParser;
  }

  @Nonnull
  @Override
  public VersionPolicy getVersionPolicy() {
    return config.versionPolicy;
  }

  @Override
  public LayoutPolicy layoutPolicy() {
    return config.layoutPolicy;
  }

  @Nullable
  @Override
  @TransactionalTouchBlob
  public Content get(final MavenPath path) throws IOException {
    log.debug("GET {} : {}", getRepository().getName(), path.getPath());

    final StorageTx tx = UnitOfWork.currentTx();

    final Asset asset = findAsset(tx, tx.findBucket(getRepository()), path);
    if (asset == null) {
      return null;
    }

    final Blob blob = tx.requireBlob(asset.requireBlobRef());
    return toContent(asset, blob);
  }

  private Content toContent(final Asset asset, final Blob blob) {
    final String contentType = asset.contentType();
    final Content content = new Content(new BlobPayload(blob, contentType));
    Content.extractFromAsset(asset, HashType.ALGORITHMS, content.getAttributes());
    return content;
  }

  @Override
  public Content put(final MavenPath path, final Payload payload)
      throws IOException
  {
    log.debug("PUT {} : {}", getRepository().getName(), path.getPath());

    try (TempBlob tempBlob = storageFacet.createTempBlob(payload, HashType.ALGORITHMS)) {
      if (path.getFileName().equals(METADATA_FILENAME) && mavenMetadataValidationEnabled) {
        
        log.debug("Validating maven-metadata.xml before storing");
        
        metadataValidator.validate(path.getPath(), tempBlob.get());
      }
      return doPut(path, payload, tempBlob);
    }
  }

  @Override
  public Content put(final MavenPath path,
                     final Path sourceFile,
                     final String contentType,
                     final AttributesMap contentAttributes,
                     final Map<HashAlgorithm, HashCode> hashes,
                     final long size)
      throws IOException
  {
    log.debug("PUT {} : {}", getRepository().getName(), path.getPath());

    return doPut(path, sourceFile, contentType, contentAttributes, hashes, size);
  }

  @Override
  public Content put(final MavenPath path,
                     final TempBlob blob,
                     final String contentType,
                     final AttributesMap contentAttributes)
      throws IOException
  {
    return doPut(path, blob, contentType, contentAttributes);
  }

  @TransactionalStoreBlob
  protected Content doPut(final MavenPath path,
      final Payload payload,
      final TempBlob tempBlob)
      throws IOException
  {
    final StorageTx tx = UnitOfWork.currentTx();

    final AssetBlob assetBlob = tx.createBlob(
        path.getPath(),
        tempBlob,
        null,
        payload.getContentType(),
        false
    );
    AttributesMap contentAttributes = null;
    if (payload instanceof Content) {
      contentAttributes = ((Content) payload).getAttributes();
    }

    return doPutAssetBlob(path, contentAttributes, tx, assetBlob);
  }

  @TransactionalStoreBlob
  protected Content doPut(final MavenPath path,
                          final Path sourceFile,
                          final String contentType,
                          final AttributesMap contentAttributes,
                          final Map<HashAlgorithm, HashCode> hashes,
                          final long size)
      throws IOException
  {
    final StorageTx tx = UnitOfWork.currentTx();

    final AssetBlob assetBlob = tx.createBlob(
        path.getPath(),
        sourceFile,
        hashes,
        null,
        contentType,
        size
    );

    return doPutAssetBlob(path, contentAttributes, tx, assetBlob);
  }

  @TransactionalStoreBlob
  protected Content doPut(final MavenPath path,
                          final TempBlob blob,
                          final String contentType,
                          final AttributesMap contentAttributes)
      throws IOException
  {
    StorageTx tx = UnitOfWork.currentTx();

    AssetBlob assetBlob = tx.createBlob(path.getPath(), blob, null, contentType, true);

    return doPutAssetBlob(path, contentAttributes, tx, assetBlob);
  }

  private Content doPutAssetBlob(final MavenPath path,
                                 final AttributesMap contentAttributes,
                                 final StorageTx tx,
                                 final AssetBlob assetBlob)
      throws IOException
  {
    if (path.getCoordinates() != null) {
      return toContent(putArtifact(tx, path, assetBlob, contentAttributes), assetBlob.getBlob());
    }
    else {
      return toContent(putFile(tx, path, assetBlob, contentAttributes), assetBlob.getBlob());
    }
  }

  private Asset putArtifact(final StorageTx tx,
      final MavenPath path,
      final AssetBlob assetBlob,
      @Nullable final AttributesMap contentAttributes)
      throws IOException
  {
    final Coordinates coordinates = checkNotNull(path.getCoordinates());
    final Bucket bucket = tx.findBucket(getRepository());
    Component component = findComponent(tx, getRepository(), path);
    boolean updatePomModel = false;
    if (component == null) {
      // Create and set top-level properties
      component = tx.createComponent(bucket, getRepository().getFormat())
          .group(coordinates.getGroupId())
          .name(coordinates.getArtifactId())
          .version(coordinates.getVersion());

      // Set format specific attributes
      final NestedAttributesMap componentAttributes = component.formatAttributes();
      componentAttributes.set(P_GROUP_ID, coordinates.getGroupId());
      componentAttributes.set(P_ARTIFACT_ID, coordinates.getArtifactId());
      componentAttributes.set(P_VERSION, coordinates.getVersion());
      componentAttributes.set(P_BASE_VERSION, coordinates.getBaseVersion());
      if (path.isPom()) {
        fillInFromModel(path, assetBlob, component.formatAttributes());
      }
      tx.saveComponent(component);
    }
    else if (path.isPom()) {
      // reduce copying by deferring update of pom.xml attributes (which involves reading the blob)
      // until after putAssetPayload, in case the blob is a duplicate and the model has not changed
      updatePomModel = true;
    }

    Asset asset = findAsset(tx, bucket, path);
    if (asset == null) {
      asset = tx.createAsset(bucket, component);

      asset.name(path.getPath());

      final NestedAttributesMap assetAttributes = asset.formatAttributes();
      assetAttributes.set(P_GROUP_ID, coordinates.getGroupId());
      assetAttributes.set(P_ARTIFACT_ID, coordinates.getArtifactId());
      assetAttributes.set(P_VERSION, coordinates.getVersion());
      assetAttributes.set(P_BASE_VERSION, coordinates.getBaseVersion());
      assetAttributes.set(P_CLASSIFIER, coordinates.getClassifier());
      assetAttributes.set(P_EXTENSION, coordinates.getExtension());
      assetAttributes.set(
          P_ASSET_KIND,
          path.isSubordinate() ? AssetKind.ARTIFACT_SUBORDINATE.name() : AssetKind.ARTIFACT.name()
      );
    }

    putAssetPayload(tx, asset, assetBlob, contentAttributes);

    tx.saveAsset(asset);

    // avoid re-reading pom.xml if it's a duplicate of the old asset
    if (updatePomModel && !assetBlob.isDuplicate()) {
      fillInFromModel(path, assetBlob, component.formatAttributes());
      tx.saveComponent(component);
    }

    return asset;
  }

  /**
   * Parses model from {@link AssetBlob} and sets {@link Component} attributes.
   */
  private void fillInFromModel(final MavenPath mavenPath,
      final AssetBlob assetBlob,
      final NestedAttributesMap componentAttributes) throws IOException
  {
    Model model = MavenModels.readModel(assetBlob.getBlob().getInputStream());
    if (model == null) {
      log.warn("Could not parse POM: {} @ {}", getRepository().getName(), mavenPath.getPath());
      return;
    }
    String packaging = model.getPackaging();
    componentAttributes.set(P_PACKAGING, packaging == null ? "jar" : packaging);
    componentAttributes.set(P_POM_NAME, model.getName());
    componentAttributes.set(P_POM_DESCRIPTION, model.getDescription());
  }

  @TransactionalStoreMetadata
  public Asset put(final MavenPath path, final AssetBlob assetBlob, final AttributesMap contentAttributes)
      throws IOException
  {
    final StorageTx tx = UnitOfWork.currentTx();

    if (path.getCoordinates() != null) {
      return putArtifact(tx, path, assetBlob, contentAttributes);
    }
    else {
      return putFile(tx, path, assetBlob, contentAttributes);
    }
  }

  @Override
  @Transactional
  public boolean exists(final MavenPath path) {
    log.debug("EXISTS {} : {}", getRepository().getName(), path.getPath());

    final StorageTx tx = UnitOfWork.currentTx();

    final Asset asset = findAsset(tx, tx.findBucket(getRepository()), path);

    return asset != null;
  }

  private Asset putFile(final StorageTx tx,
      final MavenPath path,
      final AssetBlob assetBlob,
      @Nullable final AttributesMap contentAttributes)
      throws IOException
  {
    final Bucket bucket = tx.findBucket(getRepository());
    Asset asset = findAsset(tx, bucket, path);
    if (asset == null) {
      asset = tx.createAsset(bucket, getRepository().getFormat());
      asset.name(path.getPath());
      asset.formatAttributes().set(P_ASSET_KIND, fileAssetKindFor(path));
    }

    putAssetPayload(tx, asset, assetBlob, contentAttributes);

    tx.saveAsset(asset);

    return asset;
  }

  private void putAssetPayload(final StorageTx tx,
      final Asset asset,
      final AssetBlob assetBlob,
      @Nullable final AttributesMap contentAttributes)
      throws IOException
  {
    tx.attachBlob(asset, assetBlob);
    Content.applyToAsset(asset, Content.maintainLastModified(asset, contentAttributes));
  }

  @Override
  @TransactionalDeleteBlob
  public boolean delete(final MavenPath... paths) throws IOException {
    final StorageTx tx = UnitOfWork.currentTx();

    boolean result = false;
    for (MavenPath path : paths) {
      log.trace("DELETE {} : {}", getRepository().getName(), path.getPath());
      if (path.getCoordinates() != null) {
        result = deleteArtifact(path, tx) || result;
      }
      else {
        result = deleteFile(path, tx) || result;
      }
    }
    return result;
  }

  private boolean deleteArtifact(final MavenPath path, final StorageTx tx) {
    final Component component = findComponent(tx, getRepository(), path);
    if (component == null) {
      return false;
    }
    final Asset asset = findAsset(tx, tx.findBucket(getRepository()), path);
    if (asset == null) {
      return false;
    }
    tx.deleteAsset(asset);
    if (!tx.browseAssets(component).iterator().hasNext()) {
      tx.deleteComponent(component);
    }
    return true;
  }

  private boolean deleteFile(final MavenPath path, final StorageTx tx) {
    final Asset asset = findAsset(tx, tx.findBucket(getRepository()), path);
    if (asset == null) {
      return false;
    }
    tx.deleteAsset(asset);
    return true;
  }

  private String fileAssetKindFor(final MavenPath path) {
    if (getMavenPathParser().isRepositoryMetadata(path)) {
      return AssetKind.REPOSITORY_METADATA.name();
    }
    else if (getMavenPathParser().isRepositoryIndex(path)) {
      return AssetKind.REPOSITORY_INDEX.name();
    }
    else {
      return AssetKind.OTHER.name();
    }
  }
}
