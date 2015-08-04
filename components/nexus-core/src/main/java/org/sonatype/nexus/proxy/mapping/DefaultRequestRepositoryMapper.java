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
package org.sonatype.nexus.proxy.mapping;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.regex.Pattern;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.configuration.ConfigurationException;
import org.sonatype.configuration.validation.InvalidConfigurationException;
import org.sonatype.configuration.validation.ValidationResponse;
import org.sonatype.nexus.configuration.AbstractLastingConfigurable;
import org.sonatype.nexus.configuration.CoreConfiguration;
import org.sonatype.nexus.configuration.application.ApplicationConfiguration;
import org.sonatype.nexus.configuration.model.CPathMappingItem;
import org.sonatype.nexus.configuration.model.CRepositoryGrouping;
import org.sonatype.nexus.configuration.model.CRepositoryGroupingCoreConfiguration;
import org.sonatype.nexus.configuration.validator.ApplicationConfigurationValidator;
import org.sonatype.nexus.proxy.NoSuchRepositoryException;
import org.sonatype.nexus.proxy.ResourceStore;
import org.sonatype.nexus.proxy.ResourceStoreRequest;
import org.sonatype.nexus.proxy.events.RepositoryRegistryEventRemove;
import org.sonatype.nexus.proxy.mapping.RepositoryPathMapping.MappingType;
import org.sonatype.nexus.proxy.registry.RepositoryRegistry;
import org.sonatype.nexus.proxy.repository.Repository;
import org.sonatype.sisu.goodies.eventbus.EventBus;

import com.google.common.eventbus.Subscribe;
import org.codehaus.plexus.util.StringUtils;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * The Class PathBasedRequestRepositoryMapper filters repositories to search using supplied list of filter expressions.
 * It is parametrized by java,util.Map, the contents: </p> <tt>
 * regexp1=repo1,repo2...
 * regexp2=repo3,repo4...
 * ...
 * </tt>
 * <p>
 * An example (with grouped Router and two repositories, one for central and one for inhouse in same group):
 * </p>
 * <tt>
 * /com/company/=inhouse
 * /org/apache/=central
 * </tt>
 *
 * @author cstamas
 */
