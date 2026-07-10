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
package org.sonatype.nexus.repository.rest.internal.resources.doc;

import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;

import org.sonatype.nexus.repository.rest.api.AssetXO;
import org.sonatype.nexus.repository.rest.api.ComponentXO;
import org.sonatype.nexus.repository.rest.internal.resources.SearchResource;
import org.sonatype.nexus.rest.Page;

import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

import static org.sonatype.nexus.repository.rest.internal.resources.AssetDownloadResponseProcessor.NO_SEARCH_RESULTS_FOUND;
import static org.sonatype.nexus.repository.rest.internal.resources.AssetDownloadResponseProcessor.SEARCH_RETURNED_MULTIPLE_ASSETS;
import static org.sonatype.nexus.repository.search.index.SearchConstants.GROUP;
import static org.sonatype.nexus.repository.search.index.SearchConstants.NAME;
import static org.sonatype.nexus.repository.search.index.SearchConstants.VERSION;

/**
 * Swagger documentation for {@link SearchResource}
 *
 * @since 3.4
 */
@Tag(name = "Search")
public interface SearchResourceDoc
{
  String SQL_SEARCH_RESTRICTIONS =
      "All searches require at least one criterion of at least three characters before a trailing wildcard (\\*) and cannot start with a wildcard (\\*). "
          +
          "Enclose your criteria in quotation marks to search an exact phrase; otherwise, search criteria will be split by any commas, spaces, dashes, or forward slashes.";

  String CONTINUATION_TOKEN_DESCRIPTION =
      "A token returned by a prior request. If present, the next page of results are returned";

  String SORT_DESCRIPTION =
      "The field to sort the results against, if left empty, a sort based on match weight will be used.";

  String SEARCH_AND_DL_SORT_DESCRIPTION =
      "The field to sort the results against, if left empty and more than 1 result is returned, the request will fail.";

  String DIRECTION_DESCRIPTION =
      "The direction to sort records in, defaults to ascending ('asc') for all sort fields, except version, which defaults to descending ('desc')";

  String TIMEOUT_DESCRIPTION =
      "How long to wait for search results in seconds. If this value is not provided, the system default timeout will be used.";

  String ALLOWABLE_SORT_VALUES = GROUP + ", " + NAME + ", " + VERSION + ", repository";

  String ALLOWABLE_SORT_DIRECTIONS = "asc, desc";

  @Operation(summary = "Search components", description = SQL_SEARCH_RESTRICTIONS)
  Page<ComponentXO> search(
      @Parameter(description = CONTINUATION_TOKEN_DESCRIPTION, allowEmptyValue = true) final String continuationToken,
      @Parameter(description = SORT_DESCRIPTION, allowEmptyValue = true,
          schema = @Schema(allowableValues = {GROUP, NAME, VERSION, "repository"})) final String sort,
      @Parameter(description = DIRECTION_DESCRIPTION, allowEmptyValue = true,
          schema = @Schema(allowableValues = {"asc", "desc"})) final String direction,
      @Parameter(description = TIMEOUT_DESCRIPTION, allowEmptyValue = true) final Integer timeout,
      @Context final UriInfo uriInfo);

  @Operation(summary = "Search assets", description = SQL_SEARCH_RESTRICTIONS)
  Page<AssetXO> searchAssets(
      @Parameter(description = CONTINUATION_TOKEN_DESCRIPTION) final String continuationToken,
      @Parameter(description = SORT_DESCRIPTION, allowEmptyValue = true,
          schema = @Schema(allowableValues = {GROUP, NAME, VERSION, "repository"})) final String sort,
      @Parameter(description = DIRECTION_DESCRIPTION, allowEmptyValue = true,
          schema = @Schema(allowableValues = {"asc", "desc"})) final String direction,
      @Parameter(description = TIMEOUT_DESCRIPTION, allowEmptyValue = true) final Integer timeout,
      @Context final UriInfo uriInfo);

  @Operation(summary = "Search and download asset",
      description = "Returns a 302 Found with location header field set to download URL. "
          + "Unless a sort parameter is supplied, the search must return a single asset to receive download URL.")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "400",
          description = "ValidationErrorXO{id='*', message='" + SEARCH_RETURNED_MULTIPLE_ASSETS + "'}"),
      @ApiResponse(responseCode = "404", description = NO_SEARCH_RESULTS_FOUND)
  })
  Response searchAndDownloadAssets(
      @Parameter(description = SEARCH_AND_DL_SORT_DESCRIPTION, allowEmptyValue = true,
          schema = @Schema(allowableValues = {GROUP, NAME, VERSION, "repository"})) final String sort,
      @Parameter(description = DIRECTION_DESCRIPTION, allowEmptyValue = true,
          schema = @Schema(allowableValues = {"asc", "desc"})) final String direction,
      @Parameter(description = TIMEOUT_DESCRIPTION, allowEmptyValue = true) final Integer timeout,
      @Context final UriInfo uriInfo);
}
