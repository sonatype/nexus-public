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
import org.apache.shiro.authz.annotation.RequiresPermissions;

/*
 * Endpoint used by the Nexus RM UI for component uploads
 *
 * @since 3.next
 */
@Named
@Singleton
@Path(UploadResource.RESOURCE_PATH)
public class UploadResource extends ComponentSupport implements Resource
{
  public static final String RESOURCE_PATH = "internal/ui/upload";

  private UploadService uploadService;

  private UploadConfiguration configuration;

  @Inject
  public UploadResource(final UploadService uploadService, final UploadConfiguration configuration) {
    this.uploadService = uploadService;
    this.configuration = configuration;
  }

  @Timed
  @ExceptionMetered
  @Validate
  @POST
  @Path("{repositoryName}")
  @Consumes(MediaType.MULTIPART_FORM_DATA)
  @Produces(MediaType.APPLICATION_JSON)
  @RequiresPermissions("nexus:component:add")
  public Packet postComponent(@PathParam("repositoryName") final String repositoryName,
                              @Context final HttpServletRequest request)
      throws IOException
  {
    if (!configuration.isEnabled()) {
      throw new WebApplicationException(Response.Status.NOT_FOUND);
    }
    return new Packet(uploadService.upload(repositoryName, request));
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
}
