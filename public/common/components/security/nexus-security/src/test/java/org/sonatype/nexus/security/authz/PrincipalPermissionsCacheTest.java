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
package org.sonatype.nexus.security.authz;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import org.apache.shiro.authz.Permission;
import org.apache.shiro.authz.permission.WildcardPermission;
import org.apache.shiro.subject.PrincipalCollection;
import org.apache.shiro.subject.SimplePrincipalCollection;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link PrincipalPermissionsCache}, the per-realm cache of a principal's expanded permissions. Each
 * authorizing realm owns one instance, so these tests exercise a single realm's cache keyed by principals alone.
 */
class PrincipalPermissionsCacheTest
{
  private static final String REALM = "TestRealm";

  private static final Permission READ = new WildcardPermission("app:config:read");

  private static final Permission CREATE = new WildcardPermission("app:config:create");

  private static final Permission EDIT = new WildcardPermission("app:config:edit");

  private static final Permission DELETE = new WildcardPermission("app:config:delete");

  private final PrincipalCollection alice = new SimplePrincipalCollection("alice", REALM);

  private final PrincipalCollection bob = new SimplePrincipalCollection("bob", REALM);

  /** A non-{@link WildcardPermission} implementation to exercise the non-wildcard code paths. */
  private static final class AlwaysGrantPermission
      implements Permission
  {
    @Override
    public boolean implies(final Permission p) {
      return true;
    }
  }

  private static final class NeverGrantPermission
      implements Permission
  {
    @Override
    public boolean implies(final Permission p) {
      return false;
    }
  }

  /**
   * Grants everything and counts how many times {@code implies()} is invoked, to observe results-memo effectiveness.
   */
  private static final class CountingGrantPermission
      implements Permission
  {
    private final AtomicInteger impliesCalls = new AtomicInteger();

    @Override
    public boolean implies(final Permission p) {
      impliesCalls.incrementAndGet();
      return true;
    }

    int impliesCalls() {
      return impliesCalls.get();
    }
  }

  private static PrincipalPermissionsCache enabledCache() {
    // maximumSize=250, expireAfterWrite=60m, recordStats=false, concurrencyLevel=16
    return new PrincipalPermissionsCache(true, 250, Duration.ofMinutes(60), false, 16);
  }

  private static PrincipalPermissionsCache disabledCache() {
    return new PrincipalPermissionsCache(false, -1, Duration.ZERO, false, 1);
  }

  /**
   * Supplier that records how many times it is asked to expand permissions, so tests can assert the cache resolves
   * a principal's permission set at most once.
   */
  private static final class CountingSupplier
      implements Supplier<Collection<Permission>>
  {
    private final AtomicInteger calls = new AtomicInteger();

    private final Collection<Permission> permissions;

    CountingSupplier(final Permission... permissions) {
      this.permissions = List.of(permissions);
    }

    @Override
    public Collection<Permission> get() {
      calls.incrementAndGet();
      return permissions;
    }

    int calls() {
      return calls.get();
    }
  }

  @Test
  void isEnabledReflectsConstructorArgument() {
    assertTrue(enabledCache().isEnabled());
    assertFalse(disabledCache().isEnabled());
  }

  @Test
  void permitsGrantedAndDeniesUngranted() {
    PrincipalPermissionsCache cache = enabledCache();
    CountingSupplier supplier = new CountingSupplier(READ);

    assertTrue(cache.isPermitted(alice, READ, supplier));
    assertFalse(cache.isPermitted(alice, CREATE, supplier));
  }

  @Test
  void resolvesPermissionsOncePerPrincipalRegardlessOfChecks() {
    PrincipalPermissionsCache cache = enabledCache();
    CountingSupplier supplier = new CountingSupplier(READ);

    for (int i = 0; i < 25; i++) {
      assertTrue(cache.isPermitted(alice, READ, supplier));
      assertFalse(cache.isPermitted(alice, CREATE, supplier));
      assertFalse(cache.isPermitted(alice, DELETE, supplier));
    }

    // PermissionsState is built once for alice; every later check is a cache hit.
    assertEquals(1, supplier.calls());
  }

  @Test
  void scopesCacheEntriesByPrincipal() {
    PrincipalPermissionsCache cache = enabledCache();
    CountingSupplier alicePerms = new CountingSupplier(READ);
    CountingSupplier bobPerms = new CountingSupplier(EDIT);

    assertTrue(cache.isPermitted(alice, READ, alicePerms));
    assertFalse(cache.isPermitted(alice, EDIT, alicePerms));

    assertTrue(cache.isPermitted(bob, EDIT, bobPerms));
    assertFalse(cache.isPermitted(bob, READ, bobPerms));

    assertEquals(1, alicePerms.calls());
    assertEquals(1, bobPerms.calls());
  }

