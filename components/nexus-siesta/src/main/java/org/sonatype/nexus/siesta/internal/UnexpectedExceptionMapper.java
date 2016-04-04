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
package org.sonatype.nexus.siesta.internal;

import javax.inject.Named;
import javax.inject.Singleton;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;

import org.sonatype.nexus.rest.ExceptionMapperSupport;

import static javax.ws.rs.core.MediaType.TEXT_PLAIN;

/**
 * Unexpected generic {@link Throwable} exception mapper.
 *
 * This will handle all unexpected exceptions and override the default JAX-RS implementations behavior.
 *
 * @since 3.0
 */
@Named
@Singleton
@Provider
public class UnexpectedExceptionMapper
  extends ExceptionMapperSupport<Throwable>
{
  @Override
  protected Response convert(final Throwable exception, final String id) {
    // always log unexpected exception with stack
    log.warn("(ID {}) Unexpected exception: {}", id, exception.toString(), exception);

    return Response.serverError()
        .entity(String.format("ERROR: (ID %s) %s", id, exception))
        .type(TEXT_PLAIN)
        .build();
  }
}
