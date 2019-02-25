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
package org.sonatype.nexus.repository.storage;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.validation.ConstraintViolation;
import javax.validation.constraints.NotNull;
import javax.validation.groups.Default;

import org.sonatype.nexus.blobstore.api.Blob;
import org.sonatype.nexus.blobstore.api.BlobStore;
import org.sonatype.nexus.blobstore.api.BlobStoreManager;
import org.sonatype.nexus.common.collect.NestedAttributesMap;
import org.sonatype.nexus.common.event.EventHelper;
import org.sonatype.nexus.common.hash.HashAlgorithm;
import org.sonatype.nexus.common.hash.MultiHashingInputStream;
import org.sonatype.nexus.common.node.NodeAccess;
import org.sonatype.nexus.common.stateguard.Guarded;
import org.sonatype.nexus.common.stateguard.StateGuardAspect;
import org.sonatype.nexus.orient.DatabaseInstance;
import org.sonatype.nexus.repository.FacetSupport;
import org.sonatype.nexus.repository.config.Configuration;
import org.sonatype.nexus.repository.config.ConfigurationFacet;
import org.sonatype.nexus.repository.types.HostedType;
import org.sonatype.nexus.repository.view.Payload;
import org.sonatype.nexus.repository.view.payloads.TempBlobPartPayload;
import org.sonatype.nexus.security.ClientInfo;
import org.sonatype.nexus.security.ClientInfoProvider;
import org.sonatype.nexus.validation.ConstraintViolationFactory;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Supplier;
import com.google.common.collect.ImmutableMap;
import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
import org.hibernate.validator.constraints.NotEmpty;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.lang.String.format;
import static org.sonatype.nexus.orient.transaction.OrientTransactional.inTxRetry;
import static org.sonatype.nexus.repository.FacetSupport.State.ATTACHED;
import static org.sonatype.nexus.repository.FacetSupport.State.INITIALISED;
import static org.sonatype.nexus.repository.FacetSupport.State.STARTED;
import static org.sonatype.nexus.repository.storage.MetadataNodeEntityAdapter.P_ATTRIBUTES;
import static org.sonatype.nexus.repository.storage.StorageFacetConstants.STORAGE;
import static org.sonatype.nexus.validation.ConstraintViolations.maybeAdd;
import static org.sonatype.nexus.validation.ConstraintViolations.maybePropagate;

/**
 * Default {@link StorageFacet} implementation.
 *
 * @since 3.0
 */
