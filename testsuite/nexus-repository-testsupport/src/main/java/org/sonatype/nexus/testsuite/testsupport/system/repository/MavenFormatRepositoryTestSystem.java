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
  public static final String ATTRIBUTES_MAP_KEY_MAVEN = "maven";

  public static final String ATTRIBUTES_KEY_VERSION_POLICY = "versionPolicy";

  public static final String ATTRIBUTES_KEY_LAYOUT_POLICY = "layoutPolicy";

  @Inject
  public MavenFormatRepositoryTestSystem(final RepositoryManager repositoryManager) {
    super(repositoryManager);
  }

  public MavenHostedRepositoryConfig hosted(final String name) {
    return new MavenHostedRepositoryConfig(this::createHosted).withName(name);
  }

  public Repository createHosted(final MavenHostedRepositoryConfig config) {
    return doCreate(
        applyMavenAttributes(createHostedConfiguration(config), config.getVersionPolicy(), config.getLayoutPolicy()));
  }

  public MavenProxyRepositoryConfig proxy(final String name) {
    return new MavenProxyRepositoryConfig(this::createProxy)
         .withName(name);
  }

  public Repository createProxy(final MavenProxyRepositoryConfig config) {
    Configuration cfg =
        applyMavenAttributes(createProxyConfiguration(config), config.getVersionPolicy(), config.getLayoutPolicy());
    return doCreate(cfg);
  }

  public MavenGroupRepositoryConfig group(final String name) {
    return new MavenGroupRepositoryConfig(this::createGroup)
        .withName(name);
  }

  public Repository createGroup(final MavenGroupRepositoryConfig config) {
    return doCreate(
        applyMavenAttributes(createGroupConfiguration(config), config.getVersionPolicy(), config.getLayoutPolicy()));
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
}
