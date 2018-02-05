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
package org.sonatype.nexus.analytics;

import java.util.concurrent.atomic.AtomicLong;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * Rolling (zero-based) long counter.
 *
 * @since 3.0
 */
class RollingCounter
{
  private final AtomicLong value = new AtomicLong(-1);

  private final long max;

  RollingCounter(final long max) {
    checkArgument(max > 0);
    this.max = max + 1; // inclusive
  }

  public long next() {
    long current, next;
    do {
      current = value.get();
      next = (current + 1) % max;
    }
    while (!value.compareAndSet(current, next));
    return next;
  }
}
