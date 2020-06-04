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
package org.sonatype.nexus.content.example.internal.recipe;

import java.util.Optional;

import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.content.example.ExampleContentFacet;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.http.HttpResponses;
import org.sonatype.nexus.repository.view.Content;
import org.sonatype.nexus.repository.view.Context;
import org.sonatype.nexus.repository.view.Handler;
import org.sonatype.nexus.repository.view.Payload;
import org.sonatype.nexus.repository.view.Response;
import org.sonatype.nexus.repository.view.matchers.token.TokenMatcher;

import static com.google.common.base.Preconditions.checkState;
import static org.sonatype.nexus.repository.http.HttpMethods.DELETE;
import static org.sonatype.nexus.repository.http.HttpMethods.GET;
import static org.sonatype.nexus.repository.http.HttpMethods.HEAD;
import static org.sonatype.nexus.repository.http.HttpMethods.PUT;

/**
 * Example format content handler.
 *
 * @since 3.24
 */
@Named
@Singleton
public class ExampleContentHandler
    extends ComponentSupport
    implements Handler
{
  @Override
  public Response handle(final Context context) throws Exception {

    String path = contentPath(context);
    String method = context.getRequest().getAction();
    Repository repository = context.getRepository();

    log.debug("{} repository '{}' content-path: {}", method, repository.getName(), path);

    ExampleContentFacet storage = repository.facet(ExampleContentFacet.class);

    switch (method) {
      case HEAD:
      case GET: {
        Optional<Content> content = storage.get(path);
        if (content.isPresent()) {
          return HttpResponses.ok(content.get());
        }
        return HttpResponses.notFound(path);
      }

      case PUT: {
        Payload content = context.getRequest().getPayload();
        storage.put(path, content);
        return HttpResponses.created();
      }

      case DELETE: {
        boolean deleted = storage.delete(path);
        if (deleted) {
          return HttpResponses.noContent();
        }
        return HttpResponses.notFound(path);
      }

      default:
        return HttpResponses.methodNotAllowed(method, GET, HEAD, PUT, DELETE);
    }
  }

  /**
   * Pull the parsed content path out of the context.
   */
  private static String contentPath(final Context context) {
    TokenMatcher.State state = context.getAttributes().require(TokenMatcher.State.class);
    String path = state.getTokens().get("path");
    checkState(path != null, "Missing token: path");
    return path;
  }
}
