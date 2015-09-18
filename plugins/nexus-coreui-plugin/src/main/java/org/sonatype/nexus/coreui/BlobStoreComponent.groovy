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
import javax.inject.Provider
import javax.inject.Singleton
import javax.validation.Valid
import javax.validation.constraints.NotNull
import javax.validation.groups.Default

import org.sonatype.nexus.blobstore.api.BlobStore
import org.sonatype.nexus.blobstore.api.BlobStoreConfiguration
import org.sonatype.nexus.blobstore.api.BlobStoreManager
import org.sonatype.nexus.extdirect.DirectComponentSupport
import org.sonatype.nexus.validation.Validate
import org.sonatype.nexus.validation.group.Create

import com.softwarementors.extjs.djn.config.annotations.DirectAction
import com.softwarementors.extjs.djn.config.annotations.DirectMethod
import org.apache.shiro.authz.annotation.RequiresAuthentication
import org.hibernate.validator.constraints.NotEmpty

/**
 * BlobStore {@link DirectComponent}
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
  AttributeConverter attributeConverter

  @Inject
  Map<String, Provider<BlobStore>> blobstorePrototypes;

  @DirectMethod
  List<BlobStoreXO> read() {
    blobStoreManager.browse().collect { asBlobStore(it) }
  }

  @DirectMethod
  List<ReferenceXO> readTypes() {
    blobstorePrototypes.collect { key, provider ->
      new ReferenceXO(id: key, name: key) 
    }
  }

  @DirectMethod
  @RequiresAuthentication
  @Validate(groups = [Create.class, Default.class])
  BlobStoreXO create(final @NotNull @Valid BlobStoreXO blobStore) {
    return asBlobStore(blobStoreManager.create(
        new BlobStoreConfiguration(name: blobStore.name, type: blobStore.type,
            attributes: attributeConverter.asAttributes(blobStore.attributes))))
  }

  @DirectMethod
  @RequiresAuthentication
  @Validate
  void remove(final @NotEmpty String name) {
    blobStoreManager.delete(name)
  }

  BlobStoreXO asBlobStore(final BlobStore blobStore) {
    return new BlobStoreXO(name: blobStore.blobStoreConfiguration.name,
        type: blobStore.blobStoreConfiguration.type,
        attributes: attributeConverter.asAttributes(blobStore.blobStoreConfiguration.attributes),
        blobCount: blobStore.metrics.blobCount,
        totalSize: blobStore.metrics.totalSize,
        availableSpace: blobStore.metrics.availableSpace
    )
  }
}
