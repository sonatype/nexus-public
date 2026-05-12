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
package org.sonatype.nexus.audit.internal;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import org.sonatype.nexus.audit.InitiatorProvider;
import org.sonatype.nexus.security.ClientInfo;
import org.sonatype.nexus.security.ClientInfoProvider;
import org.sonatype.nexus.security.UserIdHelper;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Default {@link InitiatorProvider} implementation.
 *
 * @since 3.1
 */
@Component
@Singleton
public class InitiatorProviderImpl
    implements InitiatorProvider
{
  protected final Logger log = LoggerFactory.getLogger(getClass());

  private final ClientInfoProvider clientInfoProvider;

  @Inject
  public InitiatorProviderImpl(final ClientInfoProvider clientInfoProvider) {
    this.clientInfoProvider = clientInfoProvider;
  }

  /**
   * When {@link ClientInfo} is available, returns {@code user-id/remote-ip}, else returns {@code user-id}.
   */
  @Override
  public String get() {
    ClientInfo clientInfo = clientInfoProvider.getCurrentThreadClientInfo();
    if (clientInfo != null) {
      return new StringBuilder()
          .append(clientInfo.getUserid())
          .append('/')
          .append(clientInfo.getRemoteIP())
          .toString();
    }
    else {
      return UserIdHelper.get();
    }
  }
}
