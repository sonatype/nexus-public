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
package org.sonatype.nexus.script.plugin.internal.security;

import java.util.List;
import java.util.Objects;

import org.sonatype.nexus.security.authz.WildcardPermission2;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Script permission.
 * Allows for fine-grained permissions on Scripts based on their name.
 *
 * @since 3.0
 */
public class ScriptPermission
    extends WildcardPermission2
{
  public static final String SYSTEM = "nexus";

  public static final String DOMAIN = "script";

  private final String name;

  private final List<String> actions;

  public ScriptPermission(String name, List<String> actions) {
    this.name = checkNotNull(name);
    this.actions = checkNotNull(actions);

    setParts(List.of(SYSTEM, DOMAIN, name), actions);
  }

  public String getName() {
    return name;
  }

  public List<String> getActions() {
    return actions;
  }

  @Override
  public boolean equals(final Object o) {
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    if (!super.equals(o)) {
      return false;
    }
    ScriptPermission that = (ScriptPermission) o;
    return Objects.equals(name, that.name) && Objects.equals(actions, that.actions);
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), name, actions);
  }
}
