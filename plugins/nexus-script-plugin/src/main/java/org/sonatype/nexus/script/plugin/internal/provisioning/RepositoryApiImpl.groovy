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
package org.sonatype.nexus.script.plugin.internal.provisioning

import javax.annotation.Nonnull
import javax.annotation.Nullable
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

import org.sonatype.nexus.blobstore.api.BlobStore
import org.sonatype.nexus.blobstore.api.BlobStoreManager
import org.sonatype.nexus.repository.Repository
import org.sonatype.nexus.repository.config.Configuration
import org.sonatype.nexus.repository.manager.RepositoryManager
import org.sonatype.nexus.repository.maven.LayoutPolicy
import org.sonatype.nexus.repository.maven.VersionPolicy
import org.sonatype.nexus.repository.storage.WritePolicy
import org.sonatype.nexus.script.plugin.RepositoryApi

import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic

import static com.google.common.base.Preconditions.checkArgument
import static com.google.common.base.Preconditions.checkNotNull

/**
 * @since 3.0
 */
@Named
@Singleton
@CompileStatic
class RepositoryApiImpl
    implements RepositoryApi
{
  @Inject
  RepositoryManager repositoryManager
  
  @Inject 
  BlobStoreManager blobStoreManager

  /**
   * Create a hosted configuration for the given recipeName.
   */
  @Nonnull
  Configuration createHosted(final String name,
                             final String recipeName,
                             final String blobStoreName = BlobStoreManager.DEFAULT_BLOBSTORE_NAME,
                             final WritePolicy writePolicy = WritePolicy.ALLOW,
                             final boolean strictContentTypeValidation = true)
  {
    checkNotNull(name)
    checkArgument(recipeName && recipeName.endsWith('-hosted'))

    new Configuration(
        repositoryName: name,
        recipeName: recipeName,
        online: true,
        attributes: [
            storage: [
                blobStoreName              : blobStoreName,
                writePolicy                : writePolicy,
                strictContentTypeValidation: strictContentTypeValidation
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
                            final String blobStoreName = BlobStoreManager.DEFAULT_BLOBSTORE_NAME,
                            final boolean strictContentTypeValidation = true)
  {
    checkNotNull(name)
    checkArgument(recipeName && recipeName.endsWith('-proxy'))

    new Configuration(
        repositoryName: name,
        recipeName: recipeName,
        online: true,
        attributes: [
            httpclient   : [
                connection: [
                    blocked  : false,
                    autoBlock: true
                ] as Map
            ] as Map,
            proxy: [
                remoteUrl     : remoteUrl,
                contentMaxAge : 1440,
                metadataMaxAge: 1440
            ] as Map,
            negativeCache: [
                enabled   : true,
                timeToLive: 1440
            ] as Map,
            storage      : [
                blobStoreName              : blobStoreName,
                strictContentTypeValidation: strictContentTypeValidation
            ] as Map
        ]
    )
  }

  /**
   * Create a group configuration for the given recipeName.
   */
  @Nonnull
  Configuration createGroup(final String name,
                            final String recipeName,
                            final String blobStoreName = BlobStoreManager.DEFAULT_BLOBSTORE_NAME,
                            final String... members)
  {
    checkNotNull(name)
    checkArgument(recipeName && recipeName.endsWith('-group'))

    new Configuration(
        repositoryName: name,
        recipeName: recipeName,
        online: true,
        attributes: [
            group: [
                memberNames: members.toList().unique()
            ] as Map,
            storage: [
                blobStoreName: blobStoreName
            ] as Map
        ]
    )
  }

  @Nonnull
  Repository createAptProxy(final String name,
                            final String remoteUrl,
                            final String blobStoreName,
                            final String distribution,
                            final boolean strictContentTypeValidation = true)
  {
    def configuration = createProxy(name, 'apt-proxy', remoteUrl, blobStoreName, strictContentTypeValidation)
    configuration.attributes.apt = ['distribution': distribution] as Map
    createRepository(configuration)
  }

  @Nonnull
  Repository createAptHosted(final String name,
                             final String distribution,
                             final String pgpPrivateKey,
                             final String pgpPassPhrase = '',
                             final String blobStoreName = BlobStoreManager.DEFAULT_BLOBSTORE_NAME,
                             final WritePolicy writePolicy = WritePolicy.ALLOW,
                             final boolean strictContentTypeValidation = true)
  {
    def configuration = createHosted(name, 'apt-hosted', blobStoreName, writePolicy, strictContentTypeValidation)
    configuration.attributes.apt = ['distribution': distribution] as Map
    configuration.attributes.aptSigning = ['keypair': pgpPrivateKey, 'passphrase': pgpPassPhrase] as Map
    createRepository(configuration)
  }

  @Nonnull
  Repository createMavenHosted(final String name,
                               final String blobStoreName = BlobStoreManager.DEFAULT_BLOBSTORE_NAME,
                               final boolean strictContentTypeValidation = true,
                               final VersionPolicy versionPolicy = VersionPolicy.RELEASE,
                               final WritePolicy writePolicy = WritePolicy.ALLOW_ONCE,
                               final LayoutPolicy layoutPolicy = LayoutPolicy.STRICT)
  {
    Configuration configuration = createHosted(name, 'maven2-hosted', blobStoreName, writePolicy, strictContentTypeValidation)
    configuration.attributes.maven = configureMaven(versionPolicy, layoutPolicy)
    createRepository(configuration)
  }

  @Nonnull
  Repository createMavenProxy(final String name,
                              final String remoteUrl,
                              final String blobStoreName = BlobStoreManager.DEFAULT_BLOBSTORE_NAME,
                              final boolean strictContentTypeValidation = true,
                              final VersionPolicy versionPolicy = VersionPolicy.RELEASE,
                              final LayoutPolicy layoutPolicy = LayoutPolicy.STRICT)
  {
    Configuration configuration = createProxy(name, 'maven2-proxy', remoteUrl, blobStoreName, strictContentTypeValidation)
    configuration.attributes.maven = configureMaven(versionPolicy, layoutPolicy)
    createRepository(configuration)
  }

  @Nonnull
  Repository createMavenGroup(final String name,
                              final List<String> members,
                              final String blobStoreName = BlobStoreManager.DEFAULT_BLOBSTORE_NAME)
  {
    Configuration configuration = createGroup(name, 'maven2-group', blobStoreName, members as String[])
    configuration.attributes.maven = configureMaven()
    createRepository(configuration)
  }

  @Nonnull
  Repository createNpmHosted(final String name,
                             final String blobStoreName = BlobStoreManager.DEFAULT_BLOBSTORE_NAME,
                             final boolean strictContentTypeValidation = true,
                             final WritePolicy writePolicy = WritePolicy.ALLOW)
  {
    createRepository(createHosted(name, 'npm-hosted', blobStoreName, writePolicy, strictContentTypeValidation))
  }

  @Nonnull
  Repository createNpmProxy(final String name,
                            final String remoteUrl,
                            final String blobStoreName = BlobStoreManager.DEFAULT_BLOBSTORE_NAME,
                            final boolean strictContentTypeValidation = true)
  {
    createRepository(createProxy(name, 'npm-proxy', remoteUrl, blobStoreName, strictContentTypeValidation))
  }

  @Nonnull
  Repository createNpmGroup(final String name,
                            final List<String> members,
                            final String blobStoreName = BlobStoreManager.DEFAULT_BLOBSTORE_NAME)
  {
    createRepository(createGroup(name, 'npm-group', blobStoreName, members as String[]))
  }

  @Nonnull
  Repository createNugetHosted(final String name,
                               final String blobStoreName = BlobStoreManager.DEFAULT_BLOBSTORE_NAME,
                               final boolean strictContentTypeValidation = true,
                               final WritePolicy writePolicy = WritePolicy.ALLOW) {
    createRepository(createHosted(name, 'nuget-hosted', blobStoreName, writePolicy, strictContentTypeValidation))
  }

  @Nonnull
  Repository createNugetProxy(final String name,
                              final String remoteUrl,
                              final String blobStoreName = BlobStoreManager.DEFAULT_BLOBSTORE_NAME,
                              final boolean strictContentTypeValidation = true)

  {
    createRepository(createProxy(name, 'nuget-proxy', remoteUrl, blobStoreName, strictContentTypeValidation))
  }

  @Nonnull
  Repository createNugetGroup(final String name,
                              final List<String> members,
                              final String blobStoreName = BlobStoreManager.DEFAULT_BLOBSTORE_NAME)
  {
    createRepository(createGroup(name, 'nuget-group', blobStoreName, members as String[]))
  }

  @Nonnull
  Repository createRawHosted(final String name,
                             final String blobStoreName = BlobStoreManager.DEFAULT_BLOBSTORE_NAME,
                             final boolean strictContentTypeValidation = false,
                             final WritePolicy writePolicy = WritePolicy.ALLOW)
  {
    createRepository(createHosted(name, 'raw-hosted', blobStoreName, writePolicy, strictContentTypeValidation))
  }

  @Nonnull
  Repository createRawProxy(final String name,
                            final String remoteUrl,
                            final String blobStoreName = BlobStoreManager.DEFAULT_BLOBSTORE_NAME,
                            final boolean strictContentTypeValidation = true)
  {
    createRepository(createProxy(name, 'raw-proxy', remoteUrl, blobStoreName, strictContentTypeValidation))
  }

  @Nonnull
  Repository createRawGroup(final String name,
                            final List<String> members,
                            final String blobStoreName = BlobStoreManager.DEFAULT_BLOBSTORE_NAME)
  {
    createRepository(createGroup(name, 'raw-group', blobStoreName, members as String[]))
  }

  @Nonnull
  Repository createDockerHosted(final String name,
                                @Nullable Integer httpPort,
                                @Nullable Integer httpsPort,
                                final String blobStoreName = BlobStoreManager.DEFAULT_BLOBSTORE_NAME,
                                final boolean strictContentTypeValidation = true,
                                final boolean v1Enabled = true,
                                final WritePolicy writePolicy = WritePolicy.ALLOW,
                                final boolean forceBasicAuth = true
  )
  {
    Configuration configuration = createHosted(name, 'docker-hosted', blobStoreName, writePolicy, strictContentTypeValidation)
    configuration.attributes.docker = configureDockerAttributes(httpPort, httpsPort, v1Enabled, forceBasicAuth)
    createRepository(configuration)
  }

  @Nonnull
  @CompileDynamic
  Repository createDockerProxy(final String name,
                               final String remoteUrl,
                               final String indexType,
                               @Nullable final String indexUrl,
                               @Nullable Integer httpPort,
                               @Nullable Integer httpsPort,
                               final String blobStoreName = BlobStoreManager.DEFAULT_BLOBSTORE_NAME,
                               final boolean strictContentTypeValidation = true,
                               final boolean v1Enabled = true,
                               final boolean forceBasicAuth = true)
  {
    Configuration configuration = createProxy(name, 'docker-proxy', remoteUrl, blobStoreName, strictContentTypeValidation)
    configuration.attributes.docker = configureDockerAttributes(httpPort, httpsPort, v1Enabled, forceBasicAuth)
    configuration.attributes.dockerProxy = [
        indexType: indexType,
        indexUrl : indexUrl
    ]
    configuration.attributes.httpclient.connection.useTrustStore = true
    createRepository(configuration)
  }

  @Nonnull
  Repository createDockerGroup(final String name,
                               @Nullable Integer httpPort,
                               @Nullable Integer httpsPort,
                               final List<String> members,
                               final boolean v1Enabled = true,
                               final String blobStoreName = BlobStoreManager.DEFAULT_BLOBSTORE_NAME,
                               final boolean forceBasicAuth = true)
  {
    Configuration configuration = createGroup(name, 'docker-group', blobStoreName, members as String[])
    configuration.attributes.docker = configureDockerAttributes(httpPort, httpsPort, v1Enabled, forceBasicAuth)
    createRepository(configuration)
  }

  @Nonnull
  Repository createBowerHosted(final String name,
                               final String blobStoreName = BlobStoreManager.DEFAULT_BLOBSTORE_NAME,
                               final boolean strictContentTypeValidation = true,
                               final WritePolicy writePolicy = WritePolicy.ALLOW)
  {
    createRepository(createHosted(name, 'bower-hosted', blobStoreName, writePolicy, strictContentTypeValidation))
  }

  @Nonnull
  Repository createBowerProxy(final String name,
                              final String remoteUrl,
                              final String blobStoreName = BlobStoreManager.DEFAULT_BLOBSTORE_NAME,
                              final boolean strictContentTypeValidation = true,
                              final boolean rewritePackageUrls = true)
  {
    def configuration = createProxy(name, 'bower-proxy', remoteUrl, blobStoreName, strictContentTypeValidation)
    configuration.attributes.bower = ['rewritePackageUrls': rewritePackageUrls] as Map
    createRepository(configuration)
  }

  @Nonnull
  Repository createBowerGroup(final String name,
                              final List<String> members,
                              final String blobStoreName = BlobStoreManager.DEFAULT_BLOBSTORE_NAME)
  {
    createRepository(createGroup(name, 'bower-group', blobStoreName, members as String[]))
  }

  @Nonnull
  Repository createPyPiHosted(final String name, final String blobStoreName = BlobStoreManager.DEFAULT_BLOBSTORE_NAME,
                              final boolean strictContentTypeValidation = true,
                              final WritePolicy writePolicy = WritePolicy.ALLOW)
  {
    createRepository(createHosted(name, 'pypi-hosted', blobStoreName, writePolicy, strictContentTypeValidation))
  }

  @Nonnull
  Repository createPyPiProxy(final String name, final String remoteUrl,
                             final String blobStoreName = BlobStoreManager.DEFAULT_BLOBSTORE_NAME,
                             final boolean strictContentTypeValidation = true)
  {
    createRepository(createProxy(name, 'pypi-proxy', remoteUrl, blobStoreName, strictContentTypeValidation))
  }

  @Nonnull
  Repository createPyPiGroup(final String name, final List<String> members,
                             final String blobStoreName = BlobStoreManager.DEFAULT_BLOBSTORE_NAME)
  {
    createRepository(createGroup(name, 'pypi-group', blobStoreName, members as String[]))
  }

  @Nonnull
  Repository createRubygemsHosted(final String name,
                                  final String blobStoreName = BlobStoreManager.DEFAULT_BLOBSTORE_NAME,
                                  final boolean strictContentTypeValidation = true,
                                  final WritePolicy writePolicy = WritePolicy.ALLOW)
  {
    createRepository(createHosted(name, 'rubygems-hosted', blobStoreName, writePolicy, strictContentTypeValidation))
  }

  @Nonnull
  Repository createRubygemsProxy(final String name,
                                 final String remoteUrl,
                                 final String blobStoreName = BlobStoreManager.DEFAULT_BLOBSTORE_NAME,
                                 final boolean strictContentTypeValidation = true)
  {
    createRepository(createProxy(name, 'rubygems-proxy', remoteUrl, blobStoreName, strictContentTypeValidation))
  }

  @Nonnull
  Repository createRubygemsGroup(final String name,
                                 final List<String> members,
                                 final String blobStoreName = BlobStoreManager.DEFAULT_BLOBSTORE_NAME)
  {
    createRepository(createGroup(name, 'rubygems-group', blobStoreName, members as String[]))
  }

  @Nonnull
  Repository createYumHosted(final String name,
                             final String blobStoreName = BlobStoreManager.DEFAULT_BLOBSTORE_NAME,
                             final boolean strictContentTypeValidation = true,
                             final WritePolicy writePolicy = WritePolicy.ALLOW,
                             final int depth = 0)
  {
    def configuration = createHosted(name, 'yum-hosted', blobStoreName, writePolicy, strictContentTypeValidation)
    configuration.attributes.yum = ['repodataDepth': depth] as Map
    createRepository(configuration)
  }

  @Nonnull
  Repository createYumProxy(final String name,
                            final String remoteUrl,
                            final String blobStoreName = BlobStoreManager.DEFAULT_BLOBSTORE_NAME,
                            final boolean strictContentTypeValidation = true)
  {
    createRepository(createProxy(name, 'yum-proxy', remoteUrl, blobStoreName, strictContentTypeValidation))
  }

  @Nonnull
  Repository createYumGroup(final String name,
                            final List<String> members,
                            final String blobStoreName = BlobStoreManager.DEFAULT_BLOBSTORE_NAME)
  {
    createRepository(createGroup(name, 'yum-group', blobStoreName, members as String[]))
  }

  @Nonnull
  Repository createGolangHosted(final String name, final String blobStoreName = BlobStoreManager.DEFAULT_BLOBSTORE_NAME,
                                final boolean strictContentTypeValidation = true,
                                final WritePolicy writePolicy = WritePolicy.ALLOW)
  {
    createRepository(createHosted(name, 'go-hosted', blobStoreName, writePolicy, strictContentTypeValidation))
  }

  @Nonnull
  Repository createGolangProxy(final String name, final String remoteUrl,
                               final String blobStoreName = BlobStoreManager.DEFAULT_BLOBSTORE_NAME,
                               final boolean strictContentTypeValidation = true)
  {
    createRepository(createProxy(name, 'go-proxy', remoteUrl, blobStoreName, strictContentTypeValidation))
  }

  @Nonnull
  Repository createGolangGroup(final String name, final List<String> members,
                               final String blobStoreName = BlobStoreManager.DEFAULT_BLOBSTORE_NAME)
  {
    createRepository(createGroup(name, 'go-group', blobStoreName, members as String[]))
  }

  @Nonnull
  Repository createGitLfsHosted(final String name,
                                final String blobStoreName = BlobStoreManager.DEFAULT_BLOBSTORE_NAME,
                                final boolean strictContentTypeValidation = true,
                                final WritePolicy writePolicy = WritePolicy.ALLOW)
  {
    createRepository(createHosted(name, 'gitlfs-hosted', blobStoreName, writePolicy, strictContentTypeValidation))
  }

  private static Map configureMaven(final VersionPolicy versionPolicy = VersionPolicy.MIXED,
                                    final LayoutPolicy layoutPolicy = LayoutPolicy.STRICT)
  {
    [versionPolicy: versionPolicy, layoutPolicy: layoutPolicy]
  }

  private static Map configureDockerAttributes(Integer httpPort, Integer httpsPort, boolean v1Enabled, boolean forceBasicAuth) {
    def docker = [:]
    if (httpPort) {
      docker.httpPort = httpPort
    }
    if (httpsPort) {
      docker.httpsPort = httpsPort
    }
    docker.v1Enabled = v1Enabled
    docker.forceBasicAuth = forceBasicAuth
    return docker
  }

  Repository createRepository(final Configuration configuration) {
    validateBlobStore(configuration)
    validateGroupMembers(configuration)
    
    return repositoryManager.create(configuration)
  }

  @CompileDynamic
  void validateGroupMembers(final Configuration configuration) {
    Collection members = configuration.attributes.group?.memberNames
    if (members) {
      def existingRepos = repositoryManager.browse().collect { Repository repository -> repository.name }
      boolean valid = members.every { String memberName ->
        existingRepos.contains(memberName)
      }
      if(!valid) {
        throw new IllegalStateException('One or more of the specified group memberNames does not actually exist')
      }
    }
  }

  @CompileDynamic
  void validateBlobStore(Configuration configuration) {
    def name = configuration.attributes.storage?.blobStoreName
    if (!blobStoreManager.browse().any { BlobStore blobStore ->
      blobStore.blobStoreConfiguration.name == name
    }) {
      throw new IllegalArgumentException("No blobStore found with name $name")
    }
  }
}