  @Test
  void matchesWildcardPermissions() {
    PrincipalPermissionsCache cache = enabledCache();
    CountingSupplier supplier = new CountingSupplier(new WildcardPermission("app:*"));

    assertTrue(cache.isPermitted(alice, READ, supplier));
    assertTrue(cache.isPermitted(alice, new WildcardPermission("app:ui:list"), supplier));
    assertFalse(cache.isPermitted(alice, new WildcardPermission("other:thing:read"), supplier));
  }

  @Test
  void matchesCommaFormMultiActionPermissions() {
    PrincipalPermissionsCache cache = enabledCache();
    // Comma-form grant must go through WildcardPermission.implies() so each action is evaluated.
    CountingSupplier supplier = new CountingSupplier(new WildcardPermission("app:config:read,edit"));

    assertTrue(cache.isPermitted(alice, READ, supplier));
    assertTrue(cache.isPermitted(alice, EDIT, supplier));
    assertFalse(cache.isPermitted(alice, DELETE, supplier));
  }

  @Test
  void matchesExactPermissionViaFastPath() {
    PrincipalPermissionsCache cache = enabledCache();
    CountingSupplier supplier = new CountingSupplier(READ);

    assertTrue(cache.isPermitted(alice, READ, supplier));
    assertFalse(cache.isPermitted(alice, CREATE, supplier));
  }

  @Test
  void deniesWhenNoPermissionsGranted() {
    PrincipalPermissionsCache cache = enabledCache();
    CountingSupplier empty = new CountingSupplier();

    assertFalse(cache.isPermitted(alice, READ, empty));
  }

  @Test
  void handlesNonWildcardPermissionGrants() {
    PrincipalPermissionsCache cache = enabledCache();
    // A non-WildcardPermission grant is treated as a wildcard-style grant (iterative implies()).
    CountingSupplier grantAll = new CountingSupplier(new AlwaysGrantPermission());
    CountingSupplier grantNone = new CountingSupplier(new NeverGrantPermission());

    assertTrue(cache.isPermitted(alice, READ, grantAll));
    assertFalse(cache.isPermitted(bob, READ, grantNone));
  }

  @Test
  void evaluatesNonWildcardCheckedPermission() {
    PrincipalPermissionsCache cache = enabledCache();
    // The checked permission itself is not a WildcardPermission: the exact-match fast path is skipped.
    CountingSupplier supplier = new CountingSupplier(new WildcardPermission("app:*"));

    assertFalse(cache.isPermitted(alice, new NeverGrantPermission(), supplier));
  }

  @Test
  void evaluatesWildcardCheckedPermissionSkippingFastPath() {
    PrincipalPermissionsCache cache = enabledCache();
    CountingSupplier supplier = new CountingSupplier(new WildcardPermission("app:*"));

    // The checked permission contains '*', so the exact-match fast path is skipped and the wildcard loop runs.
    assertTrue(cache.isPermitted(alice, new WildcardPermission("app:*"), supplier));
  }

  @Test
  void deniesWhenSupplierReturnsNullPermissions() {
    // Disabled cache routes through linearScan, which must tolerate a null permission collection.
    PrincipalPermissionsCache cache = disabledCache();
    Supplier<Collection<Permission>> nullSupplier = () -> null;

    assertFalse(cache.isPermitted(alice, READ, nullSupplier));
  }

  @Test
  void whenDisabledEvaluatesEveryTimeWithoutCaching() {
    PrincipalPermissionsCache cache = disabledCache();
    CountingSupplier supplier = new CountingSupplier(READ);

    assertTrue(cache.isPermitted(alice, READ, supplier));
    assertTrue(cache.isPermitted(alice, READ, supplier));
    assertFalse(cache.isPermitted(alice, CREATE, supplier));

    // No caching: the supplier is consulted on every check.
    assertEquals(3, supplier.calls());
  }

  @Test
  void whenPrincipalsNullEvaluatesWithoutCaching() {
    PrincipalPermissionsCache cache = enabledCache();
    CountingSupplier supplier = new CountingSupplier(READ);

    assertTrue(cache.isPermitted(null, READ, supplier));
    assertTrue(cache.isPermitted(null, READ, supplier));
    assertFalse(cache.isPermitted(null, CREATE, supplier));

    // Null principals cannot be used as a cache key, so each check evaluates directly.
    assertEquals(3, supplier.calls());
  }

  @Test
  void batchResolvesPermissionSetOnceAndAnswersEachPermission() {
    PrincipalPermissionsCache cache = enabledCache();
    CountingSupplier supplier = new CountingSupplier(new WildcardPermission("app:config:read"), EDIT);

    boolean[] result = cache.isPermitted(alice, List.of(READ, EDIT, DELETE), supplier);

    assertArrayEquals(new boolean[]{true, true, false}, result);
    // Whole batch answered from a single expansion of the permission set.
    assertEquals(1, supplier.calls());
  }

