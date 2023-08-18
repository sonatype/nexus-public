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
package org.sonatype.nexus.repository.apt.orient.internal.hosted;

import java.io.IOException;
import java.util.Optional;

import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.repository.apt.orient.OrientAptFacet;
import org.sonatype.nexus.repository.apt.internal.snapshot.AptSnapshotHandler;
import org.sonatype.nexus.repository.http.HttpResponses;
import org.sonatype.nexus.repository.view.Content;
import org.sonatype.nexus.repository.view.Context;
import org.sonatype.nexus.repository.view.Handler;
import org.sonatype.nexus.repository.view.Response;

import org.apache.commons.lang3.StringUtils;

import static org.sonatype.nexus.repository.apt.internal.ReleaseName.INRELEASE;
import static org.sonatype.nexus.repository.apt.internal.ReleaseName.RELEASE;
import static org.sonatype.nexus.repository.apt.internal.ReleaseName.RELEASE_GPG;
import static org.sonatype.nexus.repository.http.HttpMethods.GET;
import static org.sonatype.nexus.repository.http.HttpMethods.HEAD;
import static org.sonatype.nexus.repository.http.HttpMethods.POST;

/**
 * @since 3.17
 */
@Named
@Singleton
public class OrientAptHostedHandler
    extends ComponentSupport
    implements Handler
{
  @Override
  public Response handle(final Context context) throws Exception {
    String path = assetPath(context);
    String method = context.getRequest().getAction();

    switch (method) {
      case GET:
      case HEAD:
        return doGet(context, path);
      case POST:
        return doPost(context, path, method);
      default:
        return HttpResponses.methodNotAllowed(method, GET, HEAD, POST);
    }
  }

  private Response doPost(final Context context, final String path, final String method) throws IOException
  {
    OrientAptHostedFacet hostedFacet = context.getRepository().facet(OrientAptHostedFacet.class);
    if ("rebuild-indexes".equals(path)) {
      hostedFacet.rebuildMetadata();
      return HttpResponses.ok();
    }
    else if ("".equals(path)) {
      hostedFacet.ingestAsset(context.getRequest().getPayload());
      hostedFacet.invalidateMetadata();
      return HttpResponses.created();
    }
    else {
      return HttpResponses.methodNotAllowed(method, GET, HEAD);
    }
  }

  private Response doGet(final Context context, final String path) throws IOException {
    OrientAptFacet aptFacet = context.getRepository().facet(OrientAptFacet.class);
    if (isMetadataRebuildRequired(path, aptFacet)) {
      context.getRepository().facet(OrientAptHostedFacet.class).rebuildMetadata();
    }
    Optional<Content> content = aptFacet.get(path);
    return content.map(HttpResponses::ok).orElseGet(() -> HttpResponses.notFound(path));
  }

  private boolean isMetadataRebuildRequired(final String path, final OrientAptFacet aptFacet) throws IOException
  {
    if (StringUtils.startsWith(path, "dists")
        && StringUtils.endsWithAny(path, INRELEASE, RELEASE, RELEASE_GPG, "/Packages")) {
      String inReleasePath = "dists/" + aptFacet.getDistribution() + "/" + INRELEASE;
      return !aptFacet.get(inReleasePath).isPresent();
    }
    return false;
  }

  private String assetPath(final Context context) {
    return context.getAttributes().require(AptSnapshotHandler.State.class).assetPath;
  }
}
