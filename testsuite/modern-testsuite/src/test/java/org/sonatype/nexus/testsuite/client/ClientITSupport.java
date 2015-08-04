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
package org.sonatype.nexus.testsuite.client;

import java.io.IOException;

import org.sonatype.nexus.client.core.subsystem.artifact.ArtifactMaven;
import org.sonatype.nexus.client.core.subsystem.content.Content;
import org.sonatype.nexus.client.core.subsystem.repository.Repositories;
import org.sonatype.nexus.client.core.subsystem.routing.Routing;
import org.sonatype.nexus.client.core.subsystem.security.Privileges;
import org.sonatype.nexus.client.core.subsystem.security.Roles;
import org.sonatype.nexus.client.core.subsystem.security.Users;
import org.sonatype.nexus.client.core.subsystem.targets.RepositoryTarget;
import org.sonatype.nexus.client.core.subsystem.targets.RepositoryTargets;
import org.sonatype.nexus.testsuite.support.NexusRunningParametrizedITSupport;
import org.sonatype.nexus.testsuite.support.NexusStartAndStopStrategy;

import static org.sonatype.nexus.client.core.subsystem.content.Location.repositoryLocation;

@NexusStartAndStopStrategy(NexusStartAndStopStrategy.Strategy.EACH_TEST)
public abstract class ClientITSupport
    extends NexusRunningParametrizedITSupport
{

  public ClientITSupport(final String nexusBundleCoordinates) {
    super(nexusBundleCoordinates);
  }

  protected void upload(final String repositoryId, final String path)
      throws IOException
  {
    content().upload(repositoryLocation(repositoryId, path), testData().resolveFile("artifacts/" + path));
  }

  protected ArtifactMaven artifacts() {
    return client().getSubsystem(ArtifactMaven.class);
  }

  protected Content content() {
    return client().getSubsystem(Content.class);
  }

  protected Repositories repositories() {
    return client().getSubsystem(Repositories.class);
  }

  protected RepositoryTargets targets() {
    return client().getSubsystem(RepositoryTargets.class);
  }

  protected Routing routing() {
    return client().getSubsystem(Routing.class);
  }

  protected RepositoryTarget createRepoTarget(final String id) {
    return targets().create(id).withContentClass("maven2").withName(id).withPatterns(
        "some_pattern").save();
  }

  protected Roles roles() {
    return client().getSubsystem(Roles.class);
  }

  protected Users users() {
    return client().getSubsystem(Users.class);
  }

  protected Privileges privileges() {
    return client().getSubsystem(Privileges.class);
  }

}
