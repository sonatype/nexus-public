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
package org.sonatype.nexus.coreui.internal.wonderland;

import java.io.IOException;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.common.wonderland.AuthTicketService;
import org.sonatype.nexus.common.wonderland.DownloadService;
import org.sonatype.nexus.common.wonderland.DownloadService.Download;
import org.sonatype.nexus.rest.NotCacheable;
import org.sonatype.nexus.rest.Resource;

import org.apache.shiro.authz.annotation.RequiresPermissions;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.net.HttpHeaders.CONTENT_DISPOSITION;
import static com.google.common.net.HttpHeaders.CONTENT_LENGTH;
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;

/**
 * Downloads resource.
 *
 * @since 2.8
 */
@Named
@Singleton
@Path(DownloadResource.RESOURCE_URI)
public class DownloadResource
    extends ComponentSupport
    implements Resource
{
  public static final String RESOURCE_URI = "/wonderland/download";

  private final DownloadService downloadService;

  private final AuthTicketService authTicketService;

  @Inject
  public DownloadResource(final DownloadService downloadService, final AuthTicketService authTicketService) {
    this.downloadService = checkNotNull(downloadService);
    this.authTicketService = checkNotNull(authTicketService);
  }

  /**
   * Download a support ZIP file.
   */
  @GET
  @Path("{fileName}")
  @Produces("application/zip")
  @RequiresPermissions("nexus:wonderland:download")
  @NotCacheable
  public Response downloadZip(@PathParam("fileName") final String fileName)
  {
    checkNotNull(fileName);
    log.info("Download: {}", fileName);

    String authTicket = authTicketService.createTicket();

    // handle one-time auth
    if (authTicket == null) {
      throw new WebApplicationException("Missing authentication ticket", BAD_REQUEST);
    }

    try {
      Download download = downloadService.get(fileName, authTicket);

      if (download == null) {
        return Response.status(NOT_FOUND).build();
      }

      log.debug("Sending support ZIP file: {}", fileName);
      return Response.ok(download.getBytes())
          .header(CONTENT_DISPOSITION, "attachment; filename=\"${fileName}\"")
          .header(CONTENT_LENGTH, download.getLength())
          .build();
    }
    catch (IOException e) {
      log.error("Failed to serve file for download {}", fileName, e);
      throw new WebApplicationException("Failed to service file for download", INTERNAL_SERVER_ERROR);
    }
  }
}
