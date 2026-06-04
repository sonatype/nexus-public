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
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.sonatype.nexus.common.QualifierUtil;
import org.sonatype.nexus.common.event.EventManager;
import org.sonatype.nexus.common.stateguard.Guarded;
import org.sonatype.nexus.common.stateguard.StateGuardLifecycleSupport;
import org.sonatype.nexus.distributed.event.service.api.common.UserPasswordChangedDistributedEvent;
import org.sonatype.nexus.security.UserPrincipalsExpired;
import org.sonatype.nexus.security.authc.UserPasswordChanged;
import org.sonatype.nexus.security.authz.AuthorizationConfigurationChanged;
import org.sonatype.nexus.security.realm.RealmConfiguration;
import org.sonatype.nexus.security.realm.RealmConfigurationEvent;
import org.sonatype.nexus.security.realm.RealmConfigurationStore;
import org.sonatype.nexus.security.realm.RealmManager;
import org.sonatype.nexus.security.realm.SecurityRealm;

import com.google.common.collect.Lists;
import com.google.common.eventbus.AllowConcurrentEvents;
import com.google.common.eventbus.Subscribe;
import org.springframework.beans.factory.annotation.Autowired;
import jakarta.inject.Provider;
import org.apache.shiro.authc.AuthenticationInfo;
import org.apache.shiro.authz.AuthorizationInfo;
import org.apache.shiro.cache.Cache;
import org.apache.shiro.mgt.RealmSecurityManager;
import org.apache.shiro.realm.AuthenticatingRealm;
import org.apache.shiro.realm.AuthorizingRealm;
import org.apache.shiro.realm.Realm;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;
import static org.sonatype.nexus.common.stateguard.StateGuardLifecycleSupport.State.STARTED;

/**
 * Default {@link RealmManager}.
 *
 * @since 3.0
 */
