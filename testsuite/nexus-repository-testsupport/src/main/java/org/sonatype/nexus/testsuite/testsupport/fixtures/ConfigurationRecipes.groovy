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
package org.sonatype.nexus.testsuite.testsupport.fixtures

import javax.annotation.Nonnull
import javax.inject.Provider

import org.sonatype.nexus.blobstore.api.BlobStoreManager
import org.sonatype.nexus.repository.config.Configuration
import org.sonatype.nexus.repository.manager.RepositoryManager

import groovy.transform.CompileStatic

import static com.google.common.base.Preconditions.checkArgument
import static com.google.common.base.Preconditions.checkNotNull

/**
 * Common Repository configuration aspects and constants.
 */
@CompileStatic
trait ConfigurationRecipes
{
  abstract Provider<RepositoryManager> getRepositoryManagerProvider()

  /**
   * Create a hosted configuration for the given recipeName.
   */
  @Nonnull
  Configuration createHosted(final String name,
                             final String recipeName,
                             final String writePolicy = "ALLOW",
                             final boolean strictContentTypeValidation = true,
                             final String blobStoreName = BlobStoreManager.DEFAULT_BLOBSTORE_NAME,
                             final boolean latestPolicy = false)
  {
    checkNotNull(name)
    checkArgument(recipeName && recipeName.endsWith('-hosted'))

    newConfiguration(
        repositoryName: name,
        recipeName: recipeName,
        online: true,
        attributes: [
            storage: [
                blobStoreName: blobStoreName,
                writePolicy  : writePolicy,
                latestPolicy  : latestPolicy,
                strictContentTypeValidation: strictContentTypeValidation,
                dataStoreName: 'nexus'
            ] as Map
        ] as Map
    )
  }

  /**
   * Create a proxy configuration for the given recipeName.
   */
  @Nonnull
  Configuration createProxy(final String name,
                            final String recipeName,
                            final String remoteUrl,
                            final boolean strictContentTypeValidation = true,
                            final String blobStoreName = BlobStoreManager.DEFAULT_BLOBSTORE_NAME,
                            final Map<String, Object> authentication = [:],
                            final String conanVersion = "V1")
  {
    checkNotNull(name)
    checkArgument(recipeName && recipeName.endsWith('-proxy'))

    def attributes = [
        httpclient   : [
            connection: [
                blocked  : false,
                autoBlock: true
            ] as Map<String, Object>
        ] as Map<String, Object>,
        proxy        : [
            remoteUrl     : remoteUrl,
            contentMaxAge : 1440,
            metadataMaxAge: 1440
        ] as Map<String, Object>,
        negativeCache: [
            enabled   : true,
            timeToLive: 1440
        ] as Map<String, Object>,
        conan        : [
            conanVersion: conanVersion
        ] as Map<String, Object>,
        storage      : [
            blobStoreName              : blobStoreName,
            strictContentTypeValidation: strictContentTypeValidation,
            dataStoreName: 'nexus'
        ] as Map<String, Object>
    ]
    if (!authentication.isEmpty()) {
      attributes.httpclient.authentication = authentication
    }

    newConfiguration(
        repositoryName: name,
        recipeName: recipeName,
        online: true,
        attributes: attributes
    )
  }

  /**
   * Create a group configuration for the given recipeName.
   */
  @Nonnull
  Configuration createGroup(final String name,
                            final String recipeName,
                            final String... members)
  {
    createGroup(name, recipeName, 'None', members)
  }

  /**
   * Create a group configuration for the given recipeName.
   */
  @Nonnull
  Configuration createGroup(final String name,
                            final String recipeName,
                            final String groupWriteMember,
                            final String... members)
  {
    checkNotNull(name)
    checkArgument(recipeName && recipeName.endsWith('-group'))

    newConfiguration(
        repositoryName: name,
        recipeName: recipeName,
        online: true,
        attributes: [
            group  : [
                groupWriteMember: groupWriteMember,
                memberNames: members.toList()
            ] as Map<String, Object>,
            storage: [
                blobStoreName: BlobStoreManager.DEFAULT_BLOBSTORE_NAME,
                strictContentTypeValidation: true,
                dataStoreName: 'nexus'
            ] as Map<String, Object>
        ]
    )
  }

  Configuration newConfiguration(final Map map) {
    Configuration config = repositoryManagerProvider.get().newConfiguration()
    config.repositoryName = map.repositoryName
    config.recipeName = map.recipeName
    config.online = map.online
    config.attributes = map.attributes as Map

    return config
  }
}
