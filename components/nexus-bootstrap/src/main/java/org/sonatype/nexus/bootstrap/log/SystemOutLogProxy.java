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

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Logs to System.out.
 *
 * @since 2.2
 */
public class SystemOutLogProxy
    extends LogProxy
{

  private Class clazz;

  public SystemOutLogProxy(final Class clazz) {
    this.clazz = clazz;
  }

  @Override
  public void debug(final String message, Object... args) {
    message("DEBUG", message, args);
  }

  @Override
  public void info(final String message, final Object... args) {
    message("INFO", message, args);
  }

  @Override
  public void error(final String message, final Throwable e) {
    error(message);
    e.printStackTrace(System.out);
  }

  @Override
  public void error(final String message, Object... args) {
    message("ERROR", message, args);
  }

  @Override
  public void warn(final String message, Object... args) {
    message("WARN", message, args);
  }

  private void message(final String level, final String message, Object... args) {
    final String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
    System.out.println(
        timestamp + " [" + level + "] " + clazz.getSimpleName()
            + " - " + String.format(message.replace("{}", "%s"), args)
    );
  }

}
