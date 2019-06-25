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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.common.app.ManagedLifecycle;
import org.sonatype.nexus.common.entity.EntityId;
import org.sonatype.nexus.common.event.EventAware;
import org.sonatype.nexus.common.stateguard.Guarded;
import org.sonatype.nexus.common.stateguard.StateGuardLifecycleSupport;
import org.sonatype.nexus.repository.security.RepositoryContentSelectorPrivilegeDescriptor;
import org.sonatype.nexus.repository.security.RepositorySelector;
import org.sonatype.nexus.security.SecuritySystem;
import org.sonatype.nexus.security.authz.AuthorizationManager;
import org.sonatype.nexus.security.authz.NoSuchAuthorizationManagerException;
import org.sonatype.nexus.security.privilege.NoSuchPrivilegeException;
import org.sonatype.nexus.security.privilege.Privilege;
import org.sonatype.nexus.security.role.NoSuchRoleException;
import org.sonatype.nexus.security.role.Role;
import org.sonatype.nexus.security.role.RoleIdentifier;
import org.sonatype.nexus.security.user.User;
import org.sonatype.nexus.security.user.UserNotFoundException;
import org.sonatype.nexus.selector.Selector;
import org.sonatype.nexus.selector.SelectorConfiguration;
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

  private final SelectorConfigurationStore store;

  private final SecuritySystem securitySystem;

  private final LoadingCache<SelectorConfiguration, Selector> selectorCache;

  private volatile SoftReference<List<SelectorConfiguration>> cachedBrowseResult = EMPTY_CACHE;

  @Inject
  public SelectorManagerImpl(final SelectorConfigurationStore store,
                             final SecuritySystem securitySystem,
                             final SelectorFactory selectorFactory)
  {
    this.store = checkNotNull(store);
    this.securitySystem = checkNotNull(securitySystem);

    checkNotNull(selectorFactory);
    selectorCache = CacheBuilder.newBuilder().softValues().build(CacheLoader.from(config -> {
      String type = config.getType();
      String expression = config.getAttributes().get("expression");
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
  public void create(final SelectorConfiguration configuration) {
    store.create(configuration);
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
      throw new IllegalStateException("Content selector " + configuration.getName() + " is in use and cannot be deleted");
    } else {
      store.delete(configuration);
    }
  }

  @Subscribe
  @AllowConcurrentEvents
  public void on(final SelectorConfigurationEvent event) {
    cachedBrowseResult = EMPTY_CACHE;

    selectorCache.invalidateAll();
  }

  @Override
  @Guarded(by = STARTED)
  public boolean evaluate(final SelectorConfiguration selectorConfiguration, final VariableSource variableSource)
      throws SelectorEvaluationException
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
  public void toSql(final SelectorConfiguration selectorConfiguration, final SelectorSqlBuilder sqlBuilder)
      throws SelectorEvaluationException
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
  @Guarded(by = STARTED)
  public List<SelectorConfiguration> browseActive(final List<String> repositoryNames, final List<String> formats) {
    AuthorizationManager authorizationManager;
    User currentUser;

    try {
      authorizationManager = securitySystem.getAuthorizationManager(DEFAULT_SOURCE);
      currentUser = securitySystem.currentUser();
    }
    catch (NoSuchAuthorizationManagerException | UserNotFoundException e) {
      log.warn("Unable to load active content selectors", e);
      return Collections.emptyList();
    }

    if (currentUser == null) {
      return Collections.emptyList();
    }

    List<String> roleIds = currentUser.getRoles().stream().map(RoleIdentifier::getRoleId)
        .collect(toList());

    List<Role> roles = getRoles(roleIds, authorizationManager, new ArrayList<>());

    List<String> contentSelectorNames = roles.stream().map(Role::getPrivileges).flatMap(Collection::stream).map(id -> {
      try {
        return authorizationManager.getPrivilege(id);
      }
      catch (NoSuchPrivilegeException e) {
        log.debug("Unable to find privilege for id={}, continuing to check privileges", id, e);
        return null;
      }
    }).filter(Objects::nonNull).filter(repositoryFormatOrNameMatcher(repositoryNames, formats)).map(this::getContentSelector).collect(toList());

    return browse().stream().filter(selector -> contentSelectorNames.contains(selector.getName())).collect(toList());
  }

  private boolean matchesFormatOrRepository(final List<String> repositoryNames,
                                            final List<String> formats,
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

  private List<Role> getRoles(final List<String> roleIds, final AuthorizationManager authorizationManager, final List<Role> roles)
  {
    roleIds.forEach(roleId -> getRoles(roleId, authorizationManager, roles));

    return roles;
  }

  private void getRoles(final String roleId, final AuthorizationManager authorizationManager, final List<Role> roles)
  {
    try {
      Role role = authorizationManager.getRole(roleId);
      roles.add(role);
      role.getRoles().forEach(nestedRoleId -> getRoles(nestedRoleId, authorizationManager, roles));
    }
    catch (NoSuchRoleException e) {
      log.debug("Unable to find role for roleId={}, continue searching for roles", roleId, e);
    }
  }

  private Predicate<Privilege> repositoryFormatOrNameMatcher(final List<String> repositoryNames,
                                                             final List<String> formats)
  {
    return (p) -> matchesFormatOrRepository(repositoryNames, formats, p);
  }

  private String getContentSelector(final Privilege privilege) {
    return privilege.getPrivilegeProperty(P_CONTENT_SELECTOR);
  }

  private boolean isInUse(final SelectorConfiguration configuration) {
    return securitySystem.listPrivileges().stream()
        .filter(privilege -> RepositoryContentSelectorPrivilegeDescriptor.TYPE.equals(privilege.getType()))
        .anyMatch(privilege -> privilege.getPrivilegeProperty(P_CONTENT_SELECTOR).equals(configuration.getName()));
  }
}
