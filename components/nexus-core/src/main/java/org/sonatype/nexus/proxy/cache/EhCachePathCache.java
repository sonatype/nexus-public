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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.sonatype.nexus.proxy.item.RepositoryItemUid;

import com.google.common.base.Preconditions;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;
import net.sf.ehcache.Statistics;

/**
 * The Class EhCacheCache is a thin wrapper around EHCache just to make things going.
 *
 * @author cstamas
 */
public class EhCachePathCache
    extends AbstractPathCache
{
  private final String _repositoryId;

  /**
   * The ec.
   */
  private final Ehcache _ec;

  /**
   * Instantiates a new eh cache cache.
   *
   * @param cache the cache
   */
  public EhCachePathCache(final String repositoryId, final Ehcache cache) {
    this._repositoryId = Preconditions.checkNotNull(repositoryId);
    this._ec = Preconditions.checkNotNull(cache);
  }

  protected String getRepositoryId() {
    return _repositoryId;
  }

  protected Ehcache getEHCache() {
    return _ec;
  }

  @Override
  public boolean doContains(final String key) {
    return getEHCache().get(key) != null;
  }

  @Override
  public boolean doIsExpired(final String key) {
    if (getEHCache().isKeyInCache(key)) {
      Element el = getEHCache().get(key);
      if (el != null) {
        return el.isExpired();
      }
      else {
        return true;
      }
    }
    else {
      return false;
    }
  }

  @Override
  public long doGetExpirationTime(final String key) {
    final Element el = getEHCache().get(key);
    if (el != null) {
      return el.getExpirationTime();
    }
    else {
      return -1;
    }
  }

  @Override
  public void doPut(final String key, final Object element, final int expiration) {
    Element el = new Element(key, element);
    if (expiration > -1) {
      el.setTimeToLive(expiration);
    }
    getEHCache().put(el);
  }

  @Override
  public boolean doRemove(String key) {
    return getEHCache().remove(key);
  }

  @Override
  public boolean removeWithChildren(String path) {
    @SuppressWarnings("unchecked")
    List<String> keys = getEHCache().getKeys();

    String keyToRemove = makeKeyFromPath(path);

    boolean removed = false;
    for (String key : keys) {
      if (key.startsWith(keyToRemove)) {
        removed = getEHCache().remove(key) || removed;
      }
    }
    return removed;
  }

  @Override
  public boolean doPurge() {
    // getEHCache().removeAll();
    // getEHCache().flush();

    // this above is not true anymore, since the "shared-cache" implementor forgot about the fact that using purge()
    // will purge _all_ caches (it purges the one shared!), not just this repo's cache
    return removeWithChildren(RepositoryItemUid.PATH_ROOT);
  }

  @Override
  public CacheStatistics getStatistics() {
    Statistics stats = getEHCache().getStatistics();

    return new CacheStatistics(stats.getObjectCount(), stats.getCacheMisses(), stats.getCacheHits());
  }

  @SuppressWarnings("unchecked")
  @Override
  public Collection<String> listKeysInCache() {
    getEHCache().evictExpiredElements();

    List<String> keys = new ArrayList<String>();

    // this is going to be slow (if we have lots of items) but if you are concerned about speed you shouldn't call
    // this method anyway, this should only be used for information purposes

    String startsWithString = getKeyPrefix();

    for (String key : (List<String>) getEHCache().getKeys()) {
      if (key.startsWith(startsWithString)) {
        keys.add(key.substring(startsWithString.length()));
      }
    }

    return keys;
  }

  @Override
  protected String makeKeyFromPath(String path) {
    path = super.makeKeyFromPath(path);

    return getKeyPrefix() + path;
  }

  protected String getKeyPrefix() {
    return getRepositoryId() + ":";
  }

}
