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
package org.sonatype.nexus.common.event;

import java.util.function.Supplier;

import static com.google.common.base.Preconditions.checkState;
import static java.lang.Boolean.TRUE;

/**
 * Event helpers.
 *
 * @since 3.2
 */
public class EventHelper
{
  private static final ThreadLocal<Boolean> isReplicating = new ThreadLocal<>();

  private EventHelper() {
    // empty
  }

  /**
   * Is this thread currently replicating remote events from another node?
   */
  public static boolean isReplicating() {
    return TRUE.equals(isReplicating.get());
  }

  /**
   * Calls the given {@link Supplier} while flagged as replicating remote events.
   */
  public static <T> T asReplicating(final Supplier<T> supplier) {
    checkState(!isReplicating(), "Replication already in progress");
    isReplicating.set(TRUE);
    try {
      return supplier.get();
    }
    finally {
      isReplicating.remove();
    }
  }

  /**
   * Calls the given {@link Runnable} while flagged as replicating remote events.
   */
  public static void asReplicating(final Runnable runnable) {
    checkState(!isReplicating(), "Replication already in progress");
    isReplicating.set(TRUE);
    try {
      runnable.run();
    }
    finally {
      isReplicating.remove();
    }
  }
}
