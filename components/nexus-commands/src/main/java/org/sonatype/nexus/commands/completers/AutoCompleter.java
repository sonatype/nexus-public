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
package org.sonatype.nexus.commands.completers;

import java.io.File;
import java.lang.reflect.Field;
import java.util.List;

import javax.annotation.Nullable;
import javax.inject.Named;

import org.sonatype.nexus.commands.CompleterTargetAware;

import org.apache.karaf.shell.console.Completer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Completer which attempts to automatically detect which completer to install.
 *
 * Supports:
 *
 * <ul>
 *   <li>{@link Enum} to {@link EnumCompleter}</li>
 *   <li>{@link File} to {@link FileNameCompleter}</li>
 * </ul>
 *
 * @since 3.0
 */
@Named("auto")
public class AutoCompleter
    implements Completer, CompleterTargetAware
{
  private static final Logger log = LoggerFactory.getLogger(AutoCompleter.class);

  @Nullable
  private Completer delegate;

  @Override
  public void setCompleterTarget(final Field field) {
    Class<?> type = field.getType();
    if (type.isEnum()) {
      //noinspection unchecked
      delegate = new EnumCompleter((Class<Enum>)type);
    }
    else if (type.isAssignableFrom(File.class)) {
      delegate = new FileNameCompleter();
    }

    if (delegate == null) {
      log.warn("Unable to determine completer for: {}", field);
    }
    else {
      log.trace("Selected completer: {}", delegate);
    }
  }

  @Override
  public int complete(final String buffer, final int cursor, final List<String> candidates) {
    if (delegate != null) {
      return delegate.complete(buffer, cursor, candidates);
    }
    return -1;
  }
}
