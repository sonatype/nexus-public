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
package org.sonatype.nexus.commands;

import java.util.Dictionary;
import java.util.Hashtable;

import org.apache.karaf.shell.commands.Command;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.apache.felix.service.command.CommandProcessor.COMMAND_FUNCTION;
import static org.apache.felix.service.command.CommandProcessor.COMMAND_SCOPE;

/**
 * Command helpers.
 *
 * @since 3.0
 */
public class CommandHelper
{
  /**
   * Create basic configuration with scope and function name.
   */
  public static Dictionary<String, ?> config(final String scope, final String function) {
    checkNotNull(scope);
    checkNotNull(function);
    Hashtable<String, String> props = new Hashtable<>();
    props.put(COMMAND_SCOPE, scope);
    props.put(COMMAND_FUNCTION, function);
    return props;
  }

  public static Dictionary<String, ?> config(final Command command) {
    checkNotNull(command);
    return config(command.scope(), command.name());
  }
}
