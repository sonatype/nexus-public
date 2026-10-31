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
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import javax.annotation.Nullable;

import org.sonatype.nexus.blobstore.api.Blob;
import org.sonatype.nexus.blobstore.api.BlobStore;
import org.sonatype.nexus.common.app.softdelete.SoftDeletedAssetRef;
import org.sonatype.nexus.common.app.softdelete.TrashIndexRecorder;
import org.sonatype.nexus.common.collect.NestedAttributesMap;
import org.sonatype.nexus.common.entity.EntityId;
import org.sonatype.nexus.datastore.api.DataSession;
import org.sonatype.nexus.datastore.api.DuplicateKeyException;
import org.sonatype.nexus.datastore.api.SerializedAccessException;
import org.sonatype.nexus.repository.FacetSupport;
import org.sonatype.nexus.repository.IllegalOperationException;
import org.sonatype.nexus.repository.RedeployDisabledException;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.config.Configuration;
import org.sonatype.nexus.repository.config.ConfigurationFacet;
import org.sonatype.nexus.repository.config.WritePolicy;
import org.sonatype.nexus.repository.content.Asset;
import org.sonatype.nexus.repository.content.AttributeOperation;
import org.sonatype.nexus.repository.content.Component;
import org.sonatype.nexus.repository.content.ContentRepository;
import org.sonatype.nexus.repository.content.fluent.FluentAssets;
import org.sonatype.nexus.repository.content.fluent.FluentBlobs;
import org.sonatype.nexus.repository.content.fluent.FluentComponents;
import org.sonatype.nexus.repository.content.fluent.internal.FluentAssetsImpl;
import org.sonatype.nexus.repository.content.fluent.internal.FluentBlobsImpl;
import org.sonatype.nexus.repository.content.fluent.internal.FluentComponentsImpl;
import org.sonatype.nexus.repository.content.store.AssetData;
import org.sonatype.nexus.repository.content.store.ContentRepositoryData;
import org.sonatype.nexus.repository.content.store.FormatStoreManager;
import org.sonatype.nexus.repository.content.store.InternalIds;
import org.sonatype.nexus.repository.search.normalize.VersionNormalizerService;
import org.sonatype.nexus.repository.storage.BlobMetadataStorage;
import org.sonatype.nexus.repository.types.HostedType;
import org.sonatype.nexus.security.ClientInfo;
import org.sonatype.nexus.transaction.Transactional;
import org.sonatype.nexus.transaction.TransactionalStore;

import com.google.common.annotations.VisibleForTesting;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.groups.Default;
import org.springframework.beans.factory.annotation.Autowired;

