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
package org.sonatype.nexus.blobstore.rest;

import org.sonatype.nexus.rest.WebApplicationMessageException;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;

/**
 * @since 3.19
 */
public class BlobStoreResourceUtil
{
  public static WebApplicationMessageException createBlobStoreNotFoundException() {
    return new WebApplicationMessageException(
        NOT_FOUND,
        "\"Unable to find blobstore\"",
        APPLICATION_JSON
    );
  }

  public static void throwBlobStoreNotFoundException() throws WebApplicationMessageException {
    throw createBlobStoreNotFoundException();
  }

  /**
   * Throws {@link BAD_REQUEST} exception in case when BlobStore manager could not perform operation
   * (for example, blobstore is in use)
   * @param message error message
   * @throws WebApplicationMessageException
   */
  public static void throwBlobStoreBadRequestException(final String message) throws WebApplicationMessageException {
    throw new WebApplicationMessageException(
        BAD_REQUEST,
        "\"" + message + "\"",
        APPLICATION_JSON
    );
  }
}
