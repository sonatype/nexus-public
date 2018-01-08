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
package org.sonatype.nexus.repository.browse.internal.resources.doc;

import java.io.IOException;

import org.sonatype.nexus.repository.browse.api.ComponentXO;
import org.sonatype.nexus.repository.browse.internal.resources.ComponentsResource;
import org.sonatype.nexus.rest.Page;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.jboss.resteasy.annotations.providers.multipart.MultipartForm;
import org.jboss.resteasy.plugins.providers.multipart.MultipartInput;

/**
 * Swagger documentation for {@link ComponentsResource}
 *
 * @since 3.4.0
 */
@Api(value = "components")
public interface ComponentsResourceDoc
{
  @ApiOperation("List components")
  @ApiResponses(value = {
      @ApiResponse(code = 403, message = "Insufficient permissions to list components"),
      @ApiResponse(code = 422, message = "Parameter 'repository' is required")
  })
  Page<ComponentXO> getComponents(
      @ApiParam(value = "A token returned by a prior request. If present, the next page of results are returned")
      final String continuationToken,

      @ApiParam(value = "Repository from which you would like to retrieve components", required = true)
      final String repository);

  @ApiOperation("Get a single component")
  @ApiResponses(value = {
      @ApiResponse(code = 403, message = "Insufficient permissions to get component"),
      @ApiResponse(code = 404, message = "Component not found"),
      @ApiResponse(code = 422, message = "Malformed ID")
  })
  ComponentXO getComponentById(@ApiParam(value = "ID of the component to retrieve") final String id);

  @ApiOperation(value = "Delete a single component")
  @ApiResponses(value = {
      @ApiResponse(code = 204, message = "Component was successfully deleted"),
      @ApiResponse(code = 403, message = "Insufficient permissions to delete component"),
      @ApiResponse(code = 404, message = "Component not found"),
      @ApiResponse(code = 422, message = "Malformed ID")
  })
  void deleteComponent(@ApiParam(value = "ID of the component to delete") final String id);

  @ApiOperation(value = "Upload a single component",
      notes = "This API takes a multi-part form whose schema varies by repository type, therefore it's virtually " +
          "impossible to be described by the generated documentation. Instead, here's a couple of examples using curl.\n\n" +
          "curl -X POST http://nexus.local/service/rest/beta/components?repository=raw -F directory=example -F asset=@README.md -F asset.filename=readme.md\n\n" +
          "curl -X POST http://nexus.local/service/rest/beta/components?repository=nuget -F asset=@nunit.3.9.0.nupkg\n\n" +
          "curl -X POST http://nexus.local/service/rest/beta/components?repository=maven -F file=@aopalliance-1.0.jar -F file.extension=jar -F groupId=aopalliance -F artifactId=aopalliance -F version=1.0\n\n" +
          "curl -X POST http://nexus.local/service/rest/beta/components?repository=maven -F pom=@aopalliance-1.0.pom -F pom.extension=pom -F sources=@aopalliance-1.0-sources.jar -F sources.classifier=sources -F sources.extension=jar -F asset=aopalliance-1.0.jar -F asset.extension=jar\n\n"
  )
  @ApiResponses(value = {
      @ApiResponse(code = 403, message = "Insufficient permissions to upload a component"),
      @ApiResponse(code = 422, message = "Parameter 'repository' is required")
  })
  void uploadComponent(
      @ApiParam(value = "Name of the repository to which you would like to upload the component", required = true)
      final String repository,
      @ApiParam(value = "Form containing the component") @MultipartForm MultipartInput multipartInput)
      throws IOException;
}
