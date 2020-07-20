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
package org.sonatype.nexus.cleanup.preview;

import java.io.IOException;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.sonatype.nexus.cleanup.storage.CleanupPolicy;
import org.sonatype.nexus.cleanup.storage.CleanupPolicyPreviewXO;
import org.sonatype.nexus.extdirect.DirectComponent;
import org.sonatype.nexus.extdirect.DirectComponentSupport;
import org.sonatype.nexus.extdirect.model.PagedResponse;
import org.sonatype.nexus.extdirect.model.StoreLoadParameters;
import org.sonatype.nexus.extdirect.model.StoreLoadParameters.Sort;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.manager.RepositoryManager;
import org.sonatype.nexus.repository.query.QueryOptions;
import org.sonatype.nexus.repository.rest.api.ComponentXO;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.softwarementors.extjs.djn.config.annotations.DirectAction;
import com.softwarementors.extjs.djn.config.annotations.DirectMethod;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.apache.shiro.authz.annotation.RequiresPermissions;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Cleanup preview {@link DirectComponent}.
 *
 * @since 3.24
 */
@Named
@Singleton
@DirectAction(action = "cleanup_CleanupPreview")
public class CleanupPreviewComponent
    extends DirectComponentSupport
{
  private final ObjectMapper mapper = new ObjectMapper();

  private final Provider<CleanupPreviewHelper> cleanupPreviewHelper;

  private final RepositoryManager repositoryManager;

  @Inject
  public CleanupPreviewComponent(final Provider<CleanupPreviewHelper> cleanupPreviewHelper,
                                 final RepositoryManager repositoryManager)
  {
    this.cleanupPreviewHelper = checkNotNull(cleanupPreviewHelper);
    this.repositoryManager = checkNotNull(repositoryManager);
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

    return cleanupPreviewHelper.get().getSearchResults(previewXO, repository, queryOptions);
  }
}
