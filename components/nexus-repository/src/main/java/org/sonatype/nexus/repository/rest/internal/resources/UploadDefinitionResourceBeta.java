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
package org.sonatype.nexus.repository.rest.internal.resources;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.repository.rest.api.UploadDefinitionXO;
import org.sonatype.nexus.repository.upload.UploadConfiguration;
import org.sonatype.nexus.repository.upload.UploadManager;
import org.sonatype.nexus.rest.Resource;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.sonatype.nexus.rest.APIConstants.BETA_API_PREFIX;

/**
 * @deprecated since 3.next, use {@link UploadDefinitionResource} instead.
 */
@Deprecated
@Named
@Singleton
@Path(UploadDefinitionResourceBeta.BASE_PATH)
@Produces(APPLICATION_JSON)
@Consumes(APPLICATION_JSON)
public class UploadDefinitionResourceBeta
    extends ComponentSupport
    implements Resource
{
  static final String BASE_PATH = BETA_API_PREFIX + "/formats";

  private final UploadDefinitionResource delegate;

  @Inject
  public UploadDefinitionResourceBeta(final UploadManager uploadManager, final UploadConfiguration uploadConfiguration)
  {
    delegate = new UploadDefinitionResource(uploadManager, uploadConfiguration);
  }

  @Path("upload-specs")
  @GET
  public List<UploadDefinitionXO> get() {
    log.warn("Deprecated endpoint: {}, please use: {}", BASE_PATH, UploadDefinitionResource.BASE_PATH);
    return delegate.get();
  }

  @Path("{format}/upload-specs")
  @GET
  public UploadDefinitionXO get(@PathParam("format") final String format)
  {
    log.warn("Deprecated endpoint: {}, please use: {}", BASE_PATH, UploadDefinitionResource.BASE_PATH);
    return delegate.get(format);
  }
}