@Named("default")
public class StorageFacetImpl
    extends FacetSupport
    implements StorageFacet
{
  private final NodeAccess nodeAccess;

  private final BlobStoreManager blobStoreManager;

  private final Provider<DatabaseInstance> databaseInstanceProvider;

  private final BucketEntityAdapter bucketEntityAdapter;

  private final ComponentEntityAdapter componentEntityAdapter;

  private final AssetEntityAdapter assetEntityAdapter;

  private final ClientInfoProvider clientInfoProvider;

  private final ContentValidatorSelector contentValidatorSelector;

  private final MimeRulesSourceSelector mimeRulesSourceSelector;

  private final Supplier<StorageTx> txSupplier;

  private final StorageFacetManager storageFacetManager;

  private final ComponentFactory componentFactory;

  private final ConstraintViolationFactory constraintViolationFactory;

  @VisibleForTesting
  static class Config
  {
    @NotEmpty
    public String blobStoreName;

    @NotNull(groups = HostedType.ValidationGroup.class)
    public WritePolicy writePolicy;

    @NotNull
    public Boolean strictContentTypeValidation = Boolean.TRUE;

    @Override
    public String toString() {
      return getClass().getSimpleName() + "{" +
          "blobStoreName='" + blobStoreName + '\'' +
          ", writePolicy=" + writePolicy +
          ", strictContentTypeValidation=" + strictContentTypeValidation +
          '}';
    }
  }

  private Config config;

  private WritePolicySelector writePolicySelector;

  @Inject
  public StorageFacetImpl(final NodeAccess nodeAccess,
                          final BlobStoreManager blobStoreManager,
                          @Named(ComponentDatabase.NAME) final Provider<DatabaseInstance> databaseInstanceProvider,
                          final BucketEntityAdapter bucketEntityAdapter,
                          final ComponentEntityAdapter componentEntityAdapter,
                          final AssetEntityAdapter assetEntityAdapter,
                          final ClientInfoProvider clientInfoProvider,
                          final ContentValidatorSelector contentValidatorSelector,
                          final MimeRulesSourceSelector mimeRulesSourceSelector,
                          final StorageFacetManager storageFacetManager,
                          final ComponentFactory componentFactory,
                          final ConstraintViolationFactory constraintViolationFactory)
  {
    this.nodeAccess = checkNotNull(nodeAccess);
    this.blobStoreManager = checkNotNull(blobStoreManager);
    this.databaseInstanceProvider = checkNotNull(databaseInstanceProvider);

    this.bucketEntityAdapter = checkNotNull(bucketEntityAdapter);
    this.componentEntityAdapter = checkNotNull(componentEntityAdapter);
    this.assetEntityAdapter = checkNotNull(assetEntityAdapter);
    this.clientInfoProvider = checkNotNull(clientInfoProvider);
    this.contentValidatorSelector = checkNotNull(contentValidatorSelector);
    this.mimeRulesSourceSelector = checkNotNull(mimeRulesSourceSelector);
    this.storageFacetManager = checkNotNull(storageFacetManager);
    this.componentFactory = checkNotNull(componentFactory);
    this.constraintViolationFactory = checkNotNull(constraintViolationFactory);

    this.txSupplier = () -> openStorageTx(databaseInstanceProvider.get().acquire());
  }

  @Override
  protected void doValidate(final Configuration configuration) throws Exception {
    facet(ConfigurationFacet.class).validateSection(configuration, STORAGE, Config.class,
        Default.class, getRepository().getType().getValidationGroup()
    );

    StorageFacetImpl.Config configToValidate = facet(ConfigurationFacet.class)
        .readSection(configuration, STORAGE, StorageFacetImpl.Config.class);
    Set<ConstraintViolation<?>> violations = new HashSet<>();
    maybeAdd(violations, validateBlobStoreNotInGroup(configToValidate.blobStoreName));
    maybePropagate(violations, log);
  }

  ConstraintViolation<?> validateBlobStoreNotInGroup(final String blobStoreName) {
    return blobStoreManager.getParent(blobStoreName).map(groupName -> constraintViolationFactory
        .createViolation(format("%s.%s.blobStoreName", P_ATTRIBUTES, STORAGE),
            format("Blob Store '%s' is a member of Blob Store Group '%s' and cannot be set as storage", blobStoreName,
                groupName))).orElse(null);
  }

  @Override
  protected void doConfigure(final Configuration configuration) throws Exception {
    config = facet(ConfigurationFacet.class).readSection(configuration, STORAGE, Config.class);
    log.debug("Config: {}", config);
  }

  @Override
  protected void doInit(final Configuration configuration) throws Exception {
    initBucket();
    writePolicySelector = WritePolicySelector.DEFAULT;
    super.doInit(configuration);
  }

  private void initBucket() {
    // create the bucket for the repository if it doesn't exist
    inTxRetry(databaseInstanceProvider).run(db -> {
      String repositoryName = getRepository().getName();
      Bucket bucket = bucketEntityAdapter.read(db, repositoryName);
      if (bucket == null) {
        bucket = new Bucket();
        bucket.setRepositoryName(repositoryName);
        bucket.attributes(new NestedAttributesMap(P_ATTRIBUTES, new HashMap<>()));
        bucketEntityAdapter.addEntity(db, bucket);
      }
    });
  }

  @Override
  protected void doDestroy() throws Exception {
    config = null;
  }

  @Override
  protected void doDelete() throws Exception {
    // skip when replicating, origin node will delete the bucket blobs
    if (!EventHelper.isReplicating()) {
      inTxRetry(databaseInstanceProvider).run(db -> {
        Bucket bucket = bucketEntityAdapter.read(db, getRepository().getName());
        storageFacetManager.enqueueDeletion(getRepository(), blobStoreManager.get(config.blobStoreName), bucket);
      });
    }
  }

  @Override
  @Guarded(by = {INITIALISED, ATTACHED})
  public void registerWritePolicySelector(final WritePolicySelector writePolicySelector) {
    checkNotNull(writePolicySelector);
    this.writePolicySelector = writePolicySelector;
  }

  @Override
  @Guarded(by = STARTED)
  public Supplier<StorageTx> txSupplier() {
    return txSupplier;
  }

  @Override
  public TempBlob createTempBlob(final InputStream inputStream, final Iterable<HashAlgorithm> hashAlgorithms) {
    BlobStore blobStore = checkNotNull(blobStoreManager.get(config.blobStoreName));
    MultiHashingInputStream hashingStream = new MultiHashingInputStream(hashAlgorithms, inputStream);
    Blob blob = blobStore.create(hashingStream,
        ImmutableMap.of(
            BlobStore.BLOB_NAME_HEADER, "temp",
            BlobStore.CREATED_BY_HEADER, createdBy(),
            BlobStore.CREATED_BY_IP_HEADER, createdByIp(),
            BlobStore.TEMPORARY_BLOB_HEADER, ""));
    return new TempBlob(blob, hashingStream.hashes(), true, blobStore);
  }

  @Override
  public TempBlob createTempBlob(final Payload payload, final Iterable<HashAlgorithm> hashAlgorithms)
  {
    if (payload instanceof TempBlobPartPayload) {
      return ((TempBlobPartPayload) payload).getTempBlob();
    }
    try (InputStream inputStream = payload.openInputStream()) {
      return createTempBlob(inputStream, hashAlgorithms);
    }
    catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Returns the "principal name" to be used with current instance of {@link StorageTx}.
   */
  @Nonnull
  private String createdBy() {
    ClientInfo clientInfo = clientInfoProvider.getCurrentThreadClientInfo();
    if (clientInfo == null || clientInfo.getUserid() == null) {
      return "system";
    }
    return clientInfo.getUserid();
  }

  /**
   * Returns the "principal name" to be used with current instance of {@link StorageTx}.
   */
  @Nonnull
  private String createdByIp() {
    ClientInfo clientInfo = clientInfoProvider.getCurrentThreadClientInfo();
    if (clientInfo == null || clientInfo.getRemoteIP() == null) {
      return "system";
    }
    return clientInfo.getRemoteIP();
  }

  @Nonnull
  private StorageTx openStorageTx(final ODatabaseDocumentTx db) {
    BlobStore blobStore = blobStoreManager.get(config.blobStoreName);
    return StateGuardAspect.around(
        new StorageTxImpl(
            createdBy(),
            createdByIp(),
            new BlobTx(nodeAccess, blobStore),
            db,
            getRepository().getName(),
            config.writePolicy == null ? WritePolicy.ALLOW : config.writePolicy,
            writePolicySelector,
            bucketEntityAdapter,
            componentEntityAdapter,
            assetEntityAdapter,
            config.strictContentTypeValidation,
            contentValidatorSelector.validator(getRepository()),
            mimeRulesSourceSelector.ruleSource(getRepository()),
            componentFactory
        )
    );
  }

  @Override
  public BlobStore blobStore() {
    return blobStoreManager.get(config.blobStoreName);
  }
}
