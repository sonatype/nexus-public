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
package org.sonatype.nexus.coreui;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.groups.Default;

import org.sonatype.nexus.blobstore.BlobStoreDescriptor;
import org.sonatype.nexus.blobstore.BlobStoreDescriptorProvider;
import org.sonatype.nexus.blobstore.api.BlobStore;
import org.sonatype.nexus.blobstore.api.BlobStoreConfiguration;
import org.sonatype.nexus.blobstore.api.BlobStoreException;
import org.sonatype.nexus.blobstore.api.BlobStoreManager;
import org.sonatype.nexus.blobstore.api.BlobStoreMetrics;
import org.sonatype.nexus.blobstore.api.tasks.BlobStoreTaskService;
import org.sonatype.nexus.blobstore.group.BlobStoreGroup;
import org.sonatype.nexus.blobstore.quota.BlobStoreQuota;
import org.sonatype.nexus.blobstore.quota.BlobStoreQuotaSupport;
import org.sonatype.nexus.common.app.ApplicationDirectories;
import org.sonatype.nexus.common.collect.NestedAttributesMap;
import org.sonatype.nexus.extdirect.DirectComponentSupport;
import org.sonatype.nexus.extdirect.model.StoreLoadParameters;
import org.sonatype.nexus.rapture.PasswordPlaceholder;
import org.sonatype.nexus.repository.blobstore.BlobStoreConfigurationStore;
import org.sonatype.nexus.repository.manager.RepositoryManager;
import org.sonatype.nexus.repository.security.RepositoryPermissionChecker;
import org.sonatype.nexus.security.privilege.ApplicationPermission;
import org.sonatype.nexus.validation.Validate;
import org.sonatype.nexus.validation.group.Create;
import org.sonatype.nexus.validation.group.Update;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import com.google.common.annotations.VisibleForTesting;
import com.softwarementors.extjs.djn.config.annotations.DirectAction;
import com.softwarementors.extjs.djn.config.annotations.DirectMethod;
import org.apache.shiro.authz.annotation.RequiresPermissions;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.Streams.stream;
import static java.util.Collections.singletonList;
import static org.sonatype.nexus.coreui.internal.blobstore.BlobStoreInternalResource.AZURE_CONFIG;
import static org.sonatype.nexus.repository.internal.blobstore.BlobStoreConfigurationData.SECRET_ACCESS_KEY;
import static org.sonatype.nexus.security.BreadActions.READ;

/**
 * BlobStore {@link org.sonatype.nexus.extdirect.DirectComponent}.
 *
 * @since 3.0
 */
