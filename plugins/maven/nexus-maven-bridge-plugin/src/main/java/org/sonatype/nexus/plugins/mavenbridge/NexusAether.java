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
package org.sonatype.nexus.plugins.mavenbridge;

import java.util.List;

import org.sonatype.aether.RepositoryListener;
import org.sonatype.aether.RepositorySystem;
import org.sonatype.aether.RepositorySystemSession;
import org.sonatype.aether.util.DefaultRepositorySystemSession;
import org.sonatype.nexus.plugins.mavenbridge.workspace.NexusWorkspace;
import org.sonatype.nexus.proxy.maven.MavenRepository;

/**
 * Component providing Nexus integrated Aether services, like Nexus-enabled Sessions and WorkspaceReaders. It also
 * gives
 * a handle to (shared) RepositorySystem.
 *
 * @author cstamas
 */
public interface NexusAether
{
  /**
   * Creates a nexus workspace containing the particiant MavenRepositories. These repositories will fed the Aether
   * engine with artifacts (probably causing some proxying to happen too, if needed). It may be a group or just a
   * bunch of repository (or any combination of them).
   */
  NexusWorkspace createWorkspace(List<MavenRepository> participants);

  /**
   * Creates a nexus workspace containing the particiant MavenRepositories. These repositories will fed the Aether
   * engine with artifacts (probably causing some proxying to happen too, if needed). It may be a group or just a
   * bunch of repository (or any combination of them).
   */
  NexusWorkspace createWorkspace(MavenRepository... participants);

  /**
   * Returns the shared repository system instance.
   */
  RepositorySystem getRepositorySystem();

  /**
   * Returns the most basic repository system session with local repository set only.
   */
  RepositorySystemSession getDefaultRepositorySystemSession();

  /**
   * Returns a special repository system session that is "nexus enabled" (using
   * {@link #getDefaultRepositorySystemSession()} returned session). Passed in nexus workspace controls what nexus
   * repository participates in this session. The returned session is set {@code OFFLINE} since passed in
   * NexusWorkspace feeds all the artifacts needed, and Aether should not go remote (reach out of Nexus instance).
   */
  RepositorySystemSession getNexusEnabledRepositorySystemSession(NexusWorkspace nexusWorkspace,
                                                                 RepositoryListener listener);

  /**
   * Returns a special repository system session that is "nexus enabled". Passed in nexus workspace controls what
   * nexus repository participates in this session. The returned session is set {@code OFFLINE} since passed in
   * NexusWorkspace feeds all the artifacts needed, and Aether should not go remote (reach out of Nexus instance).
   */
  RepositorySystemSession getNexusEnabledRepositorySystemSession(DefaultRepositorySystemSession session,
                                                                 NexusWorkspace nexusWorkspace,
                                                                 RepositoryListener listener);
}
