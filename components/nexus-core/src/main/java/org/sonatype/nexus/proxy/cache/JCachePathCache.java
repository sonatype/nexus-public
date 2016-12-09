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
import java.util.Iterator;

import javax.cache.Cache;
import javax.cache.Cache.Entry;

import org.sonatype.nexus.proxy.item.RepositoryItemUid;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * HZ cache backed cache.
 *
 * @author cstamas
 */
public class JCachePathCache
    extends AbstractPathCache
{
  private final Cache<String, Boolean> cache;

  private final int ttl;

  public JCachePathCache(final Cache<String, Boolean> cache, final int ttl) {
    this.cache = checkNotNull(cache);
    this.ttl = ttl;
  }

  @Override
  public int ttl() {
    return ttl;
  }

  @Override
  public boolean doContains(final String key) {
    return cache.containsKey(key);
  }

  @Override
  public boolean doIsExpired(final String key) {
    return !cache.containsKey(key);
  }

  @Override
  public long doGetExpirationTime(final String key) {
    // TODO: JCache API cannot do this: get expiration time of this, it'd need verndor specific calls!
    return System.currentTimeMillis() + ttl; // TODO: we lie
  }

  @Override
  public void doPut(final String key, final Boolean element, final int expiration) {
    cache.put(key, element);
  }

  @Override
  public boolean doRemove(String key) {
    return cache.remove(key);
  }

  @Override
  public boolean removeWithChildren(String path) {
    Collection<String> keys = listKeysInCache();
    String keyToRemove = makeKeyFromPath(path);
    boolean removed = false;
    for (String key : keys) {
      if (key.startsWith(keyToRemove)) {
        removed = cache.remove(key) || removed;
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
    return new CacheStatistics(0, 0, 0);
  }

  @SuppressWarnings("unchecked")
  @Override
  public Collection<String> listKeysInCache() {
    Iterator<Entry<String, Boolean>> entryIterator = cache.iterator();
    ArrayList<String> keys = new ArrayList<>();
    while (entryIterator.hasNext()) {
      Entry<String, Boolean> entry = entryIterator.next();
      if (entry != null) {
        keys.add(entry.getKey());
      }
    }
    return keys;
  }

  @Override
  public void destroy() {
    cache.close();
  }
}
