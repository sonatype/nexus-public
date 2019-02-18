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

import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.sonatype.nexus.repository.rest.api.AssetXO;
import org.sonatype.nexus.repository.rest.api.ComponentXO;
import org.sonatype.nexus.repository.rest.internal.resources.SearchResource;
import org.sonatype.nexus.rest.Page;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

import static org.sonatype.nexus.repository.rest.internal.resources.AssetDownloadResponseProcessor.*;
import static org.sonatype.nexus.repository.search.DefaultComponentMetadataProducer.GROUP;
import static org.sonatype.nexus.repository.search.DefaultComponentMetadataProducer.NAME;
import static org.sonatype.nexus.repository.search.DefaultComponentMetadataProducer.VERSION;

/**
 * Swagger documentation for {@link SearchResource}
 *
 * @since 3.4
 */
@Api(value = "search")
public interface SearchResourceDoc
{
  String CONTINUATION_TOKEN_DESCRIPTION = "A token returned by a prior request. If present, the next page of results are returned";
  String SORT_DESCRIPTION = "The field to sort the results against, if left empty, a sort based on match weight will be used.";
  String SEARCH_AND_DL_SORT_DESCRIPTION = "The field to sort the results against, if left empty and more than 1 result is returned, the request will fail.";
  String DIRECTION_DESCRIPTION = "The direction to sort records in, defaults to ascending ('asc') for all sort fields, except version, which defaults to descending ('desc')";

  String ALLOWABLE_SORT_VALUES = GROUP + ", " + NAME + ", " + VERSION + ", repository";
  String ALLOWABLE_SORT_DIRECTIONS = "asc, desc";

  @ApiOperation("Search components")
  Page<ComponentXO> search(
      @ApiParam(value = CONTINUATION_TOKEN_DESCRIPTION, allowEmptyValue = true)
      final String continuationToken,
      @ApiParam(value = SORT_DESCRIPTION, allowEmptyValue = true, allowableValues = ALLOWABLE_SORT_VALUES)
      final String sort,
      @ApiParam(value = DIRECTION_DESCRIPTION, allowEmptyValue = true, allowableValues = ALLOWABLE_SORT_DIRECTIONS)
      final String direction,
      @Context final UriInfo uriInfo);

  @ApiOperation("Search assets")
  Page<AssetXO> searchAssets(
      @ApiParam(value = CONTINUATION_TOKEN_DESCRIPTION)
      final String continuationToken,
      @ApiParam(value = SORT_DESCRIPTION, allowEmptyValue = true, allowableValues = ALLOWABLE_SORT_VALUES)
      final String sort,
      @ApiParam(value = DIRECTION_DESCRIPTION, allowEmptyValue = true, allowableValues = ALLOWABLE_SORT_DIRECTIONS)
      final String direction,
      @Context final UriInfo uriInfo);

  @ApiOperation(value = "Search and download asset",
    notes = "Returns a 302 Found with location header field set to download URL. "
      + "Unless a sort parameter is supplied, the search must return a single asset to receive download URL.")
  @ApiResponses(value = {
      @ApiResponse(code = 400, message = SEARCH_RETURNED_MULTIPLE_ASSETS),
      @ApiResponse(code = 404, message = NO_SEARCH_RESULTS_FOUND)
  })
  Response searchAndDownloadAssets(
      @ApiParam(value = SEARCH_AND_DL_SORT_DESCRIPTION, allowEmptyValue = true, allowableValues = ALLOWABLE_SORT_VALUES)
      final String sort,
      @ApiParam(value = DIRECTION_DESCRIPTION, allowEmptyValue = true, allowableValues = ALLOWABLE_SORT_DIRECTIONS)
      final String direction,
      @Context final UriInfo uriInfo);
}
