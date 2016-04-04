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
package org.sonatype.nexus.repository.maven.internal;

import javax.annotation.Nonnull;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.repository.http.HttpResponses;
import org.sonatype.nexus.repository.maven.MavenFacet;
import org.sonatype.nexus.repository.maven.MavenPath;
import org.sonatype.nexus.repository.maven.MavenPath.Coordinates;
import org.sonatype.nexus.repository.maven.VersionPolicy;
import org.sonatype.nexus.repository.view.Context;
import org.sonatype.nexus.repository.view.Handler;
import org.sonatype.nexus.repository.view.Response;

/**
 * Maven version policy handler.
 *
 * @since 3.0
 */
@Singleton
@Named
public class VersionPolicyHandler
    extends ComponentSupport
    implements Handler
{
  @Nonnull
  @Override
  public Response handle(@Nonnull final Context context) throws Exception {
    final MavenPath path = context.getAttributes().require(MavenPath.class);
    final MavenFacet mavenFacet = context.getRepository().facet(MavenFacet.class);
    final VersionPolicy versionPolicy = mavenFacet.getVersionPolicy();
    if (path.getCoordinates() != null && !allowsArtifactRepositoryPath(versionPolicy, path.getCoordinates())) {
      return HttpResponses.badRequest("Repository version policy: " + versionPolicy + " does not allow version: " +
          path.getCoordinates().getVersion());
    }
    return context.proceed();
  }

  private boolean allowsArtifactRepositoryPath(final VersionPolicy versionPolicy, final Coordinates coordinates) {
    if (versionPolicy == VersionPolicy.SNAPSHOT) {
      return coordinates.isSnapshot();
    }
    if (versionPolicy == VersionPolicy.RELEASE) {
      return !coordinates.isSnapshot();
    }
    return true;
  }
}
