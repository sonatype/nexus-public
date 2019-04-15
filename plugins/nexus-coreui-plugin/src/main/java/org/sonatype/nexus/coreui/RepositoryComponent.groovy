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
package org.sonatype.nexus.coreui

import java.util.stream.Collectors

import javax.annotation.Nullable
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton
import javax.validation.Valid
import javax.validation.constraints.NotNull
import javax.validation.groups.Default

import org.sonatype.nexus.common.app.BaseUrlHolder
import org.sonatype.nexus.common.app.GlobalComponentLookupHelper
import org.sonatype.nexus.common.entity.DetachedEntityId
import org.sonatype.nexus.coreui.internal.search.BrowseableFormatXO
import org.sonatype.nexus.extdirect.DirectComponent
import org.sonatype.nexus.extdirect.DirectComponentSupport
import org.sonatype.nexus.extdirect.model.StoreLoadParameters
import org.sonatype.nexus.rapture.PasswordPlaceholder
import org.sonatype.nexus.rapture.StateContributor
import org.sonatype.nexus.repository.Format
import org.sonatype.nexus.repository.MissingFacetException
import org.sonatype.nexus.repository.Recipe
import org.sonatype.nexus.repository.Repository
import org.sonatype.nexus.repository.cache.RepositoryCacheUtils
import org.sonatype.nexus.repository.config.Configuration
import org.sonatype.nexus.repository.httpclient.HttpClientFacet
import org.sonatype.nexus.repository.manager.RepositoryManager
import org.sonatype.nexus.repository.search.RebuildIndexTask
import org.sonatype.nexus.repository.search.RebuildIndexTaskDescriptor
import org.sonatype.nexus.repository.security.RepositoryAdminPermission
import org.sonatype.nexus.repository.security.RepositoryPermissionChecker
import org.sonatype.nexus.repository.security.RepositorySelector
import org.sonatype.nexus.repository.types.ProxyType
import org.sonatype.nexus.scheduling.TaskConfiguration
import org.sonatype.nexus.scheduling.TaskInfo
import org.sonatype.nexus.scheduling.TaskScheduler
import org.sonatype.nexus.security.BreadActions
import org.sonatype.nexus.security.SecurityHelper
import org.sonatype.nexus.validation.Validate
import org.sonatype.nexus.validation.group.Create
import org.sonatype.nexus.validation.group.Update

import com.codahale.metrics.annotation.ExceptionMetered
import com.codahale.metrics.annotation.Timed
import com.softwarementors.extjs.djn.config.annotations.DirectAction
import com.softwarementors.extjs.djn.config.annotations.DirectMethod
import com.softwarementors.extjs.djn.config.annotations.DirectPollMethod
import groovy.transform.PackageScope
import org.apache.commons.lang3.StringUtils
import org.apache.shiro.authz.annotation.RequiresAuthentication
import org.hibernate.validator.constraints.NotEmpty

/**
 * Repository {@link DirectComponent}.
 *
 * @since 3.0
 */
