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
package org.sonatype.nexus.repository.apt.datastore.internal.hosted;

import java.io.IOException;
import java.util.Optional;

import javax.annotation.Nonnull;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.repository.apt.datastore.AptContentFacet;
import org.sonatype.nexus.repository.apt.internal.AptFacetHelper;
import org.sonatype.nexus.repository.apt.internal.AptPackageParser;
import org.sonatype.nexus.repository.apt.internal.debian.ControlFile;
import org.sonatype.nexus.repository.apt.internal.debian.PackageInfo;
import org.sonatype.nexus.repository.apt.internal.snapshot.AptSnapshotHandler;
import org.sonatype.nexus.repository.http.HttpResponses;
import org.sonatype.nexus.repository.view.Content;
import org.sonatype.nexus.repository.view.Context;
import org.sonatype.nexus.repository.view.Handler;
import org.sonatype.nexus.repository.view.Payload;
import org.sonatype.nexus.repository.view.Response;
import org.sonatype.nexus.repository.view.payloads.StreamPayload;
import org.sonatype.nexus.repository.view.payloads.TempBlob;

import org.apache.commons.lang3.StringUtils;

import static org.sonatype.nexus.repository.apt.internal.ReleaseName.INRELEASE;
import static org.sonatype.nexus.repository.apt.internal.ReleaseName.RELEASE;
import static org.sonatype.nexus.repository.apt.internal.ReleaseName.RELEASE_GPG;
import static org.sonatype.nexus.repository.http.HttpMethods.GET;
import static org.sonatype.nexus.repository.http.HttpMethods.HEAD;
import static org.sonatype.nexus.repository.http.HttpMethods.POST;

/**
 * Apt handlers
 *
 * @since 3.31
 */
@Named
@Singleton
public class AptHostedHandler
    extends ComponentSupport
    implements Handler
{
  @Nonnull
  @Override
  public Response handle(@Nonnull final Context context) throws Exception {
    String path = assetPath(context);
    String method = context.getRequest().getAction();
    AptContentFacet contentFacet = context.getRepository().facet(AptContentFacet.class);

    switch (method) {
      case GET:
      case HEAD:
        return doGet(context, path, contentFacet);
      case POST:
        return doPost(context, path, contentFacet);
      default:
        return HttpResponses.methodNotAllowed(method, GET, HEAD, POST);
    }
  }

  private Response doGet(
      final Context context,
      final String path,
      final AptContentFacet contentFacet) throws IOException
  {
    if (isMetadataRebuildRequired(path, contentFacet)) {
      context.getRepository().facet(AptHostedFacet.class).rebuildMetadata();
    }
    Optional<Content> content = contentFacet.get(path);
    return content.isPresent() ? HttpResponses.ok(content.get()) : HttpResponses.notFound(path);
  }

  private Response doPost(final Context context,
                          final String path,
                          final AptContentFacet contentFacet) throws IOException
  {
    final AptHostedFacet hostedFacet = context.getRepository().facet(AptHostedFacet.class);
    if ("rebuild-indexes".equals(path)) {
      hostedFacet.rebuildMetadata();
      return HttpResponses.ok();
    }
    else if (StringUtils.isBlank(path)) {
      final Payload payload = context.getRequest().getPayload();
      try (TempBlob tempBlob = contentFacet.getTempBlob(payload)) {
        ControlFile controlFile = AptPackageParser
            .parsePackageInfo(tempBlob)
            .getControlFile();
        String assetPath = AptFacetHelper.buildAssetPath(controlFile);
        long payloadSize = payload.getSize();
        String contentType = payload.getContentType();

        hostedFacet.
            put(assetPath, new StreamPayload(tempBlob, payloadSize, contentType),
                new PackageInfo(controlFile));
      }
      return HttpResponses.created();
    }
    else {
      return HttpResponses.methodNotAllowed(POST, GET, HEAD);
    }
  }

  private boolean isMetadataRebuildRequired(final String path, final AptContentFacet contentFacet)
  {
    if (StringUtils.startsWith(path, "dists")
        && StringUtils.endsWithAny(path, INRELEASE, RELEASE, RELEASE_GPG, "/Packages")) {
      String inReleasePath = "dists/" + contentFacet.getDistribution() + "/" + INRELEASE;
      return !contentFacet.get(inReleasePath).isPresent();
    }
    return false;
  }

  private String assetPath(final Context context) {
    return context.getAttributes().require(AptSnapshotHandler.State.class).assetPath;
  }
}
