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

import org.apache.karaf.shell.console.completer.StringsCompleter;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * {@link Enum} completer.
 *
 * @since 3.0
 */
public class EnumCompleter
    extends StringsCompleter
{
  public EnumCompleter(final Class<? extends Enum> type) {
    super(true); // case-sensitive
    checkNotNull(type);
    for (Enum n : type.getEnumConstants()) {
      getStrings().add(n.name());
    }
  }
}