@Singleton
@Named
public class DefaultRequestRepositoryMapper
    extends AbstractLastingConfigurable<CRepositoryGrouping>
    implements RequestRepositoryMapper
{
  private final RepositoryRegistry repositoryRegistry;

  private final ApplicationConfigurationValidator validator;

  /**
   * The compiled flag.
   */
  private volatile boolean compiled = false;

  private volatile List<RepositoryPathMapping> blockings = new CopyOnWriteArrayList<RepositoryPathMapping>();

  private volatile List<RepositoryPathMapping> inclusions = new CopyOnWriteArrayList<RepositoryPathMapping>();

  private volatile List<RepositoryPathMapping> exclusions = new CopyOnWriteArrayList<RepositoryPathMapping>();

  @Inject
  public DefaultRequestRepositoryMapper(EventBus eventBus, ApplicationConfiguration applicationConfiguration,
      RepositoryRegistry repositoryRegistry, ApplicationConfigurationValidator validator)
  {
    super("Repository Grouping Configuration", eventBus, applicationConfiguration);
    this.repositoryRegistry = checkNotNull(repositoryRegistry);
    this.validator = checkNotNull(validator);
  }

  // ==

  @Override
  protected void initializeConfiguration()
      throws ConfigurationException
  {
    if (getApplicationConfiguration().getConfigurationModel() != null) {
      configure(getApplicationConfiguration());
    }
  }

  @Override
  protected CoreConfiguration<CRepositoryGrouping> wrapConfiguration(Object configuration)
      throws ConfigurationException
  {
    if (configuration instanceof ApplicationConfiguration) {
      return new CRepositoryGroupingCoreConfiguration((ApplicationConfiguration) configuration);
    }
    else {
      throw new ConfigurationException("The passed configuration object is of class \""
          + configuration.getClass().getName() + "\" and not the required \""
          + ApplicationConfiguration.class.getName() + "\"!");
    }
  }

  @Override
  public boolean commitChanges()
      throws ConfigurationException
  {
    boolean wasDirty = super.commitChanges();
    if (wasDirty) {
      compiled = false;
    }
    return wasDirty;
  }

  // ==

  @Override
  public List<Repository> getMappedRepositories(Repository repository, ResourceStoreRequest request,
                                                List<Repository> resolvedRepositories)
      throws NoSuchRepositoryException
  {
    if (!compiled) {
      compile();
    }

    // NEXUS-2852: to make our life easier, we will work with repository IDs,
    // and will fill the result with Repositories at the end
    LinkedHashSet<String> reposIdSet = new LinkedHashSet<String>(resolvedRepositories.size());

    for (Repository resolvedRepositorty : resolvedRepositories) {
      reposIdSet.add(resolvedRepositorty.getId());
    }

    // if include found, add it to the list.
    boolean firstAdd = true;

    for (RepositoryPathMapping mapping : blockings) {
      if (mapping.matches(repository, request)) {
        if (log.isDebugEnabled()) {
          log.debug(
              "The request path [" + request.toString() + "] is blocked by rule " + mapping.toString());
        }

        request.addAppliedMappingsList(repository, Collections.singletonList(mapping.toString()));

        return Collections.emptyList();
      }
    }

    // for tracking what is applied
    ArrayList<RepositoryPathMapping> appliedMappings = new ArrayList<RepositoryPathMapping>();

    // include, if found a match
    // NEXUS-2852: watch to not add multiple times same repository
    // ie. you have different inclusive rules that are triggered by same request
    // and contains some repositories. This is now solved using LinkedHashSet and using repo IDs.
    for (RepositoryPathMapping mapping : inclusions) {
      if (mapping.matches(repository, request)) {
        appliedMappings.add(mapping);

        if (firstAdd) {
          reposIdSet.clear();

          firstAdd = false;
        }

        // add only those that are in initial resolvedRepositories list and that are non-user managed
        // (preserve ordering)
        if (mapping.getMappedRepositories().size() == 1
            && "*".equals(mapping.getMappedRepositories().get(0))) {
          for (Repository repo : resolvedRepositories) {
            reposIdSet.add(repo.getId());
          }
        }
        else {
          for (Repository repo : resolvedRepositories) {
            if (mapping.getMappedRepositories().contains(repo.getId()) || !repo.isUserManaged()) {
              reposIdSet.add(repo.getId());
            }
          }
        }
      }
    }

    // then, if exlude found, remove those
    for (RepositoryPathMapping mapping : exclusions) {
      if (mapping.matches(repository, request)) {
        appliedMappings.add(mapping);

        if (mapping.getMappedRepositories().size() == 1
            && "*".equals(mapping.getMappedRepositories().get(0))) {
          reposIdSet.clear();

          break;
        }

        for (String repositoryId : mapping.getMappedRepositories()) {
          Repository mappedRepository = repositoryRegistry.getRepository(repositoryId);

          // but only if is user managed
          if (mappedRepository.isUserManaged()) {
            reposIdSet.remove(mappedRepository.getId());
          }
        }
      }
    }

    // store the applied mappings to request context
    ArrayList<String> appliedMappingsList = new ArrayList<String>(appliedMappings.size());

    for (RepositoryPathMapping mapping : appliedMappings) {
      appliedMappingsList.add(mapping.toString());
    }

    request.addAppliedMappingsList(repository, appliedMappingsList);

    // log it if needed
    if (log.isDebugEnabled()) {
      if (appliedMappings.isEmpty()) {
        log.debug("No mapping exists for request path [" + request.toString() + "]");
      }
      else {
        StringBuilder sb =
            new StringBuilder("Request for path \"" + request.toString()
                + "\" with the initial list of processable repositories of \""
                + getResourceStoreListAsString(resolvedRepositories)
                + "\" got these mappings applied:\n");

        for (RepositoryPathMapping mapping : appliedMappings) {
          sb.append(" * ").append(mapping.toString()).append("\n");
        }

        log.debug(sb.toString());

        if (reposIdSet.size() == 0) {
          log.debug(
              "Mapping for path [" + request.toString()
                  + "] excluded all storages from servicing the request.");
        }
        else {
          log.debug(
              "Request path for [" + request.toString() + "] is MAPPED to reposes: " + reposIdSet);
        }
      }
    }

    ArrayList<Repository> result = new ArrayList<Repository>(reposIdSet.size());

    try {
      for (String repoId : reposIdSet) {
        result.add(repositoryRegistry.getRepository(repoId));
      }
    }
    catch (NoSuchRepositoryException e) {
      log.error(
          "Some of the Routes contains references to non-existant repositories! Please check the following mappings: \""
              + appliedMappingsList.toString() + "\".");

      throw e;
    }

    return result;
  }

  public String getResourceStoreListAsString(List<? extends ResourceStore> stores) {
    if (stores == null) {
      return "[]";
    }
    ArrayList<String> repoIdList = new ArrayList<String>(stores.size());

    for (ResourceStore store : stores) {
      if (store instanceof Repository) {
        repoIdList.add(((Repository) store).getId());
      }
      else {
        repoIdList.add(store.getClass().getName());
      }
    }

    return StringUtils.join(repoIdList.iterator(), ", ");
  }

  // ==

  protected synchronized void compile()
      throws NoSuchRepositoryException
  {
    if (compiled) {
      return;
    }

    blockings.clear();

    inclusions.clear();

    exclusions.clear();

    if (getCurrentConfiguration(false) == null) {
      if (log.isDebugEnabled()) {
        log.debug("No Routes defined, have nothing to compile.");
      }

      return;
    }

    List<CPathMappingItem> pathMappings = getCurrentConfiguration(false).getPathMappings();

    for (CPathMappingItem item : pathMappings) {
      if (CPathMappingItem.BLOCKING_RULE_TYPE.equals(item.getRouteType())) {
        blockings.add(convert(item));
      }
      else if (CPathMappingItem.INCLUSION_RULE_TYPE.equals(item.getRouteType())) {
        inclusions.add(convert(item));
      }
      else if (CPathMappingItem.EXCLUSION_RULE_TYPE.equals(item.getRouteType())) {
        exclusions.add(convert(item));
      }
      else {
        log.warn("Unknown route type: " + item.getRouteType());

        throw new IllegalArgumentException("Unknown route type: " + item.getRouteType());
      }
    }

    compiled = true;
  }

  protected RepositoryPathMapping convert(CPathMappingItem item)
      throws IllegalArgumentException
  {
    MappingType type = null;

    if (CPathMappingItem.BLOCKING_RULE_TYPE.equals(item.getRouteType())) {
      type = MappingType.BLOCKING;
    }
    else if (CPathMappingItem.INCLUSION_RULE_TYPE.equals(item.getRouteType())) {
      type = MappingType.INCLUSION;
    }
    else if (CPathMappingItem.EXCLUSION_RULE_TYPE.equals(item.getRouteType())) {
      type = MappingType.EXCLUSION;
    }
    else {
      log.warn("Unknown route type: " + item.getRouteType());

      throw new IllegalArgumentException("Unknown route type: " + item.getRouteType());
    }

    return new RepositoryPathMapping(item.getId(), type, item.getGroupId(), item.getRoutePatterns(),
        item.getRepositories());
  }

  protected CPathMappingItem convert(RepositoryPathMapping item) {
    String routeType = null;

    if (MappingType.BLOCKING.equals(item.getMappingType())) {
      routeType = CPathMappingItem.BLOCKING_RULE_TYPE;
    }
    else if (MappingType.INCLUSION.equals(item.getMappingType())) {
      routeType = CPathMappingItem.INCLUSION_RULE_TYPE;
    }
    else if (MappingType.EXCLUSION.equals(item.getMappingType())) {
      routeType = CPathMappingItem.EXCLUSION_RULE_TYPE;
    }

    CPathMappingItem result = new CPathMappingItem();
    result.setId(item.getId());
    result.setGroupId(item.getGroupId());
    result.setRepositories(item.getMappedRepositories());
    result.setRouteType(routeType);
    ArrayList<String> patterns = new ArrayList<String>(item.getPatterns().size());
    for (Pattern pattern : item.getPatterns()) {
      patterns.add(pattern.toString());
    }
    result.setRoutePatterns(patterns);
    return result;
  }

  // ==

  @Override
  public boolean addMapping(RepositoryPathMapping mapping)
      throws ConfigurationException
  {
    removeMapping(mapping.getId());

    CPathMappingItem pathItem = convert(mapping);

    // validate
    this.validate(pathItem);

    getCurrentConfiguration(true).addPathMapping(convert(mapping));

    return true;
  }

  protected void validate(CPathMappingItem pathItem)
      throws InvalidConfigurationException
  {
    ValidationResponse response = this.validator.validateGroupsSettingPathMappingItem(null, pathItem);
    if (!response.isValid()) {
      throw new InvalidConfigurationException(response);
    }
  }

  @Override
  public boolean removeMapping(String id) {
    for (Iterator<CPathMappingItem> i = getCurrentConfiguration(true).getPathMappings().iterator(); i.hasNext(); ) {
      CPathMappingItem mapping = i.next();

      if (mapping.getId().equals(id)) {
        i.remove();

        return true;
      }
    }

    return false;
  }

  @Override
  public Map<String, RepositoryPathMapping> getMappings() {
    final HashMap<String, RepositoryPathMapping> result = new HashMap<String, RepositoryPathMapping>();

    final CRepositoryGrouping config = getCurrentConfiguration(false);

    if (config != null) {
      List<CPathMappingItem> items = config.getPathMappings();

      for (CPathMappingItem item : items) {
        RepositoryPathMapping mapping = convert(item);

        result.put(mapping.getId(), mapping);
      }
    }

    return Collections.unmodifiableMap(result);
  }

  @Subscribe
  public void onEvent(final RepositoryRegistryEventRemove evt) {
    final String repoId = evt.getRepository().getId();

    List<CPathMappingItem> pathMappings = getCurrentConfiguration(true).getPathMappings();

    for (Iterator<CPathMappingItem> iterator = pathMappings.iterator(); iterator.hasNext(); ) {
      CPathMappingItem item = iterator.next();

      if (item.getGroupId().equals(repoId)) {
        iterator.remove();
      }
      else {
        item.removeRepository(repoId);
      }
    }
  }

}
