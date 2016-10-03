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
package org.sonatype.nexus.repository.browse;

import java.util.List;

import javax.annotation.Nullable;

import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.storage.Asset;
import org.sonatype.nexus.repository.storage.Component;

/**
 * Consolidates code for user browsing of repositories, specifically for the user interface.
 *
 * @since 3.1
 */
public interface BrowseService
{
  /**
   * Returns a {@link BrowseResult} for browsing the specified repository and query options.
   */
  BrowseResult<Component> browseComponents(final Repository repository,
                                           @Nullable final String filter,
                                           @Nullable final String sortProperty,
                                           @Nullable final String sortDirection,
                                           @Nullable final Integer start,
                                           @Nullable final Integer limit);

  /**
   * Returns a {@link BrowseResult} of assets for the specified component. Note that the Repository passed in is not
   * necessarily the Repository where the component resides (in the case of a group Repository).
   */
  BrowseResult<Asset> browseComponentAssets(final Repository repository, final String componentId);

  /**
   * Returns a {@link BrowseResult} of assets based on the specified information.
   */
  BrowseResult<Asset> browseAssets(final Repository repository,
                                   @Nullable final String filter,
                                   @Nullable final String sortProperty,
                                   @Nullable final String sortDirection,
                                   @Nullable final Integer start,
                                   @Nullable final Integer limit);

  /**
   * Returns a {@link BrowseResult} for previewing the specified repository based on an arbitrary content selector.
   */
  BrowseResult<Asset> previewAssets(final List<Repository> repositories,
                                    final String jexlExpression,
                                    @Nullable final String filter,
                                    @Nullable final String sortProperty,
                                    @Nullable final String sortDirection,
                                    @Nullable final Integer start,
                                    @Nullable final Integer limit);

}
