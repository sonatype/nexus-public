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
import java.util.Collection;
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
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.groups.Default;

import org.sonatype.goodies.common.ComponentSupport;
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
import org.sonatype.nexus.repository.Facet;
import org.sonatype.nexus.repository.Format;
import org.sonatype.nexus.repository.MissingFacetException;
import org.sonatype.nexus.repository.Recipe;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.cache.RepositoryCacheInvalidationService;
import org.sonatype.nexus.repository.config.Configuration;
import org.sonatype.nexus.repository.config.ConfigurationStore;
import org.sonatype.nexus.repository.httpclient.HttpClientFacet;
import org.sonatype.nexus.repository.httpclient.RemoteConnectionStatus;
import org.sonatype.nexus.repository.manager.RepositoryManager;
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
import org.apache.commons.lang3.StringUtils;
import org.apache.shiro.authz.annotation.RequiresAuthentication;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.sonatype.nexus.coreui.internal.RepositoryCleanupAttributesUtil.initializeCleanupAttributes;

@Named
@Singleton
public class RepositoryUiService
    extends ComponentSupport
    implements StateContributor
{
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

  @Inject
  public RepositoryUiService(
      final RepositoryCacheInvalidationService repositoryCacheInvalidationService,
      final RepositoryManager repositoryManager,
      @Nullable final RepositoryMetricsService repositoryMetricsService,
      final ConfigurationStore configurationStore,
      final SecurityHelper securityHelper,
      final Map<String, Recipe> recipes,
      final TaskScheduler taskScheduler,
      final GlobalComponentLookupHelper typeLookup,
      final List<Format> formats,
      final RepositoryPermissionChecker repositoryPermissionChecker)
  {
    this.repositoryCacheInvalidationService = checkNotNull(repositoryCacheInvalidationService);
    this.repositoryManager = checkNotNull(repositoryManager);
    this.repositoryMetricsService = Optional.ofNullable(repositoryMetricsService);
    this.configurationStore = checkNotNull(configurationStore);
    this.securityHelper = checkNotNull(securityHelper);
    this.recipes = new HashMap<>(checkNotNull(recipes));
    this.taskScheduler = checkNotNull(taskScheduler);
    this.typeLookup = checkNotNull(typeLookup);
    this.formats = checkNotNull(formats);
    this.repositoryPermissionChecker = checkNotNull(repositoryPermissionChecker);
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
    Collection<Repository> browseableRepositories =
        repositoryPermissionChecker.userCanBrowseRepositories(repositoryManager.browse());

    return browseableRepositories.stream()
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
    Repository repository = repositoryManager.get(repositoryXO.getName());
    securityHelper.ensurePermitted(adminPermission(repository, BreadActions.EDIT));

    // Replace stored password
    Optional.of(repositoryXO)
        .map(RepositoryXO::getAttributes)
        .map(attr -> attr.get("httpclient"))
        .map(httpclient -> httpclient.get("authentication"))
        .map(Map.class::cast)
        .ifPresent(authentication -> {
          String password = (String) authentication.get("password");
          if (PasswordPlaceholder.is(password)) {
            Optional.of(repository)
                .map(Repository::getConfiguration)
                .map(Configuration::getAttributes)
                .map(attr -> attr.get("httpclient"))
                .map(Map.class::cast)
                .map(httpclient -> httpclient.get("authentication"))
                .map(Map.class::cast)
                .map(storedAuthentication -> storedAuthentication.get("password"))
                .ifPresent(storedPassword -> authentication.put("password", storedPassword));
          }
        });

    initializeCleanupAttributes(repositoryXO);

    Configuration updatedConfiguration = repository.getConfiguration().copy();
    updatedConfiguration.setOnline(repositoryXO.getOnline());
    updatedConfiguration.setRoutingRuleId(toDetachedEntityId(repositoryXO.getRoutingRuleId()));
    updatedConfiguration.setAttributes(repositoryXO.getAttributes());

    return asRepository(repositoryManager.update(updatedConfiguration));
  }

  private DetachedEntityId toDetachedEntityId(final String s) {
    return StringUtils.isBlank(s) ? null : new DetachedEntityId(s);
  }

  @RequiresAuthentication
  @Validate
  public void remove(final @NotEmpty String name) throws Exception {
    Repository repository = repositoryManager.get(name);
    securityHelper.ensurePermitted(adminPermission(repository, BreadActions.DELETE));
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
        .ifPresent(authentication -> authentication.put("password", PasswordPlaceholder.get()));
    return attributes;
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
    }
    return statusXO;
  }

  private RepositoryStatusXO buildStatus(final Configuration configuration) {
    RepositoryStatusXO statusXO = new RepositoryStatusXO();
    statusXO.setRepositoryName(configuration.getRepositoryName());
    statusXO.setOnline(configuration.isOnline());

    Recipe recipe = recipes.get(configuration.getRecipeName());
    // TODO - should we try to aggregate status from group members?
    if (recipe.getType() instanceof ProxyType) {
      try {
        boolean loaded = StreamSupport.stream(repositoryManager.browse().spliterator(), false)
            .anyMatch(repo -> configuration.getRepositoryName().equals(repo.getName()));
        if (loaded) {
          RemoteConnectionStatus remoteStatus = repositoryManager.get(configuration.getRepositoryName())
              .facet(HttpClientFacet.class)
              .getStatus();
          statusXO.setDescription(remoteStatus.getDescription());
          if (remoteStatus.getReason() != null) {
            statusXO.setReason(remoteStatus.getReason());
          }
        }
      }
      catch (MissingFacetException e) {
        // no http client facet (usually on proxies), no remote status
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
