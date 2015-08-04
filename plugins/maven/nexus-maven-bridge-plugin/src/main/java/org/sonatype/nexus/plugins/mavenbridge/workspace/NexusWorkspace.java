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
package org.sonatype.nexus.plugins.mavenbridge.workspace;

import java.util.Collections;
import java.util.List;

import org.sonatype.aether.repository.WorkspaceReader;
import org.sonatype.nexus.proxy.maven.MavenRepository;

public class NexusWorkspace
{
  private final String id;

  private final List<MavenRepository> repositories;

  public NexusWorkspace(String id, List<MavenRepository> repositories) {
    assert id != null && id.trim().length() > 0 : "Workspace ID cannotbe null or empty!";
    assert repositories != null : "Repository cannot be null!";

    this.id = id;
    this.repositories = repositories;
  }

  public String getId() {
    return id;
  }

  public List<MavenRepository> getRepositories() {
    return Collections.unmodifiableList(repositories);
  }

  public WorkspaceReader getWorkspaceReader() {
    return new NexusWorkspaceReader(this);
  }
}
