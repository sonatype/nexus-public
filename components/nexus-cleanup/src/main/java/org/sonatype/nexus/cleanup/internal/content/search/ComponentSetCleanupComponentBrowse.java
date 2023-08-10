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
package org.sonatype.nexus.cleanup.internal.content.search;

import org.sonatype.nexus.cleanup.internal.datastore.search.criteria.AssetCleanupEvaluator;
import org.sonatype.nexus.cleanup.internal.datastore.search.criteria.ComponentCleanupEvaluator;
import org.sonatype.nexus.cleanup.storage.CleanupPolicy;
import org.sonatype.nexus.common.entity.Continuations;
import org.sonatype.nexus.extdirect.model.PagedResponse;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.content.Component;
import org.sonatype.nexus.repository.content.fluent.FluentComponent;
import org.sonatype.nexus.repository.query.QueryOptions;
import org.sonatype.nexus.scheduling.CancelableHelper;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.google.common.base.Preconditions.checkNotNull;

@Named("ComponentSetCleanupComponentBrowse")
@Singleton
public class ComponentSetCleanupComponentBrowse
    extends DataStoreCleanupComponentBrowse
    implements CleanupComponentBrowse
{
  @Inject
  public ComponentSetCleanupComponentBrowse(
      final Map<String, ComponentCleanupEvaluator> componentCriteria,
      final Map<String, AssetCleanupEvaluator> assetCriteria)
  {
    super(componentCriteria, assetCriteria);
  }

  @Override
  public Stream<FluentComponent> browse(final CleanupPolicy policy, final Repository repository) {
    checkNotNull(policy);
    checkNotNull(repository);

    validateCleanupPolicy(policy);
    return Continuations.streamOf(new ComponentSetCleanupBrowser(repository, policy)::browse);
  }

  @Override
  public PagedResponse<Component> browseByPage(
          final CleanupPolicy policy,
          final Repository repository,
          final QueryOptions options)
  {
    checkNotNull(policy);
    checkNotNull(repository);
    checkNotNull(options);
    checkNotNull(options.getStart());
    checkNotNull(options.getLimit());

    validateCleanupPolicy(policy);
    List<Component> result =
            Continuations.streamOf(new ComponentSetCleanupBrowser(repository, policy)::browse, Continuations.BROWSE_LIMIT,
                            options.getLastId())
                    .peek(__ -> CancelableHelper.checkCancellation())
                    .limit(options.getLimit())
                    .map(Component.class::cast)
                    .collect(Collectors.toList());

    // We return -1 as we don't have an inexpensive way of computing the total number of matching results
    return new PagedResponse<>(-1, result);
  }

}
