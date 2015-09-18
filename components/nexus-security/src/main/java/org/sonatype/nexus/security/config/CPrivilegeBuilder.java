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

import java.util.Arrays;

import com.google.common.base.Joiner;

import static com.google.common.base.Preconditions.checkState;

/**
 * Helper to build a {@link CPrivilege} instance.
 *
 * @since 3.0
 */
public class CPrivilegeBuilder
{
  private final CPrivilege model = new CPrivilege();

  public CPrivilegeBuilder type(final String type) {
    model.setType(type);
    return this;
  }

  public CPrivilegeBuilder id(final String id) {
    model.setId(id);
    return this;
  }

  public CPrivilegeBuilder name(final String name) {
    model.setName(name);
    return this;
  }

  public CPrivilegeBuilder description(final String description) {
    model.setDescription(description);
    return this;
  }

  public CPrivilegeBuilder readOnly(final boolean readOnly) {
    model.setReadOnly(readOnly);
    return this;
  }

  public CPrivilegeBuilder property(final String name, final String value) {
    model.setProperty(name, value);
    return this;
  }

  public CPrivilegeBuilder property(final String name, final Iterable<String> values) {
    return property(name, Joiner.on(',').join(values));
  }

  public CPrivilegeBuilder property(final String name, final String... values) {
    return property(name, Arrays.asList(values));
  }

  public CPrivilege create() {
    checkState(model.getType() != null, "Missing: type");
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
