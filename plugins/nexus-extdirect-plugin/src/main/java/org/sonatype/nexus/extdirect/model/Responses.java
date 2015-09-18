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
package org.sonatype.nexus.extdirect.model;

import javax.validation.ConstraintViolationException;

/**
 * Ext.Direct response builder.
 *
 * @since 3.0
 */
public class Responses
{
  public static Response<Object> success() {
    return success(null);
  }

  public static <T> Response<T> success(T data) {
    return new Response<>(true, data);
  }

  public static ErrorResponse error(final Throwable cause) {
    return new ErrorResponse(cause);
  }

  public static ErrorResponse error(final String message) {
    return new ErrorResponse(message);
  }

  public static ValidationResponse invalid(final ConstraintViolationException cause) {
    return new ValidationResponse(cause);
  }
}
