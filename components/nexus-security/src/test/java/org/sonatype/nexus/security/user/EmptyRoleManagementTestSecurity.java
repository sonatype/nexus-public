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
package org.sonatype.nexus.security.user;

import org.sonatype.nexus.security.config.MemorySecurityConfiguration;
import org.sonatype.nexus.security.config.memory.MemoryCPrivilege.MemoryCPrivilegeBuilder;
import org.sonatype.nexus.security.config.memory.MemoryCRole;
import org.sonatype.nexus.security.config.memory.MemoryCUser;
import org.sonatype.nexus.security.config.memory.MemoryCUserRoleMapping;

/**
 * @since 3.0
 */
public class EmptyRoleManagementTestSecurity
{

  public static MemorySecurityConfiguration securityModel() {
    return new MemorySecurityConfiguration()
        .withUsers(
            new MemoryCUser()
                .withId("admin")
                .withPassword("f865b53623b121fd34ee5426c792e5c33af8c227")
                .withFirstName("Administrator")
                .withStatus("active")
                .withEmail("admin@example.org"),
            new MemoryCUser()
                .withId("test-user")
                .withPassword("b2a0e378437817cebdf753d7dff3dd75483af9e0")
                .withFirstName("Test User")
                .withStatus("active")
                .withEmail("test-user@example.org"),
            new MemoryCUser()
                .withId("test-user-with-empty-role")
                .withPassword("b2a0e378437817cebdf753d7dff3dd75483af9e0")
                .withFirstName("Test User With Empty Role")
                .withStatus("active")
                .withEmail("test-user-with-empty-role@example.org"),
            new MemoryCUser()
                .withId("anonymous")
                .withPassword("0a92fab3230134cca6eadd9898325b9b2ae67998")
                .withFirstName("Anonynmous User")
                .withStatus("active")
                .withEmail("anonymous@example.org"))
        .withUserRoleMappings(
            new MemoryCUserRoleMapping()
                .withUserId("other-user")
                .withSource("other-realm")
                .withRoles("role2", "role3"),
            new MemoryCUserRoleMapping()
                .withUserId("admin")
                .withSource("default")
                .withRoles("role1"),
            new MemoryCUserRoleMapping()
                .withUserId("test-user")
                .withSource("default")
                .withRoles("role1", "role2"),
            new MemoryCUserRoleMapping()
                .withUserId("test-user-with-empty-role")
                .withSource("default")
                .withRoles("empty-role", "role1", "role2"),
            new MemoryCUserRoleMapping()
                .withUserId("anonymous")
                .withSource("default")
                .withRoles("role2"))
        .withPrivileges(
            new MemoryCPrivilegeBuilder("1")
                .type("method")
                .name("1")
                .description("")
                .property("method", "read")
                .property("permission", "/some/path1/")
                .build(),
            new MemoryCPrivilegeBuilder("2")
                .type("method")
                .name("2")
                .description("")
                .property("method", "read")
                .property("permission", "/some/path2/")
                .build(),
            new MemoryCPrivilegeBuilder("3")
                .type("method")
                .name("3")
                .description("")
                .property("method", "read")
                .property("permission", "/some/path/")
                .build(),
            new MemoryCPrivilegeBuilder("4")
                .type("method")
                .name("4")
                .description("")
                .property("method", "read")
                .property("permission", "/some/path/")
                .build())
        .withRoles(
            new MemoryCRole()
                .withId("role1")
                .withName("RoleOne")
                .withDescription("Role One")
                .withPrivileges("1", "2"),
            new MemoryCRole()
                .withId("role2")
                .withName("RoleTwo")
                .withDescription("Role Two")
                .withPrivileges("3", "4"),
            new MemoryCRole()
                .withId("role3")
                .withName("RoleThree")
                .withDescription("Role Three")
                .withPrivileges("1", "4"),
            new MemoryCRole()
                .withId("empty-role")
                .withName("Empty Role")
                .withDescription("Empty Role"));
  }

}
