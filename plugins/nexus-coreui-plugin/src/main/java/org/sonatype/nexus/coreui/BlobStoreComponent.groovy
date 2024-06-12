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
package org.sonatype.nexus.coreui

import javax.annotation.Nullable
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Provider
import javax.inject.Singleton
import javax.validation.Valid
import javax.validation.constraints.NotEmpty
import javax.validation.constraints.NotNull
import javax.validation.groups.Default

import org.sonatype.nexus.blobstore.BlobStoreDescriptorProvider
import org.sonatype.nexus.blobstore.api.BlobStore
import org.sonatype.nexus.blobstore.api.BlobStoreConfiguration
import org.sonatype.nexus.blobstore.api.BlobStoreException
import org.sonatype.nexus.blobstore.api.BlobStoreManager
import org.sonatype.nexus.blobstore.api.tasks.BlobStoreTaskService
import org.sonatype.nexus.blobstore.group.BlobStoreGroup
import org.sonatype.nexus.blobstore.group.BlobStoreGroupService
import org.sonatype.nexus.blobstore.group.FillPolicy
import org.sonatype.nexus.blobstore.quota.BlobStoreQuota
import org.sonatype.nexus.blobstore.quota.BlobStoreQuotaSupport
import org.sonatype.nexus.common.app.ApplicationDirectories
import org.sonatype.nexus.common.collect.NestedAttributesMap
import org.sonatype.nexus.extdirect.DirectComponentSupport
import org.sonatype.nexus.extdirect.model.StoreLoadParameters
import org.sonatype.nexus.rapture.PasswordPlaceholder
import org.sonatype.nexus.repository.blobstore.BlobStoreConfigurationStore
import org.sonatype.nexus.repository.manager.RepositoryManager
import org.sonatype.nexus.repository.security.RepositoryPermissionChecker
import org.sonatype.nexus.security.privilege.ApplicationPermission
import org.sonatype.nexus.validation.Validate
import org.sonatype.nexus.validation.group.Create
import org.sonatype.nexus.validation.group.Update

import com.codahale.metrics.annotation.ExceptionMetered
import com.codahale.metrics.annotation.Timed
import com.softwarementors.extjs.djn.config.annotations.DirectAction
import com.softwarementors.extjs.djn.config.annotations.DirectMethod
import org.apache.shiro.authz.annotation.RequiresPermissions

import static java.util.Collections.singletonList
import static org.sonatype.nexus.security.BreadActions.READ

/**
 * BlobStore {@link org.sonatype.nexus.extdirect.DirectComponent}.
 *
 * @since 3.0
 */
