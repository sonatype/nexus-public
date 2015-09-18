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
package org.sonatype.nexus.security.config;

import static com.google.common.base.Preconditions.checkState;

/**
 * Helper to build a {@link CRole} instance.
 *
 * @since 3.0
 */
public class CRoleBuilder
{
  private final CRole model = new CRole();

  public CRoleBuilder id(final String id) {
    model.setId(id);
    return this;
  }

  public CRoleBuilder name(final String name) {
    model.setName(name);
    return this;
  }

  public CRoleBuilder description(final String description) {
    model.setDescription(description);
    return this;
  }

  public CRoleBuilder privilege(final String privilege) {
    model.addPrivilege(privilege);
    return this;
  }

  public CRoleBuilder role(final String role) {
    model.addRole(role);
    return this;
  }

  public CRoleBuilder readOnly(final boolean readOnly) {
    model.setReadOnly(readOnly);
    return this;
  }

  public CRole create() {
    checkState(model.getId() != null, "Missing: id");
    if (model.getName() == null) {
      model.setName(model.getId());
    }
    if (model.getDescription() == null) {
      model.setDescription(model.getId());
    }
    return model;
  }
}
