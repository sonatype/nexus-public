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
package org.sonatype.nexus.bootstrap.log;

/**
 * A log proxy allowing redirecting output (e.g. in case that there is no slf4j available).
 *
 * @since 2.2
 */
public class LogProxy
{

  public void debug(final String message, Object... args) {
    // does nothing
  }

  public void info(final String message, final Object... args) {
    // does nothing
  }

  public void error(final String message, Object... args) {
    // does nothing
  }

  public void error(final String message, Throwable e) {
    // does nothing
  }

  public void warn(final String message, Object... args) {
    // does nothing
  }

  public static LogProxy getLogger(final Class clazz) {
    try {
      LogProxy.class.getClassLoader().loadClass("org.slf4j.Logger");
      return new Slf4jLogProxy(clazz);
    }
    catch (ClassNotFoundException e) {
      return new SystemOutLogProxy(clazz);
    }
  }

}