import static com.google.common.base.Preconditions.checkState;
import static java.lang.String.format;
import static java.util.Objects.requireNonNull;
import static java.util.Optional.ofNullable;
import static org.sonatype.nexus.blobstore.api.BlobStore.CONTENT_TYPE_HEADER;
import static org.sonatype.nexus.blobstore.api.BlobStoreManager.DEFAULT_BLOBSTORE_NAME;
import static org.sonatype.nexus.datastore.api.DataStoreManager.DEFAULT_DATASTORE_NAME;
import static org.sonatype.nexus.repository.config.ConfigurationConstants.GLOBAL_DISTRIBUTION_ENABLED;
import static org.sonatype.nexus.repository.config.ConfigurationConstants.STORAGE;
import static org.sonatype.nexus.repository.config.WritePolicy.ALLOW;
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
    public String dataStoreName = DEFAULT_DATASTORE_NAME;

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

  private FluentBlobs fluentBlobs;

  private FluentComponents fluentComponents;

  private FluentAssets fluentAssets;

  private EntityId configRepositoryId;

  private Integer contentRepositoryId;

  private AssetBlobValidator assetBlobValidator;

  @Nullable
  @Autowired(required = false)
  private TrashIndexRecorder trashIndexRecorder;

  private boolean globalDistributionEnabled;

  protected ContentFacetSupport(final FormatStoreManager formatStoreManager) {
    this.formatStoreManager = requireNonNull(formatStoreManager);
  }

  @Autowired
  @VisibleForTesting
  protected final void setDependencies(final ContentFacetDependencies dependencies) {
    this.dependencies = requireNonNull(dependencies);
  }

  /**
   * @return {@code true} if the repository's {@code storage.globalDistribution} attribute is set to {@code true}.
   */
  public final boolean isGlobalDistributionEnabled() {
    return globalDistributionEnabled;
  }

  @Override
  protected void doValidate(final Configuration configuration) throws Exception {
    facet(ConfigurationFacet.class).validateSection(configuration, STORAGE, Config.class,
        Default.class, getRepository().getType().getValidationGroup());

    Config configToValidate = facet(ConfigurationFacet.class).readSection(configuration, STORAGE, Config.class);

    Set<ConstraintViolation<?>> violations = new HashSet<>();
    // Only validate blob store group membership if the blob store exists
    // Existence validation happens in BaseRepositoryManager.validateConfiguration() during create/update
    maybeAdd(violations, validateBlobStoreNotInGroup(configToValidate.blobStoreName));
    maybePropagate(violations, log);
  }

  private ConstraintViolation<?> validateBlobStoreNotInGroup(final String blobStoreName) {
    // Skip validation if blob store doesn't exist - allow repositories to load even if their blob store
    // is missing (e.g., deleted, corrupted DB) so the application can start and the repository can be fixed/deleted
    // rather than blocking entire application startup
    BlobStore blobStore = dependencies.getBlobStoreManager().get(blobStoreName);
    if (blobStore == null) {
      return null;
    }

    return dependencies.getBlobStoreManager()
        .getParent(blobStoreName)
        .map(groupName -> dependencies.getConstraintViolationFactory()
            .createViolation(format("%s.blobStoreName", STORAGE),
                format("Blob Store '%s' is a member of Blob Store Group '%s' and cannot be set as storage",
                    blobStoreName, groupName)))
        .orElse(null);
  }

  @Override
  protected void doConfigure(final Configuration configuration) throws Exception {
    config = facet(ConfigurationFacet.class).readSection(configuration, STORAGE, Config.class);
    log.debug("Config: {}", config);

    globalDistributionEnabled = readGlobalDistributionEnabledFromConfig(configuration);

    stores = new ContentFacetStores(
        dependencies.getBlobStoreManager(), config.blobStoreName,
        formatStoreManager, config.dataStoreName);

    fluentBlobs = new FluentBlobsImpl(this, stores.blobStoreProvider);
    fluentComponents = new FluentComponentsImpl(this, stores.componentStore,
        version -> versionNormalizerService().getNormalizedVersionByFormat(version, getRepository().getFormat()));
    fluentAssets = new FluentAssetsImpl(this, stores.assetStore);
  }

  private static boolean readGlobalDistributionEnabledFromConfig(final Configuration configuration) {
    Map<String, Map<String, Object>> attributes = configuration.getAttributes();
    if (attributes == null) {
      return false;
    }
    Map<String, Object> storage = attributes.get(STORAGE);
    if (storage == null) {
      return false;
    }
    Object raw = storage.getOrDefault(GLOBAL_DISTRIBUTION_ENABLED, Boolean.FALSE);
    if (raw instanceof Boolean b) {
      return b;
    }
    return Boolean.parseBoolean(String.valueOf(raw));
  }

  @Override
  protected void doStart() throws Exception {
    // configuration is persisted by this point and has been assigned an id
    configRepositoryId = getRepository().getConfiguration().getRepositoryId();

    checkState(configRepositoryId != null, "Missing configRepositoryId");

    // get or create the associated content repository (idempotent under the create race)
    contentRepositoryId = readOrCreateContentRepository().contentRepositoryId();

    checkState(contentRepositoryId != null, "Missing contentRepositoryId");

    assetBlobValidator = dependencies.getAssetBlobValidators().selectValidator(getRepository());
  }

  @Override
  protected void doDestroy() throws Exception {
    config = null;
  }

  /**
   * Creates a {@link ContentRepository} for the associated config repository.
   */
  private ContentRepository createContentRepository() {
    ContentRepositoryData contentRepository = newContentRepository();
    contentRepository.setConfigRepositoryId(configRepositoryId);
    stores.contentRepositoryStore.createContentRepository(contentRepository);
    return contentRepository;
  }

  /**
   * Reads the associated {@link ContentRepository}, creating it if absent. Tolerant of the create race in
   * which another node inserts the shared content-repository row between our read and our insert: on a losing
   * insert we absorb the {@link DuplicateKeyException} and re-read the row the peer just committed, so this
   * facet still starts instead of failing its {@code INITIALISED -> STARTED} transition.
   */
  private ContentRepository readOrCreateContentRepository() {
    Optional<ContentRepository> existing = stores.contentRepositoryStore.readContentRepository(configRepositoryId);
    if (existing.isPresent()) {
      return existing.get();
    }
    try {
      return createContentRepository();
    }
    catch (DuplicateKeyException e) {
      log.debug("Content repository for {} was created concurrently by another node; re-reading",
          configRepositoryId, e);
      return stores.contentRepositoryStore.readContentRepository(configRepositoryId)
          .orElseThrow(() -> new IllegalStateException(
              "Missing content repository for " + configRepositoryId + " after concurrent create", e));
    }
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
  // Retry on a transient serialization failure / deadlock (e.g. the async browse/search flush of this
  // repository's content racing this delete for locks on the shared content-repository rows). PostgreSQL
  // aborts one transaction; retrying the delete once the survivor releases its locks lets it complete
  // cleanly, instead of failing the STOPPED -> DELETED transition and stranding the repository. See NEXUS-54229.
  @Transactional(retryOn = SerializedAccessException.class)
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
  public final ContentFacet attributes(final AttributeOperation change, final String key, final Object value) {
    stores.contentRepositoryStore.updateContentRepositoryAttributes(contentRepository(), change, key, value);
    return this;
  }

  @Override
  public final FluentBlobs blobs() {
    return fluentBlobs;
  }

  @Override
  public final FluentComponents components() {
    return fluentComponents;
  }

  @Override
  public final FluentAssets assets() {
    return fluentAssets;
  }

  public ContentFacetDependencies dependencies() {
    return dependencies;
  }

  @Override
  public final DataSession<?> openSession() {
    return dependencies.getDataSessionSupplier().openSession(config.dataStoreName);
  }

  public final Optional<ClientInfo> clientInfo() {
    return ofNullable(dependencies.getClientInfoProvider().getCurrentThreadClientInfo());
  }

  public final String nodeName() {
    return dependencies.getNodeAccess().getId();
  }

  public final BlobMetadataStorage blobMetadataStorage() {
    return dependencies.getBlobMetadataStorage();
  }

  public final VersionNormalizerService versionNormalizerService() {
    return dependencies.getVersionNormalizerService();
  }

  public final Repository repository() {
    return getRepository();
  }

  public final ContentFacetStores stores() {
    return stores;
  }

  public final void checkAttachAllowed(final Asset asset) {
    if (!asset.hasBlob()) {
      if (!writePolicy(asset).checkCreateAllowed()) {
        throwNotAllowed(asset, " is read-only");
      }
    }
    else {
      WritePolicy policy = writePolicy(asset);
      if (!policy.checkUpdateAllowed()) {
        // Enhance error message when ALLOW_ONCE prevents update
        String reason = policy == WritePolicy.ALLOW_ONCE
            ? " cannot be updated as asset already exists and redeploy is not allowed"
            : " cannot be updated";
        throw new RedeployDisabledException(getRepository(), asset.path(), reason);
      }
    }
  }

  public final void checkDeleteAllowed(final Asset asset) {
    if (asset.hasBlob() && !writePolicy(asset).checkDeleteAllowed()) {
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
    return getConfiguredWritePolicy();
  }

  protected WritePolicy getConfiguredWritePolicy() {
    return ofNullable(config).map(config -> config.writePolicy).orElse(ALLOW);
  }

  /**
   * Override this method to customize Content-Type validation of asset blobs.
   */
  protected String determineContentType(final Asset asset, final Blob blob, final boolean strictValidation) {
    String contentType = blob.getHeaders().get(CONTENT_TYPE_HEADER);
    return assetBlobValidator.determineContentType(strictValidation, blob::getInputStream, asset.path(), contentType);
  }

  /**
   * Override this method to customize creation of new content repositories.
   */
  protected ContentRepositoryData newContentRepository() {
    return new ContentRepositoryData();
  }

  /**
   * Removes a stale trash index entry when {@code createAsset} upserts over a soft-deleted row,
   * silently clearing {@code deleted_on}. Without this, the trash index would retain a ghost entry
   * for an asset that is no longer soft-deleted (e.g., metadata files rebuilt after component restore).
   */
  public void cleanupTrashIndex(final String assetPath) {
    if (trashIndexRecorder != null) {
      try {
        trashIndexRecorder.removeAssetByPath(getRepository().getName(), assetPath);
      }
      catch (Exception e) {
        log.debug("Could not clean up trash index for asset path '{}'", assetPath, e);
      }
    }
  }

  /**
   * Reconciles the trash index when {@code createComponent} upserts over a soft-deleted row, silently
   * clearing {@code deleted_on} (i.e. a component revived by re-upload). Removes the component's now-stale
   * "umbrella" trash entry, and re-records any sibling asset that is still soft-deleted (not part of this
   * re-upload) as its own entry so it stays individually restorable -- otherwise it would become an
   * invisible orphan once the umbrella entry is gone. Re-uploaded assets clear their own re-surfaced entry
   * via {@link #cleanupTrashIndex(String)} when they are created immediately after the component.
   */
  public void cleanupComponentTrashIndex(final Component component) {
    // Gated only on the recorder's presence (absent in editions without the soft-delete plugin), NOT on the
    // nexus.softDelete.enabled flag: a stale trash index row can be left behind by a period when the flag
    // was ON, then silently revived by a createComponent upsert while the flag is OFF. If this reconciliation
    // were gated on the current flag state, that ghost row would survive -- showing the component in Trash
    // while it is live in the repository, un-restorable ("not in the soft-deleted state"). The asset path
    // ({@link #cleanupTrashIndex(String)}) is likewise ungated, so this keeps the two paths symmetric.
    if (trashIndexRecorder == null) {
      return;
    }
    String repositoryName = getRepository().getName();
    try {
      String componentExternalId =
          InternalIds.toExternalId(InternalIds.internalComponentId(component)).getValue();
      int removed = trashIndexRecorder.removeComponentByExternalId(repositoryName, componentExternalId);

      // Fast path: if no umbrella entry was removed, this component was never in Trash (a brand-new or
      // never-deleted upload -- the overwhelmingly common case), so there are no soft-deleted siblings to
      // re-surface. Skip the per-asset scan to keep the create/upload hot path cheap.
      if (removed == 0) {
        return;
      }

      String format = getRepository().getFormat().getValue();
      Integer repositoryId = contentRepositoryId();
      // browseAllComponentAssets loads the component's assets into memory (not paginated). This is bounded: a
      // component owns only a handful of assets in every format, and this scan runs solely on the rare revive path
      // (removed > 0). Docker/OCI layers and blobs are component-less shared assets, not children here, so the
      // per-component asset count stays small -- no streaming/pagination needed (NEXUS-51733).
      for (Asset asset : stores().assetStore.browseAllComponentAssetsWithSoftDelete(component)) {
        AssetData assetData = (AssetData) asset;
        if (assetData.getDeletedOn() != null) {
          String assetExternalId =
              InternalIds.toExternalId(InternalIds.internalAssetId(asset)).getValue();
          String deletedBy = assetData.getDeletedBy() != null ? assetData.getDeletedBy() : "system";
          trashIndexRecorder.resurfaceSoftDeletedChildAsset(new SoftDeletedAssetRef(
              repositoryName, repositoryId, format, componentExternalId, assetExternalId,
              asset.path(), asset.kind(), assetData.getDeletedOn(), deletedBy));
        }
      }
    }
    catch (Exception e) {
      log.debug("Could not reconcile trash index after component upsert in '{}'", repositoryName, e);
    }
  }
}
