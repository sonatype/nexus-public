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
package org.sonatype.nexus.plugins.defaultrole.internal;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import jakarta.validation.groups.Default;

import org.sonatype.nexus.security.role.RoleExistsString;
import org.sonatype.nexus.validation.group.Create;
import org.sonatype.nexus.validation.group.CreateNonExposed;
import org.sonatype.nexus.validation.group.Load;
import org.sonatype.nexus.validation.group.Update;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests that {@link DefaultRoleCapabilityConfiguration} validation groups are correctly scoped so
 * that existence checks do not fire during capability LOAD (startup), preventing startup failures
 * when the referenced role is missing. See NEXUS-52998.
 */
public class DefaultRoleCapabilityConfigurationTest
{
  @Test
  public void roleExistsConstraint_isNotInDefaultOrLoadGroup() throws Exception {
    Field roleField = DefaultRoleCapabilityConfiguration.class.getDeclaredField("role");
    RoleExistsString annotation = roleField.getAnnotation(RoleExistsString.class);

    assertThat(annotation).isNotNull();

    List<Class<?>> groups = Arrays.asList(annotation.groups());

    // Must NOT be in Default or Load groups — these fire during startup LOAD validation
    assertThat(groups).doesNotContain(Default.class);
    assertThat(groups).doesNotContain(Load.class);
  }

  @Test
  public void roleExistsConstraint_isInCreateAndUpdateGroups() throws Exception {
    Field roleField = DefaultRoleCapabilityConfiguration.class.getDeclaredField("role");
    RoleExistsString annotation = roleField.getAnnotation(RoleExistsString.class);

    assertThat(annotation).isNotNull();

    List<Class<?>> groups = Arrays.asList(annotation.groups());

    // Must fire during user-initiated create/update operations
    assertThat(groups).contains(Create.class, CreateNonExposed.class, Update.class);
  }

  @Test
  public void configuration_storesRoleFromProperties() {
    Map<String, String> props = Map.of(DefaultRoleCapabilityConfiguration.P_ROLE, "nx-admin");

    DefaultRoleCapabilityConfiguration config = new DefaultRoleCapabilityConfiguration(props);

    assertThat(config.getRole()).isEqualTo("nx-admin");
  }

  @Test
  public void configuration_handlesAbsentRole() {
    DefaultRoleCapabilityConfiguration config = new DefaultRoleCapabilityConfiguration(Map.of());

    assertThat(config.getRole()).isNull();
  }
}
