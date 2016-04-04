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
import org.sonatype.nexus.repository.IllegalOperationException;
import org.sonatype.nexus.repository.http.HttpResponses;
import org.sonatype.nexus.repository.maven.MavenFacet;
import org.sonatype.nexus.repository.maven.MavenPath;
import org.sonatype.nexus.repository.maven.internal.MavenFacetUtils;
import org.sonatype.nexus.repository.maven.LayoutPolicy;
import org.sonatype.nexus.repository.view.Content;
import org.sonatype.nexus.repository.view.Context;
import org.sonatype.nexus.repository.view.Handler;
import org.sonatype.nexus.repository.view.Response;

import static org.sonatype.nexus.repository.http.HttpMethods.DELETE;
import static org.sonatype.nexus.repository.http.HttpMethods.GET;
import static org.sonatype.nexus.repository.http.HttpMethods.HEAD;
import static org.sonatype.nexus.repository.http.HttpMethods.PUT;

/**
 * Maven hosted handler.
 *
 * @since 3.0
 */
@Singleton
@Named
public class HostedHandler
    extends ComponentSupport
    implements Handler
{
  @Nonnull
  @Override
  public Response handle(@Nonnull final Context context) throws Exception {
    MavenPath path = context.getAttributes().require(MavenPath.class);
    MavenFacet mavenFacet = context.getRepository().facet(MavenFacet.class);
    String action = context.getRequest().getAction();
    switch (action) {
      case GET:
      case HEAD:
        return doGet(path, mavenFacet);

      case PUT:
        return doPut(context, path, mavenFacet);

      case DELETE:
        return doDelete(path, mavenFacet);

      default:
        return HttpResponses.methodNotAllowed(context.getRequest().getAction(), GET, HEAD, PUT, DELETE);
    }
  }

  private Response doGet(final MavenPath path, final MavenFacet mavenFacet) throws IOException {
    Content content = mavenFacet.get(path);
    if (content == null) {
      return HttpResponses.notFound(path.getPath());
    }
    MavenFacetUtils.mayAddETag(content);
    return HttpResponses.ok(content);
  }

  private Response doPut(@Nonnull final Context context, final MavenPath path, final MavenFacet mavenFacet)
      throws IOException
  {
    if (mavenFacet.layoutPolicy() == LayoutPolicy.STRICT
        && path.getCoordinates() == null && !mavenFacet.getMavenPathParser().isRepositoryMetadata(path)) {
      throw new IllegalOperationException("Invalid path for a Maven 2 repository");
    }
    mavenFacet.put(path, context.getRequest().getPayload());
    return HttpResponses.created();
  }

  private Response doDelete(final MavenPath path, final MavenFacet mavenFacet) throws IOException {
    boolean deleted = mavenFacet.delete(path);
    if (!deleted) {
      return HttpResponses.notFound(path.getPath());
    }
    return HttpResponses.noContent();
  }
}
