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
package org.sonatype.nexus.testsuite.testsupport.system.repository.config;

import java.util.function.Function;

import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.maven.LayoutPolicy;
import org.sonatype.nexus.repository.maven.VersionPolicy;

import static org.sonatype.nexus.testsuite.testsupport.system.RepositoryTestSystem.FORMAT_MAVEN;

public class MavenGroupRepositoryConfig
    extends GroupRepositoryConfigSupport<MavenGroupRepositoryConfig>
{
  private VersionPolicy versionPolicy = VersionPolicy.MIXED;

  private LayoutPolicy layoutPolicy = LayoutPolicy.STRICT;

  public MavenGroupRepositoryConfig() {
    this(null);
  }

  public MavenGroupRepositoryConfig(final Function<MavenGroupRepositoryConfig, Repository> repositoryFactory) {
    super(repositoryFactory);
  }

  @Override
  public String getFormat() {
    return FORMAT_MAVEN;
  }

  public MavenGroupRepositoryConfig withVersionPolicy(final VersionPolicy versionPolicy) {
    this.versionPolicy = versionPolicy;
    return this;
  }

  public VersionPolicy getVersionPolicy() {
    return versionPolicy;
  }

  public MavenGroupRepositoryConfig withLayoutPolicy(final LayoutPolicy layoutPolicy) {
    this.layoutPolicy = layoutPolicy;
    return this;
  }

  public LayoutPolicy getLayoutPolicy() {
    return layoutPolicy;
  }
}
