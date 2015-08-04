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
package org.sonatype.nexus.proxy.cache;

/**
 * The Class AbstractPathCache.
 */
public abstract class AbstractPathCache
    implements PathCache
{
  @Override
  public final boolean contains(final String path) {
    return doContains(makeKeyFromPath(path));
  }

  @Override
  public final boolean isExpired(final String path) {
    return doIsExpired(makeKeyFromPath(path));
  }

  @Override
  public final long getExpirationTime(final String path) {
    return doGetExpirationTime(makeKeyFromPath(path));
  }

  @Override
  public final void put(final String path, final Object element) {
    doPut(makeKeyFromPath(path), element, -1);
  }

  @Override
  public final void put(final String path, final Object element, final int expiration) {
    doPut(makeKeyFromPath(path), element, expiration);
  }

  @Override
  public final boolean remove(final String path) {
    if (contains(path)) {
      return doRemove(makeKeyFromPath(path));
    }
    else {
      return false;
    }
  }

  @Override
  public final boolean removeWithParents(String path) {
    boolean result = remove(path);
    int lastSlash = path.lastIndexOf("/");
    while (lastSlash > -1) {
      path = path.substring(0, lastSlash);
      boolean r = remove(path);
      result = result || r;
      lastSlash = path.lastIndexOf("/");
    }
    return result;
  }

  @Override
  public abstract boolean removeWithChildren(final String path);

  @Override
  public final boolean purge() {
    return doPurge();
  }

  // ==

  protected String makeKeyFromPath(String path) {
    while (path.startsWith("/")) {
      path = path.substring(1);
    }

    while (path.endsWith("/")) {
      path = path.substring(0, path.length() - 1);
    }

    return path;
  }

  protected abstract boolean doContains(String key);

  protected abstract boolean doIsExpired(String key);

  protected abstract long doGetExpirationTime(String key);

  protected abstract void doPut(String key, Object element, int expiration);

  protected abstract boolean doRemove(String key);

  protected abstract boolean doPurge();
}