@Named
@Singleton
@DirectAction(action = 'coreui_Repository')
class RepositoryComponent
    extends DirectComponentSupport
    implements StateContributor
{
  @Inject
  RepositoryManager repositoryManager

  @Inject
  SecurityHelper securityHelper

  @Inject
  Map<String, Recipe> recipes

  @Inject
  TaskScheduler taskScheduler

  @Inject
  GlobalComponentLookupHelper typeLookup

  @Inject
  ProxyType proxyType

  @Inject
  List<Format> formats

  @Inject
  RepositoryPermissionChecker repositoryPermissionChecker

  @DirectMethod
  @Timed
  @ExceptionMetered
  List<RepositoryXO> read() {
    browse().collect { asRepository(it) }
  }

  @DirectMethod
  @Timed
  @ExceptionMetered
  List<ReferenceXO> readRecipes() {
    recipes.findAll { key, value -> value.isFeatureEnabled() }
        .collect { key, value ->
      new ReferenceXO(
          id: key,
          name: "${value.format} (${value.type})"
      )
    }
  }

  @DirectMethod
  @Timed
  @ExceptionMetered
  List<Format> readFormats() {
   return formats
  }

  @DirectMethod
  @Timed
  @ExceptionMetered
  List<BrowseableFormatXO> getBrowseableFormats() {
    Collection<Repository> browseableRepositories = repositoryPermissionChecker.
        userCanBrowseRepositories(repositoryManager.browse())


    Set<String> repoIds = browseableRepositories.stream().map { repository ->
      repository.format.value
    }.collect(Collectors.toSet())

    return repoIds.collect { id -> new BrowseableFormatXO(id: id) }
  }

  @Override
  Map<String, Object> getState() {
    return ['browseableformats': getBrowseableFormats()]
  }

  /**
   * Retrieve a list of available repositories references.
   */
  @DirectMethod
  @Timed
  @ExceptionMetered
  List<RepositoryReferenceXO> readReferences(final @Nullable StoreLoadParameters parameters) {
    return filter(parameters).collect { Repository repository ->
      new RepositoryReferenceXO(
          id: repository.name,
          name: repository.name,
          type: repository.type.toString(),
          format: repository.format.toString(),
          versionPolicy: repository.configuration.attributes.maven?.versionPolicy,
          status: buildStatus(repository),
          url: "${BaseUrlHolder.get()}/repository/${repository.name}/" // trailing slash is important
      )
    }
  }

  /**
   * Retrieve a list of available repositories references + add an entry for all repositories '*".
   */
  @DirectMethod
  @Timed
  @ExceptionMetered
  List<RepositoryReferenceXO> readReferencesAddingEntryForAll(final @Nullable StoreLoadParameters parameters) {
    def references = readReferences(parameters)
    references <<
        new RepositoryReferenceXO(id: RepositorySelector.all().toSelector(), name: '(All Repositories)', sortOrder: 1)
    return references
  }

  /**
   * Retrieve a list of available repositories references + add an entry for all repositories '*' and an entry for
   * format 'All (format) repositories' '*(format)'".
   */
  @DirectMethod
  @Timed
  @ExceptionMetered
  List<RepositoryReferenceXO> readReferencesAddingEntriesForAllFormats(
      final @Nullable StoreLoadParameters parameters)
  {
    def references = readReferencesAddingEntryForAll(parameters)
    formats.each {
      references << new RepositoryReferenceXO(id: RepositorySelector.allOfFormat(it.value).toSelector(),
          name: '(All ' + it.value + ' Repositories)')
    }
    return references
  }

  @DirectMethod
  @Timed
  @ExceptionMetered
  @RequiresAuthentication
  @Validate(groups = [Create.class, Default.class])
  RepositoryXO create(final @NotNull @Valid RepositoryXO repositoryXO) {
    securityHelper.ensurePermitted(
        new RepositoryAdminPermission(repositoryXO.format, repositoryXO.name, [BreadActions.ADD])
    )
    return asRepository(repositoryManager.create(new Configuration(
        repositoryName: repositoryXO.name,
        recipeName: repositoryXO.recipe,
        online: repositoryXO.online,
        routingRuleId: StringUtils.isNotBlank(repositoryXO.routingRuleId) ? new DetachedEntityId(
            repositoryXO.routingRuleId) : null,
        attributes: repositoryXO.attributes
    )))
  }

  @DirectMethod
  @Timed
  @ExceptionMetered
  @RequiresAuthentication
  @Validate(groups = [Update.class, Default.class])
  RepositoryXO update(final @NotNull @Valid RepositoryXO repositoryXO) {
    Repository repository = repositoryManager.get(repositoryXO.name)
    securityHelper.ensurePermitted(adminPermission(repository, BreadActions.EDIT))

    if (PasswordPlaceholder.is(repositoryXO?.attributes?.httpclient?.authentication?.password)) {
      //Did not update the password, just use the password we already have
      repositoryXO.attributes.httpclient.authentication.password =
          repository.configuration.attributes?.httpclient?.authentication?.password
    }

    Configuration updatedConfiguration = repository.configuration.copy().with {
      online = repositoryXO.online
      routingRuleId = StringUtils.isNotBlank(repositoryXO.routingRuleId) ? new DetachedEntityId(
          repositoryXO.routingRuleId) : null
      attributes = repositoryXO.attributes
      return it
    }
    return asRepository(repositoryManager.update(updatedConfiguration))
  }

  @DirectMethod
  @Timed
  @ExceptionMetered
  @RequiresAuthentication
  @Validate
  void remove(final @NotEmpty String name) {
    Repository repository = repositoryManager.get(name)
    securityHelper.ensurePermitted(adminPermission(repository, BreadActions.DELETE))
    repositoryManager.delete(name)
  }

  @DirectMethod
  @Timed
  @ExceptionMetered
  @RequiresAuthentication
  @Validate
  String rebuildIndex(final @NotEmpty String name) {
    Repository repository = repositoryManager.get(name)
    securityHelper.ensurePermitted(adminPermission(repository, BreadActions.EDIT))
    TaskConfiguration taskConfiguration = taskScheduler.createTaskConfigurationInstance(
        RebuildIndexTaskDescriptor.TYPE_ID
    )
    taskConfiguration.setString(RebuildIndexTask.REPOSITORY_NAME_FIELD_ID, repository.name)
    TaskInfo taskInfo = taskScheduler.submit(taskConfiguration)
    return taskInfo.id
  }

  @DirectMethod
  @Timed
  @ExceptionMetered
  @RequiresAuthentication
  @Validate
  void invalidateCache(final @NotEmpty String name) {
    Repository repository = repositoryManager.get(name)
    securityHelper.ensurePermitted(adminPermission(repository, BreadActions.EDIT))
    RepositoryCacheUtils.invalidateCaches(repository)
  }

  RepositoryXO asRepository(Repository input) {
    return new RepositoryXO(
        name: input.name,
        type: input.type,
        format: input.format,
        online: input.configuration.online,
        recipe: input.configuration.recipeName,
        status: buildStatus(input),
        routingRuleId: StringUtils.
            isNotBlank(input.configuration?.routingRuleId?.value) ? input.configuration.routingRuleId.value : '',
        attributes: filterAttributes(input.configuration.copy().attributes),
        url: "${BaseUrlHolder.get()}/repository/${input.name}/" // trailing slash is important
    )
  }

  Map<String, Map<String, Object>> filterAttributes(Map<String, Map<String, Object>> attributes) {
    attributes?.httpclient?.authentication?.password = PasswordPlaceholder.get()
    return attributes
  }

  @Timed
  @ExceptionMetered
  @DirectPollMethod(event = "coreui_Repository_readStatus")
  @RequiresAuthentication
  List<RepositoryStatusXO> readStatus(final Map<String, String> params) {
    browse().collect { Repository repository -> buildStatus(repository) }
  }

  RepositoryStatusXO buildStatus(Repository repository) {
    RepositoryStatusXO statusXO = new RepositoryStatusXO(
        repositoryName: repository.name,
        online: repository.configuration.online
    )

    //TODO - should we try to aggregate status from group members?
    if (proxyType == repository.type) {
      try {
        def remoteStatus = repository.facet(HttpClientFacet).status
        statusXO.description = remoteStatus.description
        if (remoteStatus.reason) {
          statusXO.reason = remoteStatus.reason
        }
      }
      catch (MissingFacetException e) {
        // no http client facet (usually on proxies), no remote status
      }
    }
    return statusXO
  }

  @PackageScope
  Iterable<Repository> filter(final @Nullable StoreLoadParameters parameters) {
    def repositories = repositoryManager.browse()
    if (parameters) {
      String format = parameters.getFilter('format')
      if (format && format.indexOf(",") > -1) {
        def formats = format.split(",")
        repositories = repositories.findResults { Repository repository ->
          if (formats.contains(repository.format.value)) {
            return repository
          }
        }
      }
      else {
        repositories = filterIn(repositories, format, { Repository repository ->
            repository.format.value
          })
      }

      String type = parameters.getFilter('type')
      repositories = filterIn(repositories, type, { Repository repository ->
        repository.type.value
      })
      String facets = parameters.getFilter('facets')
      if (facets) {
        def facetTypes = facets.tokenize(',').collect { typeLookup.type(it) }.findAll()
        repositories = repositories.findResults { Repository repository ->
          for (Class<?> facetType : facetTypes) {
            try {
              repository.facet(facetType)
              return repository
            }
            catch (MissingFacetException ignored) {
              // facet not present, skip it
            }
          }
          return null
        }
      }
      String versionPolicies = parameters.getFilter('versionPolicies')
      repositories = filterIn(repositories, versionPolicies, { Repository repository ->
        (String) repository?.configuration?.attributes?.maven?.versionPolicy
      })
    }

    repositories = repositoryPermissionChecker.userCanBrowseRepositories(repositories)

    return repositories
  }

  Iterable<Repository> browse() {
    return repositoryPermissionChecker.userHasRepositoryAdminPermission(repositoryManager.browse(), BreadActions.READ)
  }

  RepositoryAdminPermission adminPermission(final Repository repository, final String action) {
    return new RepositoryAdminPermission(repository.format.value, repository.name, [action])
  }

  /**
   * Filters a collection by evaluating if the field dictated by filteredFieldSelector is in the list of comma
   * separated values in filter and is not in the list of comma separated values in filter that are prepended by '!'.
   * NOTE: A list of only excludes will include all other items.
   * Used to parse the filters build by {@link org.sonatype.nexus.formfields.RepositoryCombobox#getStoreFilters()}
   *
   * @param iterable The iterable to filter
   * @param filter A comma separated list of values which either match the selected field or, if prepended with '!', do
   * not match the field
   * @param filteredFieldSelector A selector for the field to match against the supplied filter list
   * @return The filtered iterable
   */
  private static <U> Collection<U> filterIn(Iterable<U> iterable, String filter, Closure<String> filteredFieldSelector) {
    if (!filter) {
      return iterable
    }
    List<String> filters = filter.split(',')

    // If the filters are only exclude type, the default behavior should be to include the other items
    def allExcludes = filters.every { String strFilter ->
      return strFilter.startsWith('!')
    }

    return iterable.findResults { U result ->
      def shouldInclude = allExcludes
      def fieldValue = filteredFieldSelector(result)
      for (String strFilter in filters) {
        if (strFilter.startsWith('!')) {
          if (fieldValue == strFilter.substring(1)) {
            shouldInclude = false
          }
        }
        else if (fieldValue == strFilter) {
          shouldInclude = true
        }
      }
      return shouldInclude ? result : null
    }
  }
}
