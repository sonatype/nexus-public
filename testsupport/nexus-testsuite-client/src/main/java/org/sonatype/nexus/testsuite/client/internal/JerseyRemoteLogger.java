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
package org.sonatype.nexus.testsuite.client.internal;

import javax.ws.rs.core.MultivaluedMap;

import org.sonatype.nexus.client.core.spi.SubsystemSupport;
import org.sonatype.nexus.client.rest.jersey.JerseyNexusClient;

import com.sun.jersey.api.client.ClientHandlerException;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.UniformInterfaceException;
import com.sun.jersey.core.util.MultivaluedMapImpl;
import org.slf4j.Logger;
import org.slf4j.Marker;
import org.slf4j.helpers.FormattingTuple;
import org.slf4j.helpers.MessageFormatter;

/**
 * Jersey based {@link Logger} Nexus Client Subsystem implementation.
 *
 * @since 2.2
 */
public class JerseyRemoteLogger
    extends SubsystemSupport<JerseyNexusClient>
    implements Logger
{

  private static String TRACE_STR = "TRACE";

  private static String DEBUG_STR = "DEBUG";

  private static String INFO_STR = "INFO";

  private static String WARN_STR = "WARN";

  private static String ERROR_STR = "ERROR";

  private final String name;

  public JerseyRemoteLogger(final JerseyNexusClient nexusClient,
                            final String name)
  {
    super(nexusClient);
    this.name = name;
  }

  private void log(final String level, final String message, final Throwable t) {
    final MultivaluedMap<String, String> queryParams = new MultivaluedMapImpl();
    queryParams.add("loggerName", name);
    queryParams.add("level", level);
    queryParams.add("message", message);
    if (t != null) {
      queryParams.add("exceptionType", t.getClass().getName());
      final String exceptionMessage = t.getMessage();
      if (exceptionMessage != null) {
        queryParams.add("exceptionMessage", exceptionMessage);
      }
    }
    try {
      getNexusClient()
          .serviceResource("loghelper", queryParams)
          .get(ClientResponse.class)
          .close();
    }
    catch (UniformInterfaceException e) {
      throw getNexusClient().convert(e);
    }
    catch (ClientHandlerException e) {
      throw getNexusClient().convert(e);
    }
  }

  private void formatAndLog(final String level, final String format, final Object arg1, final Object arg2) {
    final FormattingTuple tp = MessageFormatter.format(format, arg1, arg2);
    log(level, tp.getMessage(), tp.getThrowable());
  }

  private void formatAndLog(final String level, final String format, final Object[] argArray) {
    final FormattingTuple tp = MessageFormatter.arrayFormat(format, argArray);
    log(level, tp.getMessage(), tp.getThrowable());
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public boolean isTraceEnabled() {
    return true;
  }

  @Override
  public void trace(final String msg) {
    log(TRACE_STR, msg, null);
  }

  @Override
  public void trace(final String format, final Object arg) {
    formatAndLog(TRACE_STR, format, arg, null);
  }

  @Override
  public void trace(final String format, final Object arg1, final Object arg2) {
    formatAndLog(TRACE_STR, format, arg1, arg2);
  }

  @Override
  public void trace(final String format, final Object[] argArray) {
    formatAndLog(TRACE_STR, format, argArray);
  }

  @Override
  public void trace(final String msg, final Throwable t) {
    log(TRACE_STR, msg, t);
  }

  @Override
  public boolean isTraceEnabled(final Marker marker) {
    return isTraceEnabled();
  }

  @Override
  public void trace(final Marker marker, final String msg) {
    trace(msg);
  }

  @Override
  public void trace(final Marker marker, final String format, final Object arg) {
    trace(format, arg);
  }

  @Override
  public void trace(final Marker marker, final String format, final Object arg1, final Object arg2) {
    trace(format, arg1, arg2);
  }

  @Override
  public void trace(final Marker marker, final String format, final Object[] argArray) {
    trace(format, argArray);
  }

  @Override
  public void trace(final Marker marker, final String msg, final Throwable t) {
    trace(msg, t);
  }

  @Override
  public boolean isDebugEnabled() {
    return true;
  }

  @Override
  public void debug(final String msg) {
    log(DEBUG_STR, msg, null);
  }

  @Override
  public void debug(final String format, final Object arg) {
    formatAndLog(DEBUG_STR, format, arg, null);
  }

  @Override
  public void debug(final String format, final Object arg1, final Object arg2) {
    formatAndLog(DEBUG_STR, format, arg1, arg2);
  }

  @Override
  public void debug(final String format, final Object[] argArray) {
    formatAndLog(DEBUG_STR, format, argArray);
  }

  @Override
  public void debug(final String msg, final Throwable t) {
    log(DEBUG_STR, msg, t);
  }

  @Override
  public boolean isDebugEnabled(final Marker marker) {
    return isDebugEnabled();
  }

  @Override
  public void debug(final Marker marker, final String msg) {
    debug(msg);
  }

  @Override
  public void debug(final Marker marker, final String format, final Object arg) {
    debug(format, arg);
  }

  @Override
  public void debug(final Marker marker, final String format, final Object arg1, final Object arg2) {
    debug(format, arg1, arg2);
  }

  @Override
  public void debug(final Marker marker, final String format, final Object[] argArray) {
    debug(format, argArray);
  }

  @Override
  public void debug(final Marker marker, final String msg, final Throwable t) {
    debug(msg, t);
  }

  @Override
  public boolean isInfoEnabled() {
    return true;
  }

  @Override
  public void info(final String msg) {
    log(INFO_STR, msg, null);
  }

  @Override
  public void info(final String format, final Object arg) {
    formatAndLog(INFO_STR, format, arg, null);
  }

  @Override
  public void info(final String format, final Object arg1, final Object arg2) {
    formatAndLog(INFO_STR, format, arg1, arg2);
  }

  @Override
  public void info(final String format, final Object[] argArray) {
    formatAndLog(INFO_STR, format, argArray);
  }

  @Override
  public void info(final String msg, final Throwable t) {
    log(INFO_STR, msg, t);
  }

  @Override
  public boolean isInfoEnabled(final Marker marker) {
    return true;
  }

  @Override
  public void info(final Marker marker, final String msg) {
    info(msg);
  }

  @Override
  public void info(final Marker marker, final String format, final Object arg) {
    info(format, arg);
  }

  @Override
  public void info(final Marker marker, final String format, final Object arg1, final Object arg2) {
    info(format, arg1, arg2);
  }

  @Override
  public void info(final Marker marker, final String format, final Object[] argArray) {
    info(format, argArray);
  }

  @Override
  public void info(final Marker marker, final String msg, final Throwable t) {
    info(msg, t);
  }

  @Override
  public boolean isWarnEnabled() {
    return true;
  }

  @Override
  public void warn(final String msg) {
    log(WARN_STR, msg, null);
  }

  @Override
  public void warn(final String format, final Object arg) {
    formatAndLog(WARN_STR, format, arg, null);
  }

  @Override
  public void warn(final String format, final Object[] argArray) {
    formatAndLog(WARN_STR, format, argArray);
  }

  @Override
  public void warn(final String format, final Object arg1, final Object arg2) {
    formatAndLog(WARN_STR, format, arg1, arg2);
  }

  @Override
  public void warn(final String msg, final Throwable t) {
    log(WARN_STR, msg, t);
  }

  @Override
  public boolean isWarnEnabled(final Marker marker) {
    return isWarnEnabled();
  }

  @Override
  public void warn(final Marker marker, final String msg) {
    warn(msg);
  }

  @Override
  public void warn(final Marker marker, final String format, final Object arg) {
    warn(format, arg);
  }

  @Override
  public void warn(final Marker marker, final String format, final Object arg1, final Object arg2) {
    warn(format, arg1, arg2);
  }

  @Override
  public void warn(final Marker marker, final String format, final Object[] argArray) {
    warn(format, argArray);
  }

  @Override
  public void warn(final Marker marker, final String msg, final Throwable t) {
    warn(msg, t);
  }

  @Override
  public boolean isErrorEnabled() {
    return true;
  }

  @Override
  public void error(final String msg) {
    log(ERROR_STR, msg, null);
  }

  @Override
  public void error(final String format, final Object arg) {
    formatAndLog(ERROR_STR, format, arg, null);
  }

  @Override
  public void error(final String format, final Object arg1, final Object arg2) {
    formatAndLog(ERROR_STR, format, arg1, arg2);
  }

  @Override
  public void error(final String format, final Object[] argArray) {
    formatAndLog(ERROR_STR, format, argArray);
  }

  @Override
  public void error(final String msg, final Throwable t) {
    log(ERROR_STR, msg, t);
  }

  @Override
  public boolean isErrorEnabled(final Marker marker) {
    return isErrorEnabled();
  }

  @Override
  public void error(final Marker marker, final String msg) {
    error(msg);
  }

  @Override
  public void error(final Marker marker, final String format, final Object arg) {
    error(format, arg);
  }

  @Override
  public void error(final Marker marker, final String format, final Object arg1, final Object arg2) {
    error(format, arg1, arg2);
  }

  @Override
  public void error(final Marker marker, final String format, final Object[] argArray) {
    error(format, argArray);
  }

  @Override
  public void error(final Marker marker, final String msg, final Throwable t) {
    error(msg, t);
  }

}
