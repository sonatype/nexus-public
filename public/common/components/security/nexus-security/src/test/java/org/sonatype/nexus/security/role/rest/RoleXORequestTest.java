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
package org.sonatype.nexus.security.role.rest;

import java.util.Collections;

import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;

class RoleXORequestTest
{
  @Test
  void equals_sameFields_returnsTrue() {
    RoleXORequest a = build("id", "name", "desc");
    RoleXORequest b = build("id", "name", "desc");

    assertThat(a.equals(b), is(true));
  }

  @Test
  void equals_differentId_returnsFalse() {
    RoleXORequest a = build("id1", "name", "desc");
    RoleXORequest b = build("id2", "name", "desc");

    assertThat(a.equals(b), is(false));
  }

  @Test
  void equals_differentName_returnsFalse() {
    RoleXORequest a = build("id", "name-a", "desc");
    RoleXORequest b = build("id", "name-b", "desc");

    assertThat(a.equals(b), is(false));
  }

  @Test
  void equals_notRoleXORequest_returnsFalse() {
    RoleXORequest a = build("id", "name", "desc");

    assertThat(a.equals("not-a-role"), is(false));
  }

  @Test
  void hashCode_equalObjects_sameHashCode() {
    RoleXORequest a = build("id", "name", "desc");
    RoleXORequest b = build("id", "name", "desc");

    assertThat(a.hashCode(), is(b.hashCode()));
  }

  @Test
  void toString_includesIdAndName() {
    RoleXORequest r = build("my-id", "my-name", "my-desc");

    assertThat(r.toString(), containsString("id: my-id"));
    assertThat(r.toString(), containsString("name: my-name"));
  }

  private RoleXORequest build(final String id, final String name, final String description) {
    RoleXORequest r = new RoleXORequest();
    r.setId(id);
    r.setName(name);
    r.setDescription(description);
    r.setRoles(Collections.emptySet());
    r.setPrivileges(Collections.emptySet());
    return r;
  }
}
