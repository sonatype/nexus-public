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
package org.sonatype.nexus.common.log;

import org.slf4j.Logger;
import org.slf4j.Marker;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A delegating {@link Logger} that logs using a {@link Marker} by default.
 * 
 * @since 3.4
 */
public class MarkedLogger
    implements Logger
{
  private final Logger delegate;

  private final Marker marker;

  public MarkedLogger(final Logger delegate, final Marker marker) {
    this.delegate = checkNotNull(delegate);
    this.marker = checkNotNull(marker);
  }

  @Override
  public String getName() {
    return delegate.getName();
  }

  @Override
  public boolean isTraceEnabled() {
    return isTraceEnabled(marker);
  }

  @Override
  public void trace(final String msg) {
    trace(marker, msg);
  }

  @Override
  public void trace(final String format, final Object arg) {
    trace(marker, format, arg);
  }

  @Override
  public void trace(final String format, final Object arg1, final Object arg2) {
    trace(marker, format, arg1, arg2);
  }

  @Override
  public void trace(final String format, final Object... arguments) {
    trace(marker, format, arguments);
  }

  @Override
  public void trace(final String msg, final Throwable t) {
    trace(marker, msg, t);
  }

  @Override
  public boolean isTraceEnabled(final Marker marker) {
    return delegate.isTraceEnabled(marker);
  }

  @Override
  public void trace(final Marker marker, final String msg) {
    delegate.trace(marker, msg);
  }

  @Override
  public void trace(final Marker marker, final String format, final Object arg) {
    delegate.trace(marker, format, arg);
  }

  @Override
  public void trace(final Marker marker, final String format, final Object arg1, final Object arg2) {
    delegate.trace(marker, format, arg1, arg2);
  }

  @Override
  public void trace(final Marker marker, final String format, final Object... argArray) {
    delegate.trace(marker, format, argArray);
  }

  @Override
  public void trace(final Marker marker, final String msg, final Throwable t) {
    delegate.trace(marker, msg, t);
  }

  @Override
  public boolean isDebugEnabled() {
    return isDebugEnabled(marker);
  }

  @Override
  public void debug(final String msg) {
    debug(marker, msg);
  }

  @Override
  public void debug(final String format, final Object arg) {
    debug(marker, format, arg);
  }

  @Override
  public void debug(final String format, final Object arg1, final Object arg2) {
    debug(marker, format, arg1, arg2);
  }

  @Override
  public void debug(final String format, final Object... arguments) {
    debug(marker, format, arguments);
  }

  @Override
  public void debug(final String msg, final Throwable t) {
    debug(marker, msg, t);
  }

  @Override
  public boolean isDebugEnabled(final Marker marker) {
    return delegate.isDebugEnabled(marker);
  }

  @Override
  public void debug(final Marker marker, final String msg) {
    delegate.debug(marker, msg);
  }

  @Override
  public void debug(final Marker marker, final String format, final Object arg) {
    delegate.debug(marker, format, arg);
  }

  @Override
  public void debug(final Marker marker, final String format, final Object arg1, final Object arg2) {
    delegate.debug(marker, format, arg1, arg2);
  }

  @Override
  public void debug(final Marker marker, final String format, final Object... arguments) {
    delegate.debug(marker, format, arguments);
  }

  @Override
  public void debug(final Marker marker, final String msg, final Throwable t) {
    delegate.debug(marker, msg, t);
  }

  @Override
  public boolean isInfoEnabled() {
    return isInfoEnabled(marker);
  }

  @Override
  public void info(final String msg) {
    info(marker, msg);
  }

  @Override
  public void info(final String format, final Object arg) {
    info(marker, format, arg);
  }

  @Override
  public void info(final String format, final Object arg1, final Object arg2) {
    info(marker, format, arg1, arg2);
  }

  @Override
  public void info(final String format, final Object... arguments) {
    info(marker, format, arguments);
  }

  @Override
  public void info(final String msg, final Throwable t) {
    info(marker, msg, t);
  }

  @Override
  public boolean isInfoEnabled(final Marker marker) {
    return delegate.isInfoEnabled(marker);
  }

  @Override
  public void info(final Marker marker, final String msg) {
    delegate.info(marker, msg);
  }

  @Override
  public void info(final Marker marker, final String format, final Object arg) {
    delegate.info(marker, format, arg);
  }

  @Override
  public void info(final Marker marker, final String format, final Object arg1, final Object arg2) {
    delegate.info(marker, format, arg1, arg2);
  }

  @Override
  public void info(final Marker marker, final String format, final Object... arguments) {
    delegate.info(marker, format, arguments);
  }

  @Override
  public void info(final Marker marker, final String msg, final Throwable t) {
    delegate.info(marker, msg, t);
  }

  @Override
  public boolean isWarnEnabled() {
    return isWarnEnabled(marker);
  }

  @Override
  public void warn(final String msg) {
    warn(marker, msg);
  }

  @Override
  public void warn(final String format, final Object arg) {
    warn(marker, format, arg);
  }

  @Override
  public void warn(final String format, final Object... arguments) {
    warn(marker, format, arguments);
  }

  @Override
  public void warn(final String format, final Object arg1, final Object arg2) {
    warn(marker, format, arg1, arg2);
  }

  @Override
  public void warn(final String msg, final Throwable t) {
    warn(marker, msg, t);
  }

  @Override
  public boolean isWarnEnabled(final Marker marker) {
    return delegate.isWarnEnabled(marker);
  }

  @Override
  public void warn(final Marker marker, final String msg) {
    delegate.warn(marker, msg);
  }

  @Override
  public void warn(final Marker marker, final String format, final Object arg) {
    delegate.warn(marker, format, arg);
  }

  @Override
  public void warn(final Marker marker, final String format, final Object arg1, final Object arg2) {
    delegate.warn(marker, format, arg1, arg2);
  }

  @Override
  public void warn(final Marker marker, final String format, final Object... arguments) {
    delegate.warn(marker, format, arguments);
  }

  @Override
  public void warn(final Marker marker, final String msg, final Throwable t) {
    delegate.warn(marker, msg, t);
  }

  @Override
  public boolean isErrorEnabled() {
    return isErrorEnabled(marker);
  }

  @Override
  public void error(final String msg) {
    error(marker, msg);
  }

  @Override
  public void error(final String format, final Object arg) {
    error(marker, format, arg);
  }

  @Override
  public void error(final String format, final Object arg1, final Object arg2) {
    error(marker, format, arg1, arg2);
  }

  @Override
  public void error(final String format, final Object... arguments) {
    error(marker, format, arguments);
  }

  @Override
  public void error(final String msg, final Throwable t) {
    error(marker, msg, t);
  }

  @Override
  public boolean isErrorEnabled(final Marker marker) {
    return delegate.isErrorEnabled(marker);
  }

  @Override
  public void error(final Marker marker, final String msg) {
    delegate.error(marker, msg);
  }

  @Override
  public void error(final Marker marker, final String format, final Object arg) {
    delegate.error(marker, format, arg);
  }

  @Override
  public void error(final Marker marker, final String format, final Object arg1, final Object arg2) {
    delegate.error(marker, format, arg1, arg2);
  }

  @Override
  public void error(final Marker marker, final String format, final Object... arguments) {
    delegate.error(marker, format, arguments);
  }

  @Override
  public void error(final Marker marker, final String msg, final Throwable t) {
    delegate.error(marker, msg, t);
  }
}
