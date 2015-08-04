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
package org.sonatype.nexus.client.core.exception;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.ws.rs.core.Response;

/**
 * @since 2.3
 */
public class NexusClientErrorResponseException
    extends NexusClientResponseException
{

  private final List<ErrorMessage> errors;

  public NexusClientErrorResponseException(final String reasonPhrase,
                                           final String responseBody,
                                           final List<ErrorMessage> errorMessages)
  {
    super(message(errorMessages), Response.Status.BAD_REQUEST.getStatusCode(), reasonPhrase, responseBody);
    errors = Collections.unmodifiableList(
        errorMessages == null ? Collections.<ErrorMessage>emptyList() : errorMessages
    );
  }

  private static String message(final Collection<ErrorMessage> errors) {
    if (errors != null && !errors.isEmpty()) {
      final StringBuilder sb = new StringBuilder();
      for (final ErrorMessage error : errors) {
        if (sb.length() > 0) {
          sb.append("\n");
        }
        if (!"*".equals(error.getId())) {
          sb.append("[").append(error.getId()).append("] ");
        }
        sb.append(error.getMessage());
      }
      if (errors.size() > 1) {
        sb.insert(0, "\n");
      }
      return sb.toString();
    }
    return "Unknown";
  }

  public List<ErrorMessage> errors() {
    return errors;
  }

  // ==

  public static class ErrorMessage
  {

    private final String id;

    private final String message;

    public ErrorMessage(final String id, final String message) {
      this.id = id;
      this.message = message;
    }

    public String getId() {
      return id;
    }

    public String getMessage() {
      return message;
    }
  }
}
