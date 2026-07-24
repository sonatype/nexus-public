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
package org.sonatype.nexus.security.internal;

import java.util.ArrayList;
import java.util.List;

import org.sonatype.nexus.common.app.ManagedLifecycleManager;
import org.sonatype.nexus.common.event.EventHelper;
import org.sonatype.nexus.common.event.EventManager;
import org.sonatype.nexus.distributed.event.service.api.common.AuthorizationChangedDistributedEvent;
import org.sonatype.nexus.distributed.event.service.api.common.UserPasswordChangedDistributedEvent;
import org.sonatype.nexus.security.UserPrincipalsExpired;
import org.sonatype.nexus.security.authz.AuthorizationConfigurationChanged;
import org.sonatype.nexus.security.authz.PermissionCachingAuthorizingRealm;
import org.sonatype.nexus.security.authz.PrincipalPermissionsCache;
import org.sonatype.nexus.security.authz.PrincipalPermissionsCacheFactory;
import org.sonatype.nexus.security.realm.RealmConfiguration;
import org.sonatype.nexus.security.realm.RealmConfigurationEvent;
import org.sonatype.nexus.security.realm.RealmConfigurationStore;
import org.sonatype.nexus.security.realm.TestRealmConfiguration;

import jakarta.inject.Provider;
import org.apache.shiro.authc.AuthenticationInfo;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.authz.AuthorizationInfo;
import org.apache.shiro.cache.Cache;
import org.apache.shiro.mgt.RealmSecurityManager;
import org.apache.shiro.realm.Realm;
import org.apache.shiro.subject.PrincipalCollection;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RealmManagerImplTest
{
  @Mock
  private Provider<RealmConfiguration> initialRealmConfigurationProvider;

  @Mock
  private EventManager eventManager;

  @Mock
  private RealmConfigurationStore configStore;

  @Mock
  private RealmSecurityManager securityManager;

  @Mock(name = "A")
  private Realm realmA;

  @Mock(name = "B")
  private Realm realmB;

  @Mock
  private RealmConfigurationEvent configEvent;

  @Mock
  private AuthenticatingRealmImpl authenticatingRealm;

  @Mock
  private UserPasswordChangedDistributedEvent passwordChangedEvent;

  private RealmManagerImpl manager;

  @BeforeEach
  void setUp() {
    List<Realm> realms = List.of(realmA, realmB);
    RealmConfiguration defaultConfig = new TestRealmConfiguration();
    defaultConfig.setRealmNames(List.of("A"));
    lenient().when(initialRealmConfigurationProvider.get()).thenReturn(defaultConfig);
    manager = new RealmManagerImpl(eventManager, configStore, initialRealmConfigurationProvider, securityManager,
        realms, false);
  }

  @Test
  void testOnStoreChanged_LocalEvent() {
    when(configEvent.isLocal()).thenReturn(true);
    manager.on(configEvent);
    verifyNoInteractions(eventManager, configStore);
  }

  @Test
  void testOnStoreChanged_RemoteEvent() {
    RealmConfiguration reloadedConfig = new TestRealmConfiguration();
    reloadedConfig.setRealmNames(List.of("B"));
    when(configEvent.isLocal()).thenReturn(false);
    when(configStore.load()).thenReturn(reloadedConfig);

    manager.on(configEvent);

    verify(configStore).load();
    verify(securityManager).setRealms(List.of(realmB));
  }

  @Test
  void testOnUserPasswordChanged() {
    when(passwordChangedEvent.isLocal()).thenReturn(false);
    when(passwordChangedEvent.isClearCache()).thenReturn(true);
    when(passwordChangedEvent.getUserId()).thenReturn("testuser");
    when(securityManager.getRealms()).thenReturn(List.of(authenticatingRealm));

    manager.on(passwordChangedEvent);

    verify(authenticatingRealm).clearCache("testuser");
  }

  @Test
  void testOnUserPasswordChanged_LocalEvent() {
    when(passwordChangedEvent.isLocal()).thenReturn(true);

    manager.on(passwordChangedEvent);

    verifyNoInteractions(authenticatingRealm);
  }

  @Test
  void testOnUserPasswordChanged_ClearCache() {
    when(passwordChangedEvent.isLocal()).thenReturn(false);
    when(passwordChangedEvent.isClearCache()).thenReturn(false);

    manager.on(passwordChangedEvent);

    verifyNoInteractions(authenticatingRealm);
  }

  /**
   * Disabling a realm that is not in the active list must NOT persist or reinstall realms.
   * Otherwise migration steps that disable obsolete realms (such as CargoRealmMigrationStep)
   * cause RealmManager to cache its default configuration during the migration phase, which is
   * then never refreshed when later automated configuration writes the real realm list to the
   * store. See NEXUS-47898.
   */
  @Test
  void testDisableRealm_realmNotPresent_isNoOp() {
    manager.disableRealm("NotPresentRealm");

    verify(configStore, never()).save(any());
    verify(securityManager, never()).setRealms(anyList());
  }

  @Test
  void testDisableRealm_realmPresent_persistsAndReinstalls() {
    RealmConfiguration storedConfig = new TestRealmConfiguration();
    storedConfig.setRealmNames(new ArrayList<>(List.of("A")));
    when(configStore.load()).thenReturn(storedConfig);

    manager.disableRealm("A");

    verify(configStore).save(any(RealmConfiguration.class));
    verify(securityManager).setRealms(anyList());
  }

  @Test
  void testOnUserPrincipalsExpired_clearsAuthzCache() {
    Cache<Object, AuthorizationInfo> withCache = mockAuthorizationCache();
    PrincipalPermissionsCache permCache = mock(PrincipalPermissionsCache.class);
    StubPermissionCachingRealm authorizing = new StubPermissionCachingRealm(permCache);
    authorizing.setAuthorizationCache(withCache);
    when(securityManager.getRealms()).thenReturn(List.of(authorizing));

    manager.onEvent(new UserPrincipalsExpired("testuser", "default"));

    // Single owner: both the Shiro authz cache and this realm's permission cache are cleared so a disabled/deleted
    // user's permissions are not served stale (NEXUS-53719).
    verify(withCache).clear();
    verify(permCache).invalidateAll();
  }

  // ---- authorization-cache invalidation (NEXUS-53719) ----

  /** Minimal concrete permission-caching realm whose Shiro authz cache and permission cache can be inspected. */
  private static final class StubPermissionCachingRealm
      extends PermissionCachingAuthorizingRealm
  {
    StubPermissionCachingRealm(final PrincipalPermissionsCache cache) {
      super("Stub", factoryReturning(cache));
    }

    private static PrincipalPermissionsCacheFactory factoryReturning(final PrincipalPermissionsCache cache) {
      PrincipalPermissionsCacheFactory factory = mock(PrincipalPermissionsCacheFactory.class);
      when(factory.create(anyString())).thenReturn(cache);
      return factory;
    }

    @Override
    protected AuthenticationInfo doGetAuthenticationInfo(final AuthenticationToken token) {
      return null;
    }

    @Override
    protected AuthorizationInfo doGetAuthorizationInfo(final PrincipalCollection principals) {
      return null;
    }
  }

  @SuppressWarnings("unchecked")
  private static Cache<Object, AuthorizationInfo> mockAuthorizationCache() {
    return mock(Cache.class);
  }

  @Test
  void authorizationConfigurationChangedClearsAuthzAndPermissionCaches() {
    Cache<Object, AuthorizationInfo> withCache = mockAuthorizationCache();
    PrincipalPermissionsCache permCacheWith = mock(PrincipalPermissionsCache.class);
    StubPermissionCachingRealm authorizingWithCache = new StubPermissionCachingRealm(permCacheWith);
    authorizingWithCache.setAuthorizationCache(withCache);

    PrincipalPermissionsCache permCacheWithout = mock(PrincipalPermissionsCache.class);
    // No Shiro authz cache set -> getAuthorizationCache() == null, so the Shiro clear skips it.
    StubPermissionCachingRealm authorizingWithoutCache = new StubPermissionCachingRealm(permCacheWithout);
    // realmA is a plain Realm (neither AuthorizingRealm nor PermissionCachingAuthorizingRealm) and must be skipped.
    when(securityManager.getRealms()).thenReturn(List.of(realmA, authorizingWithoutCache, authorizingWithCache));

    manager.onEvent(new AuthorizationConfigurationChanged());

    // Shiro authz cache cleared only where present; every realm's own permission cache cleared.
    verify(withCache).clear();
    verify(permCacheWith).invalidateAll();
    verify(permCacheWithout).invalidateAll();
  }

  @Test
  void securityContributionChangedClearsAuthorizationCaches() {
    Cache<Object, AuthorizationInfo> withCache = mockAuthorizationCache();
    PrincipalPermissionsCache permCache = mock(PrincipalPermissionsCache.class);
    StubPermissionCachingRealm authorizing = new StubPermissionCachingRealm(permCache);
    authorizing.setAuthorizationCache(withCache);
    when(securityManager.getRealms()).thenReturn(List.of(authorizing));

    manager.onEvent(new SecurityContributionChangedEvent());

    verify(withCache).clear();
    verify(permCache).invalidateAll();
  }

  @Test
  void authorizationConfigurationChangedToleratesNoRealms() {
    when(securityManager.getRealms()).thenReturn(null);
    // Must not throw when the security manager reports no realms.
    manager.onEvent(new AuthorizationConfigurationChanged());
  }

  @Test
  void distributedAuthorizationChangedClearsCachesOnlyWhenReplicating() {
    Cache<Object, AuthorizationInfo> withCache = mockAuthorizationCache();
    PrincipalPermissionsCache permCache = mock(PrincipalPermissionsCache.class);
    StubPermissionCachingRealm authorizing = new StubPermissionCachingRealm(permCache);
    authorizing.setAuthorizationCache(withCache);
    // lenient: the non-replicating branch below never calls getRealms(), so strict stubbing would fail on that path.
    lenient().when(securityManager.getRealms()).thenReturn(List.of(authorizing));

    // Not replicating: ignored (no realm lookup, no clear).
    manager.onEvent(new AuthorizationChangedDistributedEvent());
    verify(withCache, never()).clear();
    verify(permCache, never()).invalidateAll();

    // Replicating: both caches cleared.
    EventHelper.asReplicating(() -> manager.onEvent(new AuthorizationChangedDistributedEvent()));
    verify(withCache).clear();
    verify(permCache).invalidateAll();
  }

  @Test
  void reloadingConfigurationClearsAuthorizationCaches() {
    // A realm-topology change (enable/disable/reorder) must drop both authz surfaces, so entries are never served
    // stale (NEXUS-53719).
    Cache<Object, AuthorizationInfo> withCache = mockAuthorizationCache();
    PrincipalPermissionsCache permCache = mock(PrincipalPermissionsCache.class);
    StubPermissionCachingRealm authorizing = new StubPermissionCachingRealm(permCache);
    authorizing.setAuthorizationCache(withCache);
    when(securityManager.getRealms()).thenReturn(List.of(authorizing));
    RealmConfiguration withA = new TestRealmConfiguration();
    withA.setRealmNames(new ArrayList<>(List.of("A")));
    RealmConfiguration afterDisable = new TestRealmConfiguration();
    afterDisable.setRealmNames(new ArrayList<>());
    // First load (lazy config init in disableRealm) sees [A]; the reload after save sees the now-empty realm set.
    when(configStore.load()).thenReturn(withA, afterDisable);

    manager.disableRealm("A"); // present -> persists + reloadConfiguration(), realm set changed [A] -> []

    verify(withCache).clear();
    verify(permCache).invalidateAll();
  }

  @Test
  void reloadingConfigurationWithUnchangedRealmSetDoesNotClearCaches() {
    // A config reload whose installed-realm set is unchanged must NOT flush the authz caches (avoids a full
    // re-expansion storm on unrelated config saves) (NEXUS-53719).
    Cache<Object, AuthorizationInfo> withCache = mockAuthorizationCache();
    PrincipalPermissionsCache permCache = mock(PrincipalPermissionsCache.class);
    StubPermissionCachingRealm authorizing = new StubPermissionCachingRealm(permCache);
    authorizing.setAuthorizationCache(withCache);
    // lenient: only the first (topology-changing) reload calls getRealms(); the second unchanged reload does not.
    lenient().when(securityManager.getRealms()).thenReturn(List.of(authorizing));
    RealmConfiguration sameConfig = new TestRealmConfiguration();
    sameConfig.setRealmNames(new ArrayList<>(List.of("A")));
    when(configStore.load()).thenReturn(sameConfig);
    when(configEvent.isLocal()).thenReturn(false);

    manager.on(configEvent); // first reload establishes topology [A] (clears)
    clearInvocations(withCache, permCache);

    manager.on(configEvent); // realm set unchanged [A] -> [A]: no clear

    verify(withCache, never()).clear();
    verify(permCache, never()).invalidateAll();
  }

  /**
   * NEXUS-53486 shutdown guard — disable path.
   * <p>
   * When the JVM is shutting down, {@link RealmManagerImpl#disableRealm(String)} must not persist
   * the mutated realm list and must not broadcast a configuration event. Otherwise capability
   * passivation during graceful shutdown corrupts the persisted realm configuration and, in HA,
   * propagates the corruption to peer nodes.
   */
  @Test
  void testDisableRealm_duringShutdown_doesNotPersistOrBroadcast() {
    RealmConfiguration storedConfig = new TestRealmConfiguration();
    storedConfig.setRealmNames(new ArrayList<>(List.of("A", "DefaultRole")));
    when(configStore.load()).thenReturn(storedConfig);

    try (MockedStatic<ManagedLifecycleManager> lifecycle = Mockito.mockStatic(ManagedLifecycleManager.class)) {
      lifecycle.when(ManagedLifecycleManager::isShuttingDown).thenReturn(true);

      manager.disableRealm("DefaultRole");

      verify(configStore, never()).save(any());
      verify(securityManager, never()).setRealms(anyList());
    }
  }

  /**
   * NEXUS-53486 shutdown guard — enable path. Same invariant as
   * {@link #testDisableRealm_duringShutdown_doesNotPersistOrBroadcast()} but exercised via
   * {@link RealmManagerImpl#enableRealm(String)} to confirm the guard sits at the persistence
   * chokepoint, not at any individual entry point.
   */
  @Test
  void testEnableRealm_duringShutdown_doesNotPersistOrBroadcast() {
    RealmConfiguration storedConfig = new TestRealmConfiguration();
    storedConfig.setRealmNames(new ArrayList<>(List.of("A")));
    when(configStore.load()).thenReturn(storedConfig);

    try (MockedStatic<ManagedLifecycleManager> lifecycle = Mockito.mockStatic(ManagedLifecycleManager.class)) {
      lifecycle.when(ManagedLifecycleManager::isShuttingDown).thenReturn(true);

      manager.enableRealm("B");

      verify(configStore, never()).save(any());
      verify(securityManager, never()).setRealms(anyList());
    }
  }

  /**
   * NEXUS-53486 shutdown guard — negative case. When the platform is running normally (not
   * shutting down) all realm-mutation paths must still persist and reinstall as before. This test
   * exists so a future edit that accidentally over-broadens the guard (e.g. always returns early)
   * is caught immediately.
   */
  @Test
  void testDisableRealm_whenNotShuttingDown_stillPersists() {
    RealmConfiguration storedConfig = new TestRealmConfiguration();
    storedConfig.setRealmNames(new ArrayList<>(List.of("A")));
    when(configStore.load()).thenReturn(storedConfig);

    try (MockedStatic<ManagedLifecycleManager> lifecycle = Mockito.mockStatic(ManagedLifecycleManager.class)) {
      lifecycle.when(ManagedLifecycleManager::isShuttingDown).thenReturn(false);

      manager.disableRealm("A");

      verify(configStore).save(any(RealmConfiguration.class));
      verify(securityManager).setRealms(anyList());
    }
  }
}
