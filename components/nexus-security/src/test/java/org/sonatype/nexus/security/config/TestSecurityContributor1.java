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

import javax.inject.Singleton;

import org.sonatype.nexus.security.config.memory.MemoryCPrivilege.MemoryCPrivilegeBuilder;
import org.sonatype.nexus.security.config.memory.MemoryCRole;

@Singleton
public class TestSecurityContributor1
    implements SecurityContributor
{
  @Override
  public MemorySecurityConfiguration getContribution() {
    return new MemorySecurityConfiguration()
        .withPrivileges(
            new MemoryCPrivilegeBuilder("priv1")
                .type("method")
                .name("priv1")
                .description("")
                .property("method", "read")
                .property("permission", "priv1-ONE")
                .build(),
            new MemoryCPrivilegeBuilder("priv2")
                .type("method")
                .name("priv2")
                .description("")
                .property("method", "read")
                .property("permission", "priv2-TWO")
                .build(),
            new MemoryCPrivilegeBuilder("priv3")
                .type("method")
                .name("priv3")
                .description("")
                .property("method", "read")
                .property("permission", "priv3-THREE")
                .build(),
            new MemoryCPrivilegeBuilder("priv4")
                .type("method")
                .name("priv4")
                .description("")
                .property("method", "read")
                .property("permission", "priv4-FOUR")
                .build(),
            new MemoryCPrivilegeBuilder("priv5")
                .type("method")
                .name("priv5")
                .description("")
                .property("method", "read")
                .property("permission", "priv5-FIVE")
                .build())
        .withRoles(
            new MemoryCRole()
                .withId("anon")
                .withName("Test Anon Role")
                .withDescription("Test Anon Role Description")
                .withPrivileges("priv1")
                .withRoles("role2"),
            new MemoryCRole()
                .withId("other")
                .withName("")
                .withDescription("")
                .withPrivileges("priv2")
                .withRoles("role2"),
            new MemoryCRole()
                .withId("role1")
                .withName("role1")
                .withDescription("role1")
                .withPrivileges("priv1")
                .withRoles("role2"),
            new MemoryCRole()
                .withId("role2")
                .withName("role2")
                .withDescription("role2")
                .withPrivileges("priv1", "priv2")
                .withRoles("role3", "role4", "role5"),
            new MemoryCRole()
                .withId("role3")
                .withName("role3")
                .withDescription("role3")
                .withPrivileges("priv1", "priv2", "priv3")
                .withRoles("role4", "role5"),
            new MemoryCRole()
                .withId("role4")
                .withName("role4")
                .withDescription("role4")
                .withPrivileges("priv1", "priv2", "priv3", "priv4"),
            new MemoryCRole()
                .withId("role5")
                .withName("role5")
                .withDescription("role5")
                .withPrivileges("priv3", "priv4", "priv5"));
  }
}
