
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
package org.sonatype.nexus.repository.security.internal;

import java.util.Arrays;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Stream;

import javax.annotation.Nullable;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import org.sonatype.nexus.repository.security.ContentPermissionChecker;
import org.sonatype.nexus.repository.security.RepositoryContentSelectorPermission;
import org.sonatype.nexus.repository.security.RepositoryViewPermission;
import org.sonatype.nexus.repository.security.SelectorEvaluationCache;
import org.sonatype.nexus.security.SecurityHelper;
import org.sonatype.nexus.selector.SelectorConfiguration;
import org.sonatype.nexus.selector.SelectorEvaluationException;
import org.sonatype.nexus.selector.SelectorManager;
import org.sonatype.nexus.selector.VariableSource;

import com.google.common.annotations.VisibleForTesting;
import org.apache.shiro.authz.Permission;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Preconditions.checkNotNull;
import org.springframework.stereotype.Component;

/**
 * Content permission checker with request-scoped selector evaluation caching.
 * <p>
 * Caches selector evaluation results per request to eliminate redundant evaluations,
 * particularly for Docker digest-based pulls from group repositories which can cause
 * N selectors × M member repos evaluations (NEXUS-50181).
 * </p>
 * <p>
 * The cache is passed as a parameter through the call chain and is managed by the
 * request handler (typically SecurityHandler), ensuring proper cleanup and preventing
 * memory leaks on long-lived threads.
 * </p>
 */
