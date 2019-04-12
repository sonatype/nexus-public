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
package org.sonatype.nexus.coreui;

import java.io.IOException;
import java.util.Arrays;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.coreui.internal.UploadService;
import org.sonatype.nexus.repository.upload.UploadConfiguration;
import org.sonatype.nexus.rest.Resource;
import org.sonatype.nexus.validation.Validate;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.shiro.authz.annotation.RequiresPermissions;

import static com.softwarementors.extjs.djn.EncodingUtils.htmlEncode;

/*
 * Endpoint used by the Nexus RM UI for component uploads
 *
 * @since 3.16
 */
@Named
@Singleton
@Path(UploadResource.RESOURCE_PATH)
public class UploadResource extends ComponentSupport implements Resource
{
  public static final String RESOURCE_PATH = "internal/ui/upload";

  private UploadService uploadService;

  private UploadConfiguration configuration;

  private ObjectMapper objectMapper;

  @Inject
  public UploadResource(final UploadService uploadService, final UploadConfiguration configuration, final ObjectMapper objectMapper) {
    this.uploadService = uploadService;
    this.configuration = configuration;
    this.objectMapper = objectMapper;
  }

  @Timed
  @ExceptionMetered
  @Validate
  @POST
  @Path("{repositoryName}")
  @Consumes(MediaType.MULTIPART_FORM_DATA)
  @Produces(MediaType.APPLICATION_JSON)
  @RequiresPermissions("nexus:component:add")
  public Response postComponent(@PathParam("repositoryName") final String repositoryName,
                                @Context final HttpServletRequest request)
      throws IOException
  {
    try {
      if (!configuration.isEnabled()) {
        throw new WebApplicationException(Response.Status.NOT_FOUND);
      }
      Packet responseJson = new Packet(uploadService.upload(repositoryName, request));
      return Response.ok().type(MediaType.TEXT_HTML_TYPE)
          .entity(htmlWrap(objectMapper.writeValueAsString(responseJson))).build();
    }
    catch (Exception e) {
      log.error("Unable to perform upload to repository {}", repositoryName, e);
      ErrorPacket responseJson = new ErrorPacket(e.getMessage());
      return Response.ok().type(MediaType.TEXT_HTML_TYPE)
          .entity(htmlWrap(objectMapper.writeValueAsString(Arrays.asList(responseJson)))).build();
    }
  }

  public static class Packet
  {
    private String data;

    public Packet(final String data) {
      this.data = data;
    }

    public boolean isSuccess() {
      return true;
    }

    public String getData() {
      return data;
    }
  }

  public static class ErrorPacket
  {
    private String message;

    public ErrorPacket(final String message) {
      this.message = message;
    }

    public boolean isSuccess() {
      return false;
    }

    public int getTid() {
      return 1;
    }

    public String getAction() {
      return "upload";
    }

    public String getMethod() {
      return "upload";
    }

    public String getType() {
      return "rpc";
    }

    public String getMessage() {
      return message;
    }
  }

  private String htmlWrap(final String contents) {
    return "<html><body><textarea>" + htmlEncode(contents) + "</textarea></body></html>";
  }
}
