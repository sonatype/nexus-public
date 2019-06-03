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
package org.sonatype.nexus.script.plugin;


import java.util.List;

import org.sonatype.nexus.common.script.ScriptApi;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.config.Configuration;
import org.sonatype.nexus.repository.maven.LayoutPolicy;
import org.sonatype.nexus.repository.maven.VersionPolicy;
import org.sonatype.nexus.repository.storage.WritePolicy;

/**
 * Repository provisioning capabilities of the repository manager.
 * 
 * @since 3.0
 */
public interface RepositoryApi
    extends ScriptApi
{
  default String getName() {
    return "repository";
  }

  /**
   * Create an Apt proxy repository.
   * @param name The name of the new Repository
   * @param remoteUrl The url of the external proxy for this Repository
   * @param blobStoreName The BlobStore the Repository should use
   * @param distribution The name of the required distribution
   * @param strictContentTypeValidation Whether or not the Repository should enforce strict content types
   * @return the newly created Repository
   */
  Repository createAptProxy(final String name,
                            final String remoteUrl,
                            final String blobStoreName,
                            final String distribution,
                            final boolean strictContentTypeValidation);

  /**
   * Create an Apt hosted repository.
   * @param name The name of the new Repository
   * @param distribution The name of the required distribution
   * @param pgpPrivateKey GPG private key
   * @param blobStoreName The BlobStore the Repository should use
   * @param writePolicy The {@link WritePolicy} for the Repository
   * @param strictContentTypeValidation Whether or not the Repository should enforce strict content types
   * @return the newly created Repository
   */
  Repository createAptHosted(final String name,
                             final String distribution,
                             final String pgpPrivateKey,
                             final String pgpPassPhrase,
                             final String blobStoreName,
                             final WritePolicy writePolicy,
                             final boolean strictContentTypeValidation);

  /**
   * Create a Maven hosted repository.
   * @param name The name of the new Repository
   * @param blobStoreName The BlobStore the Repository should use
   * @param strictContentTypeValidation Whether or not the Repository should enforce strict content types
   * @param versionPolicy The {@link VersionPolicy} for the Repository
   * @param writePolicy The {@link WritePolicy} for the Repository
   * @param layoutPolicy The {@link LayoutPolicy} for the Repository
   * @return the newly created Repository
   */
  Repository createMavenHosted(final String name,
                               final String blobStoreName,
                               final boolean strictContentTypeValidation,
                               final VersionPolicy versionPolicy,
                               final WritePolicy writePolicy,
                               final LayoutPolicy layoutPolicy);


  /**
   * Create a Maven proxy repository.
   * @param name The name of the new Repository
   * @param remoteUrl The url of the external proxy for this Repository
   * @param blobStoreName The BlobStore the Repository should use
   * @param strictContentTypeValidation Whether or not the Repository should enforce strict content types
   * @param versionPolicy The {@link VersionPolicy} for the Repository
   * @param layoutPolicy The {@link LayoutPolicy} for the Repository
   * @return the newly created Repository
   */
  Repository createMavenProxy(final String name,
                              final String remoteUrl,
                              final String blobStoreName,
                              final boolean strictContentTypeValidation,
                              final VersionPolicy versionPolicy,
                              final LayoutPolicy layoutPolicy);

  /**
   * Create a Maven group repository.
   * @param name The name of the new Repository
   * @param blobStoreName The BlobStore the Repository should use
   * @param members The names of the Repositories in the group
   * @return the newly created Repository
   */
  Repository createMavenGroup(final String name,
                              final List<String> members,
                              final String blobStoreName);

  /**
   * Create an Npm hosted repository.
   * @param name The name of the new Repository
   * @param blobStoreName The BlobStore the Repository should use
   * @param strictContentTypeValidation Whether or not the Repository should enforce strict content types
   * @param writePolicy The {@link WritePolicy} for the Repository
   * @return the newly created Repository
   */
  Repository createNpmHosted(final String name,
                             final String blobStoreName,
                             final boolean strictContentTypeValidation,
                             final WritePolicy writePolicy);

  /**
   * Create an Npm proxy repository.
   * @param name The name of the new Repository
   * @param remoteUrl The url of the external proxy for this Repository
   * @param blobStoreName The BlobStore the Repository should use
   * @param strictContentTypeValidation Whether or not the Repository should enforce strict content types
   * @return the newly created Repository
   */
  Repository createNpmProxy(final String name,
                            final String remoteUrl,
                            final String blobStoreName,
                            final boolean strictContentTypeValidation);

  /**
   * Create an Npm group repository.
   * @param name The name of the new Repository
   * @param blobStoreName The BlobStore the Repository should use
   * @param members The names of the Repositories in the group
   * @return the newly created Repository
   */
  Repository createNpmGroup(final String name,
                            final List<String> members,
                            final String blobStoreName);

  /**
   * Create a Nuget hosted repository.
   * @param name The name of the new Repository
   * @param blobStoreName The BlobStore the Repository should use
   * @param strictContentTypeValidation Whether or not the Repository should enforce strict content types
   * @param writePolicy The {@link WritePolicy} for the Repository
   * @return the newly created Repository
   */
  Repository createNugetHosted(final String name,
                               final String blobStoreName,
                               final boolean strictContentTypeValidation,
                               final WritePolicy writePolicy);


  /**
   * Create a Nuget proxy repository.
   * @param name The name of the new Repository
   * @param remoteUrl The url of the external proxy for this Repository
   * @param blobStoreName The BlobStore the Repository should use
   * @param strictContentTypeValidation Whether or not the Repository should enforce strict content types
   * @return the newly created Repository
   */
  Repository createNugetProxy(final String name,
                              final String remoteUrl,
                              final String blobStoreName,
                              final boolean strictContentTypeValidation);

  /**
   * Create a Nuget group repository.
   * @param name The name of the new Repository
   * @param blobStoreName The BlobStore the Repository should use
   * @param members The names of the Repositories in the group
   * @return the newly created Repository
   */
  Repository createNugetGroup(final String name,
                              final List<String> members,
                              final String blobStoreName);

  /**
   * Create a Raw hosted repository.
   * @param name The name of the new Repository
   * @param blobStoreName The BlobStore the Repository should use
   * @param strictContentTypeValidation Whether or not the Repository should enforce strict content types
   * @param writePolicy The {@link WritePolicy} for the Repository
   * @return the newly created Repository
   */
  Repository createRawHosted(final String name,
                             final String blobStoreName,
                             final boolean strictContentTypeValidation,
                             final WritePolicy writePolicy);


  /**
   * Create a Raw proxy repository.
   * @param name The name of the new Repository
   * @param remoteUrl The url of the external proxy for this Repository
   * @param blobStoreName The BlobStore the Repository should use
   * @param strictContentTypeValidation Whether or not the Repository should enforce strict content types
   * @return
   */
  Repository createRawProxy(final String name,
                            final String remoteUrl,
                            final String blobStoreName,
                            final boolean strictContentTypeValidation);

  /**
   * Create a Raw group repository.
   * @param name The name of the new Repository
   * @param blobStoreName The BlobStore the Repository should use
   * @param members The names of the Repositories in the group
   * @return the newly created Repository
   */
  Repository createRawGroup(final String name,
                            final List<String> members,
                            final String blobStoreName);


  /**
   * Create a Docker hosted repository.
   * @param name The name of the new Repository
   * @param httpPort The http port to accept traffic for this Repository on (optional)
   * @param httpsPort The https port to accept traffic for this Repository on (optional)
   * @param blobStoreName The BlobStore the Repository should use
   * @param v1Enabled Whether or not this Repository supports Docker V1 format
   * @param strictContentTypeValidation Whether or not the Repository should enforce strict content types
   * @param writePolicy The {@link WritePolicy} for the Repository
   * @return the newly created Repository
   */
  Repository createDockerHosted(final String name,
                                Integer httpPort,
                                Integer httpsPort,
                                final String blobStoreName,
                                final boolean v1Enabled,
                                final boolean strictContentTypeValidation,
                                final WritePolicy writePolicy);

  /**
   * Create a Docker hosted repository.
   *
   * @since 3.14
   * @param name The name of the new Repository
   * @param httpPort The http port to accept traffic for this Repository on (optional)
   * @param httpsPort The https port to accept traffic for this Repository on (optional)
   * @param blobStoreName The BlobStore the Repository should use
   * @param v1Enabled Whether or not this Repository supports Docker V1 format
   * @param strictContentTypeValidation Whether or not the Repository should enforce strict content types
   * @param writePolicy The {@link WritePolicy} for the Repository
   * @param forceBasicAuth whether to force basic auth. False is required to enable token auth which can be used for anonymous access
   * @return the newly created Repository
   */
  Repository createDockerHosted(final String name, //NOSONAR
                                Integer httpPort,
                                Integer httpsPort,
                                final String blobStoreName,
                                final boolean v1Enabled,
                                final boolean strictContentTypeValidation,
                                final WritePolicy writePolicy,
                                final boolean forceBasicAuth);

  /**
   * Create a Docker proxy repository.
   *
   * @param name                        The name of the new Repository
   * @param remoteUrl                   The url of the external proxy for this Repository
   * @param indexType                   Use 'REGISTRY' to use the proxy url for the index as well. Use 'HUB' to use the
   *                                    index from DockerHub. Use 'CUSTOM' in conjunction with the 'indexUrl' param to
   *                                    specify a custom index location
   * @param indexUrl                    The url of a 'CUSTOM' index; only used in conjunction with the 'indexType'
   *                                    parameter
   * @param httpPort                    The http port to accept traffic for this Repository on (optional)
   * @param httpsPort                   The https port to accept traffic for this Repository on (optional)
   * @param blobStoreName               The BlobStore the Repository should use
   * @param strictContentTypeValidation Whether or not the Repository should enforce strict content types
   * @param v1Enabled                   Whether or not this Repository supports Docker V1 format
   * @return the newly created Repository
   */
  Repository createDockerProxy(final String name, //NOSONAR
                               final String remoteUrl,
                               final String indexType,
                               final String indexUrl,
                               Integer httpPort,
                               Integer httpsPort,
                               final String blobStoreName,
                               final boolean strictContentTypeValidation,
                               final boolean v1Enabled);

  /**
   * Create a Docker group repository.
   * @param name The name of the new Repository
   * @param httpPort The http port to accept traffic for this Repository on (optional)
   * @param httpsPort The https port to accept traffic for this Repository on (optional)
   * @param v1Enabled Whether or not this Repository supports Docker V1 format
   * @param blobStoreName The BlobStore the Repository should use
   * @param members The names of the Repositories in the group
   * @return the newly created Repository
   */
  Repository createDockerGroup(final String name,
                               Integer httpPort,
                               Integer httpsPort,
                               final List<String> members,
                               final boolean v1Enabled,
                               final String blobStoreName);

  /**
   * Create a Bower hosted repository.
   * @param name
   * @param blobStoreName The BlobStore the Repository should use
   * @param strictContentTypeValidation Whether or not the Repository should enforce strict content types
   * @param writePolicy The {@link WritePolicy} for the Repository
   * @return the newly created Repository
   */
  Repository createBowerHosted(final String name,
                               final String blobStoreName,
                               final boolean strictContentTypeValidation,
                               final WritePolicy writePolicy);

  /**
   * Create a Bower proxy repository.
   * @param name The name of the new Repository
   * @param remoteUrl The url of the external proxy for this Repository
   * @param blobStoreName The BlobStore the Repository should use
   * @param strictContentTypeValidation Whether or not the Repository should enforce strict content types
   * @param rewritePackageUrls
   * @return the newly created Repository
   */
  Repository createBowerProxy(final String name,
                              final String remoteUrl,
                              final String blobStoreName,
                              final boolean strictContentTypeValidation,
                              final boolean rewritePackageUrls);

  /**
   * Create a Bower group repository.
   * @param name The name of the new Repository
   * @param blobStoreName The BlobStore the Repository should use
   * @param members The names of the Repositories in the group
   * @return the newly created Repository
   */
  Repository createBowerGroup(final String name,
                              final List<String> members,
                              final String blobStoreName);

  /**
   * Create a Ruby gems hosted repository.
   *
   * @param blobStoreName               The BlobStore the Repository should use
   * @param strictContentTypeValidation Whether or not the Repository should enforce strict content types
   * @param writePolicy                 The {@link WritePolicy} for the Repository
   * @return the newly created Repository
   */
  Repository createRubygemsHosted(final String name,
                                  final String blobStoreName,
                                  final boolean strictContentTypeValidation,
                                  final WritePolicy writePolicy);

  /**
   * Create a Ruby gems proxy repository.
   *
   * @param name                        The name of the new Repository
   * @param remoteUrl                   The url of the external proxy for this Repository
   * @param blobStoreName               The BlobStore the Repository should use
   * @param strictContentTypeValidation Whether or not the Repository should enforce strict content types
   * @return the newly created Repository
   */
  Repository createRubygemsProxy(final String name,
                                 final String remoteUrl,
                                 final String blobStoreName,
                                 final boolean strictContentTypeValidation);

  /**
   * Create a Rubygems group repository.
   *
   * @param name          The name of the new Repository
   * @param blobStoreName The BlobStore the Repository should use
   * @param members       The names of the Repositories in the group
   * @return the newly created Repository
   */
  Repository createRubygemsGroup(final String name,
                                 final List<String> members,
                                 final String blobStoreName);

  /**
   * Create a PyPi hosted repository.
   * @param name The name of the new Repository
   * @param blobStoreName The BlobStore the Repository should use
   * @param strictContentTypeValidation Whether or not the Repository should enforce strict content types
   * @param writePolicy The {@link WritePolicy} for the Repository
   * @return the newly created Repository
   */
  Repository createPyPiHosted(final String name,
                             final String blobStoreName,
                             final boolean strictContentTypeValidation,
                             final WritePolicy writePolicy);

  /**
   * Create a PyPi proxy repository.
   * @param name The name of the new Repository
   * @param remoteUrl The url of the external proxy for this Repository
   * @param blobStoreName The BlobStore the Repository should use
   * @param strictContentTypeValidation Whether or not the Repository should enforce strict content types
   * @return the newly created Repository
   */
  Repository createPyPiProxy(final String name,
                            final String remoteUrl,
                            final String blobStoreName,
                            final boolean strictContentTypeValidation);

  /**
   * Create a PyPi group repository.
   * @param name The name of the new Repository
   * @param blobStoreName The BlobStore the Repository should use
   * @param members The names of the Repositories in the group
   * @return the newly created Repository
   */
  Repository createPyPiGroup(final String name,
                            final List<String> members,
                            final String blobStoreName);

  /**
   * Create a Yum hosted repository.
   * @param name The name of the new Repository
   * @param blobStoreName The BlobStore the Repository should use
   * @param strictContentTypeValidation Whether or not the Repository should enforce strict content types
   * @param writePolicy The {@link WritePolicy} for the Repository
   * @param depth the repodata depth
   * @return the newly created Repository
   */
  Repository createYumHosted(final String name,
                             final String blobStoreName,
                             final boolean strictContentTypeValidation,
                             final WritePolicy writePolicy,
                             final int depth);

  /**
   * Create a Yum proxy repository.
   * @param name The name of the new Repository
   * @param remoteUrl The url of the external proxy for this Repository
   * @param blobStoreName The BlobStore the Repository should use
   * @param strictContentTypeValidation Whether or not the Repository should enforce strict content types
   * @return the newly created Repository
   */
  Repository createYumProxy(final String name,
                            final String remoteUrl,
                            final String blobStoreName,
                            final boolean strictContentTypeValidation);

  /**
   * Create a Go hosted repository.
   * @param name The name of the new Repository
   * @param blobStoreName The BlobStore the Repository should use
   * @param strictContentTypeValidation Whether or not the Repository should enforce strict content types
   * @param writePolicy The {@link WritePolicy} for the Repository
   * @return the newly created Repository
   */
  Repository createGolangHosted(final String name,
                                final String blobStoreName,
                                final boolean strictContentTypeValidation,
                                final WritePolicy writePolicy);

  /**
   * Create a Go proxy repository.
   * @param name The name of the new Repository
   * @param remoteUrl The url of the external proxy for this Repository
   * @param blobStoreName The BlobStore the Repository should use
   * @param strictContentTypeValidation Whether or not the Repository should enforce strict content types
   * @return the newly created Repository
   */
  Repository createGolangProxy(final String name,
                               final String remoteUrl,
                               final String blobStoreName,
                               final boolean strictContentTypeValidation);

  /**
   * Create a Go group repository.
   * @param name The name of the new Repository
   * @param blobStoreName The BlobStore the Repository should use
   * @param members The names of the Repositories in the group
   * @return the newly created Repository
   */
  Repository createGolangGroup(final String name,
                               final List<String> members,
                               final String blobStoreName);

  /**
   * Create a GitLFS hosted repository.
   *
   * @param name                        The name of the new Repository
   * @param blobStoreName               The BlobStore the Repository should use
   * @param strictContentTypeValidation Whether or not the Repository should enforce strict content types
   * @param writePolicy                 The {@link WritePolicy} for the Repository
   * @return                            The newly created Repository
   */
  Repository createGitLfsHosted(final String name,
                                final String blobStoreName,
                                final boolean strictContentTypeValidation,
                                final WritePolicy writePolicy);

  /**
   * Create a hosted configuration for the given recipeName.
   */
  Configuration createHosted(final String name,
                             final String recipeName,
                             final String blobStoreName,
                             final WritePolicy writePolicy,
                             final boolean strictContentTypeValidation);

  /**
   * Create a proxy configuration for the given recipeName.
   */
  Configuration createProxy(final String name,
                            final String recipeName,
                            final String remoteUrl,
                            final String blobStoreName,
                            final boolean strictContentTypeValidation);

  /**
   * Create a group configuration for the given recipeName.
   */
  Configuration createGroup(final String name,
                            final String recipeName,
                            final String blobStoreName,
                            final String... members);
}
