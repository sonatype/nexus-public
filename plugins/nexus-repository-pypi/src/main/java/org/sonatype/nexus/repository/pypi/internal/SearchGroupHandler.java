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
package org.sonatype.nexus.repository.pypi.internal;

import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

import javax.annotation.Nonnull;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.group.GroupFacet;
import org.sonatype.nexus.repository.group.GroupHandler;
import org.sonatype.nexus.repository.http.HttpResponses;
import org.sonatype.nexus.repository.http.HttpStatus;
import org.sonatype.nexus.repository.view.ContentTypes;
import org.sonatype.nexus.repository.view.Context;
import org.sonatype.nexus.repository.view.Payload;
import org.sonatype.nexus.repository.view.Request;
import org.sonatype.nexus.repository.view.Request.Builder;
import org.sonatype.nexus.repository.view.Response;
import org.sonatype.nexus.repository.view.payloads.BytesPayload;
import org.sonatype.nexus.repository.view.payloads.StringPayload;

import com.google.common.io.ByteStreams;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.sonatype.nexus.repository.http.HttpMethods.POST;
import static org.sonatype.nexus.repository.pypi.internal.PyPiSearchUtils.buildSearchResponse;
import static org.sonatype.nexus.repository.pypi.internal.PyPiSearchUtils.parseSearchResponse;
import static org.sonatype.nexus.repository.view.ViewUtils.copyLocalContextAttributes;

/**
 * Support for merging PyPI XML-RPC search results together.
 *
 * @since 3.1
 */
@Named
@Singleton
class SearchGroupHandler
    extends GroupHandler
{
  @Nonnull
  @Override
  public Response handle(@Nonnull final Context context) throws Exception {
    checkNotNull(context);
    final String method = context.getRequest().getAction();
    if (POST.equals(method)) {
      final DispatchedRepositories dispatched = context.getRequest().getAttributes()
          .getOrCreate(DispatchedRepositories.class);
      return doPost(context, dispatched);
    }
    return HttpResponses.methodNotAllowed(method, POST);
  }

  protected Response doPost(@Nonnull final Context context,
                            @Nonnull final GroupHandler.DispatchedRepositories dispatched)
      throws Exception
  {
    checkNotNull(context);
    checkNotNull(dispatched);

    Context replayableContext = buildReplayableContext(context);
    GroupFacet groupFacet = context.getRepository().facet(GroupFacet.class);

    Map<String, PyPiSearchResult> results = new LinkedHashMap<>();
    for (Entry<Repository, Response> entry : getAll(replayableContext, groupFacet.members(), dispatched).entrySet()) {
      Response response = entry.getValue();
      if (response.getStatus().getCode() == HttpStatus.OK && response.getPayload() != null) {
        processResponse(response, results);
      }
    }

    String response = buildSearchResponse(results.values());
    return HttpResponses.ok(new StringPayload(response, ContentTypes.TEXT_HTML));
  }

  /**
   * Processes a search response, adding any new entries to the map. If an entry exists already, it is left untouched
   * to preserve the ordering of results from member repositories.
   */
  private void processResponse(final Response response, final Map<String, PyPiSearchResult> results) throws Exception {
    checkNotNull(response);
    checkNotNull(results);
    Payload payload = checkNotNull(response.getPayload());
    try (InputStream in = payload.openInputStream()) {
      for (PyPiSearchResult result : parseSearchResponse(in)) {
        String key = result.getName() + " " + result.getVersion();
        if (!results.containsKey(key)) {
          results.put(key, result);
        }
      }
    }
  }

  /**
   * Builds a context that contains a request that can be "replayed" with its post body content. Since we're repeating
   * the same post request to all the search endpoints, we need to have a new request instance with a payload we can
   * read multiple times.
   */
  private Context buildReplayableContext(final Context context) throws IOException {
    checkNotNull(context);
    Request request = checkNotNull(context.getRequest());
    Payload payload = checkNotNull(request.getPayload());
    try (InputStream in = payload.openInputStream()) {
      byte[] content = ByteStreams.toByteArray(in);
      Context replayableContext = new Context(context.getRepository(),
          new Builder()
              .attributes(request.getAttributes())
              .headers(request.getHeaders())
              .action(request.getAction())
              .path(request.getPath())
              .parameters(request.getParameters())
              .payload(new BytesPayload(content, payload.getContentType()))
              .build());

      copyLocalContextAttributes(context, replayableContext);

      return replayableContext;
    }
  }
}
