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
package org.sonatype.nexus.internal.commands;

import javax.inject.Inject;
import javax.inject.Named;

import org.sonatype.nexus.security.SecurityHelper;

import org.apache.karaf.shell.api.action.Action;
import org.apache.karaf.shell.api.action.Command;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * An action to logout the currently logged in user in the console.
 *
 * @since 3.3
 */
@Named
@Command(name = "logout", scope = "nexus", description = "Remove a user from context")
public class LogoutAction
    implements Action
{
  private final SecurityHelper securityHelper;

  @Inject
  public LogoutAction(final SecurityHelper securityHelper) {
    this.securityHelper = checkNotNull(securityHelper);
  }

  @Override
  public Object execute() throws Exception {
    securityHelper.subject().logout();
    return null;
  }
}
