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
package org.sonatype.nexus.internal.security;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.sonatype.nexus.common.event.EventAware;
import org.sonatype.nexus.common.event.EventManager;
import org.sonatype.nexus.security.ClientInfo;
import org.sonatype.nexus.security.ClientInfoProvider;
import org.sonatype.nexus.security.authc.AuthenticationEvent;
import org.sonatype.nexus.security.authc.NexusAuthenticationEvent;

import com.google.common.eventbus.Subscribe;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Forwards {@link AuthenticationEvent} as {@link NexusAuthenticationEvent}.
 *
 * @since 3.0
 */
@Named
@Singleton
public class AuthenticationEventSubscriber
    implements EventAware
{
  private final Provider<EventManager> eventManager;

  private final Provider<ClientInfoProvider> clientInfoProvider;

  @Inject
  public AuthenticationEventSubscriber(
      final Provider<EventManager> eventManager,
      final Provider<ClientInfoProvider> clientInfoProvider)
  {
    this.eventManager = checkNotNull(eventManager);
    this.clientInfoProvider = checkNotNull(clientInfoProvider);
  }

  @Subscribe
  public void on(final AuthenticationEvent event) {
    final ClientInfo clientInfo = clientInfoProvider.get().getCurrentThreadClientInfo();

    ClientInfo.Builder builder = ClientInfo
        .builder()
        .userId(event.getUserId());

    if (clientInfo != null) {
      builder
          .remoteIP(clientInfo.getRemoteIP())
          .userAgent(clientInfo.getUserAgent())
          .path(clientInfo.getPath());
    }

    eventManager.get()
        .post(new NexusAuthenticationEvent(
            builder.build(),
            event.isSuccessful(),
            event.getAuthenticationFailureReasons()));
  }
}
