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

import org.sonatype.nexus.repository.Repository
import org.sonatype.nexus.repository.config.Configuration

import groovy.transform.CompileStatic

import static org.sonatype.nexus.blobstore.api.BlobStoreManager.DEFAULT_BLOBSTORE_NAME
import static org.sonatype.nexus.repository.storage.StorageFacetConstants.BLOB_STORE_NAME
import static org.sonatype.nexus.repository.storage.StorageFacetConstants.STORAGE

/**
 * Factory for Npm {@link Repository} {@link Configuration}
 */
@CompileStatic
trait NpmRepoRecipes
    extends ConfigurationRecipes
{
  @Nonnull
  Repository createNpmHosted(final String repoName,
                             final String writePolicy = "ALLOW",
                             final String blobStoreName = null)
  {
    Configuration configuration = createHosted(repoName, 'npm-hosted', writePolicy)
    if (blobStoreName != null) {
      configuration.attributes(STORAGE).set(BLOB_STORE_NAME, blobStoreName)
    }
    return createRepository(configuration)
  }

  @Nonnull
  Repository createNpmProxy(final String name,
                            final String remoteUrl,
                            final Map<String, Object> authentication = [:]) {
    createRepository(createProxy(name, 'npm-proxy', remoteUrl, true, DEFAULT_BLOBSTORE_NAME, authentication))
  }

  @Nonnull
  Repository createNpmGroup(final String name,
                            final String... members)
  {
    createRepository(createGroup(name, 'npm-group', members))
  }

  abstract Repository createRepository(final Configuration configuration)

}

