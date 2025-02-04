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
public class TestSecurityContributor2
    implements SecurityContributor
{
  @Override
  public MemorySecurityConfiguration getContribution() {
    return new MemorySecurityConfiguration()
        .withPrivileges(
            new MemoryCPrivilegeBuilder("4-test")
                .type("method")
                .name("4-test")
                .description("")
                .property("method", "read")
                .property("permission", "/some/path4/")
                .build(),
            new MemoryCPrivilegeBuilder("5-test")
                .type("method")
                .name("5-test")
                .description("")
                .property("method", "read")
                .property("permission", "/some/path5/")
                .build(),
            new MemoryCPrivilegeBuilder("6-test")
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
