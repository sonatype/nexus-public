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
package org.sonatype.nexus.testsuite.testsupport.system.repository;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.common.collect.NestedAttributesMap;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.config.Configuration;
import org.sonatype.nexus.repository.manager.RepositoryManager;
import org.sonatype.nexus.repository.maven.LayoutPolicy;
import org.sonatype.nexus.repository.maven.VersionPolicy;
import org.sonatype.nexus.testsuite.testsupport.system.repository.config.MavenGroupRepositoryConfig;
import org.sonatype.nexus.testsuite.testsupport.system.repository.config.MavenHostedRepositoryConfig;
import org.sonatype.nexus.testsuite.testsupport.system.repository.config.MavenProxyRepositoryConfig;

import static org.sonatype.nexus.testsuite.testsupport.system.RepositoryTestSystem.FORMAT_MAVEN;

@Named(FORMAT_MAVEN)
@Singleton
public class MavenFormatRepositoryTestSystem
    extends FormatRepositoryTestSystemSupport
                <MavenHostedRepositoryConfig,
                    MavenProxyRepositoryConfig,
                    MavenGroupRepositoryConfig>
    implements FormatRepositoryTestSystem
{
  public static final String ATTRIBUTES_MAP_KEY_AUTHENTICATION = "authentication";

  public static final String ATTRIBUTES_MAP_KEY_MAVEN = "maven";

  public static final String ATTRIBUTES_MAP_KEY_REPLICATION = "replication";

  public static final String ATTRIBUTES_KEY_PULL_REPLICATION_ENABLED = "preemptivePullEnabled";

  public static final String ATTRIBUTES_ASSET_PATH_REGEX = "assetPathRegex";

  public static final String ATTRIBUTES_KEY_VERSION_POLICY = "versionPolicy";

  public static final String ATTRIBUTES_KEY_LAYOUT_POLICY = "layoutPolicy";

  public static final String ATTRIBUTES_KEY_USERNAME = "username";

  public static final String ATTRIBUTES_KEY_PASSWORD = "password";

  @Inject
  public MavenFormatRepositoryTestSystem(final RepositoryManager repositoryManager) {
    super(repositoryManager);
  }

  public Repository createHosted(final MavenHostedRepositoryConfig config) throws Exception {
    return doCreate(
        applyMavenAttributes(createHostedConfiguration(config), config.getVersionPolicy(), config.getLayoutPolicy()));
  }

  public Repository createProxy(final MavenProxyRepositoryConfig config) throws Exception {
    Configuration cfg = applyMavenAttributes(createProxyConfiguration(config), config.getVersionPolicy(), config.getLayoutPolicy());
    applyAuthenticationAttributes(cfg, config.getUsername(), config.getPassword());
    applyPullReplicationAttributes(cfg, config.isPreemptivePullEnabled(), config.getAssetPathRegex());
    return doCreate(cfg);
  }

  public Repository createGroup(final MavenGroupRepositoryConfig config) throws Exception {
    return doCreate(
        applyMavenAttributes(createGroupConfiguration(config), config.getVersionPolicy(), config.getLayoutPolicy()));
  }

  private Configuration applyAuthenticationAttributes(
      final Configuration configuration,
      final String username,
      final String password
  )
  {
    NestedAttributesMap authentication = configuration.attributes(  ATTRIBUTES_MAP_KEY_AUTHENTICATION);
    addConfigIfNotNull(authentication, ATTRIBUTES_KEY_USERNAME, username);
    addConfigIfNotNull(authentication, ATTRIBUTES_KEY_PASSWORD, password);
    return configuration;
  }

  private Configuration applyMavenAttributes(
      final Configuration configuration,
      final VersionPolicy versionPolicy,
      final LayoutPolicy layoutPolicy)
  {
    NestedAttributesMap maven = configuration.attributes(ATTRIBUTES_MAP_KEY_MAVEN);
    addConfigIfNotNull(maven, ATTRIBUTES_KEY_VERSION_POLICY, versionPolicy);
    addConfigIfNotNull(maven, ATTRIBUTES_KEY_LAYOUT_POLICY, layoutPolicy);
    return configuration;
  }

  private Configuration applyPullReplicationAttributes(
      final Configuration configuration,
      final boolean replicationEnabled,
      final String assetPathRegex
  )
  {
    NestedAttributesMap replication = configuration.attributes(ATTRIBUTES_MAP_KEY_REPLICATION);
    addConfigIfNotNull(replication, ATTRIBUTES_KEY_PULL_REPLICATION_ENABLED, replicationEnabled);
    addConfigIfNotNull(replication, ATTRIBUTES_ASSET_PATH_REGEX, assetPathRegex);

    return configuration;
  }
}
