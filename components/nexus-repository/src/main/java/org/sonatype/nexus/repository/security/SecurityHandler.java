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
package org.sonatype.nexus.repository.security;

import javax.annotation.Nonnull;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.repository.view.Context;
import org.sonatype.nexus.repository.view.Handler;
import org.sonatype.nexus.repository.view.Response;

import com.google.common.annotations.VisibleForTesting;

/**
 * Security handler.
 *
 * @since 3.0
 */
@Named
@Singleton
public class SecurityHandler
    extends ComponentSupport
    implements Handler
{
  @VisibleForTesting
  static final String AUTHORIZED_KEY = "security.authorized";

  @Nonnull
  @Override
  public Response handle(@Nonnull final Context context) throws Exception {
    SecurityFacet securityFacet = context.getRepository().facet(SecurityFacet.class);

    //we employ the model that one security check per request is all that is necessary, if this handler is in a nested
    //repository (because this is a group repository), there is no need to check authz again
    if (context.getAttributes().get(AUTHORIZED_KEY) == null) {
      securityFacet.ensurePermitted(context.getRequest());
      context.getAttributes().set(AUTHORIZED_KEY, true);
    }

    return context.proceed();
  }
}
