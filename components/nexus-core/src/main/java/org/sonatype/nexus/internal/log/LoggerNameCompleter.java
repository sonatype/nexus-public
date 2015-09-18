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

import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

import org.sonatype.nexus.common.log.LogManager;

import org.apache.karaf.shell.console.Completer;
import org.apache.karaf.shell.console.completer.StringsCompleter;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Logger name completer.
 *
 * @since 3.0
 */
@Named("logger-name")
public class LoggerNameCompleter
    implements Completer
{
  private final LogManager logManager;

  @Inject
  public LoggerNameCompleter(final LogManager logManager) {
    this.logManager = checkNotNull(logManager);
  }

  @Override
  public int complete(final String buffer, final int cursor, final List<String> candidates) {
    StringsCompleter delegate = new StringsCompleter();
    delegate.getStrings().addAll(logManager.getLoggers().keySet());
    return delegate.complete(buffer, cursor, candidates);
  }
}
