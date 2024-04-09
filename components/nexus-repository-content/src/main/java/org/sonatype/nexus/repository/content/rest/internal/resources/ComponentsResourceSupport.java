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
package org.sonatype.nexus.repository.content.rest.internal.resources;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.common.entity.Continuation;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.content.facet.ContentFacet;
import org.sonatype.nexus.repository.content.fluent.FluentAsset;
import org.sonatype.nexus.repository.content.fluent.FluentComponent;
import org.sonatype.nexus.repository.rest.api.RepositoryManagerRESTAdapter;
import org.sonatype.nexus.repository.selector.ContentAuthHelper;
import org.sonatype.nexus.repository.types.GroupType;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.stream.Collectors.toList;
import static org.sonatype.nexus.repository.content.rest.internal.resources.AssetsResourceSupport.toInternalToken;
import static org.sonatype.nexus.repository.content.rest.internal.resources.AssetsResourceSupport.trim;
import static org.sonatype.nexus.repository.content.rest.internal.resources.ComponentsResource.PAGE_SIZE;

/**
 * Support class for {@link ComponentsResource} which fetches and returns only components that the user is permitted
 * to view according to {@link ContentAuthHelper#checkPathPermissions(String, String, String...)}
 *
 * @since 3.27
 */
abstract class ComponentsResourceSupport
    extends ComponentSupport
{
  static final int LIMIT = 100;

  private final ContentAuthHelper contentAuthHelper;

  private final RepositoryManagerRESTAdapter repositoryManagerRESTAdapter;

  ComponentsResourceSupport(
      final ContentAuthHelper contentAuthHelper,
      final RepositoryManagerRESTAdapter repositoryManagerRESTAdapter)
  {
    this.contentAuthHelper = checkNotNull(contentAuthHelper);
    this.repositoryManagerRESTAdapter = checkNotNull(repositoryManagerRESTAdapter);
  }

  List<FluentComponent> browse(final Repository browsedRepository, final String continuationToken)
  {
    List<FluentComponent> permittedComponents = new ArrayList<>();
    String internalToken = toInternalToken(continuationToken);
    Continuation<FluentComponent> componentContinuation = getComponents(browsedRepository, internalToken);

    while (permittedComponents.size() < PAGE_SIZE && !componentContinuation.isEmpty()) {
      permittedComponents.addAll(removeComponentsNotPermitted(browsedRepository, componentContinuation));
      componentContinuation = getComponents(browsedRepository, componentContinuation.nextContinuationToken());
    }
    return trim(permittedComponents, PAGE_SIZE);
  }

  private Continuation<FluentComponent> getComponents(Repository repository, final String continuationToken) {
    if(GroupType.NAME.equals(repository.getType().getValue())) {
      return repository.facet(ContentFacet.class).components().withOnlyGroupMemberContent()
          .browse(LIMIT, continuationToken);
    }
    return repository.facet(ContentFacet.class).components().browse(LIMIT, continuationToken);
  }

  private List<FluentComponent> removeComponentsNotPermitted(
      final Repository repository,
      final Continuation<FluentComponent> assets)
  {
    return assets.stream()
        .filter(componentPermitted(repository.getFormat().getValue(), repository.getName()))
        .collect(toList());
  }

  Predicate<FluentComponent> componentPermitted(final String format, final String repositoryName) {
    return component -> contentAuthHelper.checkPathPermissions(component.name(), format, repositoryName);
  }

  Predicate<FluentAsset> assetPermitted(Repository repository) {
    String repositoryName = repository.getName();
    Set<String> repoNames = new HashSet<>(repositoryManagerRESTAdapter.findContainingGroups(repositoryName));
    repoNames.add(repositoryName);
    return asset ->
        contentAuthHelper.checkPathPermissions(asset.path(), repository.getFormat().getValue(),
            repoNames.toArray(new String[0]));
  }
}
