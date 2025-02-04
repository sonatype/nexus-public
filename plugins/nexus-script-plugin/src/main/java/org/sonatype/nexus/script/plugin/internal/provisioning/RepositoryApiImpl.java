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
package org.sonatype.nexus.script.plugin.internal.provisioning;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.blobstore.api.BlobStoreManager;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.config.Configuration;
import org.sonatype.nexus.repository.config.WritePolicy;
import org.sonatype.nexus.repository.manager.RepositoryManager;
import org.sonatype.nexus.repository.maven.LayoutPolicy;
import org.sonatype.nexus.repository.maven.VersionPolicy;
import org.sonatype.nexus.script.plugin.RepositoryApi;

import java.util.*;
import java.util.stream.StreamSupport;

import com.google.common.annotations.VisibleForTesting;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.Collections.emptyMap;

/**
 * @since 3.0
 */
@Named
@Singleton
public class RepositoryApiImpl
    implements RepositoryApi
{
  private static final String BLOB_STORE_NAME = "blobStoreName";

  private static final String STORAGE = "storage";

  private static final String MAVEN = "maven";

  private static final String DOCKER = "docker";

  private final RepositoryManager repositoryManager;

  private final BlobStoreManager blobStoreManager;

  @Inject
  public RepositoryApiImpl(final RepositoryManager repositoryManager, final BlobStoreManager blobStoreManager) {
    this.repositoryManager = checkNotNull(repositoryManager);
    this.blobStoreManager = checkNotNull(blobStoreManager);
  }

  /**
   * Create a hosted configuration for the given recipeName.
   */
  @Nonnull
  public Configuration createHosted(
      final String name,
      final String recipeName,
      final String blobStoreName,
      final WritePolicy writePolicy,
      final boolean strictContentTypeValidation)
  {
    checkNotNull(name);
    checkArgument(recipeName != null && recipeName.endsWith("-hosted"));

    Map<String, Object> storageAttributes = new HashMap<>();
    storageAttributes.put(BLOB_STORE_NAME, blobStoreName);
    storageAttributes.put("writePolicy", writePolicy);
    storageAttributes.put("strictContentTypeValidation", strictContentTypeValidation);

    Map<String, Map<String, Object>> attributes = new HashMap<>();
    attributes.put(STORAGE, storageAttributes);

    return newConfiguration(name, recipeName, true, attributes);
  }

  /**
   * Create a proxy configuration for the given recipeName.
   */
  @Nonnull
  public Configuration createProxy(
      final String name,
      final String recipeName,
      final String remoteUrl,
      final String blobStoreName,
      final boolean strictContentTypeValidation)
  {
    checkNotNull(name);
    checkArgument(recipeName != null && recipeName.endsWith("-proxy"));
    Map<String, Map<String, Object>> attributes = new HashMap<>();

    Map<String, Object> httpClientAttributes = new HashMap<>();
    Map<String, Object> connectionAttributes = new HashMap<>();
    connectionAttributes.put("blocked", false);
    connectionAttributes.put("autoBlock", true);
    httpClientAttributes.put("connection", connectionAttributes);
    attributes.put("httpclient", httpClientAttributes);

    Map<String, Object> proxyAttributes = new HashMap<>();
    proxyAttributes.put("remoteUrl", remoteUrl);
    proxyAttributes.put("contentMaxAge", 1440);
    proxyAttributes.put("metadataMaxAge", 1440);
    attributes.put("proxy", proxyAttributes);

    Map<String, Object> negativeCacheAttributes = new HashMap<>();
    negativeCacheAttributes.put("enabled", true);
    negativeCacheAttributes.put("timeToLive", 1440);
    attributes.put("negativeCache", negativeCacheAttributes);

    Map<String, Object> storageAttributes = new HashMap<>();
    storageAttributes.put(BLOB_STORE_NAME, blobStoreName);
    storageAttributes.put("strictContentTypeValidation", strictContentTypeValidation);
    attributes.put(STORAGE, storageAttributes);
    return newConfiguration(name, recipeName, true, attributes);
  }

  @Nonnull
  public Configuration createGroup(
      final String name,
      final String recipeName,
      final String blobStoreName,
      final String... members)
  {
    checkNotNull(name);
    checkArgument(recipeName != null && recipeName.endsWith("-group"));

    Map<String, Object> groupAttributes = new HashMap<>();
    groupAttributes.put("memberNames", Arrays.stream(members).distinct().toList());

    Map<String, Object> storageAttributes = new HashMap<>();
    storageAttributes.put(BLOB_STORE_NAME, blobStoreName);

    Map<String, Map<String, Object>> attributes = new HashMap<>();
    attributes.put("group", groupAttributes);
    attributes.put(STORAGE, storageAttributes);
    return newConfiguration(name, recipeName, true, attributes);
  }

  private Configuration newConfiguration(
      final String repositoryName,
      final String recipeName,
      final boolean online,
      final Map<String, Map<String, Object>> attributes)
  {
    Configuration config = repositoryManager.newConfiguration();
    config.setRepositoryName(repositoryName);
    config.setRecipeName(recipeName);
    config.setOnline(online);
    config.setAttributes(attributes);
    config.getAttributes().get(STORAGE).put("dataStoreName", "nexus");
    return config;
  }

  @Nonnull
  public Repository createAptProxy(
      final String name,
      final String remoteUrl,
      final String blobStoreName,
      final String distribution,
      final boolean strictContentTypeValidation) throws Exception
  {
    Configuration configuration = createProxy(name, "apt-proxy", remoteUrl, blobStoreName, strictContentTypeValidation);
    Map<String, Object> aptAttributes = new HashMap<>();
    aptAttributes.put("distribution", distribution);
    configuration.getAttributes().put("apt", aptAttributes);
    return createRepository(configuration);
  }

  @Nonnull
  public Repository createAptHosted(
      final String name,
      final String distribution,
      final String pgpPrivateKey,
      final String pgpPassPhrase,
      final String blobStoreName,
      final WritePolicy writePolicy,
      final boolean strictContentTypeValidation) throws Exception
  {
    Configuration configuration =
        createHosted(name, "apt-hosted", blobStoreName, writePolicy, strictContentTypeValidation);

    Map<String, Object> aptAttributes = new HashMap<>();
    aptAttributes.put("distribution", distribution);
    configuration.getAttributes().put("apt", aptAttributes);

    Map<String, Object> aptSigningAttributes = new HashMap<>();
    aptSigningAttributes.put("keypair", pgpPrivateKey);
    aptSigningAttributes.put("passphrase", pgpPassPhrase);
    configuration.getAttributes().put("aptSigning", aptSigningAttributes);

    return createRepository(configuration);
  }

  @Nonnull
  public Repository createCocoapodsProxy(
      final String name,
      final String remoteUrl,
      final String blobStoreName,
      final boolean strictContentTypeValidation) throws Exception
  {
    Configuration configuration =
        createProxy(name, "cocoapods-proxy", remoteUrl, blobStoreName, strictContentTypeValidation);
    return createRepository(configuration);
  }

  @Nonnull
  public Repository createMavenHosted(
      final String name,
      final String blobStoreName,
      final boolean strictContentTypeValidation,
      final VersionPolicy versionPolicy,
      final WritePolicy writePolicy,
      final LayoutPolicy layoutPolicy) throws Exception
  {
    Configuration configuration =
        createHosted(name, "maven2-hosted", blobStoreName, writePolicy, strictContentTypeValidation);
    configuration.getAttributes().put(MAVEN, configureMaven(versionPolicy, layoutPolicy));
    return createRepository(configuration);
  }

  @Nonnull
  public Repository createMavenProxy(
      final String name,
      final String remoteUrl,
      final String blobStoreName,
      final boolean strictContentTypeValidation,
      final VersionPolicy versionPolicy,
      final LayoutPolicy layoutPolicy) throws Exception
  {
    Configuration configuration =
        createProxy(name, "maven2-proxy", remoteUrl, blobStoreName, strictContentTypeValidation);
    configuration.getAttributes().put(MAVEN, configureMaven(versionPolicy, layoutPolicy));
    return createRepository(configuration);
  }

  @Nonnull
  public Repository createMavenGroup(
      final String name,
      final List<String> members,
      final String blobStoreName) throws Exception
  {
    Configuration configuration = createGroup(name, "maven2-group", blobStoreName, members.toArray(new String[0]));
    configuration.getAttributes().put(MAVEN, configureMaven());
    return createRepository(configuration);
  }

  @Nonnull
  public Repository createNpmHosted(
      final String name,
      final String blobStoreName,
      final boolean strictContentTypeValidation,
      final WritePolicy writePolicy) throws Exception
  {
    return createRepository(createHosted(name, "npm-hosted", blobStoreName, writePolicy, strictContentTypeValidation));
  }

  @Nonnull
  public Repository createNpmProxy(
      final String name,
      final String remoteUrl,
      final String blobStoreName,
      final boolean strictContentTypeValidation) throws Exception
  {
    return createRepository(createProxy(name, "npm-proxy", remoteUrl, blobStoreName, strictContentTypeValidation));
  }

  @Nonnull
  public Repository createNpmGroup(
      final String name,
      final List<String> members,
      final String blobStoreName) throws Exception
  {
    return createRepository(createGroup(name, "npm-group", blobStoreName, members.toArray(new String[0])));
  }

  @Nonnull
  public Repository createNugetHosted(
      final String name,
      final String blobStoreName,
      final boolean strictContentTypeValidation,
      final WritePolicy writePolicy) throws Exception
  {
    return createRepository(
        createHosted(name, "nuget-hosted", blobStoreName, writePolicy, strictContentTypeValidation));
  }

  @Nonnull
  public Repository createNugetProxy(
      final String name,
      final String remoteUrl,
      final String blobStoreName,
      final boolean strictContentTypeValidation) throws Exception
  {
    return createRepository(createProxy(name, "nuget-proxy", remoteUrl, blobStoreName, strictContentTypeValidation));
  }

  @Nonnull
  public Repository createNugetGroup(
      final String name,
      final List<String> members,
      final String blobStoreName) throws Exception
  {
    return createRepository(createGroup(name, "nuget-group", blobStoreName, members.toArray(new String[0])));
  }

  @Nonnull
  public Repository createRawHosted(
      final String name,
      final String blobStoreName,
      final boolean strictContentTypeValidation,
      final WritePolicy writePolicy) throws Exception
  {
    return createRepository(createHosted(name, "raw-hosted", blobStoreName, writePolicy, strictContentTypeValidation));
  }

  @Nonnull
  public Repository createRawProxy(
      final String name,
      final String remoteUrl,
      final String blobStoreName,
      final boolean strictContentTypeValidation) throws Exception
  {
    return createRepository(createProxy(name, "raw-proxy", remoteUrl, blobStoreName, strictContentTypeValidation));
  }

  @Nonnull
  public Repository createRawGroup(
      final String name,
      final List<String> members,
      final String blobStoreName) throws Exception
  {
    return createRepository(createGroup(name, "raw-group", blobStoreName, members.toArray(new String[0])));
  }

  @Nonnull
  public Repository createDockerHosted(
      final String name,
      final Integer httpPort,
      final Integer httpsPort,
      final String blobStoreName,
      final boolean v1Enabled,
      final boolean strictContentTypeValidation,
      final WritePolicy writePolicy) throws Exception
  {
    return createDockerHosted(name, httpPort, httpsPort, blobStoreName, strictContentTypeValidation, v1Enabled,
        writePolicy, true);
  }

  @Nonnull
  public Repository createDockerHosted(
      final String name,
      @Nullable Integer httpPort,
      @Nullable Integer httpsPort,
      final String blobStoreName,
      final boolean strictContentTypeValidation,
      final boolean v1Enabled,
      final WritePolicy writePolicy,
      final boolean forceBasicAuth) throws Exception
  {
    Configuration configuration =
        createHosted(name, "docker-hosted", blobStoreName, writePolicy, strictContentTypeValidation);
    configuration.getAttributes()
        .put(DOCKER, configureDockerAttributes(httpPort, httpsPort, v1Enabled, forceBasicAuth));
    return createRepository(configuration);
  }

  @Override
  public Repository createDockerProxy(
      final String name,
      final String remoteUrl,
      final String indexType,
      final String indexUrl,
      final Integer httpPort,
      final Integer httpsPort,
      final String blobStoreName,
      final boolean strictContentTypeValidation,
      final boolean v1Enabled) throws Exception
  {
    return createDockerProxy(name, remoteUrl, indexType, indexUrl, httpPort, httpsPort, blobStoreName,
        strictContentTypeValidation, v1Enabled, true);
  }

  @Nonnull
  public Repository createDockerProxy(
      final String name,
      final String remoteUrl,
      final String indexType,
      @Nullable final String indexUrl,
      @Nullable Integer httpPort,
      @Nullable Integer httpsPort,
      final String blobStoreName,
      final boolean strictContentTypeValidation,
      final boolean v1Enabled,
      final boolean forceBasicAuth) throws Exception
  {
    Configuration configuration =
        createProxy(name, "docker-proxy", remoteUrl, blobStoreName, strictContentTypeValidation);

    Map<String, Object> dockerAttributes = configureDockerAttributes(httpPort, httpsPort, v1Enabled, forceBasicAuth);
    configuration.getAttributes().put(DOCKER, dockerAttributes);

    Map<String, Object> dockerProxyAttributes = new HashMap<>();
    dockerProxyAttributes.put("indexType", indexType);
    dockerProxyAttributes.put("indexUrl", indexUrl);
    configuration.getAttributes().put("dockerProxy", dockerProxyAttributes);

    Map<String, Object> httpClientAttributes = new HashMap<>();
    Map<String, Object> connectionAttributes = new HashMap<>();
    connectionAttributes.put("useTrustStore", true);
    httpClientAttributes.put("connection", connectionAttributes);
    configuration.getAttributes().put("httpclient", httpClientAttributes);

    return createRepository(configuration);
  }

  @Override
  public Repository createDockerGroup(
      final String name,
      final Integer httpPort,
      final Integer httpsPort,
      final List<String> members,
      final boolean v1Enabled,
      final String blobStoreName) throws Exception
  {
    return createDockerGroup(name, httpPort, httpsPort, members, v1Enabled, blobStoreName, true);
  }

  @Nonnull
  public Repository createDockerGroup(
      final String name,
      @Nullable Integer httpPort,
      @Nullable Integer httpsPort,
      final List<String> members,
      final boolean v1Enabled,
      final String blobStoreName,
      final boolean forceBasicAuth) throws Exception
  {
    Configuration configuration = createGroup(name, "docker-group", blobStoreName, members.toArray(new String[0]));
    Map<String, Object> dockerAttributes = configureDockerAttributes(httpPort, httpsPort, v1Enabled, forceBasicAuth);
    configuration.getAttributes().put(DOCKER, dockerAttributes);
    return createRepository(configuration);
  }

  @Nonnull
  public Repository createPyPiHosted(
      final String name,
      final String blobStoreName,
      final boolean strictContentTypeValidation,
      final WritePolicy writePolicy) throws Exception
  {
    return createRepository(createHosted(name, "pypi-hosted", blobStoreName, writePolicy, strictContentTypeValidation));
  }

  @Nonnull
  public Repository createPyPiProxy(
      final String name,
      final String remoteUrl,
      final String blobStoreName,
      final boolean strictContentTypeValidation) throws Exception
  {
    return createRepository(createProxy(name, "pypi-proxy", remoteUrl, blobStoreName, strictContentTypeValidation));
  }

  @Nonnull
  public Repository createPyPiGroup(
      final String name,
      final List<String> members,
      final String blobStoreName) throws Exception
  {
    return createRepository(createGroup(name, "pypi-group", blobStoreName, members.toArray(new String[0])));
  }

  @Nonnull
  public Repository createRubygemsHosted(
      final String name,
      final String blobStoreName,
      final boolean strictContentTypeValidation,
      final WritePolicy writePolicy) throws Exception
  {
    return createRepository(
        createHosted(name, "rubygems-hosted", blobStoreName, writePolicy, strictContentTypeValidation));
  }

  @Nonnull
  public Repository createRubygemsProxy(
      final String name,
      final String remoteUrl,
      final String blobStoreName,
      final boolean strictContentTypeValidation) throws Exception
  {
    return createRepository(createProxy(name, "rubygems-proxy", remoteUrl, blobStoreName, strictContentTypeValidation));
  }

  @Nonnull
  public Repository createRubygemsGroup(
      final String name,
      final List<String> members,
      final String blobStoreName) throws Exception
  {
    return createRepository(createGroup(name, "rubygems-group", blobStoreName, members.toArray(new String[0])));
  }

  @Nonnull
  public Repository createYumHosted(
      final String name,
      final String blobStoreName,
      final boolean strictContentTypeValidation,
      final WritePolicy writePolicy,
      final int depth) throws Exception
  {
    Configuration configuration =
        createHosted(name, "yum-hosted", blobStoreName, writePolicy, strictContentTypeValidation);
    configuration.getAttributes().put("yum", Collections.singletonMap("repodataDepth", depth));
    return createRepository(configuration);
  }

  @Nonnull
  public Repository createYumProxy(
      final String name,
      final String remoteUrl,
      final String blobStoreName,
      final boolean strictContentTypeValidation) throws Exception
  {
    return createRepository(createProxy(name, "yum-proxy", remoteUrl, blobStoreName, strictContentTypeValidation));
  }

  @Nonnull
  public Repository createYumGroup(
      final String name,
      final List<String> members,
      final String blobStoreName) throws Exception
  {
    return createRepository(createGroup(name, "yum-group", blobStoreName, members.toArray(new String[0])));
  }

  @Nonnull
  public Repository createGolangHosted(
      final String name,
      final String blobStoreName,
      final boolean strictContentTypeValidation,
      final WritePolicy writePolicy) throws Exception
  {
    return createRepository(createHosted(name, "go-hosted", blobStoreName, writePolicy, strictContentTypeValidation));
  }

  @Nonnull
  public Repository createGolangProxy(
      final String name,
      final String remoteUrl,
      final String blobStoreName,
      final boolean strictContentTypeValidation) throws Exception
  {
    return createRepository(createProxy(name, "go-proxy", remoteUrl, blobStoreName, strictContentTypeValidation));
  }

  @Nonnull
  public Repository createGolangGroup(
      final String name,
      final List<String> members,
      final String blobStoreName) throws Exception
  {
    return createRepository(createGroup(name, "go-group", blobStoreName, members.toArray(new String[0])));
  }

  @Nonnull
  public Repository createGitLfsHosted(
      final String name,
      final String blobStoreName,
      final boolean strictContentTypeValidation,
      final WritePolicy writePolicy) throws Exception
  {
    return createRepository(
        createHosted(name, "gitlfs-hosted", blobStoreName, writePolicy, strictContentTypeValidation));
  }

  private Map<String, Object> configureMaven(final VersionPolicy versionPolicy, final LayoutPolicy layoutPolicy) {
    Map<String, Object> mavenAttributes = new HashMap<>();
    mavenAttributes.put("versionPolicy", versionPolicy.toString());
    mavenAttributes.put("layoutPolicy", layoutPolicy.toString());
    return mavenAttributes;
  }

  private Map<String, Object> configureMaven() {
    return configureMaven(VersionPolicy.MIXED, LayoutPolicy.STRICT);
  }

  private Map<String, Object> configureDockerAttributes(
      Integer httpPort,
      Integer httpsPort,
      boolean v1Enabled,
      boolean forceBasicAuth)
  {
    Map<String, Object> docker = new HashMap<>();
    if (httpPort != null) {
      docker.put("httpPort", httpPort);
    }
    if (httpsPort != null) {
      docker.put("httpsPort", httpsPort);
    }
    docker.put("v1Enabled", v1Enabled);
    docker.put("forceBasicAuth", forceBasicAuth);
    return docker;
  }

  public Repository createRepository(final Configuration configuration) throws Exception {
    validateBlobStore(configuration);
    validateGroupMembers(configuration);
    return repositoryManager.create(configuration);
  }

  @VisibleForTesting
  void validateGroupMembers(final Configuration configuration) {
    Collection<String> members =
        (Collection<String>) configuration.getAttributes().getOrDefault("group", emptyMap()).get("memberNames");
    if (members != null) {
      List<String> existingRepos = StreamSupport.stream(repositoryManager.browse().spliterator(), false)
          .map(Repository::getName)
          .toList();
      boolean valid = members.stream().allMatch(existingRepos::contains);
      if (!valid) {
        throw new IllegalStateException("One or more of the specified group memberNames does not actually exist");
      }
    }
  }

  @VisibleForTesting
  void validateBlobStore(Configuration configuration) {
    String name = (String) configuration.getAttributes().getOrDefault(STORAGE, emptyMap()).get(BLOB_STORE_NAME);
    boolean exists = StreamSupport.stream(blobStoreManager.browse().spliterator(), false)
        .anyMatch(blobStore -> blobStore.getBlobStoreConfiguration().getName().equals(name));
    if (!exists) {
      throw new IllegalArgumentException("No blobStore found with name " + name);
    }
  }
}
