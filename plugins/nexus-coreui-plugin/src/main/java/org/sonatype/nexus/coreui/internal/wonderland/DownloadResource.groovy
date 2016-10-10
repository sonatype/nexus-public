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
package org.sonatype.nexus.coreui.internal.wonderland

import javax.annotation.Nullable
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton
import javax.ws.rs.GET
import javax.ws.rs.HeaderParam
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.Produces
import javax.ws.rs.QueryParam
import javax.ws.rs.WebApplicationException
import javax.ws.rs.core.Response

import org.sonatype.goodies.common.ComponentSupport
import org.sonatype.nexus.common.text.Strings2
import org.sonatype.nexus.common.wonderland.DownloadService
import org.sonatype.nexus.rest.Resource

import org.apache.shiro.authz.annotation.RequiresPermissions

import static com.google.common.base.Preconditions.checkNotNull
import static com.google.common.net.HttpHeaders.CONTENT_DISPOSITION
import static com.google.common.net.HttpHeaders.CONTENT_LENGTH
import static javax.ws.rs.core.Response.Status.BAD_REQUEST
import static javax.ws.rs.core.Response.Status.FORBIDDEN
import static javax.ws.rs.core.Response.Status.NOT_FOUND
import static org.sonatype.nexus.common.wonderland.AuthTicketService.AUTH_TICKET_HEADER

/**
 * Downloads resource.
 *
 * @since 2.8
 */
@Named
@Singleton
@Path(DownloadResource.RESOURCE_URI)
class DownloadResource
    extends ComponentSupport
    implements Resource
{
  static final String RESOURCE_URI = /* WonderlandPlugin.REST_PREFIX */ '/wonderland/download'

  private final DownloadService downloadService

  @Inject
  DownloadResource(final DownloadService downloadService) {
    this.downloadService = checkNotNull(downloadService)
  }

  /**
   * Download a support ZIP file.
   */
  @GET
  @Path('{fileName}')
  @Produces('application/zip')
  @RequiresPermissions('nexus:wonderland:download')
  Response downloadZip(final @PathParam('fileName') String fileName,
                       final @Nullable @QueryParam('t') String authTicketParam, // Base64
                       final @Nullable @HeaderParam(AUTH_TICKET_HEADER) String authTicketHeader)
  {
    checkNotNull(fileName)
    log.info 'Download: {}', fileName

    // pick authTicket from either query-param or header
    def authTicket
    if (authTicketParam) {
      // query-param needs to be base64 decoded
      authTicket = Strings2.decodeBase64(authTicketParam)
    }
    else {
      authTicket = authTicketHeader
    }

    // handle one-time auth
    if (!authTicket) {
      throw new WebApplicationException('Missing authentication ticket', BAD_REQUEST)
    }

    try {
      def file = downloadService.get(fileName, authTicket)

      if (!file.exists()) {
        return Response.status(NOT_FOUND).build()
      }

      log.debug 'Sending support ZIP file: {}', file
      return Response.ok(file.newInputStream())
          .header(CONTENT_DISPOSITION, "attachment; filename=\"${fileName}\"")
          .header(CONTENT_LENGTH, file.length())
          .build()
    }
    catch (IllegalAccessException e) {
      throw new WebApplicationException(e.toString(), FORBIDDEN)
    }
  }
}
