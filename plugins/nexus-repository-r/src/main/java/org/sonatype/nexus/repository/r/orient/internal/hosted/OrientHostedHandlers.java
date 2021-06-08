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
package org.sonatype.nexus.repository.r.orient.internal.hosted;

import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.repository.http.HttpResponses;
import org.sonatype.nexus.repository.r.orient.OrientRHostedFacet;
import org.sonatype.nexus.repository.view.Content;
import org.sonatype.nexus.repository.view.Handler;
import org.sonatype.nexus.rest.ValidationErrorsException;

import static org.sonatype.nexus.repository.r.internal.util.PackageValidator.validateArchiveUploadPath;
import static org.sonatype.nexus.repository.r.internal.util.RPathUtils.extractRequestPath;
import static org.sonatype.nexus.repository.r.internal.util.RPathUtils.getBasePath;

/**
 * R hosted handlers.
 * @since 3.28
 */
@Named
@Singleton
public final class OrientHostedHandlers
    extends ComponentSupport
{
  /**
   * Handle request for packages.
   */
  final Handler getPackages = context -> {
    String path = extractRequestPath(context);
    OrientRHostedFacet hostedFacet = context.getRepository().facet(OrientRHostedFacet.class);

    Content content;
    if ((content = hostedFacet.getStoredContent(path)) != null
        || (content = hostedFacet.buildAndPutPackagesGz(getBasePath(path))) != null) {
      return HttpResponses.ok(content);
    }

    return HttpResponses.notFound();
  };

  /**
   * Handle request for archive.
   */
  final Handler getArchive = context -> {
    String path = extractRequestPath(context);
    Content content = context.getRepository().facet(OrientRHostedFacet.class).getStoredContent(path);
    if (content != null) {
      return HttpResponses.ok(content);
    }
    return HttpResponses.notFound();
  };

  /**
   * Handle request for upload.
   */
  final Handler putArchive = context -> {
    String path = extractRequestPath(context);
    try {
      validateArchiveUploadPath(path);
    }
    catch (ValidationErrorsException e) {
      return HttpResponses.badRequest(e.getMessage());
    }
    context.getRepository().facet(OrientRHostedFacet.class).upload(path, context.getRequest().getPayload());
    return HttpResponses.ok();
  };
}
