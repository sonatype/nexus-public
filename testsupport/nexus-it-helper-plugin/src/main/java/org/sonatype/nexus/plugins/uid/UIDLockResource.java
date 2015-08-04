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
package org.sonatype.nexus.plugins.uid;

import java.util.Map;
import java.util.concurrent.CountDownLatch;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.ws.rs.Path;

import org.sonatype.nexus.proxy.NoSuchRepositoryException;
import org.sonatype.nexus.proxy.access.Action;
import org.sonatype.nexus.proxy.item.RepositoryItemUid;
import org.sonatype.nexus.proxy.registry.RepositoryRegistry;
import org.sonatype.nexus.proxy.repository.Repository;
import org.sonatype.nexus.rest.ResourceSupport;
import org.sonatype.plexus.rest.resource.PathProtectionDescriptor;
import org.sonatype.plexus.rest.resource.PlexusResource;

import com.google.common.collect.Maps;
import org.restlet.Context;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.data.Status;
import org.restlet.resource.ResourceException;
import org.restlet.resource.Variant;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * UID locking resource. Allows locking/unlocking of repository items.
 *
 * @since 2.2
 */
@Named
@Singleton
@Path(UIDLockResource.RESOURCE_URI)
public class UIDLockResource
    extends ResourceSupport
    implements PlexusResource // required for component to get configured?
{

  public static final String REPOSITORY = "repository";

  public static final String LOCK_TYPE = "lockType";

  public static final String PATH = "path";

  public static final String RESOURCE_URI =
      "/nexus-it-helper-plugin/uid/lock/{" + REPOSITORY + "}/{" + LOCK_TYPE + "}";

  private final RepositoryRegistry repositoryRegistry;

  /**
   * Map between repository items identifiers and locking threads.
   * Never null.
   */
  private final Map<String, LockThread> lockThreads;

  /**
   * Constructor for Enunciate documentation generation only.
   */
  @SuppressWarnings("UnusedDeclaration")
  public UIDLockResource() {
    throw new Error();
  }

  @Inject
  public UIDLockResource(final RepositoryRegistry repositoryRegistry) {
    this.repositoryRegistry = checkNotNull(repositoryRegistry);
    this.lockThreads = Maps.newHashMap();
    setRequireStrictChecking(false);
  }

  @Override
  public PathProtectionDescriptor getResourceProtection() {
    return null;
  }

  @Override
  public synchronized Object get(Context context, Request request, Response response, Variant variant)
      throws ResourceException
  {
    try {
      // first unlock if already locked
      delete(context, request, response);

      // then lock it for specified action
      final LockThread lockThread = new LockThread(
          getRepository(request), getPath(request), getLockType(request)
      );
      lockThreads.put(key(getRepository(request), getPath(request)), lockThread);
      lockThread.lock();
    }
    catch (final NoSuchRepositoryException e) {
      throw new ResourceException(Status.CLIENT_ERROR_NOT_FOUND);
    }
    return null;
  }

  @Override
  public synchronized void delete(final Context context, final Request request, final Response response)
      throws ResourceException
  {
    final LockThread lockThread = lockThreads.remove(key(getRepository(request), getPath(request)));
    if (lockThread != null) {
      lockThread.unlock();
    }
  }

  private String key(final String repositoryId, final String path) {
    return String.format("%s:%s", repositoryId, path);
  }

  private String getRepository(final Request request) {
    return request.getAttributes().get(REPOSITORY).toString();
  }

  private String getLockType(final Request request) {
    return request.getAttributes().get(LOCK_TYPE).toString();
  }

  private String getPath(final Request request) {
    return request.getResourceRef().getRemainingPart(false, false);
  }

  private class LockThread
      extends Thread
  {

    private final RepositoryItemUid uid;

    private final String lockType;

    private final CountDownLatch latch;

    private LockThread(final String repositoryId, final String path, final String lockType)
        throws NoSuchRepositoryException
    {
      this.lockType = lockType;
      final Repository repository = repositoryRegistry.getRepository(repositoryId);
      uid = repository.createUid(path);
      latch = new CountDownLatch(1);
    }

    private void lock() {
      this.start();
    }

    private void unlock() {
      latch.countDown();
    }

    @Override
    public void run() {
      try {
        uid.getLock().lock(Action.valueOf(lockType));
        log.info("Locked {} for {}", uid, lockType);
        latch.await();
        uid.getLock().unlock();
        log.info("Unlocked {}", uid);
      }
      catch (InterruptedException e) {
        // do nothing
      }
    }

  }

}
