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
package org.sonatype.nexus.coreui.service;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import javax.annotation.Nullable;
import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.groups.Default;

import org.sonatype.nexus.common.QualifierUtil;
import org.sonatype.nexus.common.app.BaseUrlHolder;
import org.sonatype.nexus.common.app.GlobalComponentLookupHelper;
import org.sonatype.nexus.common.entity.DetachedEntityId;
import org.sonatype.nexus.common.entity.EntityId;
import org.sonatype.nexus.coreui.ReferenceXO;
import org.sonatype.nexus.coreui.RepositoryReferenceXO;
import org.sonatype.nexus.coreui.RepositoryStatusXO;
import org.sonatype.nexus.coreui.RepositoryXO;
import org.sonatype.nexus.coreui.search.BrowseableFormatXO;
import org.sonatype.nexus.extdirect.model.StoreLoadParameters;
import org.sonatype.nexus.rapture.PasswordPlaceholder;
import org.sonatype.nexus.rapture.StateContributor;
import org.sonatype.nexus.repository.BadRequestException;
import org.sonatype.nexus.repository.Facet;
import org.sonatype.nexus.repository.Format;
import org.sonatype.nexus.common.stateguard.InvalidStateException;
import org.sonatype.nexus.repository.MissingFacetException;
import org.sonatype.nexus.repository.Recipe;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.cache.RepositoryCacheInvalidationService;
import org.sonatype.nexus.repository.config.Configuration;
import org.sonatype.nexus.repository.config.ConfigurationStore;
import org.sonatype.nexus.repository.httpclient.HttpClientFacet;
import org.sonatype.nexus.repository.httpclient.RemoteConnectionStatus;
import org.sonatype.nexus.repository.httpclient.RemoteConnectionStatusType;
import org.sonatype.nexus.repository.manager.RepositoryManager;
import org.sonatype.nexus.repository.manager.internal.BaseRepositoryManager;
import org.sonatype.nexus.repository.manager.internal.FailedRepositoryTracker;
import org.sonatype.nexus.repository.manager.internal.HttpAuthenticationSecretEncoder;
import org.sonatype.nexus.repository.rest.api.RepositoryMetricsService;
import org.sonatype.nexus.repository.search.index.RebuildIndexTask;
import org.sonatype.nexus.repository.search.index.RebuildIndexTaskDescriptor;
import org.sonatype.nexus.repository.security.RepositoryAdminPermission;
import org.sonatype.nexus.repository.security.RepositoryPermissionChecker;
import org.sonatype.nexus.repository.security.RepositorySelector;
import org.sonatype.nexus.repository.types.ProxyType;
import org.sonatype.nexus.scheduling.TaskConfiguration;
import org.sonatype.nexus.scheduling.TaskInfo;
import org.sonatype.nexus.scheduling.TaskScheduler;
import org.sonatype.nexus.security.BreadActions;
import org.sonatype.nexus.security.SecurityHelper;
import org.sonatype.nexus.validation.Validate;
import org.sonatype.nexus.validation.group.Create;
import org.sonatype.nexus.validation.group.Update;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Sets;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.apache.commons.lang3.StringUtils;
import org.apache.shiro.authz.AuthorizationException;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.sonatype.nexus.coreui.internal.RepositoryCleanupAttributesUtil.initializeCleanupAttributes;

