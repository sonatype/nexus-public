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
package org.sonatype.sisu.locks;

import java.io.File;
import java.io.FileNotFoundException;
import java.lang.management.ManagementFactory;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.management.MBeanServer;
import javax.management.ObjectName;

import com.hazelcast.config.Config;
import com.hazelcast.config.FileSystemXmlConfig;
import com.hazelcast.config.XmlConfigBuilder;
import com.hazelcast.core.DistributedObject;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import com.hazelcast.core.ISemaphore;
import com.hazelcast.core.Member;
import com.hazelcast.core.Partition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Distributed Hazelcast {@link ResourceLockFactory} implementation producing {@link HazelcastResourceLock} instances.
 * <p/>
 * {@link HazelcastResourceLock} instances are backed by Hazelcast {@link ISemaphore} and works pretty much in same
 * way as {@link LocalResourceLock}: initial permit count is {@link Integer#MAX_VALUE}. Still, the extra work happening
 * here is destruction of unused Hazelcast {@link ISemaphore} instances (removal of them from cluster) to prevent
 * memory leak, as cluster-wide created instances are not subject of JVM garbage collect.
 * <p/>
 * Creation of {@link HazelcastResourceLock} steps:
 * <ul>
 * <li>The {@link HazelcastResourceLockFactory#semaphoreLocks} Hazelcast {@link IMap} key with resource lock is
 * locked</li>
 * <li>A ref counter semaphore is get/created (Hazelcast operation)</li>
 * <li>1 permit is acquired from ref counter semaphore (marks +1 reference)</li>
 * <li>A {@link HazelcastResourceLock} backing semaphore is get/created (Hazelcast operation)</li>
 * <li>The {@link HazelcastResourceLockFactory#semaphoreLocks} Hazelcast {@link IMap} key with resource lock name is
 * unlocked</li>
 * </ul>
 * <p/>
 * Maintenance of {@link HazelcastResourceLock} reference counts:
 * <ul>
 * <li>When {@link HazelcastResourceLock} is garbage collected by JVM, it's backing semaphore name is put into {@link
 * HazelcastResourceLockFactory#finalizedSemaphoreNames} queue</li>
 * <li>The queue is scanned by {@link SemaphoreMaintainerRunnable} in a background thread, when a name is found in
 * queue, then</li>
 * <li>1 permit is released to ref counter semaphore (marks -1 reference)</li>
 * </ul>
 * <p/>
 * Removal of {@link HazelcastResourceLock} backing semaphores from cluster:
 * <ul>
 * <li>the cluster is scanned by {@link SemaphoreRemoverRunnable} in a background thread, for all ref counters, for each
 * name</li>
 * <li>if the found ref counter seems unused (has all permits), then</li>
 * <li>the {@link HazelcastResourceLockFactory#semaphoreLocks} Hazelcast {@link IMap} key with resource lock name is
 * locked</li>
 * <li>a non blocking attempt is made to acquire {@link Integer#MAX_VALUE} permits from ref counter semaphore. If
 * succeeds, it means semaphore with this name has 0 references (is unused), then</li>
 * <li>a {@link HazelcastResourceLock} backing semaphore is destroyed (Hazelcast operation)</li>
 * <li>a ref counter semaphore is destroyed (Hazelcast operation)</li>
 * <li>the {@link HazelcastResourceLockFactory#semaphoreLocks} Hazelcast {@link IMap} key with resource lock name is
 * unlocked</li>
 * </ul>
 * <p/>
 * To prevent indefinite lock-ups, all the "book-keeping" locking uses {@code tryLock/tryAcquire} with timeout of
 * {@link HazelcastResourceLockFactory#BLOCK_WAIT_SECONDS}. The returned {@link HazelcastResourceLock} instance
 * uses the {@link ISemaphore#acquire(int)} method, and is subject to indefinite locking, but that is done on purpose
 * to have semantically same behavior as "locks" locks.
 */
@Named("hazelcast")
@Singleton
final class HazelcastResourceLockFactory
    extends AbstractResourceLockFactory
{
  /**
   * The name of {@link IMap} used to synchronize refCounting and semaphore creation and destruction. Is actually
   * and empty map, just using it to obtain "named" locks from Hazelcast.
   */
  private static final String SEMAPHORE_LOCKS_MAP = "nexusSemaphoreLocks";

  /**
   * Prefix added for ref counter {@link ISemaphore} instances. Every reference acquires a permit from this semaphore,
   * if permits available are {@link Integer#MAX_VALUE}, then the semaphore belonging to this ref counter is unused.
   */
  private static final String REFCOUNTER_PREFIX = "nx-lockRef:";

  /**
   * Prefix added for backing {@link ISemaphore} instances.
   */
  private static final String SEMAPHORE_PREFIX = "nx-lock:";

  /**
   * Seconds to block-wait when trying to lock or acquire permits, before giving up. This is applied only to "book
   * keeping": locking around maintenance of UID lock backing ISempahores only.
   */
  private static final long BLOCK_WAIT_SECONDS = Long.parseLong(
      System.getProperty("hazelcast-lock-factory.blockWaitSeconds", "60")
  );

  /**
   * Remover thread sleep millis between sweep cycles when it removes unreferenced semaphores.
   */
  private static final long REMOVER_SLEEP_MILLIS = Long.parseLong(
      System.getProperty("hazelcast-lock-factory.removerSleepMillis", "5000")
  );

  // ----------------------------------------------------------------------
  // Implementation fields
  // ----------------------------------------------------------------------

  private static final Logger log = LoggerFactory.getLogger(HazelcastResourceLockFactory.class);

  private final HazelcastInstance instance;

  private final IMap<String, String> semaphoreLocks;

  private final LinkedBlockingQueue<String> finalizedSemaphoreNames;

  private final Thread semaphoreMaintainerThread;

  private final Thread semaphoreRemoverThread;

  private ObjectName jmxQuery;

  private ObjectName jmxMaster;

  // ----------------------------------------------------------------------
  // Constructors
  // ----------------------------------------------------------------------

  @Inject
  HazelcastResourceLockFactory(@Nullable @Named("hazelcast.config") final File configFile) {
    super(true);

    instance = Hazelcast.newHazelcastInstance(getHazelcastConfig(configFile));
    semaphoreLocks = instance.getMap(SEMAPHORE_LOCKS_MAP);

    // semaphore cleanup
    finalizedSemaphoreNames = new LinkedBlockingQueue<>();
    semaphoreMaintainerThread = new Thread(
        new SemaphoreMaintainerRunnable(),
        "hz-semaphore-maintainer"
    );
    semaphoreRemoverThread = new Thread(
        new SemaphoreRemoverRunnable(),
        "hz-semaphore-remover"
    );

    semaphoreMaintainerThread.start();
    semaphoreRemoverThread.start();

    try {
      final MBeanServer server = ManagementFactory.getPlatformMBeanServer();

      jmxMaster = ObjectName.getInstance(JMX_DOMAIN, "type", category());
      jmxQuery = ObjectName.getInstance(JMX_DOMAIN, properties("type", category(), "hash", "*"));
      if (!server.isRegistered(jmxMaster)) {
        server.registerMBean(new HazelcastResourceLockMBean(instance, jmxQuery), jmxMaster);
      }
    }
    catch (final Exception e) {
      log.warn("Problem registering master LocksMBean for: {}", this, e);
    }
  }

  // ----------------------------------------------------------------------
  // Public methods
  // ----------------------------------------------------------------------

  @Override
  public void shutdown() {
    semaphoreRemoverThread.interrupt();
    semaphoreMaintainerThread.interrupt();
    boolean lastMember = false;
    try {
      super.shutdown();
      final MBeanServer server = ManagementFactory.getPlatformMBeanServer();
      lastMember = server.queryNames(jmxQuery, null).isEmpty();
      if (lastMember) {
        try {
          server.unregisterMBean(jmxMaster);
        }
        catch (final Exception e) {
          log.warn("Problem unregistering master LocksMBean for: {}", this, e);
        }
      }
    }
    finally {
      if (lastMember) {
        Hazelcast.shutdownAll();
      }
      else {
        instance.getLifecycleService().shutdown();
      }
    }
  }

  // ----------------------------------------------------------------------
  // Implementation methods
  // ----------------------------------------------------------------------

  @Override
  protected String category() {
    return HazelcastResourceLock.class.getSimpleName() + 's';
  }

  @Override
  protected ResourceLock createResourceLock(final String name) {
    lock(semaphoreLocks, name);
    // we don't care about the value, we just need the keyset of existing resource lock names
    semaphoreLocks.set(name, "");
    try {
      acquire(refCounterSemaphore(name), 1);
      return new HazelcastResourceLock(name, backingSemaphore(name));
    }
    catch (InterruptedException e) {
      throw new IllegalStateException("Interrupted while acquiring refCount for name '" + name + "'", e);
    }
    finally {
      semaphoreLocks.unlock(name);
    }
  }

  /**
   * @return Hazelcast configuration; ensures semaphores start with {@link Integer#MAX_VALUE} permits by default
   */
  private static Config getHazelcastConfig(final File configFile) {
    final Config config;
    if (null != configFile && configFile.isFile()) {
      try {
        config = new FileSystemXmlConfig(configFile);
      }
      catch (final FileNotFoundException e) {
        throw new IllegalArgumentException(e.getMessage());
      }
    }
    else {
      config = new XmlConfigBuilder().build();
    }

    config.getSemaphoreConfig("default").setInitialPermits(Integer.MAX_VALUE);
    config.setClassLoader(Hazelcast.class.getClassLoader());

    return config;
  }

  /**
   * Returns the refCounter semaphore name corresponding to passed name.
   */
  private static String refCounterName(final String name) {
    return REFCOUNTER_PREFIX + name;
  }

  /**
   * Returns the backing semaphore name corresponding to passed name.
   */
  private static String semaphoreName(final String name) {
    return SEMAPHORE_PREFIX + name;
  }

  /**
   * Returns the {@link ISemaphore} instance used as ref counter for given resource lock name.
   */
  private ISemaphore refCounterSemaphore(final String name) {
    return instance.getSemaphore(refCounterName(name));
  }

  /**
   * Returns the {@link ISemaphore} instance used as backing semaphore for given resource lock name.
   */
  private ISemaphore backingSemaphore(final String name) {
    return instance.getSemaphore(semaphoreName(name));
  }

  /**
   * Applies {@link IMap#tryLock(Object, long, TimeUnit)} to lock with the passed in {@code name}.
   *
   * @throws IllegalStateException if obtaining the lock fails or is interrupted.
   */
  private static void lock(final IMap<String, String> imap, final String name) {
    try {
      if (!imap.tryLock(name, BLOCK_WAIT_SECONDS, TimeUnit.SECONDS)) {
        throw new IllegalStateException(
            "Failed to lock semaphoreLocks='" + name + "' after " + BLOCK_WAIT_SECONDS + "s");
      }
    }
    catch (InterruptedException e) {
      throw new IllegalStateException("Interrupted while locking semaphoreLocks='" + name + "'", e);
    }
  }

  /**
   * Applies {@link ISemaphore#tryAcquire(int, long, TimeUnit)} to semaphore using passed in {@code permits}.
   *
   * @throws InterruptedException  if interrupted.
   * @throws IllegalStateException if acquiring permits fails.
   */
  private static void acquire(final ISemaphore sem, final int permits) throws InterruptedException {
    if (!sem.tryAcquire(permits, BLOCK_WAIT_SECONDS, TimeUnit.SECONDS)) {
      throw new IllegalStateException(
          "Failed to acquire " + permits + " permits from semaphore " + sem.getName());
    }
  }

  /**
   * {@link ResourceLock} implemented on top of a Hazelcast {@link ISemaphore}.
   */
  final class HazelcastResourceLock
      extends AbstractSemaphoreResourceLock
  {
    // ----------------------------------------------------------------------
    // Implementation fields
    // ----------------------------------------------------------------------

    private final String name;

    private final ISemaphore sem;

    // ----------------------------------------------------------------------
    // Constructors
    // ----------------------------------------------------------------------

    HazelcastResourceLock(final String name, final ISemaphore sem) {
      this.name = name;
      this.sem = sem;
    }

    // ----------------------------------------------------------------------
    // Semaphore methods
    // ----------------------------------------------------------------------

    @Override
    protected void acquire(final int permits) {
      while (true) {
        try {
          sem.acquire(permits);
          return;
        }
        catch (final InterruptedException e) {
          Thread.currentThread().interrupt();
        }
      }
    }

    @Override
    protected void release(final int permits) {
      sem.release(permits);
    }

    @Override
    protected int availablePermits() {
      return sem.availablePermits();
    }

    @Override
    protected void finalize() throws Throwable {
      try {
        finalizedSemaphoreNames.add(name);
      }
      finally {
        super.finalize();
      }
    }
  }

  /**
   * Semaphore refCounter maintainer runnable, that runs in a separate thread and maintains ref counts based on JVM GC.
   */
  final class SemaphoreMaintainerRunnable
      implements Runnable
  {
    @Override
    public void run() {
      while (true) {
        try {
          final String name = finalizedSemaphoreNames.take();
          refCounterSemaphore(name).release();
        }
        catch (InterruptedException e) {
          log.info("Interrupted Hazelcast semaphore maintainer");
          break;
        }
        catch (Exception e) {
          log.error("Unexpected bad thing happened; continuing", e);
        }
      }
    }
  }

  /**
   * Semaphore remover runnable, that runs in a separate thread and removes semaphores with zero references. It sweeps
   * cluster-wide distributed objects, but operates only on local stored ref counter semaphores. If one found, it
   * performs the normal steps to remove the unused objects (backing semaphore, ref counter semaphore).
   */
  final class SemaphoreRemoverRunnable
      implements Runnable
  {
    @Override
    public void run() {
      while (true) {
        try {
          Thread.sleep(REMOVER_SLEEP_MILLIS);
          int totalCount = 0;
          int localCount = 0;
          int unusedCount = 0;
          int removedCount = 0;
          for (String name : semaphoreLocks.keySet()) {
            lock(semaphoreLocks, name);
            try {
              if (!semaphoreLocks.containsKey(name)) {
                // evicted during for-loop by some other member?
                // theoretically, during loop we could have a join causing repartition...
                // without this check, we could re-create the ref counter and backing semaphores here...
                continue;
              }
              totalCount++;
              ISemaphore refCounter = refCounterSemaphore(name);
              if (isLocallyStored(refCounter)) {
                localCount++;
                // do not bother with referenced instances, only with those seemingly unreferenced
                if (refCounter.availablePermits() != Integer.MAX_VALUE) {
                  continue;
                }
                ISemaphore sem = backingSemaphore(name);
                if (refCounter.tryAcquire(Integer.MAX_VALUE)) {
                  unusedCount++;
                  if (sem.tryAcquire(Integer.MAX_VALUE)) {
                    // destroy it, is unused
                    log.trace("Removing stale semaphore {}", name);
                    sem.destroy();
                    refCounter.destroy();
                    semaphoreLocks.delete(name);
                    removedCount++;
                  }
                  else {
                    // this is probably a bug or after failed partition?
                    log.warn("Found semaphore with 0 references but incomplete permits: {}; not removing it", name);
                  }
                }
              }
            }
            finally {
              semaphoreLocks.unlock(name);
            }
          }
          log.debug("HZ-Semaphores: removed={}, unused={}, local={}, total={}",
              removedCount, unusedCount, localCount, totalCount);
        }
        catch (InterruptedException e) {
          log.info("Interrupted Hazelcast semaphore remover");
          break;
        }
        catch (Exception e) {
          log.error("Unexpected bad thing happened; continuing", e);
        }
      }
    }

    /**
     * Returns {@code true} if passed in distributed object is mapped to a partition that is stored on local member.
     */
    private boolean isLocallyStored(final DistributedObject distributedObject) {
      Partition partition = instance.getPartitionService().getPartition(distributedObject.getPartitionKey());
      if (partition != null) {
        Member owner = partition.getOwner();
        if (owner != null) {
          return owner.localMember();
        }
      }
      return false; // lie but be on safe side
    }
  }
}