@Named
@Singleton
@DirectAction(action = 'coreui_Blobstore')
class BlobStoreComponent
    extends DirectComponentSupport
{
  private static final long MILLION = 1_000_000

  @Inject
  BlobStoreManager blobStoreManager

  @Inject
  BlobStoreConfigurationStore store;

  @Inject
  BlobStoreDescriptorProvider blobStoreDescriptorProvider;

  @Inject
  Map<String, BlobStoreQuota> quotaFactories

  @Inject
  ApplicationDirectories applicationDirectories

  @Inject
  RepositoryManager repositoryManager

  @Inject
  RepositoryPermissionChecker repositoryPermissionChecker

  @Inject
  Provider<BlobStoreGroupService> blobStoreGroupService

  @Inject
  Map<String, FillPolicy> fillPolicies

  @Nullable
  @Inject
  BlobStoreTaskService blobStoreTaskService;

  @DirectMethod
  @Timed
  @ExceptionMetered
  List<BlobStoreXO> read() {
    repositoryPermissionChecker.ensureUserHasAnyPermissionOrAdminAccess(
        singletonList(new ApplicationPermission('blobstores', READ)),
        READ,
        repositoryManager.browse()
    )
    def blobStores = blobStoreManager.browse()
    def blobStoreGroups = blobStores.findAll { it.blobStoreConfiguration.type == BlobStoreGroup.TYPE }.
        collect { it as BlobStoreGroup }

    store.list().collect { asBlobStoreXO(it, blobStoreGroups) }
  }

  @DirectMethod
  @Timed
  @ExceptionMetered
  List<BlobStoreXO> ReadNoneGroupEntriesIncludingEntryForAll() {
    repositoryPermissionChecker.ensureUserHasAnyPermissionOrAdminAccess(
        singletonList(new ApplicationPermission('blobstores', READ)),
        READ,
        repositoryManager.browse()
    )

    def blobStores = store.list()
        .stream()
        .filter { (it.getType() != BlobStoreGroup.TYPE) }
        .collect { asBlobStoreXO(it) };

    def allXO = new BlobStoreXO(name: '(All Blob Stores)')
    blobStores.add(allXO);

    return blobStores;
  }

  @DirectMethod
  @Timed
  @ExceptionMetered
  List<BlobStoreXO> readNames() {
    repositoryPermissionChecker.ensureUserHasAnyPermissionOrAdminAccess(
        singletonList(new ApplicationPermission('blobstores', READ)),
        READ,
        repositoryManager.browse()
    )
    store.list()
        .collect { new BlobStoreXO(name: it.name) }
  }

  @DirectMethod
  @Timed
  @ExceptionMetered
  @RequiresPermissions('nexus:blobstores:read')
  List<BlobStoreXO> readGroupable(final @Nullable StoreLoadParameters parameters) {
    def blobStores = blobStoreManager.browse()
    def blobStoreGroups = blobStores.findAll { it.blobStoreConfiguration.type == BlobStoreGroup.TYPE }.
        collect { it as BlobStoreGroup }
    def selectedBlobStoreName = parameters.getFilter('blobStoreName')
    def otherGroups = blobStoreGroups.findAll {
      !selectedBlobStoreName || it.blobStoreConfiguration.name != selectedBlobStoreName
    }

    store.list().findAll {
      it.type != BlobStoreGroup.TYPE &&
          !repositoryManager.browseForBlobStore(it.name).any() &&
          !otherGroups.any { group -> group.members.contains(it) }
    }.collect { asBlobStoreXO(it) }
  }

  @DirectMethod
  @Timed
  @ExceptionMetered
  @RequiresPermissions('nexus:blobstores:read')
  List<BlobStoreXO> readGroups() {
    store.list().findAll{ it.type == 'Group' }
        .collect { asBlobStoreXO(it) }
  }

  @DirectMethod
  @Timed
  @ExceptionMetered
  @RequiresPermissions('nexus:blobstores:read')
  List<BlobStoreTypeXO> readTypes() {
    List<BlobStoreTypeXO> readTypes = blobStoreDescriptorProvider.get().collect { key, descriptor ->
      new BlobStoreTypeXO(
          id: key,
          name: descriptor.name,
          formFields: descriptor.formFields.collect { FormFieldXO.create(it) },
          customFormName: descriptor.customFormName(),
          isModifiable: descriptor.isModifiable(),
          isConnectionTestable: descriptor.isConnectionTestable(),
          isEnabled: descriptor.isEnabled()
      )
    }
    readTypes.add(new BlobStoreTypeXO(
        id: "",
        name: "",
        customFormName: "",
        isModifiable: false,
        isEnabled: true
    ))
    return readTypes;
  }

  @DirectMethod
  @Timed
  @ExceptionMetered
  @RequiresPermissions('nexus:blobstores:read')
  List<BlobStoreQuotaTypeXO> readQuotaTypes() {
    quotaFactories.collect { key, value -> new BlobStoreQuotaTypeXO(id: key, name: value.displayName) }
  }

  @DirectMethod
  @Timed
  @ExceptionMetered
  @RequiresPermissions('nexus:blobstores:create')
  @Validate(groups = [Create, Default])
  BlobStoreXO create(final @NotNull @Valid BlobStoreXO blobStore) {
    return asBlobStoreXO(blobStoreManager.create(asConfiguration(blobStore)).getBlobStoreConfiguration())
  }

  @DirectMethod
  @Timed
  @ExceptionMetered
  @RequiresPermissions('nexus:blobstores:update')
  @Validate(groups = [Update, Default])
  BlobStoreXO update(final @NotNull @Valid BlobStoreXO blobStoreXO) {
    BlobStore blobStore = blobStoreManager.get(blobStoreXO.name)
    if (PasswordPlaceholder.is(blobStoreXO?.attributes?.s3?.secretAccessKey)) {
      //Did not update the password, just use the password we already have
      blobStoreXO.attributes.s3.secretAccessKey =
          blobStore.blobStoreConfiguration.attributes?.s3?.secretAccessKey
    }
    if (blobStoreXO != null && blobStoreXO.attributes != null && blobStoreXO.attributes["azure cloud storage"] != null &&
        PasswordPlaceholder.is(blobStoreXO.attributes["azure cloud storage"].accountKey)) {
      //Did not update the password, just use the password we already have
      blobStoreXO.attributes["azure cloud storage"].accountKey =
          blobStore.blobStoreConfiguration.attributes["azure cloud storage"].accountKey
    }
    return asBlobStoreXO(blobStoreManager.update(asConfiguration(blobStoreXO)).getBlobStoreConfiguration())
  }

  @DirectMethod
  @Timed
  @ExceptionMetered
  @RequiresPermissions('nexus:blobstores:delete')
  @Validate
  void remove(final @NotEmpty String name) {
    if (repositoryManager.isBlobstoreUsed(name)) {
      throw new BlobStoreException("Blob store (${name}) is in use by at least one repository", null)
    }
    if (blobStoreTaskService.countTasksInUseForBlobStore(name) > 0) {
      throw new BlobStoreException("Blob store (${name}) is in use by a Change Repository Blob Store task", null)
    }
    blobStoreManager.delete(name)
  }

  @DirectMethod
  @Timed
  @ExceptionMetered
  @RequiresPermissions('nexus:blobstores:read')
  PathSeparatorXO defaultWorkDirectory() {
    return new PathSeparatorXO(
        path: applicationDirectories.getWorkDirectory('blobs'),
        fileSeparator: File.separator
    )
  }

  BlobStoreConfiguration asConfiguration(final BlobStoreXO blobStoreXO) {
    if (blobStoreXO.isQuotaEnabled) {
      Map quotaAttributes = new HashMap<String, Object>()
      quotaAttributes.put(BlobStoreQuotaSupport.TYPE_KEY, blobStoreXO.quotaType)
      quotaAttributes.put(BlobStoreQuotaSupport.LIMIT_KEY, blobStoreXO.quotaLimit * MILLION)
      blobStoreXO.attributes.put(BlobStoreQuotaSupport.ROOT_KEY, quotaAttributes)
    }

    def config = blobStoreManager.newConfiguration()
    config.name = blobStoreXO.name
    config.setType(blobStoreXO.type)
    config.setAttributes(blobStoreXO.attributes)
    return config
  }

  BlobStoreXO asBlobStoreXO(
      final BlobStoreConfiguration blobStoreConfiguration,
      final Collection<BlobStoreGroup> blobStoreGroups = [])
  {
    NestedAttributesMap quotaAttributes = blobStoreConfiguration.attributes(BlobStoreQuotaSupport.ROOT_KEY)
    def blobStoreXO = new BlobStoreXO()
        .withName(blobStoreConfiguration.name)
        .withType(blobStoreConfiguration.type)
        .withAttributes(filterAttributes(blobStoreConfiguration.attributes))
        .withRepositoryUseCount(repositoryManager.blobstoreUsageCount(blobStoreConfiguration.name))
        .withTaskUseCount(blobStoreTaskService.countTasksInUseForBlobStore(blobStoreConfiguration.name))
        .withBlobStoreUseCount(blobStoreManager.blobStoreUsageCount(blobStoreConfiguration.name))
        .withInUse(repositoryManager.isBlobstoreUsed(blobStoreConfiguration.name))
        .withConvertable(blobStoreManager.isConvertable(blobStoreConfiguration.name))
        .withIsQuotaEnabled(!quotaAttributes.isEmpty())
        .withQuotaType(quotaAttributes.get(BlobStoreQuotaSupport.TYPE_KEY))
        .withQuotaLimit(quotaAttributes.get(BlobStoreQuotaSupport.LIMIT_KEY, Number.class)?.div(MILLION)?.toLong())
        .withGroupName(blobStoreGroups.find { it.members.contains(blobStoreConfiguration) }?.blobStoreConfiguration?.name)

    BlobStore blobStore = blobStoreManager.getByName().get(blobStoreConfiguration.getName())
    if (blobStore != null && blobStore.isStarted()) {
      def metrics = blobStore.metrics
      blobStoreXO.with {
        blobCount = metrics.blobCount
        totalSize = metrics.totalSize
        availableSpace = metrics.availableSpace
        unlimited = metrics.unlimited
        unavailable = metrics.unavailable
      }
    }
    else {
      blobStoreXO.unavailable = true
    }
    return blobStoreXO
  }

  @DirectMethod
  @Timed
  @ExceptionMetered
  @RequiresPermissions('nexus:blobstores:read')
  List<FillPolicyXO> fillPolicies() {
    fillPolicies.collect { id, policy ->
      new FillPolicyXO(id: id, name: policy.name)
    }
  }

  Map<String, Map<String, Object>> filterAttributes(Map<String, Map<String, Object>> attributes) {
    if (attributes?.s3?.secretAccessKey != null) {
      return [*:attributes, s3: [*:attributes.s3, secretAccessKey: PasswordPlaceholder.get()]]
    }
    else if (attributes != null && attributes.get("azure cloud storage") != null) {
      return [*:attributes, 'azure cloud storage': [*:attributes.'azure cloud storage', accountKey: PasswordPlaceholder.get()]]
    }
    else {
      return attributes
    }
  }
}
