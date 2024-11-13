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
package org.sonatype.nexus.cleanup.content.search;

import java.util.stream.Stream;

import org.sonatype.nexus.cleanup.storage.CleanupPolicy;
import org.sonatype.nexus.extdirect.model.PagedResponse;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.content.Component;
import org.sonatype.nexus.repository.content.fluent.FluentComponent;
import org.sonatype.nexus.repository.query.QueryOptions;

/**
 * Finds components to be cleaned up.
 *
 * @since 3.14
 */
public interface CleanupComponentBrowse
{
  Stream<FluentComponent> browse(CleanupPolicy policy, Repository repository);

  Stream<FluentComponent> browseIncludingAssets(CleanupPolicy policy, Repository repository);

  /**
   * Returns a paged response of components
   *
   * @param policy cleanup policy criteria
   * @param repository repository to browse
   * @param options specifies paging and sorting criteria
   * @return a page of {@link Component}
   */
  PagedResponse<Component> browseByPage(CleanupPolicy policy, Repository repository, QueryOptions options);
}
