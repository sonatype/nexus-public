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
package org.sonatype.nexus.plugins.mavenbridge.internal;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.aether.RepositoryListener;
import org.sonatype.aether.RepositorySystem;
import org.sonatype.aether.repository.LocalRepository;
import org.sonatype.aether.util.DefaultRepositorySystemSession;
import org.sonatype.nexus.configuration.application.ApplicationConfiguration;
import org.sonatype.nexus.plugins.mavenbridge.NexusAether;
import org.sonatype.nexus.plugins.mavenbridge.workspace.NexusWorkspace;
import org.sonatype.nexus.proxy.maven.MavenRepository;

import org.apache.maven.repository.internal.MavenRepositorySystemSession;

@Named
@Singleton
public class DefaultNexusAether
    implements NexusAether
{
  public static final String LOCAL_REPO_DIR = "maven2-local-repository";

  private final ApplicationConfiguration applicationConfiguration;

  private final RepositorySystem repositorySystem;

  @Inject
  public DefaultNexusAether(final ApplicationConfiguration applicationConfiguration,
                            final RepositorySystem repositorySystem)
  {
    this.applicationConfiguration = applicationConfiguration;
    this.repositorySystem = repositorySystem;
  }

  public NexusWorkspace createWorkspace(List<MavenRepository> participants) {
    if (participants == null || participants.isEmpty()) {
      throw new IllegalArgumentException(
          "Participant repositories in NexusWorkspace must have at least one element!");
    }

    return new NexusWorkspace(UUID.randomUUID().toString(), participants);
  }

  public NexusWorkspace createWorkspace(MavenRepository... participants) {
    if (participants == null) {
      throw new IllegalArgumentException(
          "Participant repositories in NexusWorkspace must have at least one element!");
    }

    return createWorkspace(Arrays.asList(participants));
  }

  public synchronized RepositorySystem getRepositorySystem() {
    return repositorySystem;
  }

  public DefaultRepositorySystemSession getDefaultRepositorySystemSession() {
    MavenRepositorySystemSession session = new MavenRepositorySystemSession();

    // can't aether work _without_ local repo?
    LocalRepository localRepo =
        new LocalRepository(applicationConfiguration.getWorkingDirectory(LOCAL_REPO_DIR));
    session.setLocalRepositoryManager(getRepositorySystem().newLocalRepositoryManager(localRepo));

    // session.setIgnoreMissingArtifactDescriptor( false );

    // session.setTransferListener( new ConsoleTransferListener( System.out ) );
    // session.setRepositoryListener( new ConsoleRepositoryListener( System.out ) );

    // session.setUpdatePolicy( "" );

    return session;
  }

  public DefaultRepositorySystemSession getNexusEnabledRepositorySystemSession(final NexusWorkspace nexusWorkspace,
                                                                               final RepositoryListener listener)
  {
    return getNexusEnabledRepositorySystemSession(getDefaultRepositorySystemSession(), nexusWorkspace, listener);
  }

  public DefaultRepositorySystemSession getNexusEnabledRepositorySystemSession(
      final DefaultRepositorySystemSession session,
      final NexusWorkspace nexusWorkspace,
      final RepositoryListener listener)
  {
    session.setWorkspaceReader(nexusWorkspace.getWorkspaceReader());
    session.setOffline(true);
    session.setRepositoryListener(listener);

    return session;
  }
}
