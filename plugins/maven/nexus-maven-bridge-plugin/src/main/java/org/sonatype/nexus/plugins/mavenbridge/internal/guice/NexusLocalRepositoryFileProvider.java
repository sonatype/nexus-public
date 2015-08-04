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
package org.sonatype.nexus.plugins.mavenbridge.internal.guice;

import java.io.File;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.sonatype.nexus.configuration.application.ApplicationConfiguration;
import org.sonatype.nexus.plugins.mavenbridge.internal.DefaultNexusAether;
import org.sonatype.sisu.maven.bridge.Names;

/**
 * This component is present only to satisfy sisu-maven-bridge dep graph, but IS NOT USED! This property is needed for
 * "shared" session, but in Nexus' "embedded server" use case, we always explicitly provide session!
 *
 * @author cstamas
 */
@Named(Names.LOCAL_REPOSITORY_DIR)
@Singleton
public class NexusLocalRepositoryFileProvider
    implements Provider<File>
{
  private final ApplicationConfiguration applicationConfiguration;

  @Inject
  public NexusLocalRepositoryFileProvider(final ApplicationConfiguration applicationConfiguration) {
    this.applicationConfiguration = applicationConfiguration;
  }

  @Override
  public File get() {
    return applicationConfiguration.getWorkingDirectory(DefaultNexusAether.LOCAL_REPO_DIR);
  }
}