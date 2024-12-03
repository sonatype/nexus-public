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

import org.sonatype.nexus.security.config.memory.MemoryCPrivilege.MemoryCPrivilegeBuilder;
import org.sonatype.nexus.security.config.memory.MemoryCRole;

public class TestSecurityContributor3
    implements SecurityContributor
{
  private static int instanceCount = 1;

  private String privId = "priv-" + instanceCount++;

  public String getId() {
    return privId;
  }

  @Override
  public MemorySecurityConfiguration getContribution() {
    return new MemorySecurityConfiguration()
        .withPrivileges(
            new MemoryCPrivilegeBuilder("4-test-" + privId)
                .type("method")
                .name("4-test")
                .description("")
                .property("method", "read")
                .property("permission", "/some/path4/")
                .build(),
            new MemoryCPrivilegeBuilder("5-test-" + privId)
                .type("method")
                .name("5-test")
                .description("")
                .property("method", "read")
                .property("permission", "/some/path5/")
                .build(),
            new MemoryCPrivilegeBuilder("6-test-" + privId)
                .type("method")
                .name("6-test")
                .description("")
                .property("method", "read")
                .property("permission", "/some/path6/")
                .build())
        .withRoles(
            new MemoryCRole()
                .withId("anon")
                .withName("")
                .withDescription("")
                .withPrivileges("4-test")
                .withRoles("other"),
            new MemoryCRole()
                .withId("other")
                .withName("Other Role")
                .withDescription("Other Role Description")
                .withPrivileges("6-test"));
  }
}
