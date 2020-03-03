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
package org.sonatype.nexus.repository.security.rest;

import java.util.Collection;

import org.sonatype.nexus.repository.security.RepositoryViewPrivilegeDescriptor;
import org.sonatype.nexus.security.privilege.Privilege;
import org.sonatype.nexus.security.privilege.rest.PrivilegeAction;

/**
 * @since 3.19
 */
public class ApiPrivilegeRepositoryView
    extends ApiPrivilegeWithRepository
{
  /**
   * for deserialization
   */
  private ApiPrivilegeRepositoryView() {
    super(RepositoryViewPrivilegeDescriptor.TYPE);
  }

  public ApiPrivilegeRepositoryView(final String name,
                                    final String description,
                                    final boolean readOnly,
                                    final String format,
                                    final String repository,
                                    final Collection<PrivilegeAction> actions)
  {
    super(RepositoryViewPrivilegeDescriptor.TYPE, name, description, readOnly, format, repository, actions);
  }

  public ApiPrivilegeRepositoryView(final Privilege privilege) {
    super(privilege);
  }
}
