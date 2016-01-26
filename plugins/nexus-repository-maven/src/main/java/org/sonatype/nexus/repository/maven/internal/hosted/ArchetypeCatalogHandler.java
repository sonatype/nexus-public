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
package org.sonatype.nexus.repository.maven.internal.hosted;

import java.io.IOException;

import javax.annotation.Nonnull;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.http.HttpResponses;
import org.sonatype.nexus.repository.maven.MavenFacet;
import org.sonatype.nexus.repository.maven.MavenHostedFacet;
import org.sonatype.nexus.repository.maven.MavenPath;
import org.sonatype.nexus.repository.maven.internal.MavenFacetUtils;
import org.sonatype.nexus.repository.view.Content;
import org.sonatype.nexus.repository.view.Context;
import org.sonatype.nexus.repository.view.Handler;
import org.sonatype.nexus.repository.view.Response;

import static org.sonatype.nexus.repository.http.HttpMethods.GET;
import static org.sonatype.nexus.repository.http.HttpMethods.HEAD;

/**
 * Maven hosted archetype catalog handler.
 *
 * @since 3.0
 */
@Singleton
@Named
public class ArchetypeCatalogHandler
    extends ComponentSupport
    implements Handler
{
  @Nonnull
  @Override
  public Response handle(@Nonnull final Context context) throws Exception {
    MavenPath path = context.getAttributes().require(MavenPath.class);
    Repository repository = context.getRepository();
    String action = context.getRequest().getAction();
    switch (action) {
      case GET:
      case HEAD:
        return doGet(path, repository);

      default:
        return HttpResponses.methodNotAllowed(context.getRequest().getAction(), GET, HEAD);
    }
  }

  /**
   * Tries to get the catalog from hosted repository, and generate it if not present.
   */
  private Response doGet(final MavenPath path, final Repository repository) throws IOException {
    MavenFacet mavenFacet = repository.facet(MavenFacet.class);
    Content content = mavenFacet.get(path);
    if (content == null) {
      // try to generate it
      repository.facet(MavenHostedFacet.class).rebuildArchetypeCatalog();
      content = mavenFacet.get(path);
      if (content == null) {
        return HttpResponses.notFound(path.getPath());
      }
    }
    MavenFacetUtils.mayAddETag(content);
    return HttpResponses.ok(content);
  }
}
