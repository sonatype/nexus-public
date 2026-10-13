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
   *
   * <p>
   * Grants are split by whether {@link WildcardPermission#implies} can be answered for them by a prefix lookup rather
   * than a scan. Those that can are indexed into a hash set (see {@link PermissionsState#getPrefixKey}); the rest
   * - an interior wildcard, comma-form actions, or a custom {@link Permission} type - keep the linear scan
   * (NEXUS-52583, NEXUS-54434).
   */
  private static final class PermissionsState
  {
    // Grant strings (lowercase) that a prefix lookup can answer in full - see getPrefixKey. A check is implied
    // when this set contains the checked permission or any of its colon-prefixes.
    final Set<String> exactPermissions;

    // Grants a prefix lookup cannot answer, and so must be matched by a full implies() scan: grants with an interior
    // '*' or a comma-form action list, plus any non-WildcardPermission implementation. Keeping the prefix-answerable
    // ones out of here is what makes this list short, and the scan therefore cheap.
    final List<Permission> generalPermissions;

    // Bounded-LRU result memo (Permission -> Boolean): recently-checked results stay hot while cold, one-shot
    // (resource-parameterised) permissions evict, so a flood of unique checks cannot permanently starve repeated hot
    // checks (NEXUS-53719). Discarded with its outer cache entry.
    final Cache<Permission, Boolean> results = CacheBuilder.newBuilder().maximumSize(MAX_RESULTS).build();

    // Invalidation generation sampled at build time; if it has since advanced, an invalidation raced us (see resolve).
    final long generation;

    PermissionsState(final Collection<Permission> permissions, final long generation) {
      this.generation = generation;
      this.exactPermissions = new HashSet<>();
      this.generalPermissions = new ArrayList<>();

      for (Permission perm : permissions) {
        String prefixKey = getPrefixKey(perm);
        if (prefixKey != null) {
          exactPermissions.add(prefixKey);
        }
        else {
          generalPermissions.add(perm);
        }
      }
    }

    /**
     * The string to index {@code perm} under in {@link #exactPermissions}, or {@code null} if a prefix lookup cannot
     * answer it and it needs the {@code implies()} scan instead.
     *
     * <p>
     * A prefix lookup suffices when the grant has no {@code '*'} and no comma-form action list, because Shiro's
     * {@link WildcardPermission#implies} then returns true exactly when the grant equals the checked permission or is a
     * colon-prefix of it. A {@code '*'} means one grant can match permissions that share no common prefix with it, and
     * a comma-form list ({@code "nexus:users:create,read"} - the shape of every built-in CRUD privilege, see
     * {@code SecurityContributorSupport.ACTION_CREATE}) must satisfy set containment rather than equality. Neither is
     * expressible as a prefix, so both keep the scan.
     *
     * <p>
     * Note that being a {@link WildcardPermission} does not imply the grant contains a wildcard. That class is simply
     * Shiro's representation of colon-and-comma structured permissions; {@code "nexus:settings:read"} is one, and holds
     * no {@code '*'} at all. Hence the explicit character check.
     */
    private static String getPrefixKey(final Permission perm) {
      if (!(perm instanceof WildcardPermission)) {
        return null;
      }
      String permStr = dropTrailingWildcards(perm.toString().toLowerCase());
      return permStr.indexOf('*') < 0 && permStr.indexOf(',') < 0 ? permStr : null;
    }

    /**
     * Strips trailing {@code ":*"} parts, which Shiro treats as redundant: a grant implies everything below the point
     * where it runs out of parts, so {@code "nexus:foo:*"} and {@code "nexus:foo"} imply exactly the same permissions.
     *
     * <p>
     * Worth doing because the all-actions variant of every auto-created repository privilege takes this form
     * ({@code "nexus:repository-view:raw:releases:*"}). Without normalising, a user granted all actions on a thousand
     * repositories would hold a thousand grants that no prefix lookup could answer, and the scan those force back
     * dominated the permission load (NEXUS-54434).
     */
    private static String dropTrailingWildcards(final String permStr) {
      String result = permStr;
      while (result.length() > 2 && result.endsWith(":*")) {
        result = result.substring(0, result.length() - 2);
      }
      return result;
    }

    /**
     * Check if this state implies the given permission.
     *
     * <p>
     * Shiro's {@link WildcardPermission#implies} returns true for a wildcard-free, comma-free grant exactly when that
     * grant equals the checked permission or is a colon-prefix of it - a grant that runs out of parts implies
     * everything below it ("nexus:foo" implies "nexus:foo:read"), and a longer grant cannot imply a shorter permission
     * unless its extra parts are wildcards, which by construction these do not have. So every such grant is answered
     * by probing {@link #exactPermissions} for the checked permission and each of its colon-prefixes, and only
     * {@link #generalPermissions} needs the linear {@code implies()} scan.
     *
     * <p>
     * That matters because the UI permission load checks the entire privilege catalogue, and a DENIED answer used to
     * cost a full scan of the user's grants. On an instance with tens of thousands of privileges (the catalogue grows
     * by 11 per repository) and a user holding thousands of per-repository grants, that product dominated the load;
     * the prefix lookup removes it (NEXUS-54434).
     *
     * <p>
     * Worked example. Suppose {@code exactPermissions} holds
     * 
     * <pre>
     *   "nexus:settings:read"
     *   "nexus:repository-view:raw:releases:read"
     * </pre>
     * 
     * Checking {@code "nexus:repository-view:raw:releases:read"} hits on the first probe. Checking
     * {@code "nexus:repository-view:raw:other:read"} walks its colon-prefixes longest-first:
     * 
     * <pre>
     *   "nexus:repository-view:raw:other:read"   miss
     *   "nexus:repository-view:raw:other"        miss
     *   "nexus:repository-view:raw"              miss
     *   "nexus:repository-view"                  miss
     *   "nexus"                                  miss
     * </pre>
     * 
     * and falls through to the scan.
     *
     * <p>
     * The probe is not conditional on the checked permission being wildcard-free, unlike the earlier fast path: a
     * wildcard-free grant CAN imply a wildcarded check, because Shiro returns true as soon as the grant runs out of
     * parts ("nexus:repository-view" implies "nexus:repository-view:*:*:*"). Prefixes containing '*' or ',' simply
     * never match, since exactPermissions holds neither.
     */
    boolean implies(final Permission permission) {
      if (!exactPermissions.isEmpty() && permission instanceof WildcardPermission) {
        String permStr = permission.toString().toLowerCase();
        // Walk the colon-prefixes longest-first. substring(0, length()) returns the same instance, so the
        // full-length probe - which is the one that hits whenever the grant is held outright - allocates nothing.
        int end = permStr.length();
        while (end > 0) {
          if (exactPermissions.contains(permStr.substring(0, end))) {
            return true;
          }
          end = permStr.lastIndexOf(':', end - 1);
        }
      }

      // Grants with an interior wildcard, comma-form actions, or a custom Permission type still need full matching.
      for (Permission granted : generalPermissions) {
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
