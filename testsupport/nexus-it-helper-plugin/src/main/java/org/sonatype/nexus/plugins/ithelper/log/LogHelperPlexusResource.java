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
package org.sonatype.nexus.plugins.ithelper.log;

import java.util.Date;

import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.plexus.rest.resource.AbstractPlexusResource;
import org.sonatype.plexus.rest.resource.PathProtectionDescriptor;

import org.restlet.Context;
import org.restlet.data.Form;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.data.Status;
import org.restlet.resource.ResourceException;
import org.restlet.resource.Variant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
@Named
public class LogHelperPlexusResource
    extends AbstractPlexusResource
{

  @Override
  public Object getPayloadInstance() {
    return null;
  }

  @Override
  public PathProtectionDescriptor getResourceProtection() {
    return new PathProtectionDescriptor(getResourceUri(), "anon");
  }

  @Override
  public String getResourceUri() {
    return "/loghelper";
  }

  @Override
  public Object get(Context context, Request request, Response response, Variant variant)
      throws ResourceException
  {
    Form form = request.getResourceRef().getQueryAsForm();

    String loggerName = form.getFirstValue("loggerName");
    String level = form.getFirstValue("level");
    String message = form.getFirstValue("message");
    String exceptionType = form.getFirstValue("exceptionType");
    String exceptionMessage = form.getFirstValue("exceptionMessage");

    if (message == null) {
      message = "A log message at " + new Date();
    }

    Throwable exception = null;
    if (exceptionType != null || exceptionMessage != null) {
      if (exceptionMessage == null) {
        exceptionMessage = "An exception thrown at " + new Date();
      }
      exception = new Exception(exceptionMessage);
      if (exceptionType != null) {
        try {
          exception =
              (Throwable) this.getClass().getClassLoader().loadClass(exceptionType).getConstructor(
                  String.class).newInstance(exceptionMessage);
        }
        catch (Exception e) {
          e.printStackTrace();
        }
      }
    }

    Logger logger;
    if (loggerName == null) {
      logger = LoggerFactory.getLogger(getClass());
    }
    else {
      logger = LoggerFactory.getLogger(loggerName);
    }

    if (level == null) {
      level = "INFO";
    }
    if (level.equalsIgnoreCase("trace")) {
      if (exception == null) {
        logger.trace(message);
      }
      else {
        logger.trace(message, exception);
      }
    }
    else if (level.equalsIgnoreCase("debug")) {
      if (exception == null) {
        logger.debug(message);
      }
      else {
        logger.debug(message, exception);
      }
    }
    else if (level.equalsIgnoreCase("warn")) {
      if (exception == null) {
        logger.warn(message);
      }
      else {
        logger.warn(message, exception);
      }
    }
    else if (level.equalsIgnoreCase("error")) {
      if (exception == null) {
        logger.error(message);
      }
      else {
        logger.error(message, exception);
      }
    }
    else {
      if (exception == null) {
        logger.info(message);
      }
      else {
        logger.info(message, exception);
      }
    }

    response.setStatus(Status.SUCCESS_OK);
    return "OK";
  }

}