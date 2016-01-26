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

import org.apache.karaf.shell.api.action.Action;
import org.apache.karaf.shell.api.console.Command;
import org.apache.karaf.shell.api.console.Session;
import org.apache.karaf.shell.impl.action.command.ActionCommand;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Provides support for Karaf {@link Action} {@link Command} implementations.
 * 
 * @since 3.0
 */
public abstract class CommandSupport
    extends ActionCommand
{
  protected final Logger log = LoggerFactory.getLogger(getClass());

  public CommandSupport(final Class<? extends Action> actionClass) {
    super(null, actionClass);
  }

  @Override
  protected abstract Action createNewAction(Session session);

  @Override
  protected abstract void releaseAction(Action action) throws Exception;
}
