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
package org.sonatype.nexus.repository.r.datastore.internal.hosted;

import java.util.Optional;

import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.repository.content.fluent.FluentAsset;
import org.sonatype.nexus.repository.http.HttpResponses;
import org.sonatype.nexus.repository.r.datastore.RContentFacet;
import org.sonatype.nexus.repository.view.Handler;
import org.sonatype.nexus.repository.view.Payload;
import org.sonatype.nexus.rest.ValidationErrorsException;

import static org.sonatype.nexus.repository.r.internal.util.PackageValidator.validateArchiveUploadPath;
import static org.sonatype.nexus.repository.r.internal.util.RPathUtils.extractRequestPath;

/**
 * R hosted handlers.
 *
 * @since 3.32
 */
@Named
@Singleton
public class RHostedHandlers
{
  public Handler getPackages = context -> {
    String assetPath = extractRequestPath(context);
    FluentAsset fluentAsset = context
        .getRepository()
        .facet(RHostedMetadataFacet.class)
        .getOrCreatePackagesGz(assetPath);
    return HttpResponses.ok(fluentAsset.download());
  };

  public Handler getArchive = context -> {
    String assetPath = extractRequestPath(context);
    Optional<FluentAsset> fluentAsset = context.getRepository()
        .facet(RContentFacet.class)
        .getAsset(assetPath);
    if (fluentAsset.isPresent()) {
      return HttpResponses.ok(fluentAsset.get().download());
    }
    return HttpResponses.notFound();
  };

  public Handler putArchive = context -> {
    String path = extractRequestPath(context);
    try {
      validateArchiveUploadPath(path);
    }
    catch (ValidationErrorsException e) {
      return HttpResponses.badRequest(e.getMessage());
    }
    Payload payload = context.getRequest().getPayload();
    context.getRepository().facet(RContentFacet.class).putPackage(payload, path);
    return HttpResponses.ok();
  };
}
