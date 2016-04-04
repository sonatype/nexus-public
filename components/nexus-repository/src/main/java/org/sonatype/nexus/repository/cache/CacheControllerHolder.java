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
package org.sonatype.nexus.repository.cache;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Holds controllers for the various standard or format specific cache types.
 *
 * @since 3.0
 */
public class CacheControllerHolder
{
  public static class CacheType
  {
    private final String typeName;

    public CacheType(final String typeName) {
      this.typeName = checkNotNull(typeName);
    }

    public String value() {
      return typeName;
    }

    @Override
    public boolean equals(final Object o) {
      if (this == o) {
        return true;
      }
      if (!(o instanceof CacheType)) {
        return false;
      }

      CacheType cacheType = (CacheType) o;

      return value().equals(cacheType.value());

    }

    @Override
    public int hashCode() {
      return value().hashCode();
    }
  }

  public static final CacheType CONTENT = new CacheType("CONTENT");

  public static final CacheType METADATA = new CacheType("METADATA");

  private final Map<CacheType, CacheController> controllers;

  public CacheControllerHolder(final CacheController contentCacheController,
                               final CacheController metadataCacheController)
  {
    controllers = new HashMap<>();
    controllers.put(CONTENT, checkNotNull(contentCacheController));
    controllers.put(METADATA, checkNotNull(metadataCacheController));
  }

  @Nonnull
  public CacheController getContentCacheController() {
    return checkNotNull(controllers.get(CONTENT));
  }

  @Nonnull
  public CacheController getMetadataCacheController() {
    return checkNotNull(controllers.get(METADATA));
  }

  @Nullable
  public CacheController get(final CacheType cacheType) {
    return controllers.get(checkNotNull(cacheType));
  }

  @Nullable
  public CacheController set(final CacheType cacheType, @Nullable final CacheController cacheController) {
    checkNotNull(cacheType);
    if (cacheController == null) {
      return controllers.remove(cacheType);
    }
    else {
      return controllers.put(cacheType, cacheController);
    }
  }

  public void invalidateCaches() {
    controllers.values().forEach(CacheController::invalidateCache);
  }
}
