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
package org.sonatype.nexus.testsuite.testsupport.system;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.common.event.EventManager;
import org.sonatype.nexus.common.log.LogManager;
import org.sonatype.nexus.common.log.LoggerLevel;

import static com.google.common.base.Preconditions.checkNotNull;

@Named
@Singleton
public class LogTestSystem
    extends TestSystemSupport
{
  private LogManager logManager;

  @Inject
  public LogTestSystem(final LogManager logManager, final EventManager eventManager) {
    super(eventManager);
    this.logManager = checkNotNull(logManager);
  }

  public void set(final String name, final LoggerLevel level) {
    logManager.setLoggerLevel(name, level);
  }

  @Override
  protected void doAfter() {
    logManager.resetLoggers();
  }
}
