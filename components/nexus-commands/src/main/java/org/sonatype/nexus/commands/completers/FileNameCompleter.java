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

import java.util.List;

import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.karaf.shell.console.Completer;

/**
 * File name completer.
 *
 * @since 3.0
 */
@Named("file-name")
@Singleton
public class FileNameCompleter
    implements Completer
{
  // NOTE: Not using org.apache.karaf.shell.console.completer.FileCompleter as it has a pointless constructor

  private final jline.console.completer.FileNameCompleter delegate = new jline.console.completer.FileNameCompleter();

  // NOTE: List<String> vs. List<CharSequence> mismatch between Karaf and JLine Completer API

  @SuppressWarnings("unchecked")
  @Override
  public int complete(final String buffer, final int cursor, final List candidates) {
    return delegate.complete(buffer, cursor, candidates);
  }
}
