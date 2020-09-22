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
import javax.inject.Provider;
import javax.inject.Singleton;

import org.sonatype.nexus.repository.group.GroupHandler;
import org.sonatype.nexus.repository.view.Context;
import org.sonatype.nexus.repository.view.Response;
import org.sonatype.nexus.repository.view.Status;

import static org.sonatype.nexus.repository.npm.internal.NpmResponses.failureWithStatusPayload;
import static org.sonatype.nexus.repository.npm.internal.NpmResponses.forbidden;

/**
 * Override certain behaviours of the standard group handler to be able to write to a group.
 *
 * @since 3.next
 */
@Named
@Singleton
public class NpmGroupWriteHandler
    extends GroupHandler
{
  @Inject
  @Named("groupWriteHandler")
  Provider<GroupHandler> groupWriteHandler;

  @Nonnull
  @Override
  public Response handle(@Nonnull final Context context) throws Exception {
    if (groupWriteHandler.get() != null) {
      Response response = groupWriteHandler.get().handle(context);

      Status status = response.getStatus();
      if (status.isSuccessful()) {
        return response;
      }
      return failureWithStatusPayload(status.getCode(), status.getMessage());
    }
    return forbidden(INSUFFICIENT_LICENSE);
  }
}
