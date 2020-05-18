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

import java.util.List;

import javax.validation.Valid;

import org.sonatype.nexus.repository.rest.api.ContentSelectorApiCreateRequest;
import org.sonatype.nexus.repository.rest.api.ContentSelectorApiResponse;
import org.sonatype.nexus.repository.rest.api.ContentSelectorApiUpdateRequest;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

import static org.sonatype.nexus.repository.http.HttpStatus.BAD_REQUEST;
import static org.sonatype.nexus.repository.http.HttpStatus.FORBIDDEN;
import static org.sonatype.nexus.repository.http.HttpStatus.NO_CONTENT;
import static org.sonatype.nexus.repository.http.HttpStatus.OK;

/**
 * Swagger documentation for {@link org.sonatype.nexus.repository.rest.internal.resources.ContentSelectorsApiResource}
 *
 * @since 3.19
 */
@Api(value = "Content selectors")
public interface ContentSelectorsResourceDoc
{
  String NAME_DESCRIPTION = "The content selector name cannot be changed after creation";

  String TYPE_DESCRIPTION = "The type of content selector the backend is using";

  String TYPE_ALLOWED_VALUES = "csel, jexl";

  String TYPE_NOTES = "All new content selectors will be created as csel selectors, jexl selectors are deprecated";

  String DESCRIPTION_DESCRIPTION = "A human-readable description";

  String EXPRESSION_DESCRIPTION = "The expression used to identify content";

  String EXPRESSION_EXAMPLE = "format == \"maven2\" and path =^ \"/org/sonatype/nexus\"";

  String EXPRESSION_NOTES = "See http://links.sonatype.com/products/nexus/selectors/docs for more details";

  @ApiOperation("List content selectors")
  @ApiResponses({
      @ApiResponse(code = OK, message = "successful operation", response = ContentSelectorApiResponse.class, responseContainer = "List"),
      @ApiResponse(code = FORBIDDEN, message = "Insufficient permissions to read content selectors")
  })
  List<ContentSelectorApiResponse> getContentSelectors();

  @ApiOperation("Create a new content selector")
  @ApiResponses({
      @ApiResponse(code = NO_CONTENT, message = "Content selector successfully created"),
      @ApiResponse(code = BAD_REQUEST, message = "Invalid request"),
      @ApiResponse(code = FORBIDDEN, message = "Insufficient permissions to create content selectors")
  })
  void createContentSelector(@Valid final ContentSelectorApiCreateRequest request);

  @ApiOperation("Get a content selector by name")
  @ApiResponses({
      @ApiResponse(code = OK, message = "successful operation", response = ContentSelectorApiResponse.class),
      @ApiResponse(code = FORBIDDEN, message = "Insufficient permissions to read the content selector")
  })
  ContentSelectorApiResponse getContentSelector(
      @ApiParam(required = true, value = "The content selector name") final String name
  );

  @ApiOperation("Update a content selector")
  @ApiResponses({
      @ApiResponse(code = NO_CONTENT, message = "Content selector updated successfully"),
      @ApiResponse(code = BAD_REQUEST, message = "Invalid request"),
      @ApiResponse(code = FORBIDDEN, message = "Insufficient permissions to update the content selector")
  })
  void updateContentSelector(
      @ApiParam(required = true, value = "The content selector name") final String name,
      @Valid final ContentSelectorApiUpdateRequest contentSelector
  );

  @ApiOperation("Delete a content selector")
  @ApiResponses({
      @ApiResponse(code = NO_CONTENT, message = "Content selector deleted successfully"),
      @ApiResponse(code = BAD_REQUEST, message = "Invalid request"),
      @ApiResponse(code = FORBIDDEN, message = "Insufficient permissions to delete the content selector")
  })
  void deleteContentSelector(final String name);
}
