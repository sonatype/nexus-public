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
package org.sonatype.nexus.rest

import javax.ws.rs.core.Response

import groovy.transform.CompileStatic
import groovy.transform.ToString

import static javax.ws.rs.core.MediaType.APPLICATION_JSON
import static javax.ws.rs.core.Response.Status.OK

/**
 * Simple API response object. Contains attributes for HTTP status, message, and a data map
 *
 * @since 3.next
 */
@CompileStatic
@ToString(includePackage = false, includeNames = true)
class SimpleApiResponse
{
  int status

  String message

  Map<String, ?> data

  static Response ok(final String message, final Map<String, ?> data) {
    final SimpleApiResponse ok = new SimpleApiResponse()
    ok.setStatus(OK.getStatusCode())
    ok.setMessage(message)
    ok.setData(data)
    return Response.status(OK).entity(ok).type(APPLICATION_JSON).build()
  }
}
