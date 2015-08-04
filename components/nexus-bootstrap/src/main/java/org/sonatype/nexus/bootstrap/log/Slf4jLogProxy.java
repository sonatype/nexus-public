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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Logs to SLF4J.
 *
 * @since 2.2
 */
public class Slf4jLogProxy
    extends LogProxy
{

  private Logger log = LoggerFactory.getLogger(getClass());

  public Slf4jLogProxy(final Logger log) {
    this.log = log;
  }

  public Slf4jLogProxy(final Class clazz) {
    this(LoggerFactory.getLogger(clazz));
  }

  @Override
  public void debug(final String message, Object... args) {
    log.debug(message, args);
  }

  @Override
  public void info(final String message, final Object... args) {
    log.info(message, args);
  }

  @Override
  public void error(final String message, Object... args) {
    log.error(message, args);
  }

  @Override
  public void error(final String message, Throwable e) {
    log.error(message, e);
  }

  @Override
  public void warn(final String message, Object... args) {
    log.warn(message, args);
  }

}
