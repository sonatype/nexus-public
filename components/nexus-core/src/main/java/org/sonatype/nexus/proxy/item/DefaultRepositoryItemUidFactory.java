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
package org.sonatype.nexus.proxy.item;

import java.lang.ref.WeakReference;
import java.util.WeakHashMap;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.proxy.NoSuchRepositoryException;
import org.sonatype.nexus.proxy.events.NexusStoppedEvent;
import org.sonatype.nexus.proxy.registry.RepositoryRegistry;
import org.sonatype.nexus.proxy.repository.Repository;
import org.sonatype.sisu.goodies.eventbus.EventBus;
import org.sonatype.sisu.locks.ResourceLockFactory;

import com.google.common.eventbus.Subscribe;
import org.codehaus.plexus.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A default factory for UIDs.
 * 
 * @author cstamas
 */
@Singleton
@Named
public class DefaultRepositoryItemUidFactory
    implements RepositoryItemUidFactory
{
  private static final Logger log = LoggerFactory.getLogger(DefaultRepositoryItemUidFactory.class);

  private final EventBus eventBus;
  
  private final RepositoryRegistry repositoryRegistry;

  private final ResourceLockFactory sisuLockFactory;

  private final WeakHashMap<DefaultRepositoryItemUidLock, WeakReference<DefaultRepositoryItemUidLock>> locks =
      new WeakHashMap<DefaultRepositoryItemUidLock, WeakReference<DefaultRepositoryItemUidLock>>();

  @Inject
  public DefaultRepositoryItemUidFactory(final EventBus eventBus, final RepositoryRegistry repositoryRegistry,
      final @Nullable @Named("${sisu-resource-locks:-local}") ResourceLockFactory sisuLockFactory)
  {
    this.eventBus = checkNotNull(eventBus);
    this.repositoryRegistry = checkNotNull(repositoryRegistry);
    this.sisuLockFactory = sisuLockFactory;
    log.debug("Lock factory: {}", sisuLockFactory);
    eventBus.register(this);
  }

  @Override
  public DefaultRepositoryItemUid createUid(final Repository repository, String path) {
    // path corrections
    if (!StringUtils.isEmpty(path)) {
      if (!path.startsWith(RepositoryItemUid.PATH_ROOT)) {
        path = RepositoryItemUid.PATH_ROOT + path;
      }

      // ban relative paths
      checkArgument(!path.contains("/.."), "Repository UID path may NOT contain relative tokens: %s", path);
    }
    else {
      path = RepositoryItemUid.PATH_ROOT;
    }

    return new DefaultRepositoryItemUid(this, repository, path);
  }

  @Override
  public DefaultRepositoryItemUid createUid(final String uidStr) throws IllegalArgumentException,
      NoSuchRepositoryException
  {
    if (uidStr.indexOf(":") > -1) {
      String[] parts = uidStr.split(":");

      if (parts.length == 2) {
        Repository repository = repositoryRegistry.getRepository(parts[0]);

        return createUid(repository, parts[1]);
      }
      else {
        throw new IllegalArgumentException(uidStr
            + " is malformed RepositoryItemUid! The proper format is '<repoId>:/path/to/something'.");
      }
    }
    else {
      throw new IllegalArgumentException(uidStr
          + " is malformed RepositoryItemUid! The proper format is '<repoId>:/path/to/something'.");
    }
  }
  
  @Override
  public DefaultRepositoryItemUidLock createUidLock(final RepositoryItemUid uid) {
    final String key = new String(uid.getKey());

    return doCreateUidLockForKey(key);
  }

  protected synchronized DefaultRepositoryItemUidLock doCreateUidLockForKey(final String key) {
    final LockResource lockResource;
    if (sisuLockFactory != null) {
      lockResource = new SisuLockResource(sisuLockFactory.getResourceLock(key));
    }
    else {
      lockResource = new SimpleLockResource();
    }
    final DefaultRepositoryItemUidLock newLock = new DefaultRepositoryItemUidLock(key, lockResource);
    final WeakReference<DefaultRepositoryItemUidLock> oldLockRef = locks.get(newLock);
    if (oldLockRef != null) {
      final RepositoryItemUidLock oldLock = oldLockRef.get();

      if (oldLock != null) {
        return oldLockRef.get();
      }
    }
    locks.put(newLock, new WeakReference<DefaultRepositoryItemUidLock>(newLock));
    return newLock;
  }

  /**
   * For UTs, not to be used in production code!
   */
  protected int locksInMap() {
    return locks.size();
  }

  @Subscribe
  public void on(final NexusStoppedEvent e) {
    eventBus.unregister(this);
    if (sisuLockFactory != null) {
      sisuLockFactory.shutdown();
    }
  }
}
