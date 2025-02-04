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
import org.sonatype.nexus.security.config.memory.MemoryCUser;
import org.sonatype.nexus.security.config.memory.MemoryCUserRoleMapping;

/**
 * @since 3.0
 */
public class DefaultSecurityConfigurationCleanerTestSecurity
{

  public static MemorySecurityConfiguration securityModel() {
    return new MemorySecurityConfiguration()
        .withUsers(
            new MemoryCUser()
                .withId("user1")
                .withPassword("user1")
                .withFirstName("first1")
                .withLastName("last1")
                .withStatus("active")
                .withEmail("email"),
            new MemoryCUser()
                .withId("user2")
                .withPassword("user2")
                .withFirstName("first2")
                .withLastName("last2")
                .withStatus("active")
                .withEmail("email"),
            new MemoryCUser()
                .withId("user3")
                .withPassword("user3")
                .withFirstName("first3")
                .withLastName("last3")
                .withStatus("active")
                .withEmail("email"),
            new MemoryCUser()
                .withId("user4")
                .withPassword("user4")
                .withFirstName("first4")
                .withLastName("last4")
                .withStatus("active")
                .withEmail("email"),
            new MemoryCUser()
                .withId("user5")
                .withPassword("user5")
                .withFirstName("first5")
                .withLastName("last5")
                .withStatus("active")
                .withEmail("email"))
        .withUserRoleMappings(
            new MemoryCUserRoleMapping()
                .withUserId("user1")
                .withSource("default")
                .withRoles("role1"),
            new MemoryCUserRoleMapping()
                .withUserId("user2")
                .withSource("default")
                .withRoles("role1", "role2"),
            new MemoryCUserRoleMapping()
                .withUserId("user3")
                .withSource("default")
                .withRoles("role1", "role2", "role3"),
            new MemoryCUserRoleMapping()
                .withUserId("user4")
                .withSource("default")
                .withRoles("role1", "role2", "role3", "role4"),
            new MemoryCUserRoleMapping()
                .withUserId("user5")
                .withSource("default")
                .withRoles("role1", "role2", "role3", "role4", "role5"))
        .withPrivileges(
            new MemoryCPrivilegeBuilder("priv1")
                .type("method")
                .name("priv1")
                .description("")
                .property("method", "read")
                .property("permission", "/some/path/")
                .build(),
            new MemoryCPrivilegeBuilder("priv2")
                .type("method")
                .name("priv2")
                .description("")
                .property("method", "read")
                .property("permission", "/some/path/")
                .build(),
            new MemoryCPrivilegeBuilder("priv3")
                .type("method")
                .name("priv3")
                .description("")
                .property("method", "read")
                .property("permission", "/some/path/")
                .build(),
            new MemoryCPrivilegeBuilder("priv4")
                .type("method")
                .name("priv4")
                .description("")
                .property("method", "read")
                .property("permission", "/some/path/")
                .build(),
            new MemoryCPrivilegeBuilder("priv5")
                .type("method")
                .name("priv5")
                .description("")
                .property("method", "read")
                .property("permission", "/some/path/")
                .build())
        .withRoles(
            new MemoryCRole()
                .withId("role1")
                .withName("role1")
                .withDescription("role1")
                .withPrivileges("priv1")
                .withRoles("role2", "role3", "role4", "role5"),
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
                .withPrivileges("priv1", "priv2", "priv3", "priv4")
                .withRoles("role5"),
            new MemoryCRole()
                .withId("role5")
                .withName("role5")
                .withDescription("role5")
                .withPrivileges("priv1", "priv2", "priv3", "priv4", "priv5"));
  }

}