@Named
@Singleton
@DirectAction(action = "coreui_Blobstore")
public class BlobStoreComponent
    extends DirectComponentSupport
{
  private static final long MILLION = 1_000_000;

  private static final String BLOB_STORES_DOMAIN = "blobstores";

  public static final String AZURE_ACCOUNT_KEY = "accountKey";

  private final BlobStoreManager blobStoreManager;

  private final BlobStoreConfigurationStore store;

  private final BlobStoreDescriptorProvider blobStoreDescriptorProvider;

  private final Map<String, BlobStoreQuota> quotaFactories;

  private final ApplicationDirectories applicationDirectories;

  private final RepositoryManager repositoryManager;

  private final RepositoryPermissionChecker repositoryPermissionChecker;

  private final BlobStoreTaskService blobStoreTaskService;

  @Inject
  public BlobStoreComponent(
      final BlobStoreManager blobStoreManager,
      final BlobStoreConfigurationStore store,
      final BlobStoreDescriptorProvider blobStoreDescriptorProvider,
      final Map<String, BlobStoreQuota> quotaFactories,
      final ApplicationDirectories applicationDirectories,
      final RepositoryManager repositoryManager,
      final RepositoryPermissionChecker repositoryPermissionChecker,
      @Nullable final BlobStoreTaskService blobStoreTaskService)
  {
    this.blobStoreManager = checkNotNull(blobStoreManager);
    this.store = checkNotNull(store);
    this.blobStoreDescriptorProvider = checkNotNull(blobStoreDescriptorProvider);
    this.quotaFactories = checkNotNull(quotaFactories);
    this.applicationDirectories = checkNotNull(applicationDirectories);
    this.repositoryManager = checkNotNull(repositoryManager);
    this.repositoryPermissionChecker = checkNotNull(repositoryPermissionChecker);
    this.blobStoreTaskService = blobStoreTaskService;
  }

  @DirectMethod
  @Timed
  @ExceptionMetered
  public List<BlobStoreXO> read() {
    repositoryPermissionChecker.ensureUserHasAnyPermissionOrAdminAccess(
        singletonList(new ApplicationPermission(BLOB_STORES_DOMAIN, READ)),
        READ,
        repositoryManager.browse());

    List<BlobStoreGroup> blobStoreGroups = getBlobStoreGroups();

    return store.list()
        .stream()
        .map(config -> asBlobStoreXO(config, blobStoreGroups))
        .collect(Collectors.toList()); // NOSONAR
  }

  @DirectMethod
  @Timed
  @ExceptionMetered
  public List<BlobStoreXO> readNoneGroupEntriesIncludingEntryForAll() {
    repositoryPermissionChecker.ensureUserHasAnyPermissionOrAdminAccess(
        singletonList(new ApplicationPermission(BLOB_STORES_DOMAIN, READ)),
        READ,
        repositoryManager.browse());

    List<BlobStoreXO> blobStores = store.list()
        .stream()
        .filter(config -> !BlobStoreGroup.TYPE.equals(config.getType()))
        .map(this::asBlobStoreXO)
        .collect(Collectors.toList());

    BlobStoreXO allXO = new BlobStoreXO().withName("(All Blob Stores)");
    blobStores.add(allXO);

    return blobStores;
  }

  @DirectMethod
  @Timed
  @ExceptionMetered
  public List<BlobStoreXO> readNames() {
    repositoryPermissionChecker.ensureUserHasAnyPermissionOrAdminAccess(
        singletonList(new ApplicationPermission(BLOB_STORES_DOMAIN, READ)),
        READ,
        repositoryManager.browse());
    return store.list()
        .stream()
        .map(config -> new BlobStoreXO().withName(config.getName()))
        .collect(Collectors.toList()); // NOSONAR
  }

  @DirectMethod
  @Timed
  @ExceptionMetered
  @RequiresPermissions("nexus:blobstores:read")
  public List<BlobStoreXO> readGroupable(@Nullable final StoreLoadParameters parameters) {
    List<BlobStoreGroup> blobStoreGroups = getBlobStoreGroups();
    String selectedBlobStoreName = Optional.ofNullable(parameters)
        .map(params -> params.getFilter("blobStoreName"))
        .orElse(null);
    List<BlobStoreGroup> otherGroups = blobStoreGroups.stream()
        .filter(group -> selectedBlobStoreName == null ||
            !group.getBlobStoreConfiguration().getName().equals(selectedBlobStoreName))
        .collect(Collectors.toList()); // NOSONAR

    return store.list()
        .stream()
        .filter(config -> !BlobStoreGroup.TYPE.equals(config.getType()) &&
            !repositoryManager.browseForBlobStore(config.getName()).iterator().hasNext() &&
            isNotInOtherGroups(config, otherGroups))
        .map(this::asBlobStoreXO)
        .collect(Collectors.toList()); // NOSONAR
  }

  @DirectMethod
  @Timed
  @ExceptionMetered
  @RequiresPermissions("nexus:blobstores:read")
  public List<BlobStoreXO> readGroups() {
    return store.list()
        .stream()
        .filter(config -> BlobStoreGroup.TYPE.equals(config.getType()))
        .map(this::asBlobStoreXO)
        .collect(Collectors.toList()); // NOSONAR
  }

  @DirectMethod
  @Timed
  @ExceptionMetered
  @RequiresPermissions("nexus:blobstores:read")
  public List<BlobStoreTypeXO> readTypes() {
    List<BlobStoreTypeXO> readTypes = blobStoreDescriptorProvider.get()
        .entrySet()
        .stream()
        .map(entry -> {
          BlobStoreDescriptor descriptor = entry.getValue();
          BlobStoreTypeXO xo = new BlobStoreTypeXO();
          xo.setId(entry.getKey());
          xo.setName(descriptor.getName());
          xo.setFormFields(descriptor.getFormFields()
              .stream()
              .map(FormFieldXO::create)
              .collect(Collectors.toCollection(ArrayList::new)));
          xo.setCustomFormName(descriptor.customFormName());
          xo.setIsModifiable(descriptor.isModifiable());
          xo.setConnectionTestable(descriptor.isConnectionTestable());
          xo.setIsEnabled(descriptor.isEnabled());
          return xo;
        })
        .collect(Collectors.toList());
    BlobStoreTypeXO emptyType = new BlobStoreTypeXO();
    emptyType.setId("");
    emptyType.setName("");
    emptyType.setCustomFormName("");
    emptyType.setIsModifiable(false);
    emptyType.setIsEnabled(true);
    readTypes.add(emptyType);
    return readTypes;
  }

  @DirectMethod
  @Timed
  @ExceptionMetered
  @RequiresPermissions("nexus:blobstores:read")
  public List<BlobStoreQuotaTypeXO> readQuotaTypes() {
    return quotaFactories.entrySet()
        .stream()
        .map(entry -> {
          BlobStoreQuotaTypeXO xo = new BlobStoreQuotaTypeXO();
          xo.setId(entry.getKey());
          xo.setName(entry.getValue().getDisplayName());
          return xo;
        })
        .collect(Collectors.toList()); // NOSONAR
  }

  @DirectMethod
  @Timed
  @ExceptionMetered
  @RequiresPermissions("nexus:blobstores:create")
  @Validate(groups = {Create.class, Default.class})
  public BlobStoreXO create(@NotNull @Valid final BlobStoreXO blobStore) throws Exception {
    return asBlobStoreXO(blobStoreManager.create(asConfiguration(blobStore)).getBlobStoreConfiguration());
  }

  @DirectMethod
  @Timed
  @ExceptionMetered
  @RequiresPermissions("nexus:blobstores:update")
  @Validate(groups = {Update.class, Default.class})
  public BlobStoreXO update(@NotNull @Valid final BlobStoreXO blobStoreXO) throws Exception {
    BlobStore blobStore = blobStoreManager.get(blobStoreXO.getName());
    if (PasswordPlaceholder.is(getS3SecretAccessKey(blobStoreXO))) {
      // Did not update the password, just use the password we already have
      blobStoreXO.getAttributes()
          .get("s3")
          .put(SECRET_ACCESS_KEY,
              blobStore.getBlobStoreConfiguration().getAttributes().get("s3").get(SECRET_ACCESS_KEY));
    }
    if (PasswordPlaceholder.is(getAzureAccountKey(blobStoreXO))) {
      // Did not update the password, just use the password we already have
      blobStoreXO.getAttributes()
          .get(AZURE_CONFIG)
          .put(AZURE_ACCOUNT_KEY,
              blobStore.getBlobStoreConfiguration().getAttributes().get(AZURE_CONFIG).get(AZURE_ACCOUNT_KEY));
    }
    return asBlobStoreXO(blobStoreManager.update(asConfiguration(blobStoreXO)).getBlobStoreConfiguration());
  }

  @DirectMethod
  @Timed
  @ExceptionMetered
  @RequiresPermissions("nexus:blobstores:delete")
  @Validate
  public void remove(@NotEmpty String name) throws Exception {
    if (repositoryManager.isBlobstoreUsed(name)) {
      throw new BlobStoreException("Blob store (" + name + ") is in use by at least one repository", null);
    }
    if (blobStoreTaskService.countTasksInUseForBlobStore(name) > 0) {
      throw new BlobStoreException("Blob store (" + name + ") is in use by a Change Repository Blob Store task", null);
    }
    blobStoreManager.delete(name);
  }

  @DirectMethod
  @Timed
  @ExceptionMetered
  @RequiresPermissions("nexus:blobstores:read")
  public PathSeparatorXO defaultWorkDirectory() {
    PathSeparatorXO xo = new PathSeparatorXO();
    xo.setPath(applicationDirectories.getWorkDirectory("blobs").getPath());
    xo.setFileSeparator(File.separator);
    return xo;
  }

  @VisibleForTesting
  BlobStoreConfiguration asConfiguration(BlobStoreXO blobStoreXO) {
    if (blobStoreXO.isQuotaEnabled()) {
      Map<String, Object> quotaAttributes = new HashMap<>();
      quotaAttributes.put(BlobStoreQuotaSupport.TYPE_KEY, blobStoreXO.getQuotaType());
      quotaAttributes.put(BlobStoreQuotaSupport.LIMIT_KEY, blobStoreXO.getQuotaLimit() * MILLION);
      blobStoreXO.getAttributes().put(BlobStoreQuotaSupport.ROOT_KEY, quotaAttributes);
    }

    BlobStoreConfiguration config = blobStoreManager.newConfiguration();
    config.setName(blobStoreXO.getName());
    config.setType(blobStoreXO.getType());
    config.setAttributes(blobStoreXO.getAttributes());
    return config;
  }

  @VisibleForTesting
  BlobStoreXO asBlobStoreXO(BlobStoreConfiguration blobStoreConfiguration) {
    return asBlobStoreXO(blobStoreConfiguration, Collections.emptyList());
  }

  @VisibleForTesting
  BlobStoreXO asBlobStoreXO(
      BlobStoreConfiguration blobStoreConfiguration,
      Collection<BlobStoreGroup> blobStoreGroups)
  {
    NestedAttributesMap quotaAttributes = blobStoreConfiguration.attributes(BlobStoreQuotaSupport.ROOT_KEY);
    BlobStoreXO blobStoreXO = new BlobStoreXO()
        .withName(blobStoreConfiguration.getName())
        .withType(blobStoreConfiguration.getType())
        .withAttributes(filterAttributes(blobStoreConfiguration.getAttributes()))
        .withRepositoryUseCount(repositoryManager.blobstoreUsageCount(blobStoreConfiguration.getName()))
        .withTaskUseCount(blobStoreTaskService.countTasksInUseForBlobStore(blobStoreConfiguration.getName()))
        .withBlobStoreUseCount(blobStoreManager.blobStoreUsageCount(blobStoreConfiguration.getName()))
        .withInUse(repositoryManager.isBlobstoreUsed(blobStoreConfiguration.getName()))
        .withConvertable(blobStoreManager.isConvertable(blobStoreConfiguration.getName()))
        .withIsQuotaEnabled(!quotaAttributes.isEmpty())
        .withQuotaType((String) quotaAttributes.get(BlobStoreQuotaSupport.TYPE_KEY))
        .withQuotaLimit(getQuotaLimit(quotaAttributes))
        .withGroupName(blobStoreGroups.stream()
            .filter(group -> getMembersConfig(group.getMembers()).contains(blobStoreConfiguration))
            .map(group -> group.getBlobStoreConfiguration().getName())
            .findFirst()
            .orElse(null));

    BlobStore blobStore = blobStoreManager.getByName().get(blobStoreConfiguration.getName());
    if (blobStore != null && blobStore.isStarted()) {
      BlobStoreMetrics metrics = blobStore.getMetrics();
      blobStoreXO.withBlobCount(metrics.getBlobCount())
          .withTotalSize(metrics.getTotalSize())
          .withAvailableSpace(metrics.getAvailableSpace())
          .withUnlimited(metrics.isUnlimited())
          .withUnavailable(metrics.isUnavailable());
    }
    else {
      blobStoreXO.withUnavailable(true);
    }
    return blobStoreXO;
  }

  private static Map<String, Map<String, Object>> filterAttributes(Map<String, Map<String, Object>> attributes) {
    if (attributes.get("s3") != null && attributes.get("s3").get(SECRET_ACCESS_KEY) != null) {
      attributes.get("s3").put(SECRET_ACCESS_KEY, PasswordPlaceholder.get());
    }
    else if (attributes.get(AZURE_CONFIG) != null &&
        attributes.get(AZURE_CONFIG).get(AZURE_ACCOUNT_KEY) != null) {
      attributes.get(AZURE_CONFIG).put(AZURE_ACCOUNT_KEY, PasswordPlaceholder.get());
    }
    return attributes;
  }

  private List<BlobStoreGroup> getBlobStoreGroups() {
    return stream(blobStoreManager.browse())
        .filter(blobStore -> BlobStoreGroup.TYPE.equals(blobStore.getBlobStoreConfiguration().getType()))
        .map(BlobStoreGroup.class::cast)
        .collect(Collectors.toList()); // NOSONAR
  }

  private String getS3SecretAccessKey(final BlobStoreXO blobStoreXO) {
    return Optional.ofNullable(blobStoreXO)
        .map(BlobStoreXO::getAttributes)
        .map(attributes -> attributes.get("s3"))
        .map(s3 -> s3.get(SECRET_ACCESS_KEY).toString())
        .orElse(null);
  }

  private String getAzureAccountKey(final BlobStoreXO blobStoreXO) {
    return Optional.ofNullable(blobStoreXO)
        .map(BlobStoreXO::getAttributes)
        .map(attributes -> attributes.get(AZURE_CONFIG))
        .map(s3 -> s3.get(AZURE_ACCOUNT_KEY).toString())
        .orElse(null);
  }

  private Long getQuotaLimit(final NestedAttributesMap quotaAttributes) {
    Long quotaLimit = null;
    Number limit = quotaAttributes.get(BlobStoreQuotaSupport.LIMIT_KEY, Number.class);
    if (limit != null) {
      quotaLimit = limit.longValue() / MILLION;
    }
    return quotaLimit;
  }

  private boolean isNotInOtherGroups(final BlobStoreConfiguration config, final List<BlobStoreGroup> otherGroups) {
    return otherGroups.stream()
        .noneMatch(group -> getMembersConfig(group.getMembers())
            .contains(config));
  }

  private List<BlobStoreConfiguration> getMembersConfig(final List<BlobStore> members) {
    return members.stream()
        .map(BlobStore::getBlobStoreConfiguration)
        .collect(Collectors.toList()); // NOSONAR
  }
}
