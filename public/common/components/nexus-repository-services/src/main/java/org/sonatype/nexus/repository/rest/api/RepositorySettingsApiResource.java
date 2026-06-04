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

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Produces;

import org.sonatype.nexus.common.QualifierUtil;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.rest.api.model.AbstractApiRepository;
import org.sonatype.nexus.rest.Resource;

import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Preconditions.checkNotNull;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import org.springframework.beans.factory.annotation.Qualifier;

/**
 * @since 3.26
 */
@Produces(APPLICATION_JSON)
@Consumes(APPLICATION_JSON)
public class RepositorySettingsApiResource
    implements Resource, RepositorySettingsApiResourceDoc
{
  protected final Logger log = LoggerFactory.getLogger(getClass());

  private final AuthorizingRepositoryManager authorizingRepositoryManager;

  private final Map<String, ApiRepositoryAdapter> convertersByFormat;

  private final ApiRepositoryAdapter defaultAdapter;

  @Autowired
  public RepositorySettingsApiResource(
      final AuthorizingRepositoryManager authorizingRepositoryManager,
      @Qualifier("default") final ApiRepositoryAdapter defaultAdapter,
      final List<ApiRepositoryAdapter> convertersByFormatList)
  {
    this.authorizingRepositoryManager = checkNotNull(authorizingRepositoryManager);
    this.defaultAdapter = checkNotNull(defaultAdapter);
    this.convertersByFormat = QualifierUtil.buildQualifierBeanMap(checkNotNull(convertersByFormatList));
  }

  @Override
  @RequiresAuthentication
  @GET
  public List<AbstractApiRepository> getRepositories() {
    return authorizingRepositoryManager.getRepositoriesWithAdmin()
        .stream()
        .map(this::convert)
        .collect(Collectors.toList());
  }

  private AbstractApiRepository convert(final Repository repository) {
    return convertersByFormat.getOrDefault(repository.getFormat().toString(), defaultAdapter).adapt(repository);
  }
}
