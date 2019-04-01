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
package org.sonatype.nexus.rest;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import static com.google.common.base.Preconditions.checkNotNull;
import static javax.ws.rs.core.MediaType.TEXT_PLAIN;

/**
 * {@link WebApplicationException} with {@link Status} and a text message.
 *
 * @since 3.8
 */
public class WebApplicationMessageException
    extends WebApplicationException
{
  public WebApplicationMessageException(final Status status, final String message) {
    this(status, message, TEXT_PLAIN);
  }

  public WebApplicationMessageException(final Status status, final Object message, final String mediaType) {
    super(Response.status(checkNotNull(status)).entity(checkNotNull(message)).type(mediaType).build());
  }
}
