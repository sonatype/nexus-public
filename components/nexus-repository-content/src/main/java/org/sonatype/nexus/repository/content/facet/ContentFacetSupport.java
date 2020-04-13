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
package org.sonatype.nexus.repository.content.facet;

import java.util.Optional;

import javax.inject.Inject;

import org.sonatype.nexus.blobstore.api.BlobStore;
import org.sonatype.nexus.blobstore.api.BlobStoreManager;
import org.sonatype.nexus.common.collect.NestedAttributesMap;
import org.sonatype.nexus.common.entity.EntityId;
import org.sonatype.nexus.common.node.NodeAccess;
import org.sonatype.nexus.datastore.api.DataSession;
import org.sonatype.nexus.datastore.api.DataSessionSupplier;
import org.sonatype.nexus.repository.FacetSupport;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.config.Configuration;
import org.sonatype.nexus.repository.content.ContentRepository;
import org.sonatype.nexus.repository.content.fluent.AttributeChange;
import org.sonatype.nexus.repository.content.fluent.FluentAssets;
import org.sonatype.nexus.repository.content.fluent.FluentBlobs;
import org.sonatype.nexus.repository.content.fluent.FluentComponents;
import org.sonatype.nexus.repository.content.fluent.internal.FluentAssetsImpl;
import org.sonatype.nexus.repository.content.fluent.internal.FluentAttributesHelper;
import org.sonatype.nexus.repository.content.fluent.internal.FluentBlobsImpl;
import org.sonatype.nexus.repository.content.fluent.internal.FluentComponentsImpl;
import org.sonatype.nexus.repository.content.store.AssetBlobStore;
import org.sonatype.nexus.repository.content.store.AssetStore;
import org.sonatype.nexus.repository.content.store.ComponentStore;
import org.sonatype.nexus.repository.content.store.ContentRepositoryData;
import org.sonatype.nexus.repository.content.store.ContentRepositoryStore;
import org.sonatype.nexus.repository.content.store.FormatStoreManager;
import org.sonatype.nexus.security.ClientInfo;
import org.sonatype.nexus.security.ClientInfoProvider;
import org.sonatype.nexus.transaction.TransactionalStore;

import com.google.common.annotations.VisibleForTesting;
import org.joda.time.DateTime;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static java.util.Optional.ofNullable;
import static org.sonatype.nexus.blobstore.api.BlobStoreManager.DEFAULT_BLOBSTORE_NAME;
import static org.sonatype.nexus.datastore.api.DataStoreManager.CONTENT_DATASTORE_NAME;
import static org.sonatype.nexus.repository.config.ConfigurationConstants.BLOB_STORE_NAME;
import static org.sonatype.nexus.repository.config.ConfigurationConstants.DATA_STORE_NAME;
import static org.sonatype.nexus.repository.config.ConfigurationConstants.STORAGE;

/**
 * {@link ContentFacet} support.
 *
 * @since 3.next
 */
