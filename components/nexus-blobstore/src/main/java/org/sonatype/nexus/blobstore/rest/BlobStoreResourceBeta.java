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
package org.sonatype.nexus.blobstore.rest;

import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response.Status;

import org.sonatype.nexus.blobstore.ConnectionChecker;
import org.sonatype.nexus.blobstore.api.BlobStoreManager;
import org.sonatype.nexus.blobstore.quota.BlobStoreQuotaService;
import org.sonatype.nexus.repository.blobstore.BlobStoreConfigurationStore;
import org.sonatype.nexus.rest.WebApplicationMessageException;

import io.swagger.annotations.Api;

import static org.sonatype.nexus.blobstore.rest.BlobStoreResourceBeta.RESOURCE_URI;
import static org.sonatype.nexus.rest.APIConstants.BETA_API_PREFIX;

/**
 * beta endpoint for BlobStore REST API
 *
 * @since 3.24
 * @deprecated moving to {@link BlobStoreResourceV1}
 */
@Api(hidden = true)
@Named
@Singleton
@Path(RESOURCE_URI)
@Deprecated
public class BlobStoreResourceBeta
    extends BlobStoreResource
{
  static final String RESOURCE_URI = BETA_API_PREFIX + "/blobstores";

  @Inject
  public BlobStoreResourceBeta(
      final BlobStoreManager blobStoreManager,
      final BlobStoreConfigurationStore store,
      final BlobStoreQuotaService quotaService,
      final Map<String, ConnectionChecker> connectionCheckers)
  {
    super(blobStoreManager, store, quotaService, connectionCheckers);
  }

  @Override
  @Deprecated
  public BlobStoreQuotaResultXO quotaStatus(final String name) {
    throw new WebApplicationMessageException(Status.BAD_REQUEST, "not supported");
  }
}
