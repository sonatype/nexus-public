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

import org.sonatype.nexus.repository.Format;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.security.config.MutableDynamicSecurityConfigurationResource;
import org.sonatype.nexus.security.config.SecurityConfiguration;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.sonatype.nexus.repository.security.BreadActions.ADD;
import static org.sonatype.nexus.repository.security.BreadActions.BROWSE;
import static org.sonatype.nexus.repository.security.BreadActions.DELETE;
import static org.sonatype.nexus.repository.security.BreadActions.EDIT;
import static org.sonatype.nexus.repository.security.BreadActions.READ;
import static org.sonatype.nexus.repository.security.RepositoryViewPrivilegeDescriptor.id;
import static org.sonatype.nexus.repository.security.RepositoryViewPrivilegeDescriptor.privilege;
import static org.sonatype.nexus.security.privilege.PrivilegeDescriptorSupport.ALL;

/**
 * Repository format security resource.
 *
 * @since 3.0
 */
public class RepositoryFormatSecurityConfigurationResource
    extends MutableDynamicSecurityConfigurationResource
{
  // NOTE: This is not ideal, but moving forward to allow sharing, refactor eventually

  private final Format format;

  public RepositoryFormatSecurityConfigurationResource(final Format format) {
    this.format = checkNotNull(format);

    // apply initial configuration
    apply(new Mutator()
    {
      @Override
      public void apply(final SecurityConfiguration model) {
        initial(model);
      }
    });
  }

  /**
   * Initial (static) security configuration.
   */
  private void initial(final SecurityConfiguration model) {
    String format = this.format.getValue();

    // add repository-view <format> ALL privileges
    model.addPrivilege(privilege(format, ALL, ALL));
    model.addPrivilege(privilege(format, ALL, BROWSE));
    model.addPrivilege(privilege(format, ALL, READ));
    model.addPrivilege(privilege(format, ALL, EDIT));
    model.addPrivilege(privilege(format, ALL, ADD));
    model.addPrivilege(privilege(format, ALL, DELETE));
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
        // add repository-view <format> <name> privileges
        model.addPrivilege(privilege(format, name, ALL));
        model.addPrivilege(privilege(format, name, BROWSE));
        model.addPrivilege(privilege(format, name, READ));
        model.addPrivilege(privilege(format, name, EDIT));
        model.addPrivilege(privilege(format, name, ADD));
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
        // remove repository-view <format> <name> privileges
        model.removePrivilege(id(format, name, ALL));
        model.removePrivilege(id(format, name, BROWSE));
        model.removePrivilege(id(format, name, READ));
        model.removePrivilege(id(format, name, EDIT));
        model.removePrivilege(id(format, name, ADD));
        model.removePrivilege(id(format, name, DELETE));
      }
    });
  }
}
