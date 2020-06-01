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
package org.sonatype.nexus.cleanup.internal.service;

import java.util.List;
import java.util.function.BiFunction;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.cleanup.preview.CleanupComponentBrowse;
import org.sonatype.nexus.cleanup.preview.CleanupPreviewHelper;
import org.sonatype.nexus.cleanup.storage.CleanupPolicy;
import org.sonatype.nexus.cleanup.storage.CleanupPolicyCriteria;
import org.sonatype.nexus.cleanup.storage.CleanupPolicyPreviewXO;
import org.sonatype.nexus.cleanup.storage.CleanupPolicyStorage;
import org.sonatype.nexus.extdirect.model.PagedResponse;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.browse.QueryOptions;
import org.sonatype.nexus.repository.rest.api.ComponentXO;
import org.sonatype.nexus.repository.rest.api.DefaultComponentXO;
import org.sonatype.nexus.repository.storage.Component;
import org.sonatype.nexus.repository.storage.StorageFacet;
import org.sonatype.nexus.transaction.Transactional;
import org.sonatype.nexus.transaction.UnitOfWork;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.stream.Collectors.toList;

/**
 * {@link CleanupPreviewHelper} implementation.
 *
 * @since 3.24
 */
@Named
@Singleton
public class CleanupPreviewHelperImpl
    extends ComponentSupport
    implements CleanupPreviewHelper
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

  private final CleanupPolicyStorage cleanupPolicyStorage;

  private final CleanupComponentBrowse cleanupComponentBrowse;

  @Inject
  public CleanupPreviewHelperImpl(final CleanupPolicyStorage cleanupPolicyStorage,
                                  final CleanupComponentBrowse cleanupComponentBrowse)
  {
    this.cleanupPolicyStorage = checkNotNull(cleanupPolicyStorage);
    this.cleanupComponentBrowse = checkNotNull(cleanupComponentBrowse);
  }

  @Override
  public PagedResponse<ComponentXO> getSearchResults(final CleanupPolicyPreviewXO previewXO,
                                                     final Repository repository,
                                                     final QueryOptions queryOptions)
  {
    CleanupPolicy cleanupPolicy = toCleanupPolicy(previewXO);

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

  private CleanupPolicy toCleanupPolicy(final CleanupPolicyPreviewXO cleanupPolicyPreviewXO) {
    CleanupPolicy policy = cleanupPolicyStorage.newCleanupPolicy();

    policy.setName("preview");
    policy.setCriteria(CleanupPolicyCriteria.toMap(cleanupPolicyPreviewXO.getCriteria()));

    return policy;
  }
}