@Component
public class RealmManagerImpl
    extends StateGuardLifecycleSupport
    implements RealmManager, ApplicationContextAware
{
  private final EventManager eventManager;

  private final RealmConfigurationStore store;

  private final Provider<RealmConfiguration> defaults;

  private final RealmSecurityManager realmSecurityManager;

  private final Map<String, Realm> availableRealms;

  private RealmConfiguration configuration;

  private final boolean enableAuthorizationRealmManagement;

  private ApplicationContext applicationContext;

  @Autowired
  public RealmManagerImpl(
      final EventManager eventManager,
      final RealmConfigurationStore store,
      @Qualifier("initial") final Provider<RealmConfiguration> defaults,
      final RealmSecurityManager realmSecurityManager,
      final List<Realm> availableRealmsList,
      @Value("${nexus.security.enableAuthorizationRealmManagement:false}") final boolean enableAuthorizationRealmManagement)
  {
    this.eventManager = checkNotNull(eventManager);
    this.store = checkNotNull(store);
    log.debug("Store: {}", store);
    this.defaults = checkNotNull(defaults);
    log.debug("Defaults: {}", defaults);
    this.realmSecurityManager = checkNotNull(realmSecurityManager);
    this.availableRealms = QualifierUtil.buildQualifierBeanMap(checkNotNull(availableRealmsList));
    this.enableAuthorizationRealmManagement = enableAuthorizationRealmManagement;
  }

  //
  // Lifecycle
  //

  @Override
  protected void doStart() throws Exception {
    installRealms();

    eventManager.register(this);
  }

  @Override
  protected void doStop() throws Exception {
    eventManager.unregister(this);

    configuration = null;

    // reset shiro caches
    Collection<Realm> realms = realmSecurityManager.getRealms();
    if (realms != null) {
      for (Realm realm : realms) {
        if (realm instanceof AuthenticatingRealm authenticatingRealm) {
          authenticatingRealm.setAuthenticationCache(null);
        }
        if (realm instanceof AuthorizingRealm authorizingRealm) {
          authorizingRealm.setAuthorizationCache(null);
        }
      }
    }
  }

  //
  // Configuration
  //

  /**
   * Load configuration from store, or use defaults.
   */
  private RealmConfiguration loadConfiguration() {
    RealmConfiguration model = store.load();

    // use defaults if no configuration was loaded from the store
    if (model == null) {
      model = defaults.get();

      // default config must not be null
      checkNotNull(model);

      log.info("Using default configuration: {}", model);
    }
    else {
      log.info("Loaded configuration: {}", model);
    }

    return model;
  }

  /**
   * Return configuration, loading if needed.
   * <p>
   * The result model should be considered _immutable_ unless copied.
   */
  private RealmConfiguration getConfigurationInternal() {
    if (configuration == null) {
      configuration = loadConfiguration();
    }
    return configuration;
  }

  /**
   * Return _copy_ of configuration.
   */
  private RealmConfiguration getConfiguration() {
    return getConfigurationInternal().copy();
  }

  private void setConfiguration(final RealmConfiguration configuration) {
    checkNotNull(configuration);

    maybeAddAuthorizingRealm(configuration.getRealmNames());

    store.save(configuration);
    reloadConfiguration();
  }

  private synchronized void reloadConfiguration() {
    configuration = store.load();
    log.info("Changing configuration: {}", configuration);
    installRealms();
  }

  //
  // Realm installation
  //

  /**
   * Resolve and install realm components.
   */
  private void installRealms() {
    List<Realm> realms = resolveRealms();
    log.debug("Installing realms: {}", realms);
    realmSecurityManager.setRealms(realms);
  }

  /**
   * Resolve configured realm components.
   */
  private List<Realm> resolveRealms() {
    List<Realm> result = Lists.newArrayList();
    RealmConfiguration model = getConfigurationInternal();

    List<String> configuredRealmIds = new ArrayList<>(model.getRealmNames());

    // just in case the realm is NOT in config, make sure it is still installed
    // without an alternate authorizing realm, nexus will have a hard time functioning when this realm isn't in place
    maybeAddAuthorizingRealm(configuredRealmIds);

    log.debug("Available realms: {}", availableRealms);

    for (String configuredRealmId : configuredRealmIds) {
      Realm realm = availableRealms.get(configuredRealmId);

      // FIXME: Resolve what purpose this is for, looks like legacy?
      if (realm == null) {
        log.debug("Failed to look up realm '{}' as a component, trying reflection", configuredRealmId);
        // If that fails, will simply use reflection to load
        try {
          realm = (Realm) getClass().getClassLoader().loadClass(configuredRealmId).newInstance();
        }
        catch (Exception e) {
          log.error("Unable to lookup security realms", e);
        }
      }

      if (realm != null) {
        result.add(realm);
      }
    }

    return result;
  }

  //
  // Helpers
  //

  @Override
  public boolean isRealmEnabled(final String realmName) {
    checkNotNull(realmName);
    return getConfigurationInternal().getRealmNames().contains(realmName);
  }

  @Override
  public void enableRealm(final String realmName, final boolean enable) {
    if (enable) {
      enableRealm(realmName);
    }
    else {
      disableRealm(realmName);
    }
  }

  @Override
  public void enableRealm(final String realmName) {
    checkNotNull(realmName);

    log.debug("Enabling realm: {}", realmName);
    RealmConfiguration model = getConfiguration();
    if (!model.getRealmNames().contains(realmName)) {
      model.getRealmNames().add(realmName);
      setConfiguration(model);
    }
    else {
      log.debug("Realm already enabled: {}", realmName);
    }
  }

  @Override
  public void enableRealm(final String realmName, final int index) {
    List<String> configuredRealms = new ArrayList<>(getConfiguration().getRealmNames());
    configuredRealms.remove(realmName);

    // fallback to default functionality in case of bad index (simply add to end of list)
    // or in case of 'moving' a realm in list and index now invalid after removing
    if (index > configuredRealms.size()) {
      log.debug("Enabling realm: {} as last member", realmName);
      configuredRealms.add(realmName);
    }
    else {
      log.debug("Enabling realm: {} at position: {}", realmName, index);
      configuredRealms.add(index, realmName);
    }

    setConfiguredRealmIds(configuredRealms);
  }

  @Override
  public void disableRealm(final String realmName) {
    checkNotNull(realmName);

    if (!enableAuthorizationRealmManagement && AuthorizingRealmImpl.NAME.equals(realmName)) {
      log.error("Cannot disable the {} realm", AuthorizingRealmImpl.NAME);
    }
    else {
      log.debug("Disabling realm: {}", realmName);
      RealmConfiguration model = getConfiguration();
      model.getRealmNames().remove(realmName);
      setConfiguration(model);
    }
  }

  //
  // Event handling
  //

  @AllowConcurrentEvents
  @Subscribe
  public void on(final RealmConfigurationEvent event) {
    if (!event.isLocal()) {
      reloadConfiguration();
    }
  }

  @Subscribe
  public void onEvent(final UserPrincipalsExpired event) {
    // TODO: we could do this better, not flushing whole cache for single user being deleted
    clearAuthcRealmCaches();
  }

  @Subscribe
  public void onEvent(final AuthorizationConfigurationChanged event) {
    // TODO: we could do this better, not flushing whole cache for single user roles being updated
    clearAuthzRealmCaches();
  }

  /**
   * Handles a user password change event
   *
   * @param event
   */
  @Subscribe
  public void onEvent(final UserPasswordChanged event) {
    if (event.isClearCache()) {
      clearAuthcRealmCacheForUserId(event.getUserId());
    }
  }

  @Subscribe
  public void on(final UserPasswordChangedDistributedEvent event) {
    if (!event.isLocal() && event.isClearCache()) {
      clearAuthcRealmCacheForUserId(event.getUserId());
    }
  }

  /**
   * Clear the authentication cache for the given userId as a result of a password change.
   */
  private void clearAuthcRealmCacheForUserId(final String userId) {
    // NOTE: we don't need to iterate all the Sec Managers, they use the same Realms, so one is fine.
    Optional
        .of(realmSecurityManager)
        .map(RealmSecurityManager::getRealms)
        .orElse(emptyList())
        .stream()
        .filter(realm -> realm instanceof AuthenticatingRealmImpl)
        .map(realm -> (AuthenticatingRealmImpl) realm)
        .findFirst()
        .ifPresent(realm -> realm.clearCache(userId));
  }

  /**
   * Looks up registered {@link AuthenticatingRealm}s, and clears their authc caches if they have it set.
   */
  private void clearAuthcRealmCaches() {
    // NOTE: we don't need to iterate all the Sec Managers, they use the same Realms, so one is fine.
    Collection<Realm> realms = realmSecurityManager.getRealms();
    if (realms != null) {
      for (Realm realm : realms) {
        if (realm instanceof AuthenticatingRealm) {
          Cache<Object, AuthenticationInfo> cache = ((AuthenticatingRealm) realm).getAuthenticationCache();
          if (cache != null) {
            log.debug("Clearing cache: {}", cache);
            cache.clear();
          }
        }
      }
    }
  }

  /**
   * Looks up registered {@link AuthorizingRealm}s, and clears their authz caches if they have it set.
   */
  private void clearAuthzRealmCaches() {
    // NOTE: we don't need to iterate all the Sec Managers, they use the same Realms, so one is fine.
    Collection<Realm> realms = realmSecurityManager.getRealms();
    if (realms != null) {
      for (Realm realm : realms) {
        if (realm instanceof AuthorizingRealm) {
          Cache<Object, AuthorizationInfo> cache = ((AuthorizingRealm) realm).getAuthorizationCache();
          if (cache != null) {
            log.debug("Clearing cache: {}", cache);
            cache.clear();
          }
        }
      }
    }
  }

  @Override
  public List<SecurityRealm> getAvailableRealms() {
    return getAvailableRealms(false);
  }

  @Override
  public List<SecurityRealm> getAvailableRealms(final boolean includeHidden) {
    return applicationContext.getBeansOfType(Realm.class)
        .values()
        .stream()
        .filter(entry -> {
          if (includeHidden || enableAuthorizationRealmManagement) {
            return true;
          }
          // don't want users to be aware of this realm any longer
          return !AuthorizingRealmImpl.NAME.equals(QualifierUtil.value(entry).orElseThrow());
        })
        // TODO the name
        .map(entry -> new SecurityRealm(QualifierUtil.value(entry).orElseThrow(), QualifierUtil.description(entry)))
        .sorted((a, b) -> a.getName().compareToIgnoreCase(b.getName()))
        .collect(toList());
  }

  @Override
  @Guarded(by = STARTED)
  public List<String> getConfiguredRealmIds() {
    List<String> availableRealmIds = getAvailableRealms().stream().map(SecurityRealm::getId).collect(toList());
    return getConfiguredRealmIds(false).stream().filter(availableRealmIds::contains).collect(toList());
  }

  @Override
  @Guarded(by = STARTED)
  public List<String> getConfiguredRealmIds(final boolean includeHidden) {
    return getConfiguration().getRealmNames().stream().filter(realmName -> {
      if (includeHidden || enableAuthorizationRealmManagement) {
        return true;
      }
      // don't want users to be aware of this realm any longer
      return !AuthorizingRealmImpl.NAME.equals(realmName);
    }).collect(toList());
  }

  @Override
  @Guarded(by = STARTED)
  public void setConfiguredRealmIds(final List<String> realmIds) {
    List<String> realmIdsToSave = new ArrayList<>(realmIds);

    RealmConfiguration realmConfiguration = getConfiguration();
    realmConfiguration.setRealmNames(realmIdsToSave);
    setConfiguration(realmConfiguration);
  }

  private void maybeAddAuthorizingRealm(final List<String> realmIds) {
    // as long as we aren't allowing user to manage the authz realm, make sure the config still stores it, just in case
    // they flip the configuration property in this class to disable, we still want their system to function properly
    if (!enableAuthorizationRealmManagement) {
      // remove existing if necessary, to make sure realm is always last
      realmIds.remove(AuthorizingRealmImpl.NAME);
      realmIds.add(AuthorizingRealmImpl.NAME);
    }
  }

  @Override
  public void setApplicationContext(final ApplicationContext applicationContext) throws BeansException {
    this.applicationContext = applicationContext;
  }
}
