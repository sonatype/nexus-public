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

import org.springframework.beans.factory.annotation.Autowired;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

import org.sonatype.nexus.coreui.internal.UploadService;
import org.sonatype.nexus.repository.ConcurrentOperationException;
import org.sonatype.nexus.repository.IllegalOperationException;
import org.sonatype.nexus.repository.RedeployDisabledException;
import org.sonatype.nexus.rest.Resource;
import org.sonatype.nexus.rest.ValidationErrorsException;
import org.sonatype.nexus.validation.Validate;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.softwarementors.extjs.djn.EncodingUtils.htmlEncode;
import org.springframework.stereotype.Component;

/*
 * Endpoint used by the Nexus RM UI for component uploads
 *
 * @since 3.16
 */
@Component
@Path(UploadResource.RESOURCE_PATH)
public class UploadResource
    implements Resource
{
  protected final Logger log = LoggerFactory.getLogger(getClass());

  public static final String RESOURCE_PATH = "internal/ui/upload";

  private UploadService uploadService;

  private ObjectMapper objectMapper;

  @Autowired
  public UploadResource(
      final UploadService uploadService,
      final ObjectMapper objectMapper)
  {
    this.uploadService = uploadService;
    this.objectMapper = objectMapper;
  }

  @Timed
  @ExceptionMetered
  @Validate
  @POST
  @Path("{repositoryName}")
  @Consumes(MediaType.MULTIPART_FORM_DATA)
  @Produces(MediaType.APPLICATION_JSON)
  @RequiresPermissions("nexus:component:create")
  public Response postComponent(
      @PathParam("repositoryName") final String repositoryName,
      @Context final HttpServletRequest request) throws IOException
  {
    return doUpload(repositoryName, request, false);
  }

  @Timed
  @ExceptionMetered
  @Validate
  @POST
  @Path("{repositoryName}")
  @Consumes(MediaType.MULTIPART_FORM_DATA)
  @Produces(MediaType.TEXT_HTML)
  @RequiresPermissions("nexus:component:create")
  public Response postComponentWithHtmlResponse(
      @PathParam("repositoryName") final String repositoryName,
      @Context final HttpServletRequest request) throws IOException
  {
    return doUpload(repositoryName, request, true);
  }

  /**
   * NEXUS-53344: shared upload entry point. The body is the same ExtJS-RPC envelope the UI has
   * always parsed (a {@link Packet} on success or a single-element array of {@link ErrorPacket}
   * on failure); only the HTTP status differs.
   *
   * <p>
   * {@link IllegalOperationException} (e.g. read-only deployment policy, duplicate-asset on a
   * hosted format) is mapped to HTTP 400 to match {@code ComponentsResource}. Every other failure
   * keeps the historical HTTP 200 + {@code success:false} body so the React form machine and the
   * legacy ExtJS {@code nx-settingsform} continue to surface the error message unchanged.
   */
  private Response doUpload(
      final String repositoryName,
      final HttpServletRequest request,
      final boolean htmlWrap) throws IOException
  {
    Status status = Status.OK;
    String body;
    try {
      Packet responseJson = new Packet(uploadService.upload(repositoryName, request));
      body = objectMapper.writeValueAsString(responseJson);
    }
    catch (RedeployDisabledException e) {
      log.debug("Re-deploy denied for repository {}: {}", repositoryName, e.getMessage());
      status = Status.CONFLICT;
      body = objectMapper.writeValueAsString(Arrays.asList(new ErrorPacket(e.getMessage())));
    }
    catch (ConcurrentOperationException e) {
      log.debug("Concurrent operation conflict for repository {}: {}", repositoryName, e.getMessage());
      status = Status.CONFLICT;
      body = objectMapper.writeValueAsString(Arrays.asList(new ErrorPacket(e.getMessage())));
    }
    catch (IllegalOperationException e) {
      log.warn("Rejected upload to repository {}: {}", repositoryName, e.getMessage());
      status = Status.BAD_REQUEST;
      body = objectMapper.writeValueAsString(Arrays.asList(new ErrorPacket(e.getMessage())));
    }
    catch (ValidationErrorsException e) {
      throw e;
    }
    catch (Exception e) {
      log.error("Unable to perform upload to repository {}", repositoryName, e);
      body = objectMapper.writeValueAsString(Arrays.asList(new ErrorPacket(e.getMessage())));
    }
    return Response.status(status)
        .type(htmlWrap ? MediaType.TEXT_HTML : MediaType.APPLICATION_JSON)
        .entity(htmlWrap ? htmlWrap(body) : body)
        .build();
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
