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
package org.sonatype.nexus.repository.maven.internal.orient


import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

import org.sonatype.nexus.blobstore.api.BlobStoreManager
import org.sonatype.nexus.repository.config.Configuration
import org.sonatype.nexus.repository.manager.DefaultRepositoriesContributor
import org.sonatype.nexus.repository.manager.RepositoryManager
import org.sonatype.nexus.repository.maven.LayoutPolicy
import org.sonatype.nexus.repository.maven.VersionPolicy
import org.sonatype.nexus.repository.maven.internal.recipes.Maven2GroupRecipe
import org.sonatype.nexus.repository.maven.internal.recipes.Maven2HostedRecipe
import org.sonatype.nexus.repository.maven.internal.recipes.Maven2ProxyRecipe
import org.sonatype.nexus.repository.storage.WritePolicy

/**
 * Provide default hosted and proxy repositories for Maven.
 *
 * @since 3.0
 */
@Named
@Singleton
class OrientMavenDefaultRepositoriesContributor
    implements DefaultRepositoriesContributor
{
  static final String DEFAULT_RELEASE_REPO = 'maven-releases'

  static final String DEFAULT_SNAPSHOT_REPO = 'maven-snapshots'

  static final String DEFAULT_CENTRAL_REPO = 'maven-central'

  static final String DEFAULT_PUBLIC_REPO = 'maven-public'

  @Inject
  RepositoryManager repositoryManager

  @Override
  List<Configuration> getRepositoryConfigurations() {
    return [
        newConfiguration(repositoryName: DEFAULT_RELEASE_REPO, recipeName: Maven2HostedRecipe.NAME, online: true, attributes:
            [
                maven  : [
                    versionPolicy: VersionPolicy.RELEASE.toString(),
                    layoutPolicy : LayoutPolicy.STRICT.toString()
                ],
                storage: [
                    blobStoreName: BlobStoreManager.DEFAULT_BLOBSTORE_NAME,
                    writePolicy: WritePolicy.ALLOW_ONCE.toString(),
                    strictContentTypeValidation: false
                ]
            ]
        ),
        newConfiguration(repositoryName: DEFAULT_SNAPSHOT_REPO, recipeName: Maven2HostedRecipe.NAME, online: true, attributes:
            [
                maven  : [
                    versionPolicy: VersionPolicy.SNAPSHOT.toString(),
                    layoutPolicy : LayoutPolicy.STRICT.toString()
                ],
                storage: [
                    blobStoreName: BlobStoreManager.DEFAULT_BLOBSTORE_NAME,
                    writePolicy: WritePolicy.ALLOW.toString(),
                    strictContentTypeValidation: false
                ]
            ]
        ),
        newConfiguration(repositoryName: DEFAULT_CENTRAL_REPO, recipeName: Maven2ProxyRecipe.NAME, online: true,
            attributes: [
                httpclient   : [
                    connection: [
                        blocked  : false,
                        autoBlock: true
                    ]
                ],
                maven        : [
                    versionPolicy: VersionPolicy.RELEASE.toString(),
                    layoutPolicy : LayoutPolicy.PERMISSIVE.toString()
                ],
                proxy: [
                    remoteUrl     : 'https://repo1.maven.org/maven2/',
                    contentMaxAge : -1,
                    metadataMaxAge: 1440
                ],
                negativeCache: [
                    enabled   : true,
                    timeToLive: 1440
                ],
                storage      : [
                    blobStoreName: BlobStoreManager.DEFAULT_BLOBSTORE_NAME,
                    strictContentTypeValidation: false
                ]
            ]
        ),
        newConfiguration(repositoryName: DEFAULT_PUBLIC_REPO, recipeName: Maven2GroupRecipe.NAME, online: true, attributes:
            [
                maven  : [
                    versionPolicy: VersionPolicy.MIXED.toString()
                ],
                group  : [
                    memberNames: [DEFAULT_RELEASE_REPO, DEFAULT_SNAPSHOT_REPO, DEFAULT_CENTRAL_REPO]
                ],
                storage: [
                    blobStoreName: BlobStoreManager.DEFAULT_BLOBSTORE_NAME
                ]
            ]
        )
    ]
  }

  private Configuration newConfiguration(final Map map) {
    Configuration config = repositoryManager.newConfiguration()
    config.with {
      repositoryName = map.repositoryName
      recipeName = map.recipeName
      online = map.online
      attributes = map.attributes as Map
    }
    return config
  }
}
