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

import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import javax.inject.Inject;
import javax.validation.ConstraintViolation;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.groups.Default;

import org.sonatype.nexus.blobstore.api.Blob;
import org.sonatype.nexus.common.collect.NestedAttributesMap;
import org.sonatype.nexus.common.entity.EntityId;
import org.sonatype.nexus.datastore.api.DataSession;
import org.sonatype.nexus.repository.FacetSupport;
import org.sonatype.nexus.repository.IllegalOperationException;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.config.Configuration;
import org.sonatype.nexus.repository.config.ConfigurationFacet;
import org.sonatype.nexus.repository.content.Asset;
import org.sonatype.nexus.repository.content.ContentRepository;
import org.sonatype.nexus.repository.content.fluent.AttributeChange;
import org.sonatype.nexus.repository.content.fluent.FluentAssets;
import org.sonatype.nexus.repository.content.fluent.FluentBlobs;
import org.sonatype.nexus.repository.content.fluent.FluentComponents;
import org.sonatype.nexus.repository.content.fluent.internal.FluentAssetsImpl;
import org.sonatype.nexus.repository.content.fluent.internal.FluentBlobsImpl;
import org.sonatype.nexus.repository.content.fluent.internal.FluentComponentsImpl;
import org.sonatype.nexus.repository.content.store.ContentRepositoryData;
import org.sonatype.nexus.repository.content.store.FormatStoreManager;
import org.sonatype.nexus.repository.types.HostedType;
import org.sonatype.nexus.security.ClientInfo;
import org.sonatype.nexus.transaction.Transactional;
import org.sonatype.nexus.transaction.TransactionalStore;

import com.google.common.annotations.VisibleForTesting;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static java.lang.String.format;
import static java.util.Optional.ofNullable;
import static org.sonatype.nexus.blobstore.api.BlobStoreManager.DEFAULT_BLOBSTORE_NAME;
import static org.sonatype.nexus.datastore.api.DataStoreManager.CONTENT_DATASTORE_NAME;
import static org.sonatype.nexus.repository.config.ConfigurationConstants.STORAGE;
import static org.sonatype.nexus.repository.content.facet.WritePolicy.ALLOW;
import static org.sonatype.nexus.repository.content.fluent.internal.FluentAttributesHelper.applyAttributeChange;
import static org.sonatype.nexus.validation.ConstraintViolations.maybeAdd;
import static org.sonatype.nexus.validation.ConstraintViolations.maybePropagate;

/**
 * {@link ContentFacet} support.
 *
 * @since 3.24
 */
