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
package org.sonatype.nexus.proxy.storage;

import java.util.HashMap;
import java.util.concurrent.atomic.AtomicInteger;

import com.google.common.collect.Maps;

/**
 * The abstract storage context.
 *
 * @author cstamas
 */
public abstract class AbstractStorageContext
    implements StorageContext
{
  private final HashMap<String, Object> context;

  private final StorageContext parent;

  private final AtomicInteger generation;

  protected AbstractStorageContext(final StorageContext parent) {
    this.context = Maps.newHashMap();
    this.parent = parent;
    this.generation = new AtomicInteger(0);
  }

  @Override
  public synchronized int getGeneration() {
    if (parent != null) {
      return parent.getGeneration() + generation.get();
    }
    return generation.get();
  }

  @Override
  public synchronized int incrementGeneration() {
    generation.incrementAndGet();
    return getGeneration();
  }

  @Override
  public StorageContext getParentStorageContext() {
    return parent;
  }

  @Override
  public Object getContextObject(String key) {
    return getContextObject(key, true);
  }

  public synchronized Object getContextObject(final String key, final boolean fallbackToParent) {
    if (context.containsKey(key)) {
      return context.get(key);
    }
    else if (fallbackToParent && parent != null) {
      return parent.getContextObject(key);
    }
    else {
      return null;
    }
  }

  @Override
  public synchronized Object putContextObject(String key, Object value) {
    final Object previous = context.put(key, value);
    incrementGeneration();
    return previous;
  }

  @Override
  public synchronized Object removeContextObject(String key) {
    final Object removed = context.remove(key);
    incrementGeneration();
    return removed;
  }

  @Override
  public boolean hasContextObject(String key) {
    return context.containsKey(key);
  }

  @Override
  public String toString() {
    return getClass().getName() + "{generation=" + generation + ", parent=" + parent + "}";
  }
}
