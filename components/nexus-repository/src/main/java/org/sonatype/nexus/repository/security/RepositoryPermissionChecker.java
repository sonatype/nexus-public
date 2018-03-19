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

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.security.SecurityHelper;
import org.sonatype.nexus.selector.SelectorManager;

import org.apache.shiro.subject.Subject;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.Collections.singletonList;
import static org.sonatype.nexus.security.BreadActions.BROWSE;
import static org.sonatype.nexus.security.BreadActions.READ;

/**
 * Repository permission checker.
 *
 * @since 3.next
 */
@Named
@Singleton
public class RepositoryPermissionChecker
{
  private final SecurityHelper securityHelper;

  private final SelectorManager selectorManager;

  @Inject
  public RepositoryPermissionChecker(final SecurityHelper securityHelper, final SelectorManager selectorManager) {
    this.securityHelper = checkNotNull(securityHelper);
    this.selectorManager = checkNotNull(selectorManager);
  }

  public boolean userCanViewRepository(final Repository repository) {
    return userHasRepositoryViewPermissionTo(READ, repository) || userHasAnyContentSelectorAccessTo(repository);
  }

  public boolean userCanBrowseRepository(final Repository repository) {
    return userHasRepositoryViewPermissionTo(BROWSE, repository) || userHasAnyContentSelectorAccessTo(repository);
  }

  private boolean userHasRepositoryViewPermissionTo(final String action, final Repository repository) {
    return securityHelper.anyPermitted(new RepositoryViewPermission(repository, action));
  }

  private boolean userHasAnyContentSelectorAccessTo(final Repository repository) {
    Subject subject = securityHelper.subject(); //Getting the subject a single time improves performance
    return selectorManager.browse().stream()
        .anyMatch(selector -> securityHelper.anyPermitted(subject,
            new RepositoryContentSelectorPermission(selector, repository, singletonList(BROWSE))));
  }
}
