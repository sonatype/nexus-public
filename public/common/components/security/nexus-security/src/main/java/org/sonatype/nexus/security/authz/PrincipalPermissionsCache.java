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
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.util.concurrent.UncheckedExecutionException;
import org.apache.shiro.authz.Permission;
import org.apache.shiro.authz.permission.WildcardPermission;
import org.apache.shiro.subject.PrincipalCollection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * Per-realm cache of a principal's fully-expanded permissions. One instance is owned by each
 * {@link PermissionCachingAuthorizingRealm} (created via {@link PrincipalPermissionsCacheFactory}); the cache is never
 * shared between realms.
 *
 * <p>
 * Shiro's stock
 * {@link org.apache.shiro.realm.AuthorizingRealm#isPermitted(Permission, org.apache.shiro.authz.AuthorizationInfo)}
 * re-expands every role into its permission set for <em>each</em> permission checked. The UI permission load checks the
 * entire privilege catalogue in a single batch, so for a user backed by many roles/privileges this degrades to
 * O(requested x granted) role resolutions and dominates login time (NEXUS-53719, generalising the NEXUS-52583 cache
 * that previously only lived in the local authorizing realm).
 *
 * <p>
 * Each authorizing realm owns its own cache instance, so entries are keyed by {@link PrincipalCollection} alone - there
 * is no cross-realm sharing to guard against, and one realm's invalidation never disturbs another realm's entries.
 * (Shiro ORs authorization across realms, so a single external-realm user is authorized by both its home realm and the
 * local authorizing realm - two distinct permission sets - but those now live in two distinct cache instances.)
 *
 * <p>
 * Invalidation is driven centrally by {@code RealmManagerImpl}, which owns the single set of authorization-change
 * event handlers and clears each realm's Shiro authorization cache alongside its {@link #invalidateAll()} here, so the
 * two never drift. A realm may also clear its own cache directly for realm-specific events (e.g. an LDAP cache clear).
 */
public class PrincipalPermissionsCache
{
  private static final Logger log = LoggerFactory.getLogger(PrincipalPermissionsCache.class);

  // Cap on the per-entry result memo: permissions can be resource-parameterised (repo name, selector, tokenId, ...),
  // so growth is not bounded by the privilege catalogue. The memo is a bounded LRU, so beyond the cap the least-
  // recently-checked results evict rather than repeated hot checks being permanently starved (NEXUS-53719).
  private static final int MAX_RESULTS = 4096;

  /**
   * Bundles the expanded permission collection for a principal with indexed lookups.
   * Separates exact permissions (no wildcards) for O(1) HashSet lookup from wildcard
   * permissions that need iterative matching (NEXUS-52583 optimization).
   */
  private static final class PermissionsState
  {
    // Exact single-action permission strings (lowercase) → fast-path for an exact-equality TRUE.
    final Set<String> exactPermissions;

    // ALL granted permissions — the authoritative source for implies() matching. A wildcard-free grant such as
    // "nexus:foo" must still imply a longer check like "nexus:foo:read" (Shiro treats missing trailing parts as
    // wildcards), so every grant participates here; exactPermissions is only an O(1) shortcut, never the sole source.
    final List<Permission> allPermissions;

    // Bounded-LRU result memo (Permission -> Boolean): recently-checked results stay hot while cold, one-shot
    // (resource-parameterised) permissions evict, so a flood of unique checks cannot permanently starve repeated hot
    // checks (NEXUS-53719). Discarded with its outer cache entry.
    final Cache<Permission, Boolean> results = CacheBuilder.newBuilder().maximumSize(MAX_RESULTS).build();

    // Invalidation generation sampled at build time; if it has since advanced, an invalidation raced us (see resolve).
    final long generation;

    PermissionsState(final Collection<Permission> permissions, final long generation) {
      this.generation = generation;
      this.exactPermissions = new HashSet<>();
      this.allPermissions = new ArrayList<>(permissions.size());

      for (Permission perm : permissions) {
        allPermissions.add(perm);
        if (perm instanceof WildcardPermission) {
          String permStr = perm.toString().toLowerCase();
          // Only single-action, wildcard-free grants qualify for the exact-equality fast path. Comma-form
          // ("nexus:settings:read,update") and '*' grants must be matched via WildcardPermission.implies() below.
          if (!permStr.contains("*") && !permStr.contains(",")) {
            exactPermissions.add(permStr);
          }
        }
      }
    }

    /**
     * Check if this state implies the given permission. Fast path returns TRUE on exact string equality; otherwise
     * every granted permission is evaluated via {@link Permission#implies} so Shiro wildcard semantics (including
     * shorter-implies-longer) are preserved.
     */
    boolean implies(final Permission permission) {
      // Fast path: an exact-equality grant always implies the checked permission.
      if (permission instanceof WildcardPermission) {
        String permStr = permission.toString().toLowerCase();
        if (!permStr.contains("*") && exactPermissions.contains(permStr)) {
          return true;
        }
      }

      // Authoritative path: full Shiro implies() over every granted permission.
      for (Permission granted : allPermissions) {
        if (granted.implies(permission)) {
          return true;
        }
      }
      return false;
    }
  }

  private final Cache<PrincipalCollection, PermissionsState> cache;

  private final boolean enabled;

  // Monotonic invalidation counter, bumped before each clear so a PermissionsState built from pre-invalidation data
  // can be detected and dropped rather than persisted, closing the invalidation TOCTOU window (NEXUS-53719).
  private final AtomicLong generation = new AtomicLong();

  public PrincipalPermissionsCache(
      final boolean enabled,
      final int cacheMaximumSize,
      final Duration expireAfterWrite,
      final boolean cacheRecordStats,
      final int cacheConcurrencyLevel)
  {
    this.enabled = enabled;

    if (enabled) {
      // Guava treats a zero expiry as "expire immediately", silently disabling the cache; fail fast instead. A single
      // write-based TTL (reusing nexus.shiro.cache.defaultTimeToLive) bounds staleness at the same interval as the
      // Shiro authz/role cache, so expanded permissions never outlive the roles they came from (NEXUS-53719).
      checkArgument(expireAfterWrite != null && !expireAfterWrite.isZero() && !expireAfterWrite.isNegative(),
          "PrincipalPermissionsCache expireAfterWrite must be positive when the cache is enabled (was %s)",
          expireAfterWrite);
    }

    // maximumSize + a write-based TTL bound memory, so no softValues() is needed; the generation guard's
    // remove(key, state) still compares by identity (PermissionsState has no equals override).
    CacheBuilder<Object, Object> cacheBuilder = CacheBuilder.newBuilder()
        .concurrencyLevel(cacheConcurrencyLevel);

    if (enabled) {
      cacheBuilder.expireAfterWrite(expireAfterWrite.toMillis(), TimeUnit.MILLISECONDS);
    }

    // Apply maximum size if configured. A value <= 0 (e.g. -1) intentionally removes the size cap entirely - with no
    // softValues(), the write-based TTL (expireAfterWrite) is then the ONLY eviction trigger, so an unbounded cache on
    // a high-cardinality realm can grow until entries age out. Prefer a positive per-realm size for such realms.
    if (cacheMaximumSize > 0) {
      cacheBuilder.maximumSize(cacheMaximumSize);
    }

    if (cacheRecordStats) {
      cacheBuilder.recordStats();
    }

    this.cache = cacheBuilder.<PrincipalCollection, PermissionsState>build();

    if (enabled) {
      log.debug("PrincipalPermissionsCache enabled: maximumSize={}, expireAfterWrite={}, recordStats={}",
          cacheMaximumSize, expireAfterWrite, cacheRecordStats);
    }
    else {
      log.debug("PrincipalPermissionsCache disabled");
    }
  }

  public boolean isEnabled() {
    return enabled;
  }

  /**
   * Answers whether {@code permission} is implied by the permissions resolved for {@code principals}, resolving and
   * caching the permission set on first use.
   *
   * <p>
   * The cache key relies on {@code PrincipalCollection} <em>value</em> equality (as implemented by Shiro's
   * {@code SimplePrincipalCollection}). A custom {@code PrincipalCollection} using reference equality would be
   * cache-transparent - still correct, but re-resolved on every check rather than shared across calls.
   *
   * @param principals the subject's principals; the cache key (matched by value equality)
   * @param permission the permission being checked
   * @param permissionSupplier lazily supplies this realm's fully expanded permission collection on a cache miss
   */
  public boolean isPermitted(
      final PrincipalCollection principals,
      final Permission permission,
      final Supplier<Collection<Permission>> permissionSupplier)
  {
    // Caching disabled (opt-out via config) or no principals to key on: evaluate directly without caching.
    if (!enabled || principals == null) {
      return linearScan(permission, permissionSupplier.get());
    }
    return answer(resolve(principals, permissionSupplier), permission);
  }

  /**
   * Batch variant: resolves this realm's permission set for {@code principals} <em>once</em>, then answers every
   * permission in {@code permissions} against it (the UI whole-catalogue permission load). Returns a parallel array.
   */
  public boolean[] isPermitted(
      final PrincipalCollection principals,
      final List<Permission> permissions,
      final Supplier<Collection<Permission>> permissionSupplier)
  {
    if (permissions == null || permissions.isEmpty()) {
      return new boolean[0];
    }
    final boolean[] result = new boolean[permissions.size()];

    if (!enabled || principals == null) {
      // Resolve once for the whole batch even when uncached, so we do not re-expand per permission.
      final Collection<Permission> perms = permissionSupplier.get();
      for (int i = 0; i < permissions.size(); i++) {
        result[i] = linearScan(permissions.get(i), perms);
      }
      return result;
    }

    final PermissionsState state = resolve(principals, permissionSupplier);
    for (int i = 0; i < permissions.size(); i++) {
      result[i] = answer(state, permissions.get(i));
    }
    return result;
  }

  /**
   * Resolves (and caches on first use) the {@link PermissionsState} for {@code principals}. At most one thread builds
   * the state per principal under concurrent login bursts ({@code get(key, Callable)} single-flights the load and does
   * a lock-free read on a hit, preventing double role expansion - NEXUS-52583). If an invalidation advanced the
   * generation while the build was in flight, the (possibly stale) entry is dropped so it is never served again, and
   * this call is answered from what it resolved - closing the invalidation TOCTOU window (NEXUS-53719).
   */
  private PermissionsState resolve(
      final PrincipalCollection principals,
      final Supplier<Collection<Permission>> permissionSupplier)
  {
    final long genBefore = generation.get();
    final PermissionsState state;
    try {
      state = cache.get(principals, () -> {
        Collection<Permission> perms = permissionSupplier.get();
        return new PermissionsState(perms == null ? List.of() : perms, genBefore);
      });
    }
    catch (ExecutionException | UncheckedExecutionException e) {
      // The supplier failed; nothing is cached. Rethrow the original cause so authorization fails closed.
      Throwable cause = e.getCause();
      if (cause instanceof RuntimeException runtime) {
        throw runtime;
      }
      if (cause instanceof Error error) {
        throw error;
      }
      throw new IllegalStateException(cause == null ? e : cause);
    }

    if (state.generation != generation.get()) {
      // remove only if still the same instance (identity), so a fresh rebuild by another thread is never clobbered.
      cache.asMap().remove(principals, state);
    }
    return state;
  }

  private static boolean answer(final PermissionsState state, final Permission permission) {
    // Atomic single-flight memo: at most one implies() scan per (state, permission) even when two threads check the
    // same permission for the same principal concurrently (two tabs / two API calls). A non-atomic getIfPresent()+put()
    // would let both threads run the full O(grants) implies() scan before either published the memo; get(key, Callable)
    // restores the pre-refactor ConcurrentHashMap.computeIfAbsent single-flight semantics while keeping the bounded LRU
    // (NEXUS-53719).
    try {
      return state.results.get(permission, () -> state.implies(permission));
    }
    catch (ExecutionException | UncheckedExecutionException e) {
      // implies() throws nothing checked, so this is effectively unreachable; unwrap defensively (as resolve() does)
      // so authorization fails closed rather than swallowing an unexpected error.
      Throwable cause = e.getCause();
      if (cause instanceof RuntimeException runtime) {
        throw runtime;
      }
      if (cause instanceof Error error) {
        throw error;
      }
      throw new IllegalStateException(cause == null ? e : cause);
    }
  }

  private static boolean linearScan(final Permission permission, final Collection<Permission> userPermissions) {
    if (userPermissions == null || userPermissions.isEmpty()) {
      return false;
    }
    for (Permission perm : userPermissions) {
      if (perm.implies(permission)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Invalidates this realm's entire cache (every principal). Bumps the generation before clearing so any state whose
   * build is in flight is detected as stale and not persisted (NEXUS-53719).
   */
  public void invalidateAll() {
    generation.incrementAndGet();
    cache.invalidateAll();
    log.debug("Principal permissions cache invalidated");
  }
}
