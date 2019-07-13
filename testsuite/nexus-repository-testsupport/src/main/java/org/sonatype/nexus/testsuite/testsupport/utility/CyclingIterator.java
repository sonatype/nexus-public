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
package org.sonatype.nexus.testsuite.testsupport.utility;

import java.util.Iterator;
import java.util.List;

import static com.google.common.base.Preconditions.checkState;

/**
 * Iterates through a List cyclically, without throwing ConcurrentModificationException.
 */
public class CyclingIterator<T>
    implements Iterator<T>
{
  private final List<T> list;

  private int index = 0;

  public CyclingIterator(final List<T> list) {
    this.list = list;
  }

  @Override
  public boolean hasNext() {
    return !list.isEmpty();
  }

  @Override
  public synchronized T next() {
    checkState(!list.isEmpty());
    if (index >= list.size()) {
      index = 0;
    }
    return list.get(index++);
  }

  @Override
  public void remove() {
    // no-op
  }
}
