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
package org.sonatype.nexus.test.utils;

import java.io.IOException;

import org.sonatype.nexus.integrationtests.RequestFacade;

import org.restlet.data.Status;

public class ITHelperLogUtils
{

  private static final String BASE_URI = "service/local/loghelper";

  public static void debug(String message)
      throws Exception
  {
    log(null, "DEBUG", message, null, null);
  }

  public static void debug(String loggerName, String message)
      throws Exception
  {
    log(loggerName, "DEBUG", message, null, null);
  }

  public static void debug(String message, Exception exception)
      throws Exception
  {
    log(null, "DEBUG", message, exception);
  }

  public static void error(String message)
      throws Exception
  {
    log(null, "ERROR", message, null, null);
  }

  public static void error(String loggerName, String message)
      throws Exception
  {
    log(loggerName, "ERROR", message, null, null);
  }

  public static void error(String message, Exception exception)
      throws Exception
  {
    log(null, "ERROR", message, exception);
  }

  public static void error(String message, String exceptionType, String exceptionMessage)
      throws Exception
  {
    log(null, "ERROR", message, exceptionType, exceptionMessage);
  }

  public static void warn(String message)
      throws Exception
  {
    log(null, "WARN", message, null, null);
  }

  public static void warn(String loggerName, String message)
      throws Exception
  {
    log(loggerName, "WARN", message, null, null);
  }

  public static void warn(String message, Exception exception)
      throws Exception
  {
    log(null, "WARN", message, exception);
  }

  public static void warn(String message, String exceptionType, String exceptionMessage)
      throws Exception
  {
    log(null, "WARN", message, exceptionType, exceptionMessage);
  }

  public static void log(String loggerName, String level, String message, Exception exception)
      throws Exception
  {
    log(loggerName, level, message, exception.getClass().getName(), exception.getMessage());
  }

  public static void log(String loggerName, String level, String message, String exceptionType,
                         String exceptionMessage)
      throws Exception
  {
    String uri = "";
    if (loggerName != null) {
      uri += "&loggerName=" + loggerName;
    }
    if (level != null) {
      uri += "&level=" + level;
    }
    if (message != null) {
      uri += "&message=" + message;
    }
    if (exceptionType != null) {
      uri += "&exceptionType=" + exceptionType;
    }
    if (exceptionMessage != null) {
      uri += "&exceptionMessage=" + exceptionMessage;
    }
    if (!uri.equals("")) {
      uri = uri.substring(1);
    }
    uri = BASE_URI + "?" + uri;

    final Status status = RequestFacade.doGetForStatus(uri);

    if (!status.isSuccess()) {
      throw new IOException("The loghelper REST resource reported an error (" + status.toString()
          + "), bailing out!");
    }

  }

}
