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
package org.sonatype.nexus.repository.manager.internal;

import java.util.Arrays;
import java.util.List;

import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.security.config.MutableSecurityContributor;
import org.sonatype.nexus.security.config.SecurityConfiguration;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.sonatype.nexus.repository.security.RepositoryAdminPrivilegeDescriptor.id;
import static org.sonatype.nexus.repository.security.RepositoryAdminPrivilegeDescriptor.privilege;
import static org.sonatype.nexus.security.BreadActions.ADD;
import static org.sonatype.nexus.security.BreadActions.BROWSE;
import static org.sonatype.nexus.security.BreadActions.DELETE;
import static org.sonatype.nexus.security.BreadActions.EDIT;
import static org.sonatype.nexus.security.BreadActions.READ;
import static org.sonatype.nexus.security.privilege.PrivilegeDescriptorSupport.ALL;

/**
 * Repository administration security contributor.
 *
 * @since 3.0
 */
@Named
@Singleton
public class RepositoryAdminSecurityContributor
    extends MutableSecurityContributor
{
  // TODO: Sort out role[-naming] scheme

  /**
   * Initial (static) security configuration.
   */
  @Override
  protected void initial(final SecurityConfiguration model) {
    maybeAddPrivilege(model, privilege(ALL, ALL, ALL));
    maybeAddPrivilege(model, privilege(ALL, ALL, BROWSE));
    maybeAddPrivilege(model, privilege(ALL, ALL, READ));
    maybeAddPrivilege(model, privilege(ALL, ALL, EDIT));
    maybeAddPrivilege(model, privilege(ALL, ALL, ADD));
    maybeAddPrivilege(model, privilege(ALL, ALL, DELETE));
  }

  /**
   * Add security configuration for given repository.
   */
  public void add(final Repository repository) {
    checkNotNull(repository);
    final String format = repository.getFormat().getValue();
    final String name = repository.getName();
    apply((model, configurationManager) -> {
      // no per-repo repository-admin ADD action
      maybeAddPrivilege(model, privilege(format, name, ALL));
      maybeAddPrivilege(model, privilege(format, name, BROWSE));
      maybeAddPrivilege(model, privilege(format, name, READ));
      maybeAddPrivilege(model, privilege(format, name, EDIT));
      maybeAddPrivilege(model, privilege(format, name, DELETE));
    });
  }

  /**
   * Remove security configuration for given repository.
   */
  public void remove(final Repository repository) {
    checkNotNull(repository);
    final String format = repository.getFormat().getValue();
    final String name = repository.getName();
    final List<String> privilegeIds = Arrays.asList(
        id(format, name, ALL),
        id(format, name, BROWSE),
        id(format, name, READ),
        id(format, name, EDIT),
        id(format, name, DELETE));
    apply((model, configurationManager) -> {
      // no per-repo repository-admin ADD action
      privilegeIds.forEach(model::removePrivilege);
      privilegeIds.forEach(configurationManager::cleanRemovedPrivilege);
    });
  }
}
