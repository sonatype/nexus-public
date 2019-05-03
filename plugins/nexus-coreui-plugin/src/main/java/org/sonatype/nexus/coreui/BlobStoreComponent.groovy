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
import javax.validation.constraints.NotNull
import javax.validation.groups.Default

import org.sonatype.nexus.blobstore.BlobStoreDescriptor
import org.sonatype.nexus.blobstore.api.BlobStore
import org.sonatype.nexus.blobstore.api.BlobStoreConfiguration
import org.sonatype.nexus.blobstore.api.BlobStoreException
import org.sonatype.nexus.blobstore.api.BlobStoreManager
import org.sonatype.nexus.blobstore.group.BlobStoreGroup
import org.sonatype.nexus.blobstore.group.BlobStoreGroupService
import org.sonatype.nexus.blobstore.group.FillPolicy
import org.sonatype.nexus.blobstore.quota.BlobStoreQuota
import org.sonatype.nexus.blobstore.quota.BlobStoreQuotaSupport
import org.sonatype.nexus.common.app.ApplicationDirectories
import org.sonatype.nexus.common.collect.NestedAttributesMap
import org.sonatype.nexus.extdirect.DirectComponentSupport
import org.sonatype.nexus.extdirect.model.StoreLoadParameters
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
import org.hibernate.validator.constraints.NotEmpty

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
  Map<String, BlobStoreDescriptor> blobStoreDescriptors

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

    blobStores.collect { asBlobStoreXO(it, blobStoreGroups) }
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

    blobStores.findAll {
      it.isGroupable() &&
          !repositoryManager.browseForBlobStore(it.blobStoreConfiguration.name).any() &&
          !otherGroups.any { group -> group.members.contains(it) }
    }.collect { asBlobStoreXO(it) }
  }

  @DirectMethod
  @Timed
  @ExceptionMetered
  @RequiresPermissions('nexus:blobstores:read')
  List<BlobStoreXO> readGroups() {
    blobStoreManager.browse().findAll{ it.blobStoreConfiguration.type == 'Group' }.collect { asBlobStoreXO(it) }
  }

  @DirectMethod
  @Timed
  @ExceptionMetered
  @RequiresPermissions('nexus:blobstores:read')
  List<BlobStoreTypeXO> readTypes() {
    blobStoreDescriptors.collect { key, descriptor ->
      new BlobStoreTypeXO(
          id: key,
          name: descriptor.name,
          formFields: descriptor.formFields.collect { FormFieldXO.create(it) },
          isModifiable: descriptor.isModifiable(),
          isEnabled: descriptor.isEnabled()
      )
    }
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
    return asBlobStoreXO(blobStoreManager.create(asConfiguration(blobStore)))
  }

  @DirectMethod
  @Timed
  @ExceptionMetered
  @RequiresPermissions('nexus:blobstores:update')
  @Validate(groups = [Update, Default])
  BlobStoreXO update(final @NotNull @Valid BlobStoreXO blobStore) {
    return asBlobStoreXO(blobStoreManager.update(asConfiguration(blobStore)))
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

  static BlobStoreConfiguration asConfiguration(final BlobStoreXO blobStoreXO) {
    if (checkBoxMapping(blobStoreXO.isQuotaEnabled)) {
      Map quotaAttributes = new HashMap<String, Object>()
      quotaAttributes.put(BlobStoreQuotaSupport.TYPE_KEY, blobStoreXO.quotaType)
      quotaAttributes.put(BlobStoreQuotaSupport.LIMIT_KEY, blobStoreXO.quotaLimit * MILLION)
      blobStoreXO.attributes.put(BlobStoreQuotaSupport.ROOT_KEY, quotaAttributes)
    }

    new BlobStoreConfiguration(
        name: blobStoreXO.name,
        type: blobStoreXO.type,
        attributes: blobStoreXO.attributes
    )
  }

  private static boolean checkBoxMapping(final String value) {
    return value != null && ('true'.equalsIgnoreCase(value) ||
    'on'.equalsIgnoreCase(value)  || '1'.equalsIgnoreCase(value))
  }

  BlobStoreXO asBlobStoreXO(final BlobStore blobStore, final Collection<BlobStoreGroup> blobStoreGroups = []) {
    NestedAttributesMap quotaAttributes = blobStore.getBlobStoreConfiguration().attributes(BlobStoreQuotaSupport.ROOT_KEY)
    def blobStoreXO = new BlobStoreXO(
        name: blobStore.blobStoreConfiguration.name,
        type: blobStore.blobStoreConfiguration.type,
        attributes: blobStore.blobStoreConfiguration.attributes,
        repositoryUseCount: repositoryManager.blobstoreUsageCount(blobStore.blobStoreConfiguration.name),
        blobStoreUseCount: blobStoreManager.blobStoreUsageCount(blobStore.blobStoreConfiguration.name),
        inUse: repositoryManager.isBlobstoreUsed(blobStore.blobStoreConfiguration.name),
        promotable: blobStoreManager.isPromotable(blobStore.blobStoreConfiguration.name),
        isQuotaEnabled: !quotaAttributes.isEmpty(),
        quotaType: quotaAttributes.get(BlobStoreQuotaSupport.TYPE_KEY),
        quotaLimit: quotaAttributes.get(BlobStoreQuotaSupport.LIMIT_KEY, Number.class)?.div(MILLION)?.toLong(),
        groupName: blobStoreGroups.find { it.members.contains(blobStore) }?.blobStoreConfiguration?.name
    )
    if (blobStore.isStarted()) {
      blobStoreXO.with {
        blobCount = blobStore.metrics.blobCount
        totalSize = blobStore.metrics.totalSize
        availableSpace = blobStore.metrics.availableSpace
        unlimited = blobStore.metrics.unlimited
      }
    }
    return blobStoreXO
  }

  @DirectMethod
  @Timed
  @ExceptionMetered
  @RequiresPermissions('nexus:blobstores:update')
  @Validate(groups = [Update.class, Default.class])
  BlobStoreXO promoteToGroup(final @NotNull @Valid String fromName) {
    BlobStore from = blobStoreManager.get(fromName)
    if (blobStoreGroupService.get()?.isEnabled() && blobStoreManager.isPromotable(fromName)) {
      return asBlobStoreXO(blobStoreGroupService.get().promote(from))
    }
    throw new BlobStoreException("Blob store (${fromName}) could not be promoted to a blob store group", null)
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
}
