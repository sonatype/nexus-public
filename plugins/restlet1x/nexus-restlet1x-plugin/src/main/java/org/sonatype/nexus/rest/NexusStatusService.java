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
package org.sonatype.nexus.rest;

import java.util.HashMap;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.ApplicationStatusSource;
import org.sonatype.nexus.SystemStatus;
import org.sonatype.nexus.web.BaseUrlHolder;
import org.sonatype.plexus.rest.representation.VelocityRepresentation;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.restlet.Context;
import org.restlet.data.MediaType;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.data.Status;
import org.restlet.resource.Representation;
import org.restlet.service.StatusService;

//
// FIXME: This is duplicated error-page handling, which will go away once restlet is removed.
//

/**
 * Nexus specific status service that simply assembles an "error page" out of a Velocity template but watching to HTML
 * escape any content that might come from external (ie. query param).
 *
 * @author cstamas
 */
@Named
@Singleton
public class NexusStatusService
    extends StatusService
{
  private final ApplicationStatusSource applicationStatusSource;

  @Inject
  public NexusStatusService(final ApplicationStatusSource applicationStatusSource)
  {
    this.applicationStatusSource = applicationStatusSource;
  }

  public Representation getRepresentation(final Status status, final Request request, final Response response) {
    final HashMap<String, Object> dataModel = new HashMap<String, Object>();

    final SystemStatus systemStatus = applicationStatusSource.getSystemStatus();
    dataModel.put("request", request);
    dataModel.put("nexusVersion", systemStatus.getVersion());
    dataModel.put("nexusRoot", BaseUrlHolder.get());

    dataModel.put("statusCode", status.getCode());
    dataModel.put("statusName", status.getName());
    dataModel.put("errorDescription", StringEscapeUtils.escapeHtml(status.getDescription()));

    if (null != status.getThrowable()) {
      dataModel.put("errorStackTrace",
          StringEscapeUtils.escapeHtml(ExceptionUtils.getStackTrace(status.getThrowable())));
    }

    final VelocityRepresentation representation =
        new VelocityRepresentation(Context.getCurrent(), "/templates/errorPageContentHtml.vm",
            getClass().getClassLoader(), dataModel, MediaType.TEXT_HTML);

    return representation;
  }
}
