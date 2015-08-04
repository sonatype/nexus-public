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
package org.sonatype.nexus.log;

import java.io.IOException;

/**
 * LogManager MBean interface (intentionally narrowed to same as UI supports, as this is the only thing proven useful
 * and used).
 *
 * @author cstamas
 * @since 2.1
 */
public interface LogManagerMBean
{
  String getRootLoggerLevel()
      throws IOException;

  /**
   * @since 2.7
   */
  void makeRootLoggerLevelOff()
      throws IOException;

  void makeRootLoggerLevelTrace()
      throws IOException;

  void makeRootLoggerLevelDebug()
      throws IOException;

  void makeRootLoggerLevelInfo()
      throws IOException;

  void makeRootLoggerLevelWarn()
      throws IOException;

  /**
   * @since 2.7
   */
  void makeRootLoggerLevelError()
      throws IOException;

  void makeRootLoggerLevelDefault()
      throws IOException;
}
