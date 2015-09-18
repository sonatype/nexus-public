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

import java.io.IOException;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.common.hash.HashAlgorithm;
import org.sonatype.nexus.repository.http.HttpResponses;
import org.sonatype.nexus.repository.maven.MavenFacet;
import org.sonatype.nexus.repository.maven.MavenPath;
import org.sonatype.nexus.repository.util.TypeTokens;
import org.sonatype.nexus.repository.view.Content;
import org.sonatype.nexus.repository.view.Context;
import org.sonatype.nexus.repository.view.Handler;
import org.sonatype.nexus.repository.view.Response;

import com.google.common.hash.HashCode;

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
    final MavenPath path = context.getAttributes().require(MavenPath.class);
    final MavenFacet mavenFacet = context.getRepository().facet(MavenFacet.class);
    final String action = context.getRequest().getAction();
    switch (action) {
      case GET:
      case HEAD: {
        return doGet(path, mavenFacet);
      }

      case PUT: {
        return doPut(context, path, mavenFacet);
      }

      case DELETE: {
        return doDelete(path, mavenFacet);
      }

      default:
        return HttpResponses.methodNotAllowed(context.getRequest().getAction(), GET, HEAD, PUT, DELETE);
    }
  }

  private Response doGet(final MavenPath path, final MavenFacet mavenFacet) throws IOException {
    final Content content = mavenFacet.get(path);
    if (content == null) {
      return HttpResponses.notFound(path.getPath());
    }
    mayAddETag(content);
    return HttpResponses.ok(content);
  }

  private Response doPut(final @Nonnull Context context, final MavenPath path, final MavenFacet mavenFacet)
      throws IOException
  {
    mavenFacet.put(path, context.getRequest().getPayload());
    return HttpResponses.created();
  }

  private Response doDelete(final MavenPath path, final MavenFacet mavenFacet) throws IOException {
    final boolean deleted = mavenFacet.delete(path);
    if (!deleted) {
      return HttpResponses.notFound(path.getPath());
    }
    return HttpResponses.noContent();
  }

  /**
   * Adds {@link Content#CONTENT_ETAG} content attribute if not present. In case of hosted repositories, this is safe
   * and even good thing to do, as the content is hosted here only and NX is content authority.
   */
  private void mayAddETag(final Content content) {
    if (content.getAttributes().contains(Content.CONTENT_ETAG)) {
      return;
    }
    final Map<HashAlgorithm, HashCode> hashCodes = content.getAttributes()
        .require(Content.CONTENT_HASH_CODES_MAP, TypeTokens.HASH_CODES_MAP);
    final HashCode sha1HashCode = hashCodes.get(HashAlgorithm.SHA1);
    if (sha1HashCode != null) {
      content.getAttributes().set(Content.CONTENT_ETAG, "{SHA1{" + sha1HashCode.toString() + "}}");
    }
  }
}