public abstract class ContentFacetSupport
    extends FacetSupport
    implements ContentFacet, TransactionalStore<DataSession<?>>
{
  private final FormatStoreManager formatStoreManager;

  @VisibleForTesting
  static class Config
  {
    @NotEmpty
    public String blobStoreName = DEFAULT_BLOBSTORE_NAME;

    @NotEmpty
    public String dataStoreName = CONTENT_DATASTORE_NAME;

    @NotNull(groups = HostedType.ValidationGroup.class)
    public WritePolicy writePolicy;

    @NotNull
    public Boolean strictContentTypeValidation = Boolean.TRUE;

    @Override
    public String toString() {
      return getClass().getSimpleName() + "{" +
          "blobStoreName='" + blobStoreName + '\'' +
          ", dataStoreName='" + dataStoreName + '\'' +
          ", writePolicy=" + writePolicy +
          ", strictContentTypeValidation=" + strictContentTypeValidation +
          '}';
    }
  }

  private ContentFacetDependencies dependencies;

  private Config config;

  private ContentFacetStores stores;

  private EntityId configRepositoryId;

  private Integer contentRepositoryId;

  private AssetBlobValidator assetBlobValidator;

  protected ContentFacetSupport(final FormatStoreManager formatStoreManager) {
    this.formatStoreManager = checkNotNull(formatStoreManager);
  }

  @Inject
  @VisibleForTesting
  protected final void setDependencies(final ContentFacetDependencies dependencies) {
    this.dependencies = checkNotNull(dependencies);
  }

  @Override
  protected void doValidate(final Configuration configuration) throws Exception {
    facet(ConfigurationFacet.class).validateSection(configuration, STORAGE, Config.class,
        Default.class, getRepository().getType().getValidationGroup()
    );

    Config configToValidate = facet(ConfigurationFacet.class).readSection(configuration, STORAGE, Config.class);

    Set<ConstraintViolation<?>> violations = new HashSet<>();
    maybeAdd(violations, validateBlobStoreNotInGroup(configToValidate.blobStoreName));
    maybePropagate(violations, log);
  }

  private ConstraintViolation<?> validateBlobStoreNotInGroup(final String blobStoreName) {
    return dependencies.blobStoreManager.getParent(blobStoreName)
        .map(groupName -> dependencies.constraintViolationFactory.
            createViolation(format("%s.blobStoreName", STORAGE),
            format("Blob Store '%s' is a member of Blob Store Group '%s' and cannot be set as storage",
                blobStoreName, groupName)))
        .orElse(null);
  }

  @Override
  protected void doConfigure(final Configuration configuration) throws Exception {
    config = facet(ConfigurationFacet.class).readSection(configuration, STORAGE, Config.class);
    log.debug("Config: {}", config);

    stores = new ContentFacetStores(
        dependencies.blobStoreManager, config.blobStoreName,
        formatStoreManager, config.dataStoreName);
  }

  @Override
  protected void doStart() throws Exception {
    // configuration is persisted by this point and has been assigned an id
    configRepositoryId = getRepository().getConfiguration().getRepositoryId();

    checkState(configRepositoryId != null, "Missing configRepositoryId");

    // get or create the associated content repository
    contentRepositoryId = stores.contentRepositoryStore.readContentRepository(configRepositoryId)
        .orElseGet(this::createContentRepository).contentRepositoryId();

    checkState(contentRepositoryId != null, "Missing contentRepositoryId");

    assetBlobValidator = dependencies.assetBlobValidators.selectValidator(getRepository());
  }

  @Override
  protected void doDestroy() throws Exception {
    config = null;
  }

  /**
   * Creates a {@link ContentRepository} for the associated config repository.
   */
  private final ContentRepository createContentRepository() {
    ContentRepositoryData contentRepository = newContentRepository();
    contentRepository.setConfigRepositoryId(configRepositoryId);
    stores.contentRepositoryStore.createContentRepository(contentRepository);
    return contentRepository;
  }

  /**
   * Retrieves the latest {@link ContentRepository} data from the store.
   */
  protected final ContentRepository contentRepository() {
    return stores.contentRepositoryStore
        .readContentRepository(configRepositoryId)
        .orElseThrow(() -> new IllegalStateException("Missing content repository for " + configRepositoryId));
  }

  @Override
  @Transactional
  protected void doDelete() throws Exception {
    if (configRepositoryId != null) {
      stores.assetStore.deleteAssets(contentRepositoryId);
      stores.componentStore.deleteComponents(contentRepositoryId);
      stores.contentRepositoryStore.deleteContentRepository(configRepositoryId);
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
  public final OffsetDateTime created() {
    return contentRepository().created();
  }

  @Override
  public final OffsetDateTime lastUpdated() {
    return contentRepository().lastUpdated();
  }

  @Override
  public final ContentFacet attributes(final AttributeChange change, final String key, final Object value) {
    ContentRepository contentRepository = contentRepository();
    if (applyAttributeChange(contentRepository, change, key, value)) {
      stores.contentRepositoryStore.updateContentRepositoryAttributes(contentRepository);
    }
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
    return dependencies.dataSessionSupplier.openSession(config.dataStoreName);
  }

  public final Optional<ClientInfo> clientInfo() {
    return ofNullable(dependencies.clientInfoProvider.getCurrentThreadClientInfo());
  }

  public final String nodeName() {
    return dependencies.nodeAccess.getId();
  }

  public final Repository repository() {
    return getRepository();
  }

  public final ContentFacetStores stores() {
    return stores;
  }

  public final void checkAttachAllowed(final Asset asset) {
    if (!asset.blob().isPresent()) {
      if (!writePolicy(asset).checkCreateAllowed()) {
        throwNotAllowed(asset, " is read-only");
      }
    }
    else if (!writePolicy(asset).checkUpdateAllowed()) {
      throwNotAllowed(asset, " cannot be updated");
    }
  }

  public final void checkDeleteAllowed(final Asset asset) {
    if (asset.blob().isPresent() && !writePolicy(asset).checkDeleteAllowed()) {
      throwNotAllowed(asset, " cannot be deleted");
    }
  }

  private void throwNotAllowed(final Asset asset, final String reason) {
    throw new IllegalOperationException(repository().getName() + asset.path() + reason);
  }

  public final String checkContentType(final Asset asset, final Blob blob) {
    return determineContentType(asset, blob, config.strictContentTypeValidation);
  }

  /**
   * Override this method to customize the write-policy of selected assets.
   */
  protected WritePolicy writePolicy(final Asset asset) {
    return ofNullable(config.writePolicy).orElse(ALLOW);
  }

  /**
   * Override this method to customize Content-Type validation of asset blobs.
   */
  protected String determineContentType(final Asset asset, final Blob blob, final boolean strictValidation) {
    return assetBlobValidator.determineContentType(asset, blob, strictValidation);
  }

  /**
   * Override this method to customize creation of new content repositories.
   */
  protected ContentRepositoryData newContentRepository() {
    return new ContentRepositoryData();
  }
}
