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
package org.sonatype.nexus.internal.selector;

import java.lang.ref.SoftReference;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import javax.cache.Cache;
import javax.cache.expiry.CreatedExpiryPolicy;
import javax.cache.expiry.Duration;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.goodies.common.Time;
import org.sonatype.nexus.cache.CacheHelper;
import org.sonatype.nexus.common.app.ManagedLifecycle;
import org.sonatype.nexus.common.entity.EntityId;
import org.sonatype.nexus.common.event.EventAware;
import org.sonatype.nexus.common.stateguard.Guarded;
import org.sonatype.nexus.common.stateguard.StateGuardLifecycleSupport;
import org.sonatype.nexus.datastore.api.DuplicateKeyException;
import org.sonatype.nexus.distributed.event.service.api.common.RoleConfigurationEvent;
import org.sonatype.nexus.distributed.event.service.api.common.SelectorConfigurationChangedEvent;
import org.sonatype.nexus.repository.security.RepositoryContentSelectorPrivilegeDescriptor;
import org.sonatype.nexus.repository.security.RepositorySelector;
import org.sonatype.nexus.rest.ValidationErrorsException;
import org.sonatype.nexus.security.SecuritySystem;
import org.sonatype.nexus.security.anonymous.AnonymousHelper;
import org.sonatype.nexus.security.authz.AuthorizationManager;
import org.sonatype.nexus.security.authz.NoSuchAuthorizationManagerException;
import org.sonatype.nexus.security.privilege.Privilege;
import org.sonatype.nexus.security.role.Role;
import org.sonatype.nexus.security.role.RoleEvent;
import org.sonatype.nexus.security.role.RoleIdentifier;
import org.sonatype.nexus.security.user.User;
import org.sonatype.nexus.security.user.UserManager;
import org.sonatype.nexus.security.user.UserNotFoundException;
import org.sonatype.nexus.selector.CselToSql;
import org.sonatype.nexus.selector.Selector;
import org.sonatype.nexus.selector.SelectorConfiguration;
import org.sonatype.nexus.selector.SelectorConfigurationStore;
import org.sonatype.nexus.selector.SelectorEvaluationException;
import org.sonatype.nexus.selector.SelectorFactory;
import org.sonatype.nexus.selector.SelectorManager;
import org.sonatype.nexus.selector.SelectorSqlBuilder;
import org.sonatype.nexus.selector.VariableSource;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableList;
import com.google.common.eventbus.AllowConcurrentEvents;
import com.google.common.eventbus.Subscribe;
import org.apache.commons.lang3.StringUtils;
import org.apache.shiro.subject.Subject;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.stream.Collectors.toList;
import static org.sonatype.nexus.common.app.ManagedLifecycle.Phase.SERVICES;
import static org.sonatype.nexus.common.stateguard.StateGuardLifecycleSupport.State.STARTED;
import static org.sonatype.nexus.repository.security.RepositoryContentSelectorPrivilegeDescriptor.P_CONTENT_SELECTOR;
import static org.sonatype.nexus.repository.security.RepositoryContentSelectorPrivilegeDescriptor.P_REPOSITORY;
import static org.sonatype.nexus.security.user.UserManager.DEFAULT_SOURCE;

/**
 * Default {@link SelectorManager} implementation.
 *
 * @since 3.1
 */
