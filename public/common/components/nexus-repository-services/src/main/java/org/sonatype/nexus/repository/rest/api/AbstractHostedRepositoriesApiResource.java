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
package org.sonatype.nexus.repository.rest.api;

import org.sonatype.nexus.repository.config.Configuration;
import org.sonatype.nexus.repository.rest.api.model.AbstractApiRepository;
import org.sonatype.nexus.repository.rest.api.model.HostedRepositoryApiRequest;
import org.sonatype.nexus.repository.rest.api.model.HostedStorageAttributes;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.ws.rs.BeanParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @since 3.28
 */
public class AbstractHostedRepositoriesApiResource<S extends HostedStorageAttributes, T extends HostedRepositoryApiRequest<S>>
    extends AbstractRepositoriesApiResource<T>
{
  protected HostedRepositoryApiRequestToConfigurationConverter<S, T> configurationConverter;

  @Autowired
  public void setConfigurationConverter(
      final HostedRepositoryApiRequestToConfigurationConverter<S, T> configurationConverter)
  {
    this.configurationConverter = configurationConverter;
  }

  @Override
  public Configuration adapt(final T request) {
    return configurationConverter.convert(request);
  }

  @GET
  @Path("/{repositoryName}")
  @Operation(summary = "Get repository")
  @Override
  public AbstractApiRepository getRepository(
      @Parameter(hidden = true) @BeanParam final FormatAndType formatAndType,
      @PathParam("repositoryName") final String repositoryName)
  {
    return super.getRepository(formatAndType, repositoryName);
  }
}
