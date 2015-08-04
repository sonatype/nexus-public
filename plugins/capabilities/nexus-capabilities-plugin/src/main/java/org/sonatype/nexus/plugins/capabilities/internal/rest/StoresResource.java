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
package org.sonatype.nexus.plugins.capabilities.internal.rest;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.UriInfo;

import org.sonatype.nexus.capabilities.model.SelectableEntryXO;
import org.sonatype.nexus.capability.CapabilitiesPlugin;
import org.sonatype.nexus.capability.spi.SelectableEntryProvider;
import org.sonatype.nexus.capability.spi.SelectableEntryProvider.Parameters;
import org.sonatype.nexus.formfields.RepositoryCombobox;
import org.sonatype.nexus.proxy.access.NexusItemAuthorizer;
import org.sonatype.nexus.proxy.registry.RepositoryRegistry;
import org.sonatype.nexus.proxy.repository.Repository;
import org.sonatype.sisu.goodies.common.ComponentSupport;
import org.sonatype.sisu.goodies.i18n.I18N;
import org.sonatype.sisu.goodies.i18n.MessageBundle;
import org.sonatype.sisu.siesta.common.Resource;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.codehaus.plexus.util.StringUtils;

import static com.google.common.base.Preconditions.checkNotNull;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.MediaType.APPLICATION_XML;

/**
 * Stores REST resource.
 *
 * @since 2.7
 */
