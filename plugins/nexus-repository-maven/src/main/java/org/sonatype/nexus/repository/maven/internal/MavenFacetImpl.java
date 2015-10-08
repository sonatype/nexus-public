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
import java.io.InputStream;
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
import org.sonatype.nexus.common.io.TempStreamSupplier;
import org.sonatype.nexus.repository.FacetSupport;
import org.sonatype.nexus.repository.cache.CacheInfo;
import org.sonatype.nexus.repository.config.Configuration;
import org.sonatype.nexus.repository.config.ConfigurationFacet;
import org.sonatype.nexus.repository.maven.MavenFacet;
import org.sonatype.nexus.repository.maven.MavenPath;
import org.sonatype.nexus.repository.maven.MavenPath.Coordinates;
import org.sonatype.nexus.repository.maven.MavenPath.HashType;
import org.sonatype.nexus.repository.maven.MavenPathParser;
import org.sonatype.nexus.repository.maven.policy.VersionPolicy;
import org.sonatype.nexus.repository.storage.Asset;
import org.sonatype.nexus.repository.storage.AssetBlob;
import org.sonatype.nexus.repository.storage.Bucket;
import org.sonatype.nexus.repository.storage.Component;
import org.sonatype.nexus.repository.storage.ComponentQuery;
import org.sonatype.nexus.repository.storage.ComponentQuery.Builder;
import org.sonatype.nexus.repository.storage.StorageFacet;
import org.sonatype.nexus.repository.storage.StorageTx;
import org.sonatype.nexus.repository.types.HostedType;
import org.sonatype.nexus.repository.types.ProxyType;
import org.sonatype.nexus.repository.view.Content;
import org.sonatype.nexus.repository.view.Payload;
import org.sonatype.nexus.repository.view.payloads.BlobPayload;
import org.sonatype.nexus.transaction.Transactional;
import org.sonatype.nexus.transaction.UnitOfWork;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Supplier;
import com.orientechnologies.common.concur.ONeedRetryException;
import com.orientechnologies.orient.core.storage.ORecordDuplicatedException;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.Arrays.asList;

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
    implements MavenFacet, MavenAttributes
{
  private final Map<String, MavenPathParser> mavenPathParsers;

  @VisibleForTesting
  static final String CONFIG_KEY = "maven";

  @VisibleForTesting
  static class Config
  {
    @NotNull(groups = {HostedType.ValidationGroup.class, ProxyType.ValidationGroup.class})
    public VersionPolicy versionPolicy;

    @Override
    public String toString() {
      return getClass().getSimpleName() + "{" +
          "versionPolicy=" + versionPolicy +
          '}';
    }
  }

  private Config config;

  private MavenPathParser mavenPathParser;

  private StorageFacet storageFacet;

  @Inject
  public MavenFacetImpl(final Map<String, MavenPathParser> mavenPathParsers) {
    this.mavenPathParsers = checkNotNull(mavenPathParsers);
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
    storageFacet.registerWritePolicySelector(new MavenWritePolicySelector(mavenPathParser));
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

  @Nullable
  @Override
  @Transactional(retryOn = IllegalStateException.class, swallow = ONeedRetryException.class)
  public Content get(final MavenPath path) throws IOException {
    log.debug("GET {} : {}", getRepository().getName(), path.getPath());

    final StorageTx tx = UnitOfWork.currentTransaction();

    final Asset asset = findAsset(tx, tx.getBucket(), path);
    if (asset == null) {
      return null;
    }
    if (asset.markAsAccessed()) {
      tx.saveAsset(asset);
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

    try (TempStreamSupplier streamSupplier = new TempStreamSupplier(payload.openInputStream())) {
      return doPut(path, payload, streamSupplier);
    }
  }

  @Transactional(retryOn = {ONeedRetryException.class, ORecordDuplicatedException.class})
  protected Content doPut(final MavenPath path,
                          final Payload payload,
                          final Supplier<InputStream> streamSupplier)
      throws IOException
  {
    final StorageTx tx = UnitOfWork.currentTransaction();

    final AssetBlob assetBlob = tx.createBlob(
        path.getPath(),
        streamSupplier,
        HashType.ALGORITHMS,
        null,
        payload.getContentType(),
        false
    );
    AttributesMap contentAttributes = null;
    if (payload instanceof Content) {
      contentAttributes = ((Content) payload).getAttributes();
    }

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
    Component component = findComponent(tx, path);
    if (component == null) {
      // Create and set top-level properties
      component = tx.createComponent(tx.getBucket(), getRepository().getFormat())
          .group(coordinates.getGroupId())
          .name(coordinates.getArtifactId())
          .version(coordinates.getVersion());

      // Set format specific attributes
      final NestedAttributesMap componentAttributes = component.formatAttributes();
      componentAttributes.set(P_GROUP_ID, coordinates.getGroupId());
      componentAttributes.set(P_ARTIFACT_ID, coordinates.getArtifactId());
      componentAttributes.set(P_VERSION, coordinates.getVersion());
      componentAttributes.set(P_BASE_VERSION, coordinates.getBaseVersion());
      tx.saveComponent(component);
    }

    Asset asset = findAsset(tx, tx.getBucket(), path);
    if (asset == null) {
      asset = tx.createAsset(tx.getBucket(), component);

      asset.name(path.getPath());
      asset.formatAttributes().set(StorageFacet.P_PATH, path.getPath());

      final NestedAttributesMap assetAttributes = asset.formatAttributes();
      assetAttributes.set(P_GROUP_ID, coordinates.getGroupId());
      assetAttributes.set(P_ARTIFACT_ID, coordinates.getArtifactId());
      assetAttributes.set(P_VERSION, coordinates.getVersion());
      assetAttributes.set(P_BASE_VERSION, coordinates.getBaseVersion());
      assetAttributes.set(P_CLASSIFIER, coordinates.getClassifier());
      assetAttributes.set(P_EXTENSION, coordinates.getExtension());
    }

    putAssetPayload(tx, asset, assetBlob, contentAttributes);
    asset.markAsAccessed();
    tx.saveAsset(asset);

    return asset;
  }

  private Asset putFile(final StorageTx tx,
                        final MavenPath path,
                        final AssetBlob assetBlob,
                        @Nullable final AttributesMap contentAttributes)
      throws IOException
  {
    Asset asset = findAsset(tx, tx.getBucket(), path);
    if (asset == null) {
      asset = tx.createAsset(tx.getBucket(), getRepository().getFormat());
      asset.name(path.getPath());
      asset.formatAttributes().set(StorageFacet.P_PATH, path.getPath());
    }

    putAssetPayload(tx, asset, assetBlob, contentAttributes);
    asset.markAsAccessed();
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
  @Transactional
  public boolean delete(final MavenPath... paths) throws IOException {
    final StorageTx tx = UnitOfWork.currentTransaction();

    boolean result = false;
    for (MavenPath path : paths) {
      log.debug("DELETE {} : {}", getRepository().getName(), path.getPath());
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
    final Component component = findComponent(tx, path);
    if (component == null) {
      return false;
    }
    final Asset asset = findAsset(tx, tx.getBucket(), path);
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
    final Asset asset = findAsset(tx, tx.getBucket(), path);
    if (asset == null) {
      return false;
    }
    tx.deleteAsset(asset);
    return true;
  }

  @Override
  @Transactional(retryOn = ONeedRetryException.class)
  public boolean setCacheInfo(final MavenPath path, final Content content, final CacheInfo cacheInfo)
      throws IOException
  {
    final StorageTx tx = UnitOfWork.currentTransaction();

    // by EntityId
    Asset asset = Content.findAsset(tx, content);
    if (asset == null) {
      // by format coordinates
      asset = findAsset(tx, tx.getBucket(), path);
    }
    if (asset == null) {
      log.debug("Attempting to set cache info for non-existent maven asset {}", path.getPath());
      return false;
    }

    log.debug("Updating cacheInfo of {} to {}", path.getPath(), cacheInfo);
    CacheInfo.applyToAsset(asset, cacheInfo);
    tx.saveAsset(asset);

    return true;
  }

  /**
   * Finds component by maven path.
   */
  @Nullable
  private Component findComponent(final StorageTx tx,
                                  final MavenPath mavenPath)
  {
    final Coordinates coordinates = mavenPath.getCoordinates();

    final ComponentQuery query = new Builder()
        .where("group").eq(coordinates.getGroupId())
        .and("name").eq(coordinates.getArtifactId())
        .and("version").eq(coordinates.getVersion())
        .build();

    final Iterable<Component> components = tx
        .findComponents(query.getWhere(), query.getParameters(), asList(getRepository()), null);
    if (components.iterator().hasNext()) {
      return components.iterator().next();
    }
    return null;
  }

  /**
   * Finds asset by key.
   */
  @Nullable
  private Asset findAsset(final StorageTx tx,
                          final Bucket bucket,
                          final MavenPath mavenPath)
  {
    // The maven path is stored in the asset 'name' field, which is indexed (the maven format-specific key is not).
    return tx.findAssetWithProperty(StorageFacet.P_NAME, mavenPath.getPath(), bucket);
  }
}
