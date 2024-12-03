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
package org.sonatype.nexus.security;

import org.sonatype.nexus.security.config.MemorySecurityConfiguration;
import org.sonatype.nexus.security.config.memory.MemoryCUser;
import org.sonatype.nexus.security.config.memory.MemoryCUserRoleMapping;

/**
 * @since 3.0
 */
public class BaseSecurityConfig
{
  public static MemorySecurityConfiguration get() {
    return new MemorySecurityConfiguration().withUsers(
        new MemoryCUser().withId("admin")
            .withPassword("f865b53623b121fd34ee5426c792e5c33af8c227")
            .withFirstName("Administrator")
            .withStatus("active")
            .withEmail("admin@example.org"),
        new MemoryCUser().withId("deployment")
            .withPassword("b2a0e378437817cebdf753d7dff3dd75483af9e0")
            .withFirstName("Deployment")
            .withLastName("User")
            .withStatus("active")
            .withEmail("deployment@example.org"),
        new MemoryCUser().withId("anonymous")
            .withPassword("0a92fab3230134cca6eadd9898325b9b2ae67998")
            .withFirstName("Nexus")
            .withLastName("Anonynmous User")
            .withStatus("active")
            .withEmail("anonymous@example.org"))
        .withUserRoleMappings(new MemoryCUserRoleMapping().withUserId("admin").withSource("default").withRoles("admin"),
            new MemoryCUserRoleMapping().withUserId("deployment")
                .withSource("default")
                .withRoles("deployment",
                    "repo-all-full"),
            new MemoryCUserRoleMapping().withUserId("anonymous")
                .withSource("default")
                .withRoles("anonymous",
                    "repo-all-read"));
  }
}