@Named
@Singleton
@Path(StoresResource.RESOURCE_URI)
public class StoresResource
    extends ComponentSupport
    implements Resource
{

  public static final String RESOURCE_URI = CapabilitiesPlugin.REST_PREFIX + "/stores";

  private static interface Messages
      extends MessageBundle
  {

    @DefaultMessage("(All Repositories)")
    String allRepositoriesName();

  }

  private static final Messages messages = I18N.create(Messages.class);

  private final Map<String, SelectableEntryProvider> namedProviders;

  private final RepositoryRegistry repositoryRegistry;

  private final NexusItemAuthorizer nexusItemAuthorizer;

  @Inject
  public StoresResource(final RepositoryRegistry repositoryRegistry,
                        final NexusItemAuthorizer nexusItemAuthorizer,
                        final Map<String, SelectableEntryProvider> namedProviders)
  {
    this.repositoryRegistry = checkNotNull(repositoryRegistry);
    this.nexusItemAuthorizer = checkNotNull(nexusItemAuthorizer);
    this.namedProviders = checkNotNull(namedProviders);
  }

  /**
   * Returns repositories filtered based on query parameters.
   */
  @GET
  @Path("/repositories")
  @Produces({APPLICATION_XML, APPLICATION_JSON})
  @RequiresPermissions("nexus:repositories:read")
  public List<SelectableEntryXO> getRepositories(
      final @QueryParam(RepositoryCombobox.ALL_REPOS_ENTRY) Boolean allReposEntry,
      final @QueryParam(RepositoryCombobox.REGARDLESS_VIEW_PERMISSIONS) Boolean regardlessViewPermissions,
      final @QueryParam(RepositoryCombobox.FACET) List<String> facets,
      final @QueryParam(RepositoryCombobox.CONTENT_CLASS) List<String> contentClasses)
  {
    final Predicate<Repository> predicate = Predicates.and(removeNulls(
        hasRightsToView(regardlessViewPermissions),
        hasAnyOfFacets(facets),
        hasNoneOfFacets(facets),
        hasAnyOfContentClasses(contentClasses),
        hasNoneOfContentClasses(contentClasses)
    ));

    List<SelectableEntryXO> entries = Lists.transform(
        Lists.newArrayList(Iterables.filter(
            repositoryRegistry.getRepositories(),
            new Predicate<Repository>()
            {
              @Override
              public boolean apply(@Nullable final Repository input) {
                return input != null && predicate.apply(input);
              }
            }
        )),
        new Function<Repository, SelectableEntryXO>()
        {
          @Override
          public SelectableEntryXO apply(final Repository input) {
            return new SelectableEntryXO().withId(input.getId()).withName(input.getName());
          }
        }
    );

    if (allReposEntry != null && allReposEntry) {
      entries = Lists.newArrayList(entries);
      entries.add(0, new SelectableEntryXO().withId("*").withName(messages.allRepositoriesName()));
    }

    return entries;
  }

  /**
   * Delegates retrieval of {@link SelectableEntryXO} to the named {@link SelectableEntryProvider}.
   */
  @GET
  @Path("/provider/{name}")
  @Produces({APPLICATION_XML, APPLICATION_JSON})
  public List<SelectableEntryXO> getFromProvider(final @PathParam("name") String name,
                                                 final @Context UriInfo uriInfo)
  {
    SelectableEntryProvider provider = namedProviders.get(name);
    List<SelectableEntryXO> entries = null;
    if (provider == null) {
      log.warn("Could not find a {} bounded to name {}", SelectableEntryProvider.class.getName(), name);
    }
    else {
      try {
        entries = provider.get(asParameters(uriInfo.getQueryParameters()));
      }
      catch (Exception e) {
        log.warn(
            "Provider {} failed to provide the list of entries due to {}/{}",
            provider, e.getClass().getName(), e.getMessage(), log.isDebugEnabled() ? e : null
        );
      }
    }
    if (entries == null) {
      return Lists.newArrayList();
    }
    return entries;
  }

  private Parameters asParameters(final MultivaluedMap<String, String> queryParameters) {
    return new Parameters()
    {
      @Override
      public String getFirst(final String name) {
        return queryParameters.getFirst(name);
      }

      @Override
      public List<String> get(final String name) {
        return queryParameters.get(name);
      }
    };
  }

  private Predicate<Repository> hasRightsToView(final Boolean skipPermissions) {
    if (skipPermissions == null || !skipPermissions) {
      return new Predicate<Repository>()
      {
        @Override
        public boolean apply(@Nullable final Repository input) {
          return input != null && nexusItemAuthorizer.isViewable(
              NexusItemAuthorizer.VIEW_REPOSITORY_KEY, input.getId()
          );
        }
      };
    }
    return null;
  }

  private Predicate<Repository> hasAnyOfFacets(@Nullable final List<String> facets) {
    if (facets != null && !facets.isEmpty()) {
      List<Predicate<Repository>> predicates = Lists.newArrayList();
      for (String facet : facets) {
        if (StringUtils.isNotEmpty(facet) && !facet.startsWith("!")) {
          try {
            final Class<?> facetClass = getClass().getClassLoader().loadClass(facet);
            predicates.add(new Predicate<Repository>()
            {
              @Override
              public boolean apply(@Nullable final Repository input) {
                return input != null && input.getRepositoryKind().isFacetAvailable(facetClass);
              }
            });
          }
          catch (ClassNotFoundException e) {
            log.warn("Repositories will not be filtered by facet {} as it could not be loaded", facet);
          }
        }
      }
      if (!predicates.isEmpty()) {
        if (predicates.size() == 1) {
          return predicates.get(0);
        }
        return Predicates.or(predicates);
      }
    }
    return null;
  }

  private Predicate<Repository> hasNoneOfFacets(@Nullable final List<String> facets) {
    if (facets != null && !facets.isEmpty()) {
      List<Predicate<Repository>> predicates = Lists.newArrayList();
      for (String facet : facets) {
        if (StringUtils.isNotEmpty(facet) && facet.startsWith("!")) {
          String actualFacet = facet.substring(1);
          try {
            final Class<?> facetClass = getClass().getClassLoader().loadClass(actualFacet);
            predicates.add(new Predicate<Repository>()
            {
              @Override
              public boolean apply(@Nullable final Repository input) {
                return input != null && !input.getRepositoryKind().isFacetAvailable(facetClass);
              }
            });
          }
          catch (ClassNotFoundException e) {
            log.warn("Repositories will not be filtered by facet {} as it could not be loaded", actualFacet);
          }
        }
      }
      if (!predicates.isEmpty()) {
        if (predicates.size() == 1) {
          return predicates.get(0);
        }
        return Predicates.and(predicates);
      }
    }
    return null;
  }

  private Predicate<Repository> hasAnyOfContentClasses(final List<String> contentClasses) {
    if (contentClasses != null && !contentClasses.isEmpty()) {
      List<Predicate<Repository>> predicates = Lists.newArrayList();
      for (final String contentClass : contentClasses) {
        if (StringUtils.isNotEmpty(contentClass) && !contentClass.startsWith("!")) {
          predicates.add(new Predicate<Repository>()
          {
            @Override
            public boolean apply(@Nullable final Repository input) {
              return input != null && input.getRepositoryContentClass().getId().equals(contentClass);
            }
          });
        }
      }
      if (!predicates.isEmpty()) {
        if (predicates.size() == 1) {
          return predicates.get(0);
        }
        return Predicates.or(predicates);
      }
    }
    return null;
  }

  private Predicate<Repository> hasNoneOfContentClasses(final List<String> contentClasses) {
    if (contentClasses != null && !contentClasses.isEmpty()) {
      List<Predicate<Repository>> predicates = Lists.newArrayList();
      for (final String contentClass : contentClasses) {
        if (StringUtils.isNotEmpty(contentClass) && contentClass.startsWith("!")) {
          predicates.add(new Predicate<Repository>()
          {
            @Override
            public boolean apply(@Nullable final Repository input) {
              return input != null && !input.getRepositoryContentClass().getId().equals(contentClass.substring(1));
            }
          });
        }
      }
      if (!predicates.isEmpty()) {
        return Predicates.or(predicates);
      }
    }
    return null;
  }

  private static <T> Iterable<T> removeNulls(final T... values) {
    return removeNulls(Arrays.asList(values));
  }

  private static <T> Iterable<T> removeNulls(final Iterable<T> values) {
    return Iterables.filter(values, Predicates.notNull());
  }

}
