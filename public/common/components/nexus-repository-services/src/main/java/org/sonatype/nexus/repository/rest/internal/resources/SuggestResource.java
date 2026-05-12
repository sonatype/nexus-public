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
package org.sonatype.nexus.repository.rest.internal.resources;

import java.util.List;
import java.util.stream.Collectors;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

import org.sonatype.nexus.repository.rest.api.SuggestXO;
import org.sonatype.nexus.repository.search.ComponentSearchResult;
import org.sonatype.nexus.repository.search.SearchRequest;
import org.sonatype.nexus.repository.search.SearchResponse;
import org.sonatype.nexus.repository.search.SearchService;
import org.sonatype.nexus.rest.Resource;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Preconditions.checkNotNull;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.sonatype.nexus.common.app.FeatureFlags.PREVIEW_UI_SETTINGS_ENABLED;
import static org.sonatype.nexus.rest.APIConstants.V1_API_PREFIX;

/**
 * Lightweight search suggestions endpoint for autocomplete/typeahead functionality.
 * Returns minimal component data to optimize response size and latency.
 */
@Component
@Singleton
@ConditionalOnProperty(name = PREVIEW_UI_SETTINGS_ENABLED, havingValue = "true", matchIfMissing = true)
@Path(SuggestResource.RESOURCE_URI)
@Produces(APPLICATION_JSON)
@Consumes(APPLICATION_JSON)
@Api(value = "Search", description = "Search suggestions for autocomplete")
public class SuggestResource
    implements Resource
{
  protected final Logger log = LoggerFactory.getLogger(getClass());

  public static final String RESOURCE_URI = V1_API_PREFIX + "/search/suggest";

  private static final int DEFAULT_LIMIT = 10;

  private static final int MAX_LIMIT = 20;

  private static final int MIN_QUERY_LENGTH = 2;

  private final SearchService searchService;

  @Inject
  public SuggestResource(final SearchService searchService) {
    this.searchService = checkNotNull(searchService);
  }

  @GET
  @ApiOperation(
      value = "Get search suggestions for autocomplete",
      notes = "Returns lightweight component suggestions matching the query. " +
          "Optimized for fast typeahead with minimal response payload.",
      response = SuggestXO.class,
      responseContainer = "List")
  @ApiResponses({
      @ApiResponse(code = 200, message = "Suggestions returned successfully"),
      @ApiResponse(code = 400, message = "Query too short (minimum 2 characters)")
  })
  public List<SuggestXO> suggest(
      @ApiParam(value = "Search query (minimum 2 characters)", required = true) @QueryParam("q") final String query,

      @ApiParam(value = "Filter by format (e.g., maven2, npm, pypi)") @QueryParam("format") final String format,

      @ApiParam(
          value = "Maximum number of suggestions (default: 10, max: 20)") @QueryParam("limit") final Integer limit)
  {
    // Validate query length
    if (query == null || query.trim().length() < MIN_QUERY_LENGTH) {
      return List.of(); // Return empty for short queries
    }

    // Determine limit
    int effectiveLimit = DEFAULT_LIMIT;
    if (limit != null && limit > 0) {
      effectiveLimit = Math.min(limit, MAX_LIMIT);
    }

    // Build search request - NO assets, minimal data
    SearchRequest.Builder requestBuilder = SearchRequest.builder()
        .searchFilter("keyword", query.trim())
        .limit(effectiveLimit);
    // Note: NOT calling .includeAssets() - this keeps response small

    // Add format filter if specified
    if (format != null && !format.isBlank()) {
      requestBuilder.searchFilter("format", format.trim());
    }

    SearchRequest request = requestBuilder.build();
    SearchResponse response = searchService.search(request);

    // Map to lightweight DTOs
    return response.getSearchResults()
        .stream()
        .map(this::toSuggestXO)
        .collect(Collectors.toList());
  }

  private SuggestXO toSuggestXO(final ComponentSearchResult result) {
    return new SuggestXO(
        result.getId(),
        result.getName(),
        result.getGroup(),
        result.getVersion(),
        result.getFormat(),
        result.getRepositoryName());
  }
}
