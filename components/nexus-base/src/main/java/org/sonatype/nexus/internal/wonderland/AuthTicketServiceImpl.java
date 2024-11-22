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
package org.sonatype.nexus.internal.wonderland;

import java.util.Optional;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.common.wonderland.AuthTicketService;
import org.sonatype.nexus.wonderland.AuthTicketCache;

import org.apache.shiro.SecurityUtils;
import org.apache.shiro.subject.Subject;

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
  public AuthTicketServiceImpl(
      final AuthTicketGenerator authTicketGenerator,
      final AuthTicketCache authTicketCache)
  {
    this.authTicketGenerator = checkNotNull(authTicketGenerator);
    this.authTicketCache = checkNotNull(authTicketCache);
  }

  @Override
  public String createTicket(final String user, final String realmName) {
    String ticket = authTicketGenerator.generate();
    authTicketCache.add(user, ticket, realmName);
    return ticket;
  }

  @Override
  @Nullable
  public String createTicket() {
    Subject subject = SecurityUtils.getSubject();
    if (subject != null) {
      Optional<String> realmName = subject.getPrincipals().getRealmNames().stream().findFirst();
      return createTicket(subject.getPrincipal().toString(), realmName.orElse(null));
    }

    return null;
  }

  @Override
  public boolean redeemTicket(final String user, final String ticket, final String realmName) {
    checkNotNull(ticket);
    return authTicketCache.remove(user, ticket, realmName);
  }

  @Override
  public boolean redeemTicket(final String ticket) {
    Subject subject = SecurityUtils.getSubject();
    if (subject != null) {
      Optional<String> realmName = subject.getPrincipals().getRealmNames().stream().findFirst();
      return redeemTicket(subject.getPrincipal().toString(), ticket, realmName.orElse(null));
    }

    return false;
  }
}
