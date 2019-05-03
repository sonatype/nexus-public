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
package org.sonatype.nexus.cleanup.storage;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.BiFunction;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.groups.Default;

import org.sonatype.nexus.cleanup.config.CleanupPolicyConfiguration;
import org.sonatype.nexus.cleanup.service.CleanupComponentBrowse;
import org.sonatype.nexus.extdirect.DirectComponent;
import org.sonatype.nexus.extdirect.DirectComponentSupport;
import org.sonatype.nexus.extdirect.model.PagedResponse;
import org.sonatype.nexus.extdirect.model.StoreLoadParameters;
import org.sonatype.nexus.extdirect.model.StoreLoadParameters.Sort;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.browse.QueryOptions;
import org.sonatype.nexus.repository.manager.RepositoryManager;
import org.sonatype.nexus.repository.rest.api.ComponentXO;
import org.sonatype.nexus.repository.rest.api.DefaultComponentXO;
import org.sonatype.nexus.repository.security.RepositoryAdminPermission;
import org.sonatype.nexus.repository.security.RepositoryPermissionChecker;
import org.sonatype.nexus.repository.storage.Component;
import org.sonatype.nexus.repository.storage.StorageFacet;
import org.sonatype.nexus.transaction.Transactional;
import org.sonatype.nexus.transaction.UnitOfWork;
import org.sonatype.nexus.validation.Validate;
import org.sonatype.nexus.validation.group.Create;
import org.sonatype.nexus.validation.group.Update;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import com.softwarementors.extjs.djn.config.annotations.DirectAction;
import com.softwarementors.extjs.djn.config.annotations.DirectMethod;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.hibernate.validator.constraints.NotEmpty;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.lang.Integer.MAX_VALUE;
import static java.util.Collections.singletonList;
import static java.util.Optional.ofNullable;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.Stream.concat;
import static org.sonatype.nexus.cleanup.storage.CleanupPolicy.NONE_POLICY;
import static org.sonatype.nexus.cleanup.storage.CleanupPolicyXO.fromCleanupPolicy;
import static org.sonatype.nexus.cleanup.storage.CleanupPolicyXO.mergeIntoCleanupPolicy;
import static org.sonatype.nexus.cleanup.storage.CleanupPolicyXO.toCleanupPolicy;
import static org.sonatype.nexus.security.BreadActions.ADD;
import static org.sonatype.nexus.security.BreadActions.READ;

/**
 * Cleanup policies {@link DirectComponent}.
 *
 * @since 3.14
 */
