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
package org.sonatype.nexus.api.rest.selfhosted.support;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.StreamingOutput;

import org.sonatype.nexus.internal.support.SupportZipXO;
import org.sonatype.nexus.rest.Resource;
import org.sonatype.nexus.supportzip.SupportZipGenerator;
import org.sonatype.nexus.common.log.SupportZipGeneratorRequest;
import org.sonatype.nexus.supportzip.SupportZipGenerator.Result;

import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;

import java.text.SimpleDateFormat;
import java.util.Date;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Preconditions.checkNotNull;
import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;
import static jakarta.ws.rs.core.MediaType.APPLICATION_OCTET_STREAM;
import static org.sonatype.nexus.common.supportzip.SupportZipConstants.REST_SUPPORT_RESOURCE_URI;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Resource for support API.
 *
 * @since 3.13
 */
@Component
@Path(REST_SUPPORT_RESOURCE_URI)
@Tag(name = "Support")
public class SupportResource
    implements Resource
{
  protected final Logger log = LoggerFactory.getLogger(getClass());

  private final SupportZipGenerator supportZipGenerator;

  @Autowired
  public SupportResource(final SupportZipGenerator supportZipGenerator) {
    this.supportZipGenerator = checkNotNull(supportZipGenerator);
  }

  @RequiresAuthentication
  @RequiresPermissions("nexus:atlas:create")
  @Operation(summary = "Creates and downloads a support zip")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "successful operation"),
      @ApiResponse(responseCode = "403", description = "Insufficient permissions to generate support zip")
  })
  @Consumes(APPLICATION_JSON)
  @Produces(APPLICATION_OCTET_STREAM)
  @POST
  @Path("/supportzip")
  public Response supportzip(final SupportZipGeneratorRequest request) {
    String name = "support-" + new SimpleDateFormat("yyyyMMdd-HHmmss").format(new Date()) + "-1.zip";

    StreamingOutput entity = output -> supportZipGenerator.generate(request, "support", output);
    return Response.ok(entity).header("Content-Disposition", "attachment; filename=\"" + name + "\"").build();
  }

  @RequiresAuthentication
  @RequiresPermissions("nexus:atlas:create")
  @Operation(summary = "Creates a support zip and returns the path")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "successful operation",
          content = @Content(schema = @Schema(implementation = SupportZipXO.class))),
      @ApiResponse(responseCode = "403", description = "Insufficient permissions to generate support zip")
  })
  @Consumes(APPLICATION_JSON)
  @Produces(APPLICATION_JSON)
  @POST
  @Path("/supportzippath")
  public SupportZipXO supportzippath(final SupportZipGeneratorRequest request) {
    Result result = supportZipGenerator.generate(request);
    return new SupportZipXO(result.getLocalPath(), result.getFilename(), result.getSize(), result.isTruncated());
  }
}
