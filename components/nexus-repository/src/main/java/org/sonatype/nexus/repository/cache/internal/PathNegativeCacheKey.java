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
package org.sonatype.nexus.repository.cache.internal;

import org.sonatype.nexus.repository.cache.NegativeCacheKey;

import static com.google.common.base.Preconditions.checkNotNull;

// TODO: implement Externalizable

/**
 * A path based {@link NegativeCacheKey}.
 *
 * @since 3.0
 */
public class PathNegativeCacheKey
    implements NegativeCacheKey
{
  private final String path;

  public PathNegativeCacheKey(final String path) {
    this.path = checkNotNull(path);
  }

  /**
   * @param key child key
   * @return true if child key path starts with this key path
   */
  @Override
  public boolean isParentOf(final NegativeCacheKey key) {
    checkNotNull(key);
    return path.endsWith("/")
        && key instanceof PathNegativeCacheKey
        && ((PathNegativeCacheKey) key).path.startsWith(path);
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    PathNegativeCacheKey that = (PathNegativeCacheKey) o;

    return path.equals(that.path);
  }

  @Override
  public int hashCode() {
    return path.hashCode();
  }

  @Override
  public String toString() {
    return getClass().getSimpleName() + "{" +
        "path='" + path + '\'' +
        '}';
  }

}
