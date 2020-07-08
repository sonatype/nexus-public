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
package org.sonatype.nexus.content.maven.internal.recipe;

import java.io.IOException;

import javax.annotation.Nonnull;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.content.maven.MavenContentFacet;
import org.sonatype.nexus.repository.IllegalOperationException;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.http.HttpResponses;
import org.sonatype.nexus.repository.maven.LayoutPolicy;
import org.sonatype.nexus.repository.maven.MavenPath;
import org.sonatype.nexus.repository.maven.MavenPath.Coordinates;
import org.sonatype.nexus.repository.view.Context;
import org.sonatype.nexus.repository.view.Handler;
import org.sonatype.nexus.repository.view.Response;

import static org.sonatype.nexus.repository.http.HttpMethods.DELETE;
import static org.sonatype.nexus.repository.http.HttpMethods.GET;
import static org.sonatype.nexus.repository.http.HttpMethods.HEAD;
import static org.sonatype.nexus.repository.http.HttpMethods.PUT;

/**
 * @since 3.25.0
 */
@Named
@Singleton
public class MavenContentHandler
    extends ComponentSupport
    implements Handler
{
  @Override
  public Response handle(@Nonnull final Context context) throws Exception {
    MavenPath mavenPath = contentPath(context);
    String path = mavenPath.getPath();
    String method = context.getRequest().getAction();
    Repository repository = context.getRepository();

    log.debug("{} repository '{}' content-path: {}", method, repository.getName(), path);

    MavenContentFacet storage = repository.facet(MavenContentFacet.class);

    switch (method) {
      case HEAD:
      case GET:
        return doGet(path, storage);

      case PUT:
        doPut(context, mavenPath, storage);
        return HttpResponses.created();

      case DELETE:
        return doDelete(mavenPath, storage);

      default:
        return HttpResponses.methodNotAllowed(method, GET, HEAD, PUT, DELETE);
    }
  }

  private MavenPath contentPath(@Nonnull final Context context) {
    return context.getAttributes().require(MavenPath.class);
  }

  private Response doGet(final String path, final MavenContentFacet storage) throws IOException {
    return storage
        .get(path)
        .map(HttpResponses::ok)
        .orElseGet(() -> HttpResponses.notFound(path));
  }

  private void doPut(
      @Nonnull final Context context,
      final MavenPath mavenPath,
      final MavenContentFacet storage)
      throws IOException
  {
    validatePathForStrictLayoutPolicy(mavenPath, storage);
    storage.put(mavenPath, context.getRequest().getPayload());
  }

  private void validatePathForStrictLayoutPolicy(final MavenPath mavenPath, final MavenContentFacet storage) {
    if (storage.layoutPolicy() == LayoutPolicy.STRICT
        && isValidSnapshot(mavenPath.getCoordinates())
        && !storage.getMavenPathParser().isRepositoryMetadata(mavenPath)) {
      throw new IllegalOperationException(" Invalid mavenPath for a Maven 2 repository");
    }
  }

  private Response doDelete(final MavenPath mavenPath, final MavenContentFacet storage) throws IOException {
    boolean deleted = storage.delete(mavenPath);
    if (deleted) {
      return HttpResponses.noContent();
    }
    return HttpResponses.notFound(mavenPath.getPath());
  }

  private boolean isValidSnapshot(Coordinates coordinates) {
    return coordinates == null || (coordinates.isSnapshot() &&
        !coordinates.getVersion().equals(coordinates.getBaseVersion()) &&
        (coordinates.getTimestamp() == null || coordinates.getBuildNumber() == null));
  }
}