  @Test
  void batchWithNullOrEmptyReturnsEmptyWithoutResolving() {
    PrincipalPermissionsCache cache = enabledCache();
    CountingSupplier supplier = new CountingSupplier(READ);

    assertEquals(0, cache.isPermitted(alice, List.of(), supplier).length);
    assertEquals(0, cache.isPermitted(alice, (List<Permission>) null, supplier).length);
    assertEquals(0, supplier.calls());
  }

  @Test
  void batchWhenDisabledResolvesOncePerBatch() {
    PrincipalPermissionsCache cache = disabledCache();
    CountingSupplier supplier = new CountingSupplier(READ);

    boolean[] result = cache.isPermitted(alice, List.of(READ, CREATE), supplier);

    assertArrayEquals(new boolean[]{true, false}, result);
    // Even uncached, the permission set is expanded once for the batch rather than per permission.
    assertEquals(1, supplier.calls());
  }

  @Test
  void invalidateAllForcesReResolution() {
    PrincipalPermissionsCache cache = enabledCache();
    CountingSupplier supplier = new CountingSupplier(READ);

    assertTrue(cache.isPermitted(alice, READ, supplier));
    assertEquals(1, supplier.calls());

    cache.invalidateAll();

    assertTrue(cache.isPermitted(alice, READ, supplier));
    assertEquals(2, supplier.calls());
  }

  @Test
  void invalidateAllClearsEveryPrincipalsEntries() {
    // invalidateAll() must evict entries for every principal, not just one; each principal then re-resolves.
    PrincipalPermissionsCache cache = enabledCache();
    CountingSupplier alicePerms = new CountingSupplier(READ);
    CountingSupplier bobPerms = new CountingSupplier(EDIT);

    assertTrue(cache.isPermitted(alice, READ, alicePerms));
    assertTrue(cache.isPermitted(bob, EDIT, bobPerms));
    assertEquals(1, alicePerms.calls());
    assertEquals(1, bobPerms.calls());

    cache.invalidateAll();

    assertTrue(cache.isPermitted(alice, READ, alicePerms));
    assertTrue(cache.isPermitted(bob, EDIT, bobPerms));
    assertEquals(2, alicePerms.calls());
    assertEquals(2, bobPerms.calls());
  }

  @Test
  void concurrentIsPermittedBuildsPermissionsStateExactlyOnce() throws Exception {
    // The cache's whole premise: under a concurrent login burst for the same principal, the expensive permission
    // expansion runs at most once. Fan out N threads that hit the same key simultaneously and assert the supplier is
    // invoked exactly once (get(key, Callable) single-flights the load).
    PrincipalPermissionsCache cache = enabledCache();
    AtomicInteger calls = new AtomicInteger();
    Supplier<Collection<Permission>> slowSupplier = () -> {
      calls.incrementAndGet();
      try {
        Thread.sleep(50); // widen the race window so a broken (non-atomic) build would double-count
      }
      catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      }
      return List.of(READ);
    };

    int threads = 16;
    ExecutorService pool = Executors.newFixedThreadPool(threads);
    try {
      CountDownLatch start = new CountDownLatch(1);
      List<Future<Boolean>> futures = new ArrayList<>();
      for (int i = 0; i < threads; i++) {
        futures.add(pool.submit(() -> {
          start.await();
          return cache.isPermitted(alice, READ, slowSupplier);
        }));
      }
      start.countDown(); // release all threads at once
      for (Future<Boolean> f : futures) {
        assertTrue(f.get(5, TimeUnit.SECONDS));
      }
    }
    finally {
      pool.shutdownNow();
    }

