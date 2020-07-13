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
package org.sonatype.nexus.content.maven.internal;

import javax.annotation.Nonnull;
import javax.inject.Inject;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.content.maven.MavenContentFacet;
import org.sonatype.nexus.repository.http.HttpResponses;
import org.sonatype.nexus.repository.maven.MavenPath;
import org.sonatype.nexus.repository.maven.MavenPath.Coordinates;
import org.sonatype.nexus.repository.maven.VersionPolicy;
import org.sonatype.nexus.repository.maven.internal.VersionPolicyValidator;
import org.sonatype.nexus.repository.view.Context;
import org.sonatype.nexus.repository.view.Handler;
import org.sonatype.nexus.repository.view.Response;

import static org.sonatype.nexus.repository.http.HttpMethods.GET;
import static org.sonatype.nexus.repository.http.HttpMethods.HEAD;

/**
 * @since 3.25
 */
public class VersionPolicyHandler
    extends ComponentSupport
    implements Handler
{
  private final VersionPolicyValidator versionPolicyValidator;

  @Inject
  public VersionPolicyHandler(VersionPolicyValidator versionPolicyValidator) {
    this.versionPolicyValidator = versionPolicyValidator;
  }

  @Nonnull
  @Override
  public Response handle(@Nonnull final Context context) throws Exception {
    String httpMethod = context.getRequest().getAction();
    if (GET.equals(httpMethod) || HEAD.equals(httpMethod)) {
      return context.proceed();
    }
    final MavenPath path = context.getAttributes().require(MavenPath.class);
    final MavenContentFacet mavenFacet = context.getRepository().facet(MavenContentFacet.class);
    final VersionPolicy versionPolicy = mavenFacet.getVersionPolicy();
    final Coordinates coordinates = path.getCoordinates();
    if (coordinates != null && !versionPolicyValidator.validArtifactPath(versionPolicy, coordinates)) {
      return HttpResponses.badRequest("Repository version policy: " + versionPolicy + " does not allow version: " +
          coordinates.getVersion());
    }
    if (!versionPolicyValidator.validMetadataPath(versionPolicy, path.main().getPath())) {
      return HttpResponses.badRequest("Repository version policy: " + versionPolicy +
          " does not allow metadata in path: " + path.getPath());
    }
    return context.proceed();
  }
}