@Component
@Singleton
public class RepositoryUiService
    implements StateContributor
{
  protected final Logger log = LoggerFactory.getLogger(getClass());

  private final RepositoryCacheInvalidationService repositoryCacheInvalidationService;

  private final RepositoryManager repositoryManager;

  private final Optional<RepositoryMetricsService> repositoryMetricsService;

  private final ConfigurationStore configurationStore;

  private final SecurityHelper securityHelper;

  private final Map<String, Recipe> recipes;

  private final TaskScheduler taskScheduler;

  private final GlobalComponentLookupHelper typeLookup;

  private final List<Format> formats;

  private final RepositoryPermissionChecker repositoryPermissionChecker;

  private final FailedRepositoryTracker failedRepositoryTracker;

  private final HttpAuthenticationSecretEncoder httpAuthenticationSecretEncoder;

  public static final String INVALID_BLOBSTORENAME_EXCEPTION_MESSAGE =
      "Only letters, digits, underscores(_), hyphens(-), and dots(.) are allowed and may not start with underscore or dot.";

  @Inject
  public RepositoryUiService(
      final RepositoryCacheInvalidationService repositoryCacheInvalidationService,
      final RepositoryManager repositoryManager,
      @Nullable final RepositoryMetricsService repositoryMetricsService,
      final ConfigurationStore configurationStore,
      final SecurityHelper securityHelper,
      final List<Recipe> recipesList,
      final TaskScheduler taskScheduler,
      final GlobalComponentLookupHelper typeLookup,
      final List<Format> formats,
      final RepositoryPermissionChecker repositoryPermissionChecker,
      final FailedRepositoryTracker failedRepositoryTracker,
      final HttpAuthenticationSecretEncoder httpAuthenticationSecretEncoder)
  {
    this.repositoryCacheInvalidationService = checkNotNull(repositoryCacheInvalidationService);
    this.repositoryManager = checkNotNull(repositoryManager);
    this.repositoryMetricsService = Optional.ofNullable(repositoryMetricsService);
    this.configurationStore = checkNotNull(configurationStore);
    this.securityHelper = checkNotNull(securityHelper);
    this.recipes = new HashMap<>(QualifierUtil.buildQualifierBeanMap(checkNotNull(recipesList)));
    this.taskScheduler = checkNotNull(taskScheduler);
    this.typeLookup = checkNotNull(typeLookup);
    this.formats = checkNotNull(formats);
    this.repositoryPermissionChecker = checkNotNull(repositoryPermissionChecker);
    this.failedRepositoryTracker = checkNotNull(failedRepositoryTracker);
    this.httpAuthenticationSecretEncoder = checkNotNull(httpAuthenticationSecretEncoder);
  }

  public List<RepositoryXO> read() {
    return StreamSupport.stream(browse().spliterator(), false)
        .map(this::asRepository)
        .collect(Collectors.toList());
  }

  public List<ReferenceXO> readRecipes() {
    return recipes.entrySet()
        .stream()
        .filter(entry -> entry.getValue().isFeatureEnabled())
        .map(RepositoryUiService::toReference)
        .collect(Collectors.toList());
  }

  private static ReferenceXO toReference(final Entry<String, Recipe> recipe) {
    ReferenceXO xo = new ReferenceXO();
    xo.setId(recipe.getKey());
    xo.setName(String.format("%s (%s)", recipe.getValue().getFormat(), recipe.getValue().getType()));
    return xo;
  }

  public List<Format> readFormats() {
    return formats;
  }

  public List<BrowseableFormatXO> getBrowseableFormats() {
    return StreamSupport.stream(repositoryManager.browse().spliterator(), false)
        .map(Repository::getFormat)
        .map(Format::getValue)
        .distinct()
        .map(RepositoryUiService::toBrowseableFormat)
        .collect(Collectors.toList());
  }

  private static BrowseableFormatXO toBrowseableFormat(final String format) {
    BrowseableFormatXO xo = new BrowseableFormatXO();
    xo.setId(format);
    return xo;
  }

  @Override
  public Map<String, Object> getState() {
    return Collections.singletonMap("browseableformats", getBrowseableFormats());
  }

  /**
   * Retrieve a list of available repositories references.
   */
  public List<RepositoryReferenceXO> readReferences(final @Nullable StoreLoadParameters parameters) {
    List<RepositoryReferenceXO> references = StreamSupport.stream(filter(parameters).spliterator(), false)
        .map(repository -> new RepositoryReferenceXO(repository.getRepositoryName(), repository.getRepositoryName(),
            getType(repository), getFormat(repository), getVersionPolicy(repository),
            getUrl(repository.getRepositoryName()), getBlobStoreName(repository), buildStatus(repository)))
        .collect(Collectors.toList());
    references = filterForAutocomplete(parameters, references);
    return references;
  }

  private String getBlobStoreName(final Configuration repository) {
    return repository.getAttributes()
        .get("storage")
        .get("blobStoreName")
        .toString();
  }

  private static String getVersionPolicy(final Configuration configuration) {
    return Optional.of(configuration)
        .map(Configuration::getAttributes)
        .map(attr -> attr.get("maven"))
        .map(maven -> maven.get("versionPolicy"))
        .map(String.class::cast)
        .orElse(null);
  }

  @VisibleForTesting
  static List<RepositoryReferenceXO> filterForAutocomplete(
      final @Nullable StoreLoadParameters parameters,
      final List<RepositoryReferenceXO> references)
  {
    if (StringUtils.isNotBlank(parameters.getQuery())) {
      return references.stream()
          .filter(repo -> repo.getName().startsWith(parameters.getQuery()))
          .collect(Collectors.toList());
    }
    return references;
  }

  /**
   * Retrieve a list of available repositories references + add an entry for all repositories '*".
   */
  public List<RepositoryReferenceXO> readReferencesAddingEntryForAll(final @Nullable StoreLoadParameters parameters) {
    List<RepositoryReferenceXO> references = readReferences(parameters);
    RepositoryReferenceXO all = new RepositoryReferenceXO(RepositorySelector.all().toSelector(), "(All Repositories)",
        null, null, null, null, null, null, 1);
    references.add(all);
    return references;
  }

  /**
   * Retrieve a list of available repositories references + add an entry for all repositories '*' and an entry for
   * format 'All (format) repositories' '*(format)'".
   */
  public List<RepositoryReferenceXO> readReferencesAddingEntriesForAllFormats(
      final @Nullable StoreLoadParameters parameters)
  {
    List<RepositoryReferenceXO> references = readReferencesAddingEntryForAll(parameters);
    formats.stream().forEach(format -> {
      references.add(new RepositoryReferenceXO(RepositorySelector.allOfFormat(format.getValue()).toSelector(),
          "(All " + format.getValue() + " Repositories)", null, null, null, null, null, null));
    });
    return references;
  }

  @RequiresAuthentication
  @Validate(groups = {Create.class, Default.class})
  public RepositoryXO create(final @NotNull @Valid RepositoryXO repositoryXO) throws Exception {
    securityHelper.ensurePermitted(new RepositoryAdminPermission(repositoryXO.getFormat(), repositoryXO.getName(),
        Collections.singletonList(BreadActions.ADD)));

    initializeCleanupAttributes(repositoryXO);

    Configuration config = repositoryManager.newConfiguration();
    config.setRepositoryName(repositoryXO.getName());
    config.setRecipeName(repositoryXO.getRecipe());
    config.setOnline(repositoryXO.getOnline());

    Optional.ofNullable(repositoryXO)
        .map(RepositoryXO::getRoutingRuleId)
        .filter(StringUtils::isNotBlank)
        .map(DetachedEntityId::new)
        .ifPresent(config::setRoutingRuleId);

    config.setAttributes(repositoryXO.getAttributes());

    return asRepository(repositoryManager.create(config));
  }

  @RequiresAuthentication
  @Validate(groups = {Update.class, Default.class})
  public RepositoryXO update(final @NotNull @Valid RepositoryXO repositoryXO) throws Exception {
    String name = repositoryXO.getName();

    // Check if repository exists but failed to load (NEXUS-51389)
    if (failedRepositoryTracker.hasFailed(name)) {
      return updateFailedRepository(repositoryXO);
    }

    Repository repository = safelyGetRepository(name, BreadActions.EDIT);

    // Replace stored password and bearerTokenId if placeholders are used
    Optional.of(repositoryXO)
        .map(RepositoryXO::getAttributes)
        .map(attr -> attr.get("httpclient"))
        .map(httpclient -> httpclient.get("authentication"))
        .map(Map.class::cast)
        .ifPresent(authentication -> {
          restoreSecretIfPlaceholder(repository, authentication, "password");
          restoreSecretIfPlaceholder(repository, authentication, "bearerTokenId");
        });

    initializeCleanupAttributes(repositoryXO);

    Configuration updatedConfiguration = repository.getConfiguration().copy();
    updatedConfiguration.setOnline(repositoryXO.getOnline());
    updatedConfiguration.setRoutingRuleId(toDetachedEntityId(repositoryXO.getRoutingRuleId()));
    updatedConfiguration.setAttributes(repositoryXO.getAttributes());

    return asRepository(repositoryManager.update(updatedConfiguration));
  }

  /**
   * Updates a repository that failed to load by updating its configuration directly
   * and attempting to retry loading it.
   */
  private RepositoryXO updateFailedRepository(final RepositoryXO repositoryXO) throws Exception {
    String name = repositoryXO.getName();
    log.warn("Updating failed repository '{}' directly via configuration store", name);

    // Get configuration from store
    Configuration configuration = configurationStore.list()
        .stream()
        .filter(c -> c.getRepositoryName().equalsIgnoreCase(name))
        .findFirst()
        .orElseThrow(() -> new BadRequestException("Repository configuration not found: " + name));

    // Check permissions using configuration-based permission
    Recipe recipe = recipes.get(configuration.getRecipeName());
    if (recipe == null) {
      throw new BadRequestException("Recipe not found for repository: " + name);
    }
    securityHelper.ensurePermitted(new RepositoryAdminPermission(
        recipe.getFormat().getValue(), name, Collections.singletonList(BreadActions.EDIT)));

    initializeCleanupAttributes(repositoryXO);

    // Update configuration
    Configuration updatedConfiguration = configuration.copy();
    updatedConfiguration.setOnline(repositoryXO.getOnline());
    updatedConfiguration.setRoutingRuleId(toDetachedEntityId(repositoryXO.getRoutingRuleId()));
    updatedConfiguration.setAttributes(repositoryXO.getAttributes());

    // Encode authentication secrets before saving (normally done by RepositoryManager.update)
    httpAuthenticationSecretEncoder.encodeHttpAuthPassword(
        configuration.getAttributes(), updatedConfiguration.getAttributes());

    configurationStore.update(updatedConfiguration);

    // Try to load the repository now that configuration is updated
    if (repositoryManager instanceof BaseRepositoryManager) {
      Optional<Repository> retried = ((BaseRepositoryManager<?>) repositoryManager).retryFailedRepository(name);
      if (retried.isPresent()) {
        log.info("Repository '{}' recovered after configuration update", name);
        return asRepository(retried.get());
      }
    }

    // Still failed - return config-based representation
    log.warn("Repository '{}' still cannot load after configuration update", name);
    return asRepository(updatedConfiguration);
  }

  private static final String BEARER_TOKEN = "bearerToken";

  private static final String BEARER_TOKEN_ID = "bearerTokenId";

  /**
   * Restores a secret value from the stored repository configuration if the provided value is a placeholder.
   *
   * @param repository the repository containing the stored configuration
   * @param authentication the authentication map to potentially update
   * @param secretKey the key of the secret to check (e.g., "password" or "bearerTokenId")
   */
  private static void restoreSecretIfPlaceholder(
      final Repository repository,
      final Map<String, Object> authentication,
      final String secretKey)
  {
    String value = (String) authentication.get(secretKey);
    if (PasswordPlaceholder.is(value)) {
      Optional<Map<String, Object>> storedAuth = Optional.of(repository)
          .map(Repository::getConfiguration)
          .map(Configuration::getAttributes)
          .map(attr -> attr.get("httpclient"))
          .map(Map.class::cast)
          .map(httpclient -> httpclient.get("authentication"))
          .map(Map.class::cast);

      storedAuth
          .map(auth -> auth.get(secretKey))
          .ifPresentOrElse(
              storedValue -> authentication.put(secretKey, storedValue),
              () -> {
                // For bearerTokenId, fallback to bearerToken key for pre-migration repositories
                if (BEARER_TOKEN_ID.equals(secretKey)) {
                  storedAuth.map(auth -> auth.get(BEARER_TOKEN))
                      .ifPresent(oldValue -> authentication.put(secretKey, oldValue));
                }
              });
    }
  }

  private DetachedEntityId toDetachedEntityId(final String s) {
    return StringUtils.isBlank(s) ? null : new DetachedEntityId(s);
  }

  @RequiresAuthentication
  @Validate
  public void remove(final @NotEmpty String name) throws Exception {
    safelyGetRepository(name, BreadActions.DELETE);
    repositoryManager.delete(name);
  }

  @RequiresAuthentication
  @Validate
  public String rebuildIndex(final @NotEmpty String name) {
    Repository repository = repositoryManager.get(name);
    securityHelper.ensurePermitted(adminPermission(repository, BreadActions.EDIT));
    TaskConfiguration taskConfiguration =
        taskScheduler.createTaskConfigurationInstance(RebuildIndexTaskDescriptor.TYPE_ID);
    taskConfiguration.setString(RebuildIndexTask.REPOSITORY_NAME_FIELD_ID, repository.getName());
    TaskInfo taskInfo = taskScheduler.submit(taskConfiguration);
    return taskInfo.getId();
  }

  @RequiresAuthentication
  @Validate
  public void invalidateCache(final @NotEmpty String name) {
    Repository repository = repositoryManager.get(name);
    securityHelper.ensurePermitted(adminPermission(repository, BreadActions.EDIT));
    repositoryCacheInvalidationService.processCachesInvalidation(repository);
  }

  @VisibleForTesting
  RepositoryXO asRepository(final Repository input) {
    RepositoryXO xo = new RepositoryXO();
    xo.setName(input.getName());
    xo.setType(input.getType().getValue());
    xo.setFormat(input.getFormat().getValue());
    xo.setOnline(input.getConfiguration().isOnline());
    xo.setRecipe(input.getConfiguration().getRecipeName());
    xo.setStatus(buildStatus(input));

    String routingRuleId = Optional.of(input)
        .map(Repository::getConfiguration)
        .map(Configuration::getRoutingRuleId)
        .map(EntityId::getValue)
        .filter(StringUtils::isNotBlank)
        .orElse("");
    xo.setRoutingRuleId(routingRuleId);

    xo.setAttributes(filterAttributes(input.getConfiguration().copy().getAttributes()));
    xo.setUrl(getUrl(input.getName()));

    return xo;
  }

  private Repository safelyGetRepository(final String name, final String action) {
    final Repository repository = repositoryManager.get(name);
    if (repository == null) {
      throw new BadRequestException("User is not authorized to " + action + " " + name +
          " or repository does not exist.");
    }
    try {
      securityHelper.ensurePermitted(adminPermission(repository, action));
    }
    catch (AuthorizationException e) {
      throw new BadRequestException("User is not authorized to " + action + " " + name +
          " or repository does not exist.");
    }
    return repository;
  }

  private RepositoryXO asRepository(final Configuration input) {
    RepositoryXO xo = new RepositoryXO();
    xo.setName(input.getRepositoryName());
    xo.setType(getType(input));
    xo.setFormat(getFormat(input));
    xo.setSize(getSize(input));
    xo.setOnline(input.isOnline());
    xo.setRecipe(input.getRecipeName());
    xo.setStatus(buildStatus(input));

    String routingRuleId = Optional.of(input)
        .map(Configuration::getRoutingRuleId)
        .map(EntityId::getValue)
        .filter(StringUtils::isNotBlank)
        .orElse("");
    xo.setRoutingRuleId(routingRuleId);

    xo.setAttributes(filterAttributes(input.copy().getAttributes()));
    xo.setUrl(getUrl(input.getRepositoryName()));

    return xo;
  }

  private static String getUrl(final String repositoryName) {
    return BaseUrlHolder.get() + "/repository/" + repositoryName + "/"; // trailing slash is important
  }

  private static Map<String, Map<String, Object>> filterAttributes(final Map<String, Map<String, Object>> attributes) {
    Optional.ofNullable(attributes)
        .map(attr -> attr.get("httpclient"))
        .map(httpclient -> httpclient.get("authentication"))
        .map(Map.class::cast)
        .ifPresent(authentication -> {
          replaceSecretWithPlaceholder(authentication, "password");
          replaceSecretWithPlaceholder(authentication, BEARER_TOKEN_ID);
          putPlaceholderForOldBearerTokenKey(authentication);
        });

    // Normalize contentDisposition null to INLINE for repositories without content disposition
    normalizeContentDisposition(attributes, "maven");
    normalizeContentDisposition(attributes, "raw");

    return attributes;
  }

  private static void normalizeContentDisposition(
      final Map<String, Map<String, Object>> attributes,
      final String format)
  {
    if (attributes == null) {
      return;
    }

    Map<String, Object> formatAttrs = attributes.get(format);
    if (formatAttrs == null) {
      // format attributes don't exist -> create them with INLINE
      formatAttrs = new HashMap<>();
      formatAttrs.put("contentDisposition", "INLINE");
      attributes.put(format, formatAttrs);
    }
    else if (!formatAttrs.containsKey("contentDisposition") || formatAttrs.get("contentDisposition") == null) {
      // no key or key exists with null value -> set INLINE
      formatAttrs.put("contentDisposition", "INLINE");
    }
  }

  private static void putPlaceholderForOldBearerTokenKey(final Map<String, Object> authentication) {
    // Pre-migration: bearerToken exists but bearerTokenId doesn't - show placeholder for UI
    if (!authentication.containsKey(BEARER_TOKEN_ID) && authentication.containsKey(BEARER_TOKEN)) {
      authentication.put(BEARER_TOKEN_ID, PasswordPlaceholder.get());
    }
  }

  /**
   * Replaces a secret value with a placeholder if it exists in the authentication map.
   *
   * @param authentication the authentication map to update
   * @param secretKey the key of the secret to replace (e.g., "password" or "bearerTokenId")
   */
  private static void replaceSecretWithPlaceholder(
      final Map<String, Object> authentication,
      final String secretKey)
  {
    if (authentication.containsKey(secretKey) && authentication.get(secretKey) != null) {
      authentication.put(secretKey, PasswordPlaceholder.get());
    }
  }

  @RequiresAuthentication
  public List<RepositoryStatusXO> readStatus(final Map<String, String> params) {
    return StreamSupport.stream(browse().spliterator(), true)
        .map(this::buildStatus)
        .collect(Collectors.toList());
  }

  private RepositoryStatusXO buildStatus(final Repository repository) {
    RepositoryStatusXO statusXO = new RepositoryStatusXO();
    statusXO.setRepositoryName(repository.getName());
    statusXO.setOnline(repository.getConfiguration().isOnline());

    // TODO - should we try to aggregate status from group members?
    if (repository.getType() instanceof ProxyType) {
      try {
        RemoteConnectionStatus remoteStatus = repository.facet(HttpClientFacet.class).getStatus();
        statusXO.setDescription(remoteStatus.getDescription());
        if (remoteStatus.getReason() != null) {
          statusXO.setReason(remoteStatus.getReason());
        }
      }
      catch (MissingFacetException e) {
        // no http client facet (usually on proxies), no remote status
      }
      catch (InvalidStateException e) {
        // repository is not in STARTED state (e.g., being deleted), skip status query
        log.debug("Cannot get status for repository {} - invalid state: {}", repository.getName(), e.getMessage());
      }
    }
    return statusXO;
  }

  private RepositoryStatusXO buildStatus(final Configuration configuration) {
    RepositoryStatusXO statusXO = new RepositoryStatusXO();
    statusXO.setRepositoryName(configuration.getRepositoryName());

    // Check if repository failed to load (NEXUS-51389)
    Optional<FailedRepositoryTracker.RepositoryFailure> failure =
        failedRepositoryTracker.getFailure(configuration.getRepositoryName());
    if (failure.isPresent()) {
      statusXO.setOnline(false);
      statusXO.setDescription(RemoteConnectionStatusType.FAILED.getDescription());
      statusXO.setReason(failure.get().getReason());
      return statusXO;
    }

    statusXO.setOnline(configuration.isOnline());

    Recipe recipe = recipes.get(configuration.getRecipeName());
    // TODO - should we try to aggregate status from group members?
    if (recipe.getType() instanceof ProxyType) {
      try {
        Repository repository = repositoryManager.get(configuration.getRepositoryName());
        if (repository != null) {
          RemoteConnectionStatus remoteStatus = repository.facet(HttpClientFacet.class).getStatus();
          statusXO.setDescription(remoteStatus.getDescription());
          if (remoteStatus.getReason() != null) {
            statusXO.setReason(remoteStatus.getReason());
          }
        }
      }
      catch (MissingFacetException e) {
        // no http client facet (usually on proxies), no remote status
      }
      catch (InvalidStateException e) {
        // repository is not in STARTED state (e.g., being deleted), skip status query
        log.debug("Cannot get status for repository {} - invalid state: {}",
            configuration.getRepositoryName(), e.getMessage());
      }
    }
    return statusXO;
  }

  @VisibleForTesting
  public Iterable<Configuration> filter(final @Nullable StoreLoadParameters parameters) {
    Function<Configuration, String> configToFormat =
        configuration -> recipes.get(configuration.getRecipeName()).getFormat().getValue();

    List<Configuration> configurations = configurationStore.list();
    if (parameters != null) {
      String format = parameters.getFilter("format");
      if (format != null && format.indexOf(",") > -1) {
        Set<String> formats = Sets.newHashSet(format.split(","));

        configurations = configurations.stream()
            .filter(configuration -> formats.contains(configToFormat.apply(configuration)))
            .collect(Collectors.toList());
      }
      else {
        configurations = filterIn(configurations, format, configToFormat);
      }

      String type = parameters.getFilter("type");
      configurations = filterIn(configurations, type, this::getType);

      List<Class<Facet>> facetTypes = toFacetList(parameters.getFilter("facets"));
      if (!facetTypes.isEmpty()) {
        configurations = configurations.stream()
            .filter(configuration -> Optional.ofNullable(repositoryManager.get(configuration.getRepositoryName()))
                .map(repositoryHasAnyFacet(facetTypes))
                .orElse(false))
            .collect(Collectors.toList());
      }
      String versionPolicies = parameters.getFilter("versionPolicies");

      configurations = filterIn(configurations, versionPolicies, configuration -> Optional.of(configuration)
          .map(Configuration::getAttributes)
          .map(attr -> attr.get("maven"))
          .map(Map.class::cast)
          .map(maven -> maven.get("versionPolicy"))
          .map(String.class::cast)
          .orElse(null));
    }

    configurations = repositoryPermissionChecker
        .userCanBrowseRepositories(configurations.toArray(new Configuration[0]));

    return configurations;
  }

  private Function<Repository, Boolean> repositoryHasAnyFacet(final List<Class<Facet>> facetTypes) {
    return repository -> {
      for (Class<Facet> facetType : facetTypes) {
        try {
          repository.facet(facetType);
          return true;
        }
        catch (MissingFacetException ignored) {
          // facet not present, skip it
        }
      }
      return false;
    };
  }

  private List<Class<Facet>> toFacetList(final String facets) {
    if (facets == null) {
      return Collections.emptyList();
    }
    return Arrays.asList(facets.split(","))
        .stream()
        .filter(StringUtils::isNotBlank)
        .map(typeLookup::type)
        .map(clazz -> (Class<Facet>) clazz)
        .collect(Collectors.toList());
  }

  private Iterable<Configuration> browse() {
    return repositoryPermissionChecker.userHasRepositoryAdminPermissionFor(configurationStore.list(),
        BreadActions.READ);
  }

  RepositoryAdminPermission adminPermission(final Repository repository, final String action) {
    return new RepositoryAdminPermission(repository.getFormat().getValue(), repository.getName(),
        Collections.singletonList(action));
  }

  private String getFormat(final Configuration configuration) {
    Recipe recipe = recipes.get(configuration.getRecipeName());
    return recipe.getFormat().getValue();
  }

  private String getType(final Configuration configuration) {
    Recipe recipe = recipes.get(configuration.getRecipeName());
    return recipe.getType().getValue();
  }

  private Long getSize(final Configuration configuration) {
    return repositoryMetricsService
        .flatMap(s -> s.get(configuration.getRepositoryName()).map(repoMetrics -> repoMetrics.totalSize))
        .orElse(null);
  }

  /**
   * Filters a collection by evaluating if the field dictated by filteredFieldSelector is in the list of comma separated
   * values in filter and is not in the list of comma separated values in filter that are prepended by '!'. NOTE: A list
   * of only excludes will include all other items. Used to parse the filters build by
   * {@link org.sonatype.nexus.formfields.RepositoryCombobox#getStoreFilters()}
   *
   * @param iterable The iterable to filter
   * @param filter A comma separated list of values which either match the selected field or, if
   *          prepended with '!', do not match the field
   * @param filteredFieldSelector A selector for the field to match against the supplied filter list
   * @return The filtered iterable
   */
  private static <U> List<U> filterIn(
      final Iterable<U> iterable,
      final String filter,
      final Function<U, String> filteredFieldSelector)
  {
    if (filter == null) {
      return iterable instanceof List
          ? (List<U>) iterable
          : StreamSupport.stream(iterable.spliterator(), false).collect(Collectors.toList());
    }

    List<String> filters = Arrays.asList(filter.split(","));

    // If the filters are only exclude type, the default behavior should be to include the other items
    boolean allExcludes = filters.stream().allMatch(strFilter -> strFilter.startsWith("!"));

    return StreamSupport.stream(iterable.spliterator(), false)
        .filter(result -> {
          String fieldValue = filteredFieldSelector.apply(result);

          boolean shouldInclude = allExcludes;

          for (String strFilter : filters) {
            if (strFilter.startsWith("!")) {
              if (Objects.equals(fieldValue, strFilter.substring(1))) {
                shouldInclude = false;
              }
            }
            else if (Objects.equals(fieldValue, strFilter)) {
              shouldInclude = true;
            }
          }
          return shouldInclude;
        })
        .collect(Collectors.toList());
  }

  public void addRecipe(final String format, final Recipe recipe) {
    recipes.put(format, recipe);
  }
}
