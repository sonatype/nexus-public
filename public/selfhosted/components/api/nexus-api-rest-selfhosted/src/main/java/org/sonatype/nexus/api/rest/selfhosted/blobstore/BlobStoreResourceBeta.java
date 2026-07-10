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
package org.sonatype.nexus.api.rest.selfhosted.blobstore;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response.Status;

import org.sonatype.nexus.api.rest.common.blobstore.model.BlobStoreQuotaResultXO;
import org.sonatype.nexus.blobstore.ConnectionChecker;
import org.sonatype.nexus.blobstore.api.BlobStoreManager;
import org.sonatype.nexus.blobstore.quota.BlobStoreQuotaService;
import org.sonatype.nexus.repository.blobstore.BlobStoreConfigurationStore;
import org.sonatype.nexus.rest.WebApplicationMessageException;

import io.swagger.v3.oas.annotations.Hidden;
import org.springframework.stereotype.Component;

import static org.sonatype.nexus.api.rest.selfhosted.blobstore.BlobStoreResourceBeta.RESOURCE_URI;
import static org.sonatype.nexus.rest.APIConstants.BETA_API_PREFIX;

/**
 * beta endpoint for BlobStore REST API
 *
 * @since 3.24
 * @deprecated moving to {@link BlobStoreResourceV1}
 */
@Hidden
@Component
@Path(RESOURCE_URI)
@Deprecated
public class BlobStoreResourceBeta
    extends BlobStoreResource
{
  static final String RESOURCE_URI = BETA_API_PREFIX + "/blobstores";

  @Autowired
  public BlobStoreResourceBeta(
      final BlobStoreManager blobStoreManager,
      final BlobStoreConfigurationStore store,
      final BlobStoreQuotaService quotaService,
      final List<ConnectionChecker> connectionCheckers)
  {
    super(blobStoreManager, store, quotaService, connectionCheckers);
  }

  @Override
  @Deprecated
  public BlobStoreQuotaResultXO quotaStatus(final String name) {
    throw new WebApplicationMessageException(Status.BAD_REQUEST, "not supported");
  }
}
