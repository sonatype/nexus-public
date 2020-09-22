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
package org.sonatype.repository.conan.internal.security.token;

import javax.inject.Inject;
import javax.inject.Named;

import org.sonatype.nexus.repository.FacetSupport;
import org.sonatype.nexus.repository.view.Context;
import org.sonatype.nexus.repository.view.Response;
import org.sonatype.nexus.repository.view.Status;
import org.sonatype.nexus.repository.view.payloads.StringPayload;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.sonatype.nexus.repository.http.HttpStatus.OK;
import static org.sonatype.nexus.repository.http.HttpStatus.UNAUTHORIZED;
import static org.sonatype.nexus.repository.view.ContentTypes.TEXT_PLAIN;

/**
 * @since 3.next
 */
@Named
public class ConanTokenFacetImpl
    extends FacetSupport
    implements ConanTokenFacet
{
  private final ConanTokenManager conanTokenManager;

  @Inject
  public ConanTokenFacetImpl(final ConanTokenManager conanTokenManager) {
    this.conanTokenManager = checkNotNull(conanTokenManager);
  }

  @Override
  public Response login(final Context context) {
    String token = conanTokenManager.login();
    if(null != token) {
      return new Response.Builder()
          .status(Status.success(OK))
          .payload(new StringPayload(token, TEXT_PLAIN))
          .build();
    }
    return badCredentials("Bad username or password");
  }

  @Override
  public Response user(final Context context) {
    String user = conanTokenManager.user();
    if(null != user) {
      return new Response.Builder()
          .status(Status.success(OK))
          .payload(new StringPayload(user, TEXT_PLAIN))
          .build();
    }
    return badCredentials("Unknown user");
  }

  @Override
  public Response logout(final Context context) {
    return null;
  }


  static Response badCredentials(final String message) {
    return new Response.Builder()
        .status(Status.failure(UNAUTHORIZED))
        .payload(new StringPayload(message, TEXT_PLAIN))
        .build();
  }
}
