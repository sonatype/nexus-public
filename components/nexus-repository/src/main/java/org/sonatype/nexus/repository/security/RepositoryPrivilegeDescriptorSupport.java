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
package org.sonatype.nexus.repository.security;

import java.util.List;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response.Status;

import org.sonatype.nexus.repository.Format;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.manager.RepositoryManager;
import org.sonatype.nexus.repository.security.rest.ApiPrivilegeWithRepository;
import org.sonatype.nexus.repository.security.rest.ApiPrivilegeWithRepositoryRequest;
import org.sonatype.nexus.rest.WebApplicationMessageException;
import org.sonatype.nexus.security.privilege.PrivilegeDescriptorSupport;
import org.sonatype.nexus.security.privilege.rest.PrivilegeAction;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * @since 3.19
 */
public abstract class RepositoryPrivilegeDescriptorSupport<T extends ApiPrivilegeWithRepository, Y extends ApiPrivilegeWithRepositoryRequest>
    extends PrivilegeDescriptorSupport<T,Y>
{
  public static final String INVALID_REPOSITORY = "\"Invalid repository '%s' supplied.\"";

  public static final String INVALID_FORMAT_FOR_REPOSITORY = "\"Invalid format '%s' supplied for repository '%s'.\"";

  public static final String INVALID_FORMAT = "\"Invalid format '%s' supplied.\"";

  private final RepositoryManager repositoryManager;

  private final List<Format> formats;

  public RepositoryPrivilegeDescriptorSupport(final String type, final RepositoryManager repositoryManager, final List<Format> formats) {
    super(type);
    this.repositoryManager = checkNotNull(repositoryManager);
    this.formats = checkNotNull(formats);
  }

  @Override
  public void validate(final Y apiPrivilege) {
    validateActions(apiPrivilege, PrivilegeAction.getBreadActions());
    validateRepositoryAndFormat(apiPrivilege);
  }

  protected void validateRepositoryAndFormat(Y apiPrivilege) {
    String repo = apiPrivilege.getRepository();
    String format = apiPrivilege.getFormat();

    Repository repository = repositoryManager.get(repo);

    //we have an actual repo id, make sure valid
    if (!("*".equals(repo)) && repository == null) {
      throw new WebApplicationMessageException(Status.BAD_REQUEST,
          String.format(INVALID_REPOSITORY, apiPrivilege.getRepository()), MediaType.APPLICATION_JSON);
    }

    //we have an actual repo id and actual format, make sure valid
    if (!("*".equals(format)) && repository != null && !repository.getFormat().getValue().equals(format)) {
      throw new WebApplicationMessageException(Status.BAD_REQUEST,
          String.format(INVALID_FORMAT_FOR_REPOSITORY, apiPrivilege.getFormat(), apiPrivilege.getRepository()),
          MediaType.APPLICATION_JSON);
    }

    //we have an actual format and a * for repo, validate format is supported in system
    if (!("*".equals(format)) && formats.stream().noneMatch(f -> f.getValue().equals(format))) {
      throw new WebApplicationMessageException(Status.BAD_REQUEST,
          String.format(INVALID_FORMAT, apiPrivilege.getFormat()), MediaType.APPLICATION_JSON);
    }
  }
}