public abstract class ContentFacetSupport
    extends FacetSupport
    implements ContentFacet, TransactionalStore<DataSession<?>>
{
  private final FormatStoreManager formatStoreManager;

  private DataSessionSupplier dataSessionSupplier;

  private BlobStoreManager blobStoreManager;

  private ClientInfoProvider clientInfoProvider;

  private NodeAccess nodeAccess;

  private String blobStoreName;

  private String contentStoreName;

  private EntityId configRepositoryId;

  private Integer contentRepositoryId;

  private BlobStore blobStore;

  private ContentRepositoryStore<?> contentRepositoryStore;

  private ComponentStore<?> componentStore;

  private AssetStore<?> assetStore;

  private AssetBlobStore<?> assetBlobStore;

  protected ContentFacetSupport(final FormatStoreManager formatStoreManager) {
    this.formatStoreManager = checkNotNull(formatStoreManager);
  }

  @Inject
  @VisibleForTesting
  protected final void setDependencies(final DataSessionSupplier dataSessionSupplier,
                                       final BlobStoreManager blobStoreManager,
                                       final ClientInfoProvider clientInfoProvider,
                                       final NodeAccess nodeAccess)
  {
    this.dataSessionSupplier = checkNotNull(dataSessionSupplier);
    this.blobStoreManager = checkNotNull(blobStoreManager);
    this.clientInfoProvider = checkNotNull(clientInfoProvider);
    this.nodeAccess = checkNotNull(nodeAccess);
  }

  @Override
  protected void doConfigure(final Configuration configuration) {

    NestedAttributesMap storageAttributes = configuration.attributes(STORAGE);
    blobStoreName = storageAttributes.get(BLOB_STORE_NAME, String.class, DEFAULT_BLOBSTORE_NAME);
    contentStoreName = storageAttributes.get(DATA_STORE_NAME, String.class, CONTENT_DATASTORE_NAME);

    blobStore = blobStoreManager.get(blobStoreName);

    contentRepositoryStore = formatStoreManager.contentRepositoryStore(contentStoreName);
    componentStore = formatStoreManager.componentStore(contentStoreName);
    assetStore = formatStoreManager.assetStore(contentStoreName);
    assetBlobStore = formatStoreManager.assetBlobStore(contentStoreName);
  }

  @Override
  protected void doStart() throws Exception {
    // configuration is persisted by this point and has been assigned an id
    configRepositoryId = getRepository().getConfiguration().getRepositoryId();

    checkState(configRepositoryId != null, "Missing configRepositoryId");

    // get or create the associated content repository
    contentRepositoryId = contentRepositoryStore.readContentRepository(configRepositoryId)
        .orElseGet(this::createContentRepository).contentRepositoryId();

    checkState(contentRepositoryId != null, "Missing contentRepositoryId");
  }

  /**
   * Creates a {@link ContentRepository} for the associated config repository.
   */
  private final ContentRepository createContentRepository() {
    ContentRepositoryData contentRepository = newContentRepository();
    contentRepository.setConfigRepositoryId(configRepositoryId);
    contentRepositoryStore.createContentRepository(contentRepository);
    return contentRepository;
  }

  /**
   * Override this method to customize creation of new content repositories.
   */
  protected ContentRepositoryData newContentRepository() {
    return new ContentRepositoryData();
  }

  /**
   * Retrieves the latest {@link ContentRepository} data from the store.
   */
  protected final ContentRepository contentRepository() {
    return contentRepositoryStore
        .readContentRepository(configRepositoryId)
        .orElseThrow(() -> new IllegalStateException("Missing content repository for " + configRepositoryId));
  }

  @Override
  protected void doDelete() throws Exception {
    if (configRepositoryId != null) {
      contentRepositoryStore.deleteContentRepository(configRepositoryId);
    }
  }

  @Override
  public final EntityId configRepositoryId() {
    return configRepositoryId;
  }

  @Override
  public final Integer contentRepositoryId() {
    return contentRepositoryId;
  }

  @Override
  public final NestedAttributesMap attributes() {
    return contentRepository().attributes();
  }

  @Override
  public final DateTime created() {
    return contentRepository().created();
  }

  @Override
  public final DateTime lastUpdated() {
    return contentRepository().lastUpdated();
  }

  @Override
  public final ContentFacet attributes(final AttributeChange change, final String key, final Object value) {
    ContentRepository contentRepository = contentRepository();
    FluentAttributesHelper.apply(contentRepository, change, key, value);
    contentRepositoryStore.updateContentRepositoryAttributes(contentRepository);
    return this;
  }

  @Override
  public final FluentBlobs blobs() {
    return new FluentBlobsImpl(this);
  }

  @Override
  public final FluentComponents components() {
    return new FluentComponentsImpl(this);
  }

  @Override
  public final FluentAssets assets() {
    return new FluentAssetsImpl(this);
  }

  @Override
  public final DataSession<?> openSession() {
    return dataSessionSupplier.openSession(contentStoreName);
  }

  public final FormatStoreManager formatStoreManager() {
    return formatStoreManager;
  }

  public final BlobStoreManager blobStoreManager() {
    return blobStoreManager;
  }

  public final Optional<ClientInfo> clientInfo() {
    return ofNullable(clientInfoProvider.getCurrentThreadClientInfo());
  }

  public final String nodeName() {
    return nodeAccess.getId();
  }

  public final Repository repository() {
    return getRepository();
  }

  public final String blobStoreName() {
    return blobStoreName;
  }

  public final String contentStoreName() {
    return contentStoreName;
  }

  public final BlobStore blobStore() {
    return blobStore;
  }

  public final ComponentStore<?> componentStore() {
    return componentStore;
  }

  public final AssetStore<?> assetStore() {
    return assetStore;
  }

  public final AssetBlobStore<?> assetBlobStore() {
    return assetBlobStore;
  }
}
