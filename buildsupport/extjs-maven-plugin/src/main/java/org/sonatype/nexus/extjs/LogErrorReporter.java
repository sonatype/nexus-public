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
package org.sonatype.nexus.extjs;

import org.apache.maven.plugin.logging.Log;
import org.mozilla.javascript.ErrorReporter;
import org.mozilla.javascript.EvaluatorException;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Mojo {@link Log} adapting {@link ErrorReporter}.
 *
 * @since 3.0
 */
public class LogErrorReporter
  implements ErrorReporter
{
  private final Log log;

  public LogErrorReporter(final Log log) {
    this.log = checkNotNull(log);
  }

  private String format(final String message, final String sourceName, final int line, final String lineSource, final int lineOffset) {
    return String.format("%s (%s#%d:%d): %s", message, sourceName, line, lineOffset, lineSource);
  }

  @Override
  public void warning(final String message, final String sourceName, final int line, final String lineSource, final int lineOffset) {
    log.warn(format(message, sourceName, line, lineSource, lineOffset));
  }

  @Override
  public void error(final String message, final String sourceName, final int line, final String lineSource, final int lineOffset){
    log.error(format(message, sourceName, line, lineSource, lineOffset));
  }

  @Override
  public EvaluatorException runtimeError(final String message, final String sourceName, final int line, final String lineSource, final int lineOffset) {
    return new EvaluatorException(message, sourceName, line, lineSource, lineOffset);
  }
}
