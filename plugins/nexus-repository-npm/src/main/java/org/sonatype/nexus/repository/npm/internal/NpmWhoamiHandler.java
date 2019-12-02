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
package org.sonatype.nexus.repository.npm.internal;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.repository.http.HttpResponses;
import org.sonatype.nexus.repository.view.Context;
import org.sonatype.nexus.repository.view.Handler;
import org.sonatype.nexus.repository.view.Response;
import org.sonatype.nexus.repository.view.payloads.StringPayload;
import org.sonatype.nexus.security.SecuritySystem;
import org.sonatype.nexus.security.user.User;

import com.fasterxml.jackson.databind.ObjectMapper;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.sonatype.nexus.repository.view.ContentTypes.APPLICATION_JSON;

/**
 * @since 3.20
 */
@Named
@Singleton
public class NpmWhoamiHandler
    implements Handler
{
  private final SecuritySystem securitySystem;

  private final ObjectMapper objectMapper;

  @Inject
  public NpmWhoamiHandler(final SecuritySystem securitySystem, final ObjectMapper objectMapper) {
    this.securitySystem = checkNotNull(securitySystem);
    this.objectMapper = checkNotNull(objectMapper);
  }

  @Nonnull
  @Override
  public Response handle(@Nonnull final Context context) throws Exception
  {
    User user = securitySystem.currentUser();

    Username username = new Username(user == null ? "anonymous" : user.getUserId());

    StringPayload payload = new StringPayload(objectMapper.writeValueAsString(username), APPLICATION_JSON);

    return HttpResponses.ok(payload);
  }

  private static final class Username {
    private final String username;

    public Username(final String username) {
      this.username = username;
    }

    public String getUsername() {
      return username;
    }
  }
}