@Named
@Singleton
@ManagedLifecycle(phase = SERVICES)
public class SelectorManagerImpl
    extends StateGuardLifecycleSupport
    implements SelectorManager, EventAware
{

  private static final SoftReference<List<SelectorConfiguration>> EMPTY_CACHE = new SoftReference<>(null);

  private static final String USER_CACHE_KEY = "SelectorManager";

  private final SelectorConfigurationStore store;

  private final SecuritySystem securitySystem;

  private final LoadingCache<SelectorConfiguration, Selector> selectorCache;

  private final CacheHelper cacheHelper;

  private final Duration userCacheTimeout;

  private volatile SoftReference<List<SelectorConfiguration>> cachedBrowseResult = EMPTY_CACHE;

  private Map<String, Role> rolesCache = Collections.emptyMap();

  private Cache<String, User> userCache;

  @Inject
  public SelectorManagerImpl(
      final SelectorConfigurationStore store,
      final SecuritySystem securitySystem,
      final SelectorFactory selectorFactory,
      final CacheHelper cacheHelper,
      @Named("${nexus.shiro.cache.defaultTimeToLive:-2m}") final Time userCacheTimeout)
  {
    this.store = checkNotNull(store);
    this.securitySystem = checkNotNull(securitySystem);
    this.cacheHelper = cacheHelper;
    this.userCacheTimeout = new Duration(userCacheTimeout.getUnit(), userCacheTimeout.getValue());

    checkNotNull(selectorFactory);
    selectorCache = CacheBuilder.newBuilder().softValues().build(CacheLoader.from(config -> {
      String type = config.getType();
      String expression = config.getAttributes().get(SelectorConfiguration.EXPRESSION);
      return selectorFactory.createSelector(type, expression);
    }));
  }

  @Override
  @Guarded(by = STARTED)
  public List<SelectorConfiguration> browse() {
    List<SelectorConfiguration> result;

    // double-checked lock to minimize caching attempts
    if ((result = cachedBrowseResult.get()) == null) {
      synchronized (this) {
        if ((result = cachedBrowseResult.get()) == null) {
          result = ImmutableList.copyOf(store.browse());
          // maintain this result in memory-sensitive cache
          cachedBrowseResult = new SoftReference<>(result);
        }
      }
    }

    return result;
  }

  @Override
  @Guarded(by = STARTED)
  public List<SelectorConfiguration> browse(final String selectorType) {
    checkNotNull(selectorType);
    return browse().stream().filter(config -> selectorType.equals(config.getType())).collect(toList());
  }

  @Override
  @Guarded(by = STARTED)
  public SelectorConfiguration read(final EntityId entityId) {
    return store.read(entityId);
  }

  @Override
  @Guarded(by = STARTED)
  public SelectorConfiguration readByName(final String name) {
    return store.getByName(name);
  }

  @Override
  @Guarded(by = STARTED)
  public Optional<SelectorConfiguration> findByName(final String name) {
    return browse().stream().filter(selector -> StringUtils.equals(name, selector.getName())).findFirst();
  }

  @Override
  @Guarded(by = STARTED)
  public void create(final SelectorConfiguration configuration) {
    store.create(configuration);
  }

  @Override
  public void create(
      final String name,
      final String type,
      final String description,
      final Map<String, String> attributes)
  {
    SelectorConfiguration selectorConfiguration = store.newSelectorConfiguration();
    selectorConfiguration.setName(name);
    selectorConfiguration.setType(type);
    selectorConfiguration.setDescription(description);
    selectorConfiguration.setAttributes(attributes);
    try {
      store.create(selectorConfiguration);
    }
    catch (DuplicateKeyException e) {
      throw new ValidationErrorsException("name", "A selector with the same name already exists. Name must be unique.");
    }
  }

  @Override
  @Guarded(by = STARTED)
  public void update(final SelectorConfiguration configuration) {
    store.update(configuration);
  }

  @Override
  @Guarded(by = STARTED)
  public void delete(final SelectorConfiguration configuration) {
    if (isInUse(configuration)) {
      throw new IllegalStateException(
          "Content selector " + configuration.getName() + " is in use and cannot be deleted");
    }
    else {
      store.delete(configuration);
    }
  }

  @Subscribe
  @AllowConcurrentEvents
  public void on(final SelectorConfigurationEvent event) {
    cachedBrowseResult = EMPTY_CACHE;
    rolesCache = Collections.emptyMap();

    selectorCache.invalidateAll();
  }

  @Subscribe
  @AllowConcurrentEvents
  public void on(final RoleEvent event) {
    rolesCache = Collections.emptyMap();
  }

  /**
   * Handles a selector configuration change event from another nodes.
   *
   * @param event the {@link SelectorConfigurationChangedEvent} with event type.
   */
  @Subscribe
  public void on(final SelectorConfigurationChangedEvent event) {
    log.debug("Selector configuration has {} on a remote node. Invalidate the cache.", event.getEventType());
    cachedBrowseResult = EMPTY_CACHE;
    rolesCache = Collections.emptyMap();
    selectorCache.invalidateAll();
  }

  /**
   * Handles a role configuration change event from another nodes.
   *
   * @param event the {@link RoleConfigurationEvent} with event type.
   */
  @Subscribe
  public void on(final RoleConfigurationEvent event) {
    rolesCache = Collections.emptyMap();
  }

  @Override
  @Guarded(by = STARTED)
  public boolean evaluate(
      final SelectorConfiguration selectorConfiguration,
      final VariableSource variableSource) throws SelectorEvaluationException
  {
    try {
      return selectorCache.get(selectorConfiguration).evaluate(variableSource);
    }
    catch (Exception e) {
      throw new SelectorEvaluationException(
          "Selector '" + selectorConfiguration.getName() + "' cannot be evaluated", e);
    }
  }

  @Override
  public void toSql(
      final SelectorConfiguration selectorConfiguration,
      final SelectorSqlBuilder sqlBuilder) throws SelectorEvaluationException
  {
    try {
      selectorCache.get(selectorConfiguration).toSql(sqlBuilder);
    }
    catch (Exception e) {
      throw new SelectorEvaluationException(
          "Selector '" + selectorConfiguration.getName() + "' cannot be represented as SQL", e);
    }
  }

  @Override
  public <T> void toSql(
      final SelectorConfiguration selectorConfiguration,
      final T sqlBuilder,
      final CselToSql<T> cselToSql) throws SelectorEvaluationException
  {
    try {
      selectorCache.get(selectorConfiguration).toSql(sqlBuilder, cselToSql);
    }
    catch (Exception e) {
      throw new SelectorEvaluationException(
          "Selector '" + selectorConfiguration.getName() + "' cannot be represented as SQL", e);
    }
  }

  @Override
  @Guarded(by = STARTED)
  public List<SelectorConfiguration> browseActive(
      final Collection<String> repositoryNames,
      final Collection<String> formats)
  {
    AuthorizationManager authorizationManager;
    User currentUser;

    try {
      authorizationManager = securitySystem.getAuthorizationManager(DEFAULT_SOURCE);
      currentUser = getCurrentUser();
    }
    catch (NoSuchAuthorizationManagerException | UserNotFoundException e) {
      log.warn("Unable to load active content selectors", e);
      return Collections.emptyList();
    }

    if (currentUser == null) {
      return Collections.emptyList();
    }

    List<String> roleIds = currentUser.getRoles()
        .stream()
        .map(RoleIdentifier::getRoleId)
        .collect(toList());

    Set<String> privilegeIds = getRoles(roleIds, authorizationManager).stream()
        .map(Role::getPrivileges)
        .flatMap(Collection::stream)
        .collect(Collectors.toSet());

    List<String> contentSelectorNames = authorizationManager.getPrivileges(privilegeIds)
        .stream()
        .filter(repositoryFormatOrNameMatcher(repositoryNames, formats))
        .map(this::getContentSelector)
        .collect(toList());

    return browse().stream().filter(selector -> contentSelectorNames.contains(selector.getName())).collect(toList());
  }

  @Override
  public SelectorConfiguration newSelectorConfiguration() {
    return store.newSelectorConfiguration();
  }

  @Override
  public SelectorConfiguration newSelectorConfiguration(
      final String name,
      final String type,
      final String description,
      final Map<String, ?> attributes)
  {
    SelectorConfiguration selectorConfiguration = store.newSelectorConfiguration();
    selectorConfiguration.setName(name);
    selectorConfiguration.setType(type);
    selectorConfiguration.setDescription(description);
    selectorConfiguration.setAttributes(attributes);
    return selectorConfiguration;
  }

  private User getCurrentUser() throws UserNotFoundException {
    Subject subject = securitySystem.getSubject();
    User currentUser = null;

    if (subject.isAuthenticated() || AnonymousHelper.isAnonymous(subject)) {
      Cache<String, User> cache = getUserCache();
      String userKey = subject.getPrincipal().toString() + subject.getPrincipals().getRealmNames().toString();
      currentUser = cache.get(userKey);
      if (currentUser == null) {
        currentUser = securitySystem.currentUser();
        if (currentUser != null) {
          cache.put(userKey, currentUser);
        }
      }
    }
    return currentUser;
  }

  private Cache<String, User> getUserCache() {
    if (userCache == null) {
      userCache = cacheHelper.maybeCreateCache(USER_CACHE_KEY, CreatedExpiryPolicy.factoryOf(userCacheTimeout));
    }
    return userCache;
  }

  private boolean matchesFormatOrRepository(
      final Collection<String> repositoryNames,
      final Collection<String> formats,
      final Privilege privilege)
  {
    String type = privilege.getType();
    String selector = privilege.getProperties().get(P_REPOSITORY);

    if (selector == null) {
      return false;
    }

    RepositorySelector repositorySelector = RepositorySelector.fromSelector(selector);

    boolean isRepositoryContentSelector = RepositoryContentSelectorPrivilegeDescriptor.TYPE.equals(type);
    boolean matchesFormat = formats.contains(repositorySelector.getFormat()) || repositorySelector.isAllFormats();
    boolean matchesRepositoryName = repositoryNames.contains(repositorySelector.getName());

    boolean isMatchingFormat =
        isRepositoryContentSelector && matchesFormat && repositorySelector.isAllRepositories();
    boolean isMatchingRepository =
        isRepositoryContentSelector && matchesRepositoryName;

    return isMatchingFormat || isMatchingRepository;
  }

  private List<Role> getRoles(
      final List<String> roleIds,
      final AuthorizationManager authorizationManager)
  {
    try {
      Map<String, Role> roleMap, cacheSnapshot;
      cacheSnapshot = rolesCache;
      if (cacheSnapshot.isEmpty()) {
        // Remote roles can't contribute privileges, or have nested roles.
        rolesCache = roleMap = securitySystem.listRoles(UserManager.DEFAULT_SOURCE)
            .stream()
            .collect(Collectors.toMap(Role::getRoleId, Function.identity()));
      }
      else {
        roleMap = cacheSnapshot;
      }

      Set<String> results = new HashSet<>();
      roleIds.forEach(roleId -> traverseRoleTree(roleId, roleMap, results));
      return results.stream()
          .map(roleMap::get)
          .collect(Collectors.toList());
    }
    catch (NoSuchAuthorizationManagerException e) {
      // This should never happen in practice
      log.error("Missing default user manager", e);
      throw new RuntimeException(e);
    }
  }

  private void traverseRoleTree(final String roleId, final Map<String, Role> roleMap, final Set<String> results) {
    if (results.contains(roleId)) {
      // already visited
      return;
    }
    Role role = roleMap.get(roleId);
    if (role == null) {
      // missing role
      return;
    }
    results.add(roleId);
    role.getRoles().forEach(childId -> traverseRoleTree(childId, roleMap, results));
  }

  private Predicate<Privilege> repositoryFormatOrNameMatcher(
      final Collection<String> repositoryNames,
      final Collection<String> formats)
  {
    return (p) -> matchesFormatOrRepository(repositoryNames, formats, p);
  }

  private String getContentSelector(final Privilege privilege) {
    return privilege.getPrivilegeProperty(P_CONTENT_SELECTOR);
  }

  private boolean isInUse(final SelectorConfiguration configuration) {
    return securitySystem.listPrivileges()
        .stream()
        .filter(privilege -> RepositoryContentSelectorPrivilegeDescriptor.TYPE.equals(privilege.getType()))
        .anyMatch(privilege -> privilege.getPrivilegeProperty(P_CONTENT_SELECTOR).equals(configuration.getName()));
  }
}
