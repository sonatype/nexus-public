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
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;

import org.sonatype.nexus.rest.ExceptionMapperSupport;

/**
 * Standard {@link WebApplicationException} exception mapper.
 *
 * This is needed to restore default response behavior when {@link UnexpectedExceptionMapper} is installed.
 *
 * @since 3.0
 */
@Named
@Singleton
@Provider
public class WebappExceptionMapper
  extends ExceptionMapperSupport<WebApplicationException>
{
  @Override
  protected Response convert(final WebApplicationException exception, final String id) {
    return exception.getResponse();
  }
}
