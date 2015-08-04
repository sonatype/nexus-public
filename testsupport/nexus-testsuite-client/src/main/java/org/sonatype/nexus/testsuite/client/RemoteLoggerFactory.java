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
package org.sonatype.nexus.testsuite.client;

import org.slf4j.Logger;

/**
 * Remote Logger Nexus Client Subsystem. Created logger will forward logging to a Nexus REST resource, so log messages
 * will appear in nexus.log.
 *
 * @since 2.2
 */
public interface RemoteLoggerFactory
{

  /**
   * Return a logger named according to the name parameter.
   *
   * @param name of the logger
   * @return logger. Never null.
   */
  Logger getLogger(final String name);

  /**
   * Return a logger named corresponding to the class passed as parameter.
   *
   * @param clazz the returned logger will be named after clazz
   * @return logger. Never null.
   */
  Logger getLogger(final Class clazz);

}
