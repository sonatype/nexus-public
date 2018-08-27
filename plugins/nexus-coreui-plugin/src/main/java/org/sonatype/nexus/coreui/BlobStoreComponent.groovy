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

import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton
import javax.validation.Valid
import javax.validation.constraints.NotNull
import javax.validation.groups.Default

import org.sonatype.nexus.blobstore.BlobStoreDescriptor
import org.sonatype.nexus.blobstore.api.BlobStore
import org.sonatype.nexus.blobstore.api.BlobStoreConfiguration
import org.sonatype.nexus.blobstore.api.BlobStoreException
import org.sonatype.nexus.blobstore.api.BlobStoreManager
import org.sonatype.nexus.blobstore.group.BlobStorePromoter
import org.sonatype.nexus.common.app.ApplicationDirectories
import org.sonatype.nexus.extdirect.DirectComponentSupport
import org.sonatype.nexus.repository.manager.RepositoryManager
import org.sonatype.nexus.validation.Validate
import org.sonatype.nexus.validation.group.Create
import org.sonatype.nexus.validation.group.Update

import com.codahale.metrics.annotation.ExceptionMetered
import com.codahale.metrics.annotation.Timed
import com.softwarementors.extjs.djn.config.annotations.DirectAction
import com.softwarementors.extjs.djn.config.annotations.DirectMethod
import org.apache.shiro.authz.annotation.RequiresPermissions
import org.hibernate.validator.constraints.NotEmpty

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
  @Inject
  BlobStoreManager blobStoreManager

  @Inject
  Map<String, BlobStoreDescriptor> blobstoreDescriptors

  @Inject
  ApplicationDirectories applicationDirectories

  @Inject
  RepositoryManager repositoryManager

  @Inject
  BlobStorePromoter blobStorePromoter

  @DirectMethod
  @Timed
  @ExceptionMetered
  @RequiresPermissions('nexus:blobstores:read')
  List<BlobStoreXO> read() {
    blobStoreManager.browse().collect { asBlobStore(it) }
  }

  @DirectMethod
  @Timed
  @ExceptionMetered
  @RequiresPermissions('nexus:blobstores:read')
  List<BlobStoreTypeXO> readTypes() {
    blobstoreDescriptors.findAll { key, descriptor ->
      descriptor.enabled
    }.collect { key, descriptor ->
      new BlobStoreTypeXO(
          id: key,
          name: descriptor.name,
          formFields: descriptor.formFields.collect { FormFieldXO.create(it) },
          isModifiable: descriptor.isModifiable()
      )
    }
  }

  @DirectMethod
  @Timed
  @ExceptionMetered
  @RequiresPermissions('nexus:blobstores:create')
  @Validate(groups = [Create, Default])
  BlobStoreXO create(final @NotNull @Valid BlobStoreXO blobStore) {
    return asBlobStore(blobStoreManager.create(
        new BlobStoreConfiguration(
            name: blobStore.name,
            type: blobStore.type,
            attributes: blobStore.attributes
        )
    ))
  }

  @DirectMethod
  @Timed
  @ExceptionMetered
  @RequiresPermissions('nexus:blobstores:update')
  @Validate(groups = [Update, Default])
  BlobStoreXO update(final @NotNull @Valid BlobStoreXO blobStore) {
    return asBlobStore(blobStoreManager.update(
        new BlobStoreConfiguration(
            name: blobStore.name,
            type: blobStore.type,
            attributes: blobStore.attributes
        )
    ))
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

  BlobStoreXO asBlobStore(final BlobStore blobStore) {
    return new BlobStoreXO(
        name: blobStore.blobStoreConfiguration.name,
        type: blobStore.blobStoreConfiguration.type,
        attributes: blobStore.blobStoreConfiguration.attributes,
        blobCount: blobStore.metrics.blobCount,
        totalSize: blobStore.metrics.totalSize,
        availableSpace: blobStore.metrics.availableSpace,
        unlimited: blobStore.metrics.unlimited,
        repositoryUseCount: repositoryManager.blobstoreUsageCount(blobStore.blobStoreConfiguration.name),
        promotable: blobStore.promotable
    )
  }

  @DirectMethod
  @Timed
  @ExceptionMetered
  @RequiresPermissions('nexus:blobstores:update')
  @Validate(groups = [Update.class, Default.class])
  BlobStoreXO promoteToGroup(final @NotNull @Valid String fromName) {
    BlobStore from = blobStoreManager.get(fromName)
    if (from.promotable) {
      return asBlobStore(blobStorePromoter.promote(from))
    }
    throw new BlobStoreException("Blob store (${fromName}) could not be promoted to a blob store group", null)
  }

}
