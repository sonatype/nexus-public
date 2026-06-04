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
package org.sonatype.nexus.api.rest.selfhosted.formats;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;

import org.sonatype.nexus.api.rest.selfhosted.formats.model.UploadDefinitionXO;
import org.sonatype.nexus.repository.rest.api.UploadFieldDefinitionXO;
import org.sonatype.nexus.repository.upload.UploadDefinition;
import org.sonatype.nexus.repository.upload.UploadManager;
import org.sonatype.nexus.rest.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.lang.String.format;
import static java.util.stream.Collectors.toList;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.sonatype.nexus.rest.APIConstants.V1_API_PREFIX;
import org.springframework.stereotype.Component;

/**
 * @since 3.10
 */
@Component
@Path(UploadDefinitionResource.BASE_PATH)
@Produces(APPLICATION_JSON)
@Consumes(APPLICATION_JSON)
public class UploadDefinitionResource
    implements Resource, UploadDefinitionResourceDoc
{
  protected final Logger log = LoggerFactory.getLogger(getClass());

  public static final String BASE_PATH = V1_API_PREFIX + "/formats";

  private final UploadManager uploadManager;

  @Autowired
  public UploadDefinitionResource(final UploadManager uploadManager) {
    this.uploadManager = checkNotNull(uploadManager);
  }

  @Path("upload-specs")
  @GET
  public List<UploadDefinitionXO> get() {
    log.debug("Get upload definitions for all formats.");

    return uploadManager.getAvailableDefinitions().stream().map(this::from).collect(toList());
  }

  @Path("{format}/upload-specs")
  @GET
  public UploadDefinitionXO get(@PathParam("format") final String format) {
    log.debug("Get upload definition for format '{}'.", format);

    UploadDefinition uploadDefinition = uploadManager.getByFormat(format);

    if (uploadDefinition == null || !uploadDefinition.isApiUpload()) {
      log.debug("Could not find upload definition for format '{}'.", format);
      throw new NotFoundException(format("Unable to locate upload definition for format '%s'", format));
    }

    return from(uploadDefinition);
  }

  private UploadDefinitionXO from(UploadDefinition def) {
    UploadDefinitionXO xo = UploadDefinitionXO.from(def);
    UploadFieldDefinitionXO fieldXo = new UploadFieldDefinitionXO();
    fieldXo.setName("asset");
    fieldXo.setType("FILE");
    fieldXo.setOptional(false);
    xo.getAssetFields().add(fieldXo);
    return xo;
  }
}
