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

import java.io.IOException;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response.Status;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.common.app.FeatureFlag;
import org.sonatype.nexus.repository.IllegalOperationException;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.content.rest.internal.resources.doc.ComponentsResourceDoc;
import org.sonatype.nexus.repository.rest.api.RepositoryManagerRESTAdapter;
import org.sonatype.nexus.repository.upload.UploadConfiguration;
import org.sonatype.nexus.repository.upload.UploadManager;
import org.sonatype.nexus.rest.Resource;
import org.sonatype.nexus.rest.WebApplicationMessageException;

import static com.google.common.base.Preconditions.checkNotNull;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.sonatype.nexus.repository.http.HttpStatus.NOT_FOUND;
import static org.sonatype.nexus.rest.APIConstants.V1_API_PREFIX;

/**
 * @since 3.24
 */
@FeatureFlag(name = "nexus.datastore.enabled")
@Named
@Singleton
@Path(ComponentsResource.RESOURCE_URI)
@Produces(APPLICATION_JSON)
@Consumes(APPLICATION_JSON)
public class ComponentsResource
    extends ComponentSupport
    implements Resource, ComponentsResourceDoc
{
  public static final String RESOURCE_URI = V1_API_PREFIX + "/components";

  private final RepositoryManagerRESTAdapter repositoryManagerRESTAdapter;

  private final UploadManager uploadManager;

  private final UploadConfiguration uploadConfiguration;

  @Inject
  public ComponentsResource(
      final RepositoryManagerRESTAdapter repositoryManagerRESTAdapter,
      final UploadManager uploadManager,
      final UploadConfiguration uploadConfiguration)
  {
    this.repositoryManagerRESTAdapter = checkNotNull(repositoryManagerRESTAdapter);
    this.uploadManager = checkNotNull(uploadManager);
    this.uploadConfiguration = checkNotNull(uploadConfiguration);
  }

  @Override
  @POST
  @Consumes(MediaType.MULTIPART_FORM_DATA)
  public void uploadComponent(@QueryParam("repository") final String repositoryId,
                              @Context final HttpServletRequest request)
      throws IOException
  {
    if (!uploadConfiguration.isEnabled()) {
      throw new WebApplicationException(NOT_FOUND);
    }

    Repository repository = repositoryManagerRESTAdapter.getRepository(repositoryId);

    try {
      uploadManager.handle(repository, request);
    } catch (IllegalOperationException e) {
      throw new WebApplicationMessageException(Status.BAD_REQUEST, e.getMessage());
    }
  }
}