@Named
@Singleton
@DirectAction(action = "cleanup_CleanupPolicy")
public class CleanupPolicyComponent
    extends DirectComponentSupport
{
  private static final BiFunction<Component, String, ComponentXO> COMPONENT_CONVERTER = (component, repositoryName) ->
  {
    DefaultComponentXO defaultComponentXO = new DefaultComponentXO();
    defaultComponentXO.setRepository(repositoryName);
    defaultComponentXO.setGroup(component.group());
    defaultComponentXO.setName(component.name());
    defaultComponentXO.setVersion(component.version());
    defaultComponentXO.setFormat(component.format());
    return defaultComponentXO;
  };

  private final ObjectMapper mapper = new ObjectMapper();

  private static final List<CleanupPolicyXO> NONE_POLICY_LIST =
      singletonList(fromCleanupPolicy(NONE_POLICY, MAX_VALUE));

  private final CleanupPolicyStorage cleanupPolicyStorage;

  private final CleanupComponentBrowse cleanupComponentBrowse;

  private final RepositoryManager repositoryManager;

  private final Map<String, CleanupPolicyConfiguration> cleanupPolicyConfiguration;

  private final CleanupPolicyConfiguration defaultCleanupPolicyConfiguration;

  private final RepositoryPermissionChecker repositoryPermissionChecker;

  @Inject
  public CleanupPolicyComponent(final CleanupPolicyStorage cleanupPolicyStorage,
                                final CleanupComponentBrowse cleanupComponentBrowse,
                                final RepositoryManager repositoryManager,
                                final Map<String, CleanupPolicyConfiguration> cleanupPolicyConfiguration,
                                final RepositoryPermissionChecker repositoryPermissionChecker)
  {
    this.cleanupPolicyStorage = checkNotNull(cleanupPolicyStorage);
    this.cleanupComponentBrowse = checkNotNull(cleanupComponentBrowse);
    this.repositoryManager = checkNotNull(repositoryManager);
    this.cleanupPolicyConfiguration = checkNotNull(cleanupPolicyConfiguration);
    this.defaultCleanupPolicyConfiguration = checkNotNull(cleanupPolicyConfiguration.get("default"));
    this.repositoryPermissionChecker = repositoryPermissionChecker;
  }

  /**
   * Retrieve {@link CleanupPolicy}s by format + add the {@link CleanupPolicy#NONE_POLICY} policy as the first
   * option for all repositories.
   *
   * @return a list of {@link CleanupPolicy}s
   */
  @DirectMethod
  @Timed
  @ExceptionMetered
  public List<CleanupPolicyXO> readByFormat(final StoreLoadParameters parameters) {
    return ofNullable(parameters.getFilter("format"))
        .map(format -> {
          ensureUserHasPermissionToCleanupPolicyByFormat(format);
          return format;
        })
        .map(this::getAllByFormat)
        .map(this::prependNonePolicy)
        .orElse(NONE_POLICY_LIST);
  }

  /**
   * Retrieve all {@link CleanupPolicy}s
   *
   * @return a list of {@link CleanupPolicy}s
   */
  @DirectMethod
  @Timed
  @ExceptionMetered
  @RequiresPermissions("nexus:*")
  public List<CleanupPolicyXO> readAll() {
    return cleanupPolicyStorage.getAll().stream().map(CleanupPolicyXO::fromCleanupPolicy).collect(toList());
  }

  /**
   * Create {@link CleanupPolicy} from a {@link CleanupPolicyXO} and store it.
   *
   * @param cleanupPolicyXO - {@link CleanupPolicyXO}
   * @return CleanupPolicyXO from created {@link CleanupPolicy}
   */
  @DirectMethod
  @Timed
  @ExceptionMetered
  @RequiresAuthentication
  @RequiresPermissions("nexus:*")
  @Validate(groups = {Create.class, Default.class})
  public CleanupPolicyXO create(@NotNull @Valid final CleanupPolicyXO cleanupPolicyXO) {
    return fromCleanupPolicy(cleanupPolicyStorage.add(toCleanupPolicy(cleanupPolicyXO)));
  }

  /**
   * Update an existing {@link CleanupPolicy} from a {@link CleanupPolicyXO}.
   *
   * @param cleanupPolicyXO - {@link CleanupPolicyXO}
   * @return CleanupPolicyXO from updated {@link CleanupPolicy}
   */
  @DirectMethod
  @Timed
  @ExceptionMetered
  @RequiresAuthentication
  @RequiresPermissions("nexus:*")
  @Validate(groups = {Update.class, Default.class})
  public CleanupPolicyXO update(@NotNull @Valid final CleanupPolicyXO cleanupPolicyXO) {
    CleanupPolicy cleanupPolicy = cleanupPolicyStorage.get(cleanupPolicyXO.getName());
    return fromCleanupPolicy(cleanupPolicyStorage.update(mergeIntoCleanupPolicy(cleanupPolicyXO, cleanupPolicy)));
  }

  /**
   * Remove an existing {@link CleanupPolicy}.
   *
   * @param name - Unique name opf a {@link CleanupPolicy}
   */
  @DirectMethod
  @Timed
  @ExceptionMetered
  @RequiresAuthentication
  @RequiresPermissions("nexus:*")
  @Validate
  public void remove(final @NotEmpty String name) {
    cleanupPolicyStorage.remove(cleanupPolicyStorage.get(name));
  }

  /**
   * Load configuration to check whether a field should be enabled for a given format
   */
  @DirectMethod
  @Timed
  @ExceptionMetered
  @RequiresAuthentication
  @RequiresPermissions("nexus:*")
  @Validate
  public Map<String, Map<String, Boolean>> getApplicableFields(final List<String> fields) {
    Map<String, Map<String, Boolean>> applicability = new HashMap<>();
    for (Entry<String, CleanupPolicyConfiguration> config : cleanupPolicyConfiguration.entrySet()) {
      String format = config.getKey();

      Map<String, Boolean> fieldApplicability = fields.stream()
          .collect(toMap(identity(), f -> isFieldApplicable(format, f)));

      applicability.put(format, fieldApplicability);
    }

    return applicability;
  }

  /**
   * Returns an object with usage counts. Currently counts the amount of repositories
   * that are used by a given Cleanup Policy name.
   */
  @DirectMethod
  @Timed
  @ExceptionMetered
  @RequiresAuthentication
  @Validate
  public Map<String, Object> usage(final String cleanupPolicyName) {
    return ImmutableMap.of("repositoryCount", repositoryCount(cleanupPolicyName));
  }

  private boolean isFieldApplicable(final @NotEmpty String format, final String field) {
    CleanupPolicyConfiguration config = cleanupPolicyConfiguration.get(format);

    if (!config.getConfiguration().containsKey(field)) {
      return defaultCleanupPolicyConfiguration.getConfiguration().get(field);
    }

    return config.getConfiguration().get(field);
  }


  /**
   * Preview components that would be deleted using this {@link CleanupPolicy}
   * @param parameters
   * @return paged results of {@link ComponentXO}
   * @throws IOException
   */
  @Nullable
  @DirectMethod
  @Timed
  @ExceptionMetered
  @RequiresAuthentication
  @RequiresPermissions("nexus:*")
  public PagedResponse<ComponentXO> previewCleanup(final StoreLoadParameters parameters) throws IOException {
    String policy = parameters.getFilter("cleanupPolicy");
    String filter = parameters.getFilter("filter");
    if(policy == null || parameters.getSort() == null) {
      return null;
    }

    Sort sort = parameters.getSort().get(0);
    QueryOptions queryOptions = new QueryOptions(filter,
        sort.getProperty(),
        sort.getDirection(),
        parameters.getStart(),
        parameters.getLimit());

    CleanupPolicyPreviewXO previewXO = mapper
        .readValue(policy, CleanupPolicyPreviewXO.class);

    Repository repository = repositoryManager.get(previewXO.getRepositoryName());
    if(repository == null) {
      return null;
    }

    return getSearchResults(previewXO, repository, queryOptions);
  }

  private List<CleanupPolicyXO> getAllByFormat(final String format) {
    return cleanupPolicyStorage.getAllByFormat(format).stream()
        .map(CleanupPolicyXO::fromCleanupPolicy)
        .collect(toList());
  }

  private List<CleanupPolicyXO> prependNonePolicy(final List<CleanupPolicyXO> policyList) {
    return concat(NONE_POLICY_LIST.stream(), policyList.stream()).collect(toList());
  }

  private PagedResponse<ComponentXO> getSearchResults(final CleanupPolicyPreviewXO previewXO,
                                                      final Repository repository,
                                                      final QueryOptions queryOptions)
  {
    CleanupPolicy cleanupPolicy = CleanupPolicyPreviewXO.toCleanupPolicy(previewXO);

    UnitOfWork.begin(repository.facet(StorageFacet.class).txSupplier());
    try {
      return searchForComponents(repository, cleanupPolicy, queryOptions);
    } finally {
      UnitOfWork.end();
    }
  }

  @Transactional
  protected PagedResponse<ComponentXO> searchForComponents(final Repository repository,
                                                           final CleanupPolicy cleanupPolicy,
                                                           final QueryOptions queryOptions)
  {
    PagedResponse<Component> components = cleanupComponentBrowse.browseByPage(cleanupPolicy, repository, queryOptions);

    List<ComponentXO> componentXOS = components.getData().stream()
        .map(item -> COMPONENT_CONVERTER.apply(item, repository.getName()))
        .collect(toList());

    return new PagedResponse<>(components.getTotal(), componentXOS);
  }

  private long repositoryCount(final String cleanupPolicyName) {
    return repositoryManager.browseForCleanupPolicy(cleanupPolicyName).count();
  }

  private void ensureUserHasPermissionToCleanupPolicyByFormat(final String format) {
    RepositoryAdminPermission permission = new RepositoryAdminPermission(format, "*", singletonList(ADD));
    repositoryPermissionChecker.ensureUserHasAnyPermissionOrAdminAccess(
        singletonList(permission),
        READ,
        repositoryManager.browse()
    );
  }
}
