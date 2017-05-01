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
package org.sonatype.nexus.repository.browse.internal.resources;

import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.WebApplicationException;

import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.manager.RepositoryManager;
import org.sonatype.nexus.repository.security.RepositoryContentSelectorPermission;
import org.sonatype.nexus.repository.security.RepositoryViewPermission;
import org.sonatype.nexus.security.SecurityHelper;
import org.sonatype.nexus.selector.SelectorManager;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.Collections.singletonList;
import static java.util.Optional.ofNullable;
import static org.sonatype.nexus.repository.http.HttpStatus.FORBIDDEN;
import static org.sonatype.nexus.repository.http.HttpStatus.UNPROCESSABLE_ENTITY;
import static org.sonatype.nexus.security.BreadActions.BROWSE;
import static org.sonatype.nexus.security.BreadActions.READ;

/**
 * An implementation of the {@link RepositoryManagerRESTAdapter}
 *
 * @since 3.4
 */
@Named
public class RepositoryManagerRESTAdapterImpl
    implements RepositoryManagerRESTAdapter
{
  private final RepositoryManager repositoryManager;

  private final SecurityHelper securityHelper;

  private final SelectorManager selectorManager;

  @Inject
  public RepositoryManagerRESTAdapterImpl(final RepositoryManager repositoryManager,
                                          final SecurityHelper securityHelper,
                                          final SelectorManager selectorManager)
  {
    this.repositoryManager = checkNotNull(repositoryManager);
    this.securityHelper = checkNotNull(securityHelper);
    this.selectorManager = checkNotNull(selectorManager);
  }

  @Override
  public Repository getRepository(final String repositoryId) {
    if (repositoryId == null) {
      throw new WebApplicationException("repositoryId is required.", UNPROCESSABLE_ENTITY);
    }
    Repository repository = ofNullable(repositoryManager.get(repositoryId))
        .orElseThrow(() -> new NotFoundException("Unable to locate repository with id " + repositoryId));

    if (userCanBrowseRepository(repository)) {
      //browse implies complete access to the repository.
      return repository;
    }
    else if (userCanViewRepository(repository)) {
      //user knows the repository exists but does not have the appropriate permission to browse, return a 403
      throw new WebApplicationException(FORBIDDEN);
    }
    else {
      //User does not know the repository exists because they can not VIEW or BROWSE, return a 404 
      throw new NotFoundException("Unable to locate repository with id " + repositoryId);
    }
  }

  private boolean userCanViewRepository(final Repository repository) {
    return userHasReadPermission(repository) || userHasAnyContentSelectorAccess(repository);
  }

  private boolean userCanBrowseRepository(final Repository repository) {
    return userHasBrowsePermissions(repository) || userHasAnyContentSelectorAccess(repository);
  }

  private boolean userHasBrowsePermissions(final Repository repository) {
    return securityHelper.anyPermitted(
        new RepositoryViewPermission(repository.getFormat().getValue(), repository.getName(), BROWSE));
  }

  private boolean userHasReadPermission(final Repository repository) {
    return securityHelper.anyPermitted(
        new RepositoryViewPermission(repository.getFormat().getValue(), repository.getName(), READ));
  }

  private boolean userHasAnyContentSelectorAccess(final Repository repository) {
    return selectorManager.browse().stream().anyMatch(sc -> securityHelper.anyPermitted(
        new RepositoryContentSelectorPermission(sc.getName(), repository.getFormat().getValue(), repository.getName(),
            singletonList(BROWSE))));
  }
}
