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
package org.sonatype.nexus.internal.log;

import java.lang.reflect.Field;

import ch.qos.logback.classic.LoggerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Unwraps pax-logging classes to access the active logback {@link LoggerContext}
 *
 * Several duplicates of this class exist.
 */
public final class LogbackContextProvider
{
  private static final String M_DELEGATE = "m_delegate";

  private static LoggerContext context;

  public static LoggerContext get() {
    if (context == null) {
      load();
    }
    return context;
  }

  private static void load() {
    Logger logger = LoggerFactory.getLogger(LogbackContextProvider.class);

    maybeLogback(logger);

    if (context != null) {
      return;
    }
    maybePaxLogger(logger);
  }

  private static void maybeLogback(final Logger logger) {
    if (logger instanceof ch.qos.logback.classic.Logger) {
      context = (LoggerContext) getField(logger, "loggerContext");
    }
  }

  private static void maybePaxLogger(final Logger logger) {
    if (!logger.getClass().getName().equals("org.ops4j.pax.logging.slf4j.Slf4jLogger")) {
      return;
    }

    Object delegate = getField(logger, M_DELEGATE);

    if (delegate.getClass().getSimpleName().contains("TrackingLogger")) {
      delegate = getField(delegate, M_DELEGATE);
    }

    if (!delegate.getClass().getSimpleName().contains("PaxLoggerImpl")) {
      return;
    }

    maybeLogback((Logger) getField(delegate, M_DELEGATE));
  }

  private static Object getField(final Object obj, final String fieldName) {
    try {
      Class<?> clazz = obj.getClass();
      Field field = clazz.getDeclaredField(fieldName);

      field.setAccessible(true);

      return field.get(obj);

    }
    catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException e) {
      throw new RuntimeException(e);
    }
  }

  private LogbackContextProvider() {
    // private
  }
}
