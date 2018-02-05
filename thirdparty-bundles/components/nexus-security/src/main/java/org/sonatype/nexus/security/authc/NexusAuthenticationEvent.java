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
package org.sonatype.nexus.security.authc;

import java.util.Date;

import org.sonatype.nexus.security.ClientInfo;

// FIXME: Sort out why we have 2 events: NexusAuthenticationEvent and AuthenticationEvent

/**
 * Event fired when authentication validation is performed (someone tries to log in).
 */
public class NexusAuthenticationEvent
{
  private final ClientInfo clientInfo;

  private final boolean successful;

  private final Date date;

  public NexusAuthenticationEvent(final ClientInfo info, final boolean successful) {
    this.clientInfo = info;
    this.successful = successful;
    this.date = new Date();
  }

  public ClientInfo getClientInfo() {
    return clientInfo;
  }

  public boolean isSuccessful() {
    return successful;
  }

  public Date getEventDate() {
    return date;
  }
}
