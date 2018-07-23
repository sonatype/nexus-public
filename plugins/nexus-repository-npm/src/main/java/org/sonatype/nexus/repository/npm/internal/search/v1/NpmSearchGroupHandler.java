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
package org.sonatype.nexus.repository.npm.internal.search.v1;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.group.GroupFacet;
import org.sonatype.nexus.repository.group.GroupHandler;
import org.sonatype.nexus.repository.http.HttpResponses;
import org.sonatype.nexus.repository.http.HttpStatus;
import org.sonatype.nexus.repository.view.ContentTypes;
import org.sonatype.nexus.repository.view.Context;
import org.sonatype.nexus.repository.view.Parameters;
import org.sonatype.nexus.repository.view.Payload;
import org.sonatype.nexus.repository.view.Request;
import org.sonatype.nexus.repository.view.Response;
import org.sonatype.nexus.repository.view.payloads.StringPayload;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.Comparator.comparingDouble;
import static java.util.stream.Collectors.toList;

/**
 * Support for merging npm V1 search results together.
 *
 * @since 3.7
 */
@Named
@Singleton
class NpmSearchGroupHandler
    extends GroupHandler
{
  private final NpmSearchParameterExtractor npmSearchParameterExtractor;

  private final NpmSearchResponseFactory npmSearchResponseFactory;

  private final NpmSearchResponseMapper npmSearchResponseMapper;

  private final int v1SearchMaxResults;

  @Inject
  public NpmSearchGroupHandler(final NpmSearchParameterExtractor npmSearchParameterExtractor,
                               final NpmSearchResponseFactory npmSearchResponseFactory,
                               final NpmSearchResponseMapper npmSearchResponseMapper,
                               @Named("${nexus.npm.v1SearchMaxResults:-250}") final int v1SearchMaxResults)
  {
    this.npmSearchParameterExtractor = checkNotNull(npmSearchParameterExtractor);
    this.npmSearchResponseFactory = checkNotNull(npmSearchResponseFactory);
    this.npmSearchResponseMapper = checkNotNull(npmSearchResponseMapper);
    this.v1SearchMaxResults = v1SearchMaxResults;
  }

  @Override
  protected Response doGet(@Nonnull final Context context,
                           @Nonnull final DispatchedRepositories dispatched)
      throws Exception
  {
    checkNotNull(context);
    checkNotNull(dispatched);

    Request request = context.getRequest();
    Parameters parameters = request.getParameters();
    String text = npmSearchParameterExtractor.extractText(parameters);

    NpmSearchResponse response;
    if (text.isEmpty()) {
      response = npmSearchResponseFactory.buildEmptyResponse();
    }
    else {
      response = searchMembers(context, dispatched, parameters);
    }

    String content = npmSearchResponseMapper.writeString(response);
    return HttpResponses.ok(new StringPayload(content, ContentTypes.APPLICATION_JSON));
  }

  /**
   * Searches the member repositories, returning the merged response for all contacted member repositories that returned
   * a valid search response.
   */
  private NpmSearchResponse searchMembers(final Context context,
                                          final DispatchedRepositories dispatched,
                                          final Parameters parameters) throws Exception
  {
    // preserve original from and size for repeated queries
    int from = npmSearchParameterExtractor.extractFrom(parameters);
    int size = npmSearchParameterExtractor.extractSize(parameters);

    // if the text is empty, then we override the original parameters to return a full set from each upstream source
    // we could make multiple queries to make this more efficient, but this is the simplest implementation for now
    parameters.replace("from", "0");
    parameters.replace("size", Integer.toString(v1SearchMaxResults));

    // sort all the merged results by normalized search score, then build the result responses to send back
    GroupFacet groupFacet = context.getRepository().facet(GroupFacet.class);
    Set<Entry<Repository, Response>> entries = getAll(context, groupFacet.members(), dispatched).entrySet();
    List<NpmSearchResponseObject> mergedResponses = mergeAndNormalizeResponses(entries);
    mergedResponses.sort(comparingDouble(NpmSearchResponseObject::getSearchScore).reversed());
    List<NpmSearchResponseObject> mergedResponseObjects = mergedResponses.stream()
        .skip(from)
        .limit(size)
        .collect(toList());

    return npmSearchResponseFactory.buildResponseForObjects(mergedResponseObjects);
  }

  /**
   * Merges the responses from all the specified repositories, normalizing the search scores. Each package name is only
   * returned once for the first time it is encountered in the search results, with scores for each retained entry being
   * normalized to a scale of [0, 1] and sorted descending.
   */
  private List<NpmSearchResponseObject> mergeAndNormalizeResponses(final Set<Entry<Repository, Response>> entries) {

    Map<String, NpmSearchResponseObject> results = new LinkedHashMap<>();
    for (Entry<Repository, Response> entry : entries) {

      // do NOT ignore the rest of the search results just because we had an issue with ONE response
      NpmSearchResponse searchResponse = parseSearchResponse(entry.getKey(), entry.getValue());
      if (searchResponse == null) {
        continue;
      }

      // should never happen, but if there are no actual objects in the response, just continue with the next one
      List<NpmSearchResponseObject> searchResponseObjects = searchResponse.getObjects();
      if (searchResponseObjects == null) {
        continue;
      }

      // normalize each incoming score based on the first store we obtain from the package entries in the results, since
      // that's going to be the highest score for each batch.
      Double highestScore = null;
      for (NpmSearchResponseObject searchResponseObject : searchResponseObjects) {

        // ensure that we only grab existing objects that have names and scores (should always be present, but just to
        // be safe in the event of semantically incorrect but syntactically valid JSON, we should check and filter them)
        if (!isValidResponseObject(searchResponseObject)) {
          continue;
        }

        // if we do not already have a highest score, we should obtain one from the first valid search response we
        // encounter, using it to normalize the subsequent search scores using this one as the "highest" search score
        if (highestScore == null) {
          highestScore = searchResponseObject.getSearchScore();
        }

        // add this result to the list with a normalized search score from 0 to 1 based on the highest score we first
        // encountered at the start of the responses, assuming we have not already encountered the same package name
        searchResponseObject.setSearchScore(searchResponseObject.getSearchScore() / highestScore);
        results.putIfAbsent(searchResponseObject.getPackageEntry().getName(), searchResponseObject);

      }
    }

    return new ArrayList<>(results.values());
  }

  /**
   * Determines whether or not a response object is valid enough to be processed, i.e. that the appropriate score and
   * name fields, at a minimum, were populated by the sender. This should ensure that we can process the a particular
   * response object without encountering NPEs later on.
   */
  private boolean isValidResponseObject(@Nullable final NpmSearchResponseObject responseObject) {
    return responseObject != null &&
        responseObject.getSearchScore() != null &&
        responseObject.getPackageEntry() != null &&
        responseObject.getPackageEntry().getName() != null;
  }

  /**
   * Parses a search response, returning the marshaled {@link NpmSearchResponse}.
   */
  @Nullable
  private NpmSearchResponse parseSearchResponse(final Repository repository, final Response response) {
    Payload payload = response.getPayload();
    if (response.getStatus().getCode() == HttpStatus.OK && payload != null) {
      try (InputStream in = payload.openInputStream()) {
        return npmSearchResponseMapper.readFromInputStream(in);
      }
      catch (IOException e) {
        if (log.isDebugEnabled()) {
          log.warn("Unable to process search response for repository {}, skipping", repository.getName(), e);
        }
        else {
          log.warn("Unable to process search response for repository {}, cause: {}, skipping", repository.getName(),
              e.getMessage());
        }
      }
    }
    return null;
  }
}
