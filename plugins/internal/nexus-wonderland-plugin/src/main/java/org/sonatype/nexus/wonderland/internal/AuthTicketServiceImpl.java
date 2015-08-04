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
package org.sonatype.nexus.wonderland.internal;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.wonderland.AuthTicketService;
import org.sonatype.sisu.goodies.common.ComponentSupport;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Default {@link AuthTicketService} implementation.
 *
 * @since 2.7
 */
@Named
@Singleton
public class AuthTicketServiceImpl
    extends ComponentSupport
    implements AuthTicketService
{
  private final AuthTicketGenerator authTicketGenerator;

  private final AuthTicketCache authTicketCache;

  @Inject
  public AuthTicketServiceImpl(final AuthTicketGenerator authTicketGenerator,
                               final AuthTicketCache authTicketCache)
  {
    this.authTicketGenerator = checkNotNull(authTicketGenerator);
    this.authTicketCache = checkNotNull(authTicketCache);
  }

  @Override
  public String createTicket() {
    String ticket = authTicketGenerator.generate();
    authTicketCache.add(ticket);
    return ticket;
  }

  @Override
  public boolean redeemTicket(final String ticket) {
    checkNotNull(ticket);
    return authTicketCache.remove(ticket);
  }
}