    assertEquals(1, calls.get());
  }

  @Test
  void memoHitsAcrossValueEqualWildcardInstances() {
    // Regression guard against the assumption that the results memo is useless for WildcardPermission checks.
    // Shiro's WildcardPermission overrides equals()/hashCode() on its parsed parts (value equality), so checking the
    // same permission STRING via two DISTINCT WildcardPermission instances must hit the memo and NOT re-scan grants.
    PrincipalPermissionsCache cache = enabledCache();
    CountingGrantPermission grant = new CountingGrantPermission();
    CountingSupplier supplier = new CountingSupplier(grant);

    assertTrue(cache.isPermitted(alice, new WildcardPermission("app:config:read"), supplier));
    int afterFirst = grant.impliesCalls();
    assertTrue(afterFirst >= 1); // first (cold) check scans the grant

    // Distinct instance, identical permission string -> value-equal memo key -> no additional grant scan.
    assertTrue(cache.isPermitted(alice, new WildcardPermission("app:config:read"), supplier));
    assertEquals(afterFirst, grant.impliesCalls());
    assertEquals(1, supplier.calls()); // permission set expanded exactly once
  }

  @Test
  void enabledCacheToleratesNullSupplier() {
    // The enabled path must tolerate a null permission collection just as the disabled linearScan path does.
    PrincipalPermissionsCache cache = enabledCache();
    Supplier<Collection<Permission>> nullSupplier = () -> null;

    assertFalse(cache.isPermitted(alice, READ, nullSupplier));
  }

  @Test
  void enabledCacheWithZeroExpiryFailsFast() {
    // Guava treats a zero expiry as "expire immediately", silently disabling the cache; reject it (and a negative) up
    // front.
    assertThrows(IllegalArgumentException.class,
        () -> new PrincipalPermissionsCache(true, 250, Duration.ZERO, false, 16));
    assertThrows(IllegalArgumentException.class,
        () -> new PrincipalPermissionsCache(true, 250, Duration.ofMinutes(-1), false, 16));
    // A disabled cache imposes no such constraint (zero expiry is irrelevant when nothing is cached).
    assertFalse(new PrincipalPermissionsCache(false, -1, Duration.ZERO, false, 1).isEnabled());
  }

  @Test
  void invalidationRacingAnInflightResolveIsNotPersistedAsStale() throws Exception {
    // TOCTOU guard (NEXUS-53719): an invalidation firing mid-resolve must not persist the now-stale state. Guava's
    // get(key, Callable) re-inserts a value even if the entry was cleared during the load, so the generation guard is
    // what detects the raced build and drops it. Drive a slow resolve, invalidate mid-flight, assert re-resolution.
    PrincipalPermissionsCache cache = enabledCache();
    CountDownLatch supplierEntered = new CountDownLatch(1);
    CountDownLatch invalidationDone = new CountDownLatch(1);
    Supplier<Collection<Permission>> slow = () -> {
      supplierEntered.countDown();
      try {
        // Deterministically hold the in-flight resolve open until invalidateAll() has completed, so the build
        // provably straddles the invalidation (no reliance on wall-clock timing / no CI flakiness).
        invalidationDone.await(5, TimeUnit.SECONDS);
      }
      catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      }
      return List.of(READ);
    };

    ExecutorService pool = Executors.newSingleThreadExecutor();
    try {
      Future<Boolean> inflight = pool.submit(() -> cache.isPermitted(alice, READ, slow));
      supplierEntered.await();
      cache.invalidateAll(); // bumps the generation immediately, then clears
      invalidationDone.countDown(); // release the resolve now that the invalidation has completed
      assertTrue(inflight.get(5, TimeUnit.SECONDS)); // the in-flight call still answers from what it resolved
    }
    finally {
      pool.shutdownNow();
    }

    // Nothing stale was persisted: a fresh check with empty grants must be denied and must re-resolve.
    CountingSupplier afterRevoke = new CountingSupplier();
    assertFalse(cache.isPermitted(alice, READ, afterRevoke));
    assertEquals(1, afterRevoke.calls());
  }

  @Test
  void resultMemoStaysBoundedForResourceParameterisedPermissions() {
    // The per-entry result memo is a bounded LRU (MAX_RESULTS); beyond the cap the least-recently-checked results
    // evict, but the permission set itself is still expanded exactly once. Exercise well past the 4096 cap.
    PrincipalPermissionsCache cache = enabledCache();
    CountingSupplier supplier = new CountingSupplier(new WildcardPermission("app:*"));

    for (int i = 0; i < 4200; i++) {
      assertTrue(cache.isPermitted(alice, new WildcardPermission("app:res" + i + ":read"), supplier));
    }
    // The permission set was still expanded exactly once despite thousands of distinct checks.
    assertEquals(1, supplier.calls());
  }

  @Test
  void shorterGrantImpliesLongerPermission() {
    // Shiro treats missing trailing parts as wildcards: a wildcard-free grant "app:config" must imply the longer
    // "app:config:read". The exact-match fast path must NOT swallow this (regression guard for the shorter-implies-
    // longer wildcard semantics).
    PrincipalPermissionsCache cache = enabledCache();
    CountingSupplier supplier = new CountingSupplier(new WildcardPermission("app:config"));

    assertTrue(cache.isPermitted(alice, new WildcardPermission("app:config:read"), supplier));
    assertTrue(cache.isPermitted(alice, new WildcardPermission("app:config"), supplier));
    assertFalse(cache.isPermitted(alice, new WildcardPermission("app:other:read"), supplier));
  }
}
