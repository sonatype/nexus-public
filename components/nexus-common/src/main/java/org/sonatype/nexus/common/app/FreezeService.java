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
package org.sonatype.nexus.common.app;

import java.util.List;
import java.util.function.Supplier;

import static java.util.UUID.randomUUID;

/**
 * Supports freezing the application, making it read-only.
 *
 * @since 3.21
 */
public interface FreezeService
{
  /**
   * User request to freeze the application, disallowing writes.
   *
   * @param reason Human-readable reason why the application was frozen
   *
   * @see #cancelFreeze()
   */
  void requestFreeze(String reason);

  /**
   * Cancels the user freeze request.
   *
   * The application may remain frozen if there are system task freeze requests.
   *
   * @see #requestFreeze(String)
   */
  void cancelFreeze();

  /**
   * Request to freeze the application during a system task, disallowing writes.
   *
   * There may be multiple overlapping freeze requests, but only one per-token.
   *
   * @param token Unique token that must be used to cancel this request
   * @param reason Human-readable reason why the application was frozen
   *
   * @see #taskCancelFreeze(String)
   */
  void taskRequestFreeze(String token, String reason);

  /**
   * Cancels the system task freeze request associated with the given token.
   *
   * The application may remain frozen if there are any other freeze requests.
   *
   * @param token Unique token used to request the original freeze
   *
   * @see #taskRequestFreeze(String, String)
   */
  void taskCancelFreeze(String token);

  /**
   * Unilaterally cancels all freeze requests.
   *
   * Should only be used as a last resort to unfreeze the application.
   *
   * @return The cancelled requests
   */
  List<FreezeRequest> cancelAllFreezeRequests();

  /**
   * Temporarily freezes the application during the given system task.
   *
   * @param reason Human-readable reason why the application was frozen
   * @param operation The system task to perform while frozen
   */
  default void freezeDuring(String reason, Runnable operation) {
    // generate a unique freeze token for this request
    String token = randomUUID() + "@" + operation.hashCode();
    taskRequestFreeze(token, reason);
    try {
      operation.run();
    }
    finally {
      taskCancelFreeze(token);
    }
  }

  /**
   * Temporarily freezes the application during the given system task.
   *
   * @param reason Human-readable reason why the application was frozen
   * @param operation The system task to perform while frozen
   */
  default <T> T freezeDuring(String reason, Supplier<T> operation) {
    // generate a unique freeze token for this request
    String token = randomUUID() + "@" + operation.hashCode();
    taskRequestFreeze(token, reason);
    try {
      return operation.get();
    }
    finally {
      taskCancelFreeze(token);
    }
  }

  /**
   * @return Is the application currently frozen?
   */
  boolean isFrozen();

  /**
   * @return Is the application frozen by user request?
   *
   * @since 3.24
   */
  default boolean isFrozenByUser() {
    return currentFreezeRequests().stream().anyMatch(FreezeRequest::isUserRequest);
  }

  /**
   * @return The currently active freeze requests, if any exist
   */
  List<FreezeRequest> currentFreezeRequests();

  /**
   * Ensures that the application is currently readable.
   *
   * @throws NotReadableException when the application is not readable
   */
  void checkReadable(String errorMessage);

  /**
   * Ensures that the application is currently writable.
   *
   * @throws NotWritableException when the application is not writable
   */
  void checkWritable(String errorMessage);
}
