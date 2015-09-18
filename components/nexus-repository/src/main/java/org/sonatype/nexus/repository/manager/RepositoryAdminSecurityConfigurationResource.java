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
package org.sonatype.nexus.repository.manager;

import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.security.config.MutableDynamicSecurityConfigurationResource;
import org.sonatype.nexus.security.config.SecurityConfiguration;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.sonatype.nexus.repository.security.BreadActions.ADD;
import static org.sonatype.nexus.repository.security.BreadActions.BROWSE;
import static org.sonatype.nexus.repository.security.BreadActions.DELETE;
import static org.sonatype.nexus.repository.security.BreadActions.EDIT;
import static org.sonatype.nexus.repository.security.BreadActions.READ;
import static org.sonatype.nexus.repository.security.RepositoryAdminPrivilegeDescriptor.id;
import static org.sonatype.nexus.repository.security.RepositoryAdminPrivilegeDescriptor.privilege;
import static org.sonatype.nexus.security.privilege.PrivilegeDescriptorSupport.ALL;

/**
 * Repository administration security resource.
 *
 * @since 3.0
 */
@Named
@Singleton
public class RepositoryAdminSecurityConfigurationResource
    extends MutableDynamicSecurityConfigurationResource
{
  public RepositoryAdminSecurityConfigurationResource() {
    apply(new Mutator()
    {
      @Override
      public void apply(final SecurityConfiguration model) {
        initial(model);
      }
    });
  }

  // TODO: Sort out role[-naming] scheme

  /**
   * Initial (static) security configuration.
   */
  private void initial(final SecurityConfiguration model) {
    model.addPrivilege(privilege(ALL, ALL, ALL));
    model.addPrivilege(privilege(ALL, ALL, BROWSE));
    model.addPrivilege(privilege(ALL, ALL, READ));
    model.addPrivilege(privilege(ALL, ALL, EDIT));
    model.addPrivilege(privilege(ALL, ALL, ADD));
    model.addPrivilege(privilege(ALL, ALL, DELETE));
  }

  /**
   * Add security configuration for given repository.
   */
  public void add(final Repository repository) {
    checkNotNull(repository);
    final String format = repository.getFormat().getValue();
    final String name = repository.getName();
    apply(new Mutator()
    {
      @Override
      public void apply(final SecurityConfiguration model) {
        // no per-repo repository-admin ADD action
        model.addPrivilege(privilege(format, name, ALL));
        model.addPrivilege(privilege(format, name, BROWSE));
        model.addPrivilege(privilege(format, name, READ));
        model.addPrivilege(privilege(format, name, EDIT));
        model.addPrivilege(privilege(format, name, DELETE));
      }
    });
  }

  /**
   * Remove security configuration for given repository.
   */
  public void remove(final Repository repository) {
    checkNotNull(repository);
    final String format = repository.getFormat().getValue();
    final String name = repository.getName();
    apply(new Mutator()
    {
      @Override
      public void apply(final SecurityConfiguration model) {
        // no per-repo repository-admin ADD action
        model.removePrivilege(id(format, name, ALL));
        model.removePrivilege(id(format, name, BROWSE));
        model.removePrivilege(id(format, name, READ));
        model.removePrivilege(id(format, name, EDIT));
        model.removePrivilege(id(format, name, DELETE));
      }
    });
  }
}