@Component
@Singleton
public class ContentPermissionCheckerImpl
    implements ContentPermissionChecker
{
  protected final Logger log = LoggerFactory.getLogger(getClass());

  private final SecurityHelper securityHelper;

  private final SelectorManager selectorManager;

  @Inject
  public ContentPermissionCheckerImpl(
      final SecurityHelper securityHelper,
      final SelectorManager selectorManager)
  {
    this.securityHelper = checkNotNull(securityHelper);
    this.selectorManager = checkNotNull(selectorManager);
  }

  @Override
  @Deprecated
  public void clearRequestCache() {
    // No-op - cache is now managed externally via SelectorEvaluationCache parameter
  }

  /**
   * Generates a cache key for selector evaluation caching.
   *
   * @param repositoryName the repository name
   * @param selectorConfigName the selector configuration name
   * @param variableSource the variable source for evaluation
   * @return cache key string
   */
  @VisibleForTesting
  String generateCacheKey(
      final String repositoryName,
      final String selectorConfigName,
      final VariableSource variableSource)
  {
    String userId = securityHelper.subject().getPrincipal() != null
        ? securityHelper.subject().getPrincipal().toString()
        : "anonymous";

    // Extract path from variableSource for cache key
    String path = variableSource.get("path")
        .map(Object::toString)
        .orElse("");

    return userId + ":" + repositoryName + ":" + selectorConfigName + ":" + path;
  }

  /**
   * Evaluates a selector with optional caching support.
   *
   * @param selectorConfiguration the selector configuration
   * @param variableSource the variable source for evaluation
   * @param repositoryName the repository name (for cache key)
   * @param cache optional cache (null to disable caching)
   * @return true if selector evaluates to true, false otherwise
   */
  @VisibleForTesting
  boolean evaluateSelectorWithCache(
      final SelectorConfiguration selectorConfiguration,
      final VariableSource variableSource,
      final String repositoryName,
      @Nullable final SelectorEvaluationCache cache)
  {
    // If no cache provided, evaluate directly
    if (cache == null) {
      return evaluateSelectorDirect(selectorConfiguration, variableSource);
    }

    String cacheKey = generateCacheKey(repositoryName, selectorConfiguration.getName(), variableSource);

    // Check cache first
    Boolean cachedResult = cache.get(cacheKey);
    if (cachedResult != null) {
      if (log.isDebugEnabled()) {
        log.debug("Cache hit for selector '{}' on repository '{}' (key: {})",
            selectorConfiguration.getName(), repositoryName, cacheKey);
      }
      return cachedResult;
    }

    // Cache miss - evaluate and store
    try {
      boolean result = selectorManager.evaluate(selectorConfiguration, variableSource);
      cache.put(cacheKey, result);

      if (log.isDebugEnabled()) {
        log.debug("Cache miss for selector '{}' on repository '{}', evaluated to {} (key: {})",
            selectorConfiguration.getName(), repositoryName, result, cacheKey);
      }

      return result;
    }
    catch (SelectorEvaluationException e) {
      // Don't cache errors - let them fail through
      if (log.isTraceEnabled()) {
        log.debug("Selector evaluation error for '{}': {}", selectorConfiguration.getName(), e.getMessage(), e);
      }
      else {
        log.debug("Selector evaluation error for '{}': {}", selectorConfiguration.getName(), e.getMessage());
      }
      return false;
    }
  }

  /**
   * Evaluates a selector directly without caching.
   *
   * @param selectorConfiguration the selector configuration
   * @param variableSource the variable source for evaluation
   * @return true if selector evaluates to true, false otherwise
   */
  private boolean evaluateSelectorDirect(
      final SelectorConfiguration selectorConfiguration,
      final VariableSource variableSource)
  {
    try {
      return selectorManager.evaluate(selectorConfiguration, variableSource);
    }
    catch (SelectorEvaluationException e) {
      // Don't cache errors - let them fail through
      if (log.isTraceEnabled()) {
        log.debug("Selector evaluation error for '{}': {}", selectorConfiguration.getName(), e.getMessage(), e);
      }
      else {
        log.debug("Selector evaluation error for '{}': {}", selectorConfiguration.getName(), e.getMessage());
      }
      return false;
    }
  }

  @VisibleForTesting
  public boolean isViewPermitted(final String repositoryName, final String repositoryFormat, final String action) {
    return securityHelper.anyPermitted(new RepositoryViewPermission(repositoryFormat, repositoryName, action));
  }

  @VisibleForTesting
  public boolean isViewPermitted(final String repositoryName, final String repositoryFormat, final String... actions) {
    return securityHelper.anyPermitted(permissionsFor(repositoryName, repositoryFormat, actions));
  }

  @VisibleForTesting
  public boolean isViewPermitted(final Set<String> repoNames, final String repositoryFormat, final String... actions) {
    return securityHelper.anyPermitted(permissionsFor(repoNames, repositoryFormat, actions));
  }

  private static RepositoryViewPermission[] permissionsFor(
      final String repositoryName,
      final String repositoryFormat,
      final String... actions)
  {
    return permissionsStreamFor(repositoryName, repositoryFormat, actions)
        .toArray(RepositoryViewPermission[]::new);
  }

  private static RepositoryContentSelectorPermission[] contentPermissionsFor(
      final String repositoryName,
      final String repositoryFormat,
      final SelectorConfiguration selectorConfiguration,
      final String... actions)
  {
    return contentPermissionsStreamFor(selectorConfiguration.getName(), repositoryFormat, repositoryName, actions)
        .toArray(RepositoryContentSelectorPermission[]::new);
  }

  private static RepositoryViewPermission[] permissionsFor(
      final Set<String> repoNames,
      final String repoFormat,
      final String... actions)
  {
    return repoNames.stream()
        .flatMap(repoName -> permissionsStreamFor(repoName, repoFormat, actions))
        .toArray(RepositoryViewPermission[]::new);
  }

  private static Stream<RepositoryViewPermission> permissionsStreamFor(
      final String repositoryName,
      final String repositoryFormat,
      final String... actions)
  {
    return Arrays.stream(actions).map(action -> new RepositoryViewPermission(repositoryFormat, repositoryName, action));
  }

  private static Stream<RepositoryContentSelectorPermission> contentPermissionsStreamFor(
      final String selectorConfigurationName,
      final String repositoryFormat,
      final String repositoryName,
      final String... actions)
  {
    return Arrays.stream(actions)
        .map(action -> new RepositoryContentSelectorPermission(selectorConfigurationName, repositoryFormat,
            repositoryName, Arrays.asList(action)));
  }

  @VisibleForTesting
  public boolean isViewPermitted(
      final Set<String> repositoryNames,
      final String repositoryFormat,
      final String action)
  {
    RepositoryViewPermission[] perms = permissionsFor(repositoryNames, repositoryFormat, action);
    if (perms.length > 0) {
      return securityHelper.anyPermitted(perms);
    }
    return false;
  }

  @VisibleForTesting
  public boolean isContentPermitted(
      final String repositoryName,
      final String repositoryFormat,
      final String action,
      final SelectorConfiguration selectorConfiguration,
      final VariableSource variableSource)
  {
    return isContentPermitted(repositoryName, repositoryFormat, action, selectorConfiguration, variableSource, null);
  }

  @VisibleForTesting
  public boolean isContentPermitted(
      final String repositoryName,
      final String repositoryFormat,
      final String action,
      final SelectorConfiguration selectorConfiguration,
      final VariableSource variableSource,
      @Nullable final SelectorEvaluationCache cache)
  {
    RepositoryContentSelectorPermission perm = new RepositoryContentSelectorPermission(
        selectorConfiguration.getName(), repositoryFormat, repositoryName, Arrays.asList(action));

    // make sure subject has the selector permission before evaluating it, because that's a cheaper/faster check
    return securityHelper.anyPermitted(perm)
        && evaluateSelectorWithCache(selectorConfiguration, variableSource, repositoryName, cache);
  }

  @VisibleForTesting
  public boolean isContentPermittedAnyOf(
      final String repositoryName,
      final String repositoryFormat,
      final SelectorConfiguration selectorConfiguration,
      final VariableSource variableSource,
      final String... actions)
  {
    return isContentPermittedAnyOf(repositoryName, repositoryFormat, selectorConfiguration, variableSource, null,
        actions);
  }

  @VisibleForTesting
  public boolean isContentPermittedAnyOf(
      final String repositoryName,
      final String repositoryFormat,
      final SelectorConfiguration selectorConfiguration,
      final VariableSource variableSource,
      @Nullable final SelectorEvaluationCache cache,
      final String... actions)
  {
    Permission[] permissions =
        contentPermissionsFor(repositoryName, repositoryFormat, selectorConfiguration, actions);
    // make sure subject has the selector permission before evaluating it, because that's a cheaper/faster check
    return securityHelper.anyPermitted(permissions)
        && evaluateSelectorWithCache(selectorConfiguration, variableSource, repositoryName, cache);
  }

  @VisibleForTesting
  public boolean isContentPermittedAnyOf(
      final Set<String> repositoryNames,
      final String repositoryFormat,
      final SelectorConfiguration selectorConfiguration,
      final VariableSource variableSource,
      final String... actions)
  {
    return isContentPermittedAnyOf(repositoryNames, repositoryFormat, selectorConfiguration, variableSource, null,
        actions);
  }

  @VisibleForTesting
  public boolean isContentPermittedAnyOf(
      final Set<String> repositoryNames,
      final String repositoryFormat,
      final SelectorConfiguration selectorConfiguration,
      final VariableSource variableSource,
      @Nullable final SelectorEvaluationCache cache,
      final String... actions)
  {
    Permission[] permissions = repositoryNames.stream()
        .flatMap(repoName -> contentPermissionsStreamFor(selectorConfiguration.getName(), repositoryFormat, repoName,
            actions))
        .toArray(Permission[]::new);
    // make sure subject has the selector permission before evaluating it, because that's a cheaper/faster check
    // For multiple repos, use first repo name in cache key (selector evaluation is same for all)
    String cacheKeyRepo = repositoryNames.isEmpty() ? "" : repositoryNames.iterator().next();
    return securityHelper.anyPermitted(permissions)
        && evaluateSelectorWithCache(selectorConfiguration, variableSource, cacheKeyRepo, cache);
  }

  private void logMsgAndMaybeException(final SelectorEvaluationException ex) {
    if (log.isTraceEnabled()) {
      log.debug(ex.getMessage(), ex);
    }
    else {
      log.debug(ex.getMessage());
    }
  }

  @VisibleForTesting
  public boolean isContentPermitted(
      final Set<String> repositoryNames,
      final String repositoryFormat,
      final String action,
      final SelectorConfiguration selectorConfiguration,
      final VariableSource variableSource)
  {
    return isContentPermitted(repositoryNames, repositoryFormat, action, selectorConfiguration, variableSource, null);
  }

  @VisibleForTesting
  public boolean isContentPermitted(
      final Set<String> repositoryNames,
      final String repositoryFormat,
      final String action,
      final SelectorConfiguration selectorConfiguration,
      final VariableSource variableSource,
      @Nullable final SelectorEvaluationCache cache)
  {
    RepositoryContentSelectorPermission[] perms = repositoryNames.stream()
        .map(
            repositoryName -> new RepositoryContentSelectorPermission(selectorConfiguration.getName(), repositoryFormat,
                repositoryName, Arrays.asList(action)))
        .toArray(RepositoryContentSelectorPermission[]::new);
    if (perms.length > 0) {
      // make sure subject has the selector permission before evaluating it, because that's a cheaper/faster check
      // For multiple repos, use first repo name in cache key (selector evaluation is same for all)
      String cacheKeyRepo = repositoryNames.isEmpty() ? "" : repositoryNames.iterator().next();
      return securityHelper.anyPermitted(perms)
          && evaluateSelectorWithCache(selectorConfiguration, variableSource, cacheKeyRepo, cache);
    }

    return false;
  }

  @Override
  public boolean isPermitted(
      final String repositoryName,
      final String repositoryFormat,
      final String action,
      final VariableSource variableSource)
  {
    return isPermitted(repositoryName, repositoryFormat, action, variableSource, null);
  }

  @Override
  public boolean isPermitted(
      final String repositoryName,
      final String repositoryFormat,
      final String action,
      final VariableSource variableSource,
      @Nullable final SelectorEvaluationCache cache)
  {
    // check view perm first, if applicable, grant access
    if (isViewPermitted(repositoryName, repositoryFormat, action)) {
      return true;
    }
    // otherwise check the content selector perms
    return selectorManager.browse()
        .stream()
        .anyMatch(
            config -> isContentPermitted(repositoryName, repositoryFormat, action, config, variableSource, cache));
  }

  @Override
  public boolean isPermittedJexlOnly(
      final String repositoryName,
      final String repositoryFormat,
      final String action,
      final VariableSource variableSource)
  {
    // check view perm first, if applicable, grant access
    if (isViewPermitted(repositoryName, repositoryFormat, action)) {
      return true;
    }
    // otherwise check the content selector perms
    return selectorManager.browseJexl()
        .stream()
        .anyMatch(config -> isContentPermitted(repositoryName, repositoryFormat, action, config, variableSource));
  }

  @Override
  public boolean isPermitted(
      final Set<String> repositoryNames,
      final String repositoryFormat,
      final String action,
      final VariableSource variableSource)
  {
    return isPermitted(repositoryNames, repositoryFormat, action, variableSource, null);
  }

  @Override
  public boolean isPermitted(
      final Set<String> repositoryNames,
      final String repositoryFormat,
      final String action,
      final VariableSource variableSource,
      @Nullable final SelectorEvaluationCache cache)
  {
    if (repositoryNames.isEmpty()) {
      return false;
    }

    if (isViewPermitted(repositoryNames, repositoryFormat, action)) {
      return true;
    }
    return selectorManager.browseActive(repositoryNames, Collections.singletonList(repositoryFormat))
        .stream()
        .anyMatch(
            config -> isContentPermitted(repositoryNames, repositoryFormat, action, config, variableSource, cache));
  }

  @Override
  public boolean isPermittedJexlOnlyAnyOf(
      final String repositoryName,
      final String repositoryFormat,
      final VariableSource variableSource,
      final String... actions)
  {
    // check view perm first, if applicable, grant access
    if (isViewPermitted(repositoryName, repositoryFormat, actions)) {
      return true;
    }
    // otherwise check the content selector perms
    return selectorManager.browseJexl()
        .stream()
        .anyMatch(config -> isContentPermittedAnyOf(repositoryName, repositoryFormat, config, variableSource, actions));
  }

  @Override
  public boolean isPermittedAnyOf(
      final String repositoryName,
      final String repositoryFormat,
      final VariableSource variableSource,
      final String... actions)
  {
    // check view perm first, if applicable, grant access
    if (isViewPermitted(repositoryName, repositoryFormat, actions)) {
      return true;
    }
    // otherwise check the content selector perms
    return selectorManager.browse()
        .stream()
        .anyMatch(config -> isContentPermittedAnyOf(repositoryName, repositoryFormat, config, variableSource, actions));
  }

  @Override
  public boolean isPermittedAnyOf(
      final Set<String> repositoryNames,
      final String repositoryFormat,
      final VariableSource variableSource,
      final String... actions)
  {
    if (repositoryNames.isEmpty()) {
      return false;
    }

    if (isViewPermitted(repositoryNames, repositoryFormat, actions)) {
      return true;
    }
    return selectorManager.browse()
        .stream()
        .anyMatch(
            config -> isContentPermittedAnyOf(repositoryNames, repositoryFormat, config, variableSource, actions));
  }
}
