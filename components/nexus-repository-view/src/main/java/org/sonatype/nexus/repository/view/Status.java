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
package org.sonatype.nexus.repository.view;

import java.io.Serializable;

import javax.annotation.Nullable;

// TODO: implement Externalizable?  This is made Serialzable just for cache usage, may need to revisit to avoid needing
// TODO: ... to have the cache semantics leak into this core api.  Negative-cache may want to wrap or we may want to
// TODO: ... consider an interface instead, though that has non-trivial impact to code-base atm?

/**
 * A representation of response status with an optional message.
 *
 * @since 3.0
 */
public class Status
  implements Serializable
{
  private final boolean successful;

  private final int code;

  private final String message;

  public Status(final boolean successful, final int code, @Nullable final String message) {
    this.successful = successful;
    this.code = code;
    this.message = message;
  }

  public Status(final boolean successful, final int code) {
    this(successful, code, null);
  }

  public boolean isSuccessful() {
    return successful;
  }

  public int getCode() {
    return code;
  }

  @Nullable
  public String getMessage() {
    return message;
  }

  @Override
  public String toString() {
    return getClass().getSimpleName() + "{" +
        "successful=" + successful +
        ", code=" + code +
        ", message='" + message + '\'' +
        '}';
  }

  //
  // Helpers
  //

  public static Status success(final int code, final String message) {
    return new Status(true, code, message);
  }

  public static Status success(final int code) {
    return new Status(true, code);
  }

  public static Status failure(final int code, final String message) {
    return new Status(false, code, message);
  }

  public static Status failure(final int code) {
    return new Status(false, code);
  }
}
