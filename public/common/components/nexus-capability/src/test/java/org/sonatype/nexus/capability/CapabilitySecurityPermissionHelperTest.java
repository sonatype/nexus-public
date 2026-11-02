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
package org.sonatype.nexus.capability;

import org.sonatype.nexus.security.SecurityHelper;
import org.sonatype.nexus.security.privilege.ApplicationPermission;

import org.apache.shiro.authz.AuthorizationException;
import org.apache.shiro.authz.Permission;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Tests {@link CapabilitySecurityPermissionHelper}, which gates security-sensitive capability types behind
 * {@code nexus:settings:update} (CVE-2026-14645 / NEXUS-54726).
 */
@ExtendWith(MockitoExtension.class)
public class CapabilitySecurityPermissionHelperTest
{
  @Mock
  private SecurityHelper securityHelper;

  @Mock
  private CapabilityDescriptorRegistry capabilityDescriptorRegistry;

  @Mock
  private CapabilityDescriptor capabilityDescriptor;

  @Captor
  private ArgumentCaptor<Permission> permissionCaptor;

  private CapabilitySecurityPermissionHelper underTest;

  @BeforeEach
  public void setUp() {
    underTest = new CapabilitySecurityPermissionHelper(securityHelper, capabilityDescriptorRegistry);
  }

  @Test
  public void ensurePermittedForDescriptor_checksSettingsUpdate_whenSensitive() {
    when(capabilityDescriptor.requiresSettingsUpdatePermission()).thenReturn(true);

    underTest.ensurePermittedForDescriptor(capabilityDescriptor);

    verify(securityHelper).ensurePermitted(permissionCaptor.capture());
    Permission permission = permissionCaptor.getValue();
    assertThat(permission, is(instanceOf(ApplicationPermission.class)));
    ApplicationPermission applicationPermission = (ApplicationPermission) permission;
    // Assert the concrete nexus:settings:update permission, not just implication (which any wildcard would pass).
    assertThat(applicationPermission.getDomain(), is("settings"));
    assertThat(applicationPermission.getActions(), contains("update"));
  }

  @Test
  public void ensurePermittedForDescriptor_doesNothing_whenNotSensitive() {
    when(capabilityDescriptor.requiresSettingsUpdatePermission()).thenReturn(false);

    underTest.ensurePermittedForDescriptor(capabilityDescriptor);

    verify(securityHelper, never()).ensurePermitted(any(Permission.class));
  }

  @Test
  public void ensurePermittedForDescriptor_doesNothing_whenDescriptorNull() {
    underTest.ensurePermittedForDescriptor(null);

    verifyNoInteractions(securityHelper);
  }

  @Test
  public void ensurePermittedForType_resolvesDescriptor_andChecks_whenSensitive() {
    CapabilityType type = CapabilityType.capabilityType("rutauth");
    when(capabilityDescriptorRegistry.get(type)).thenReturn(capabilityDescriptor);
    when(capabilityDescriptor.requiresSettingsUpdatePermission()).thenReturn(true);

    underTest.ensurePermittedForType(type);

    verify(securityHelper).ensurePermitted(any(Permission.class));
  }

  @Test
  public void ensurePermittedForType_doesNothing_whenTypeNull() {
    underTest.ensurePermittedForType(null);

    verifyNoInteractions(securityHelper);
    verifyNoInteractions(capabilityDescriptorRegistry);
  }

  @Test
  public void ensurePermittedForDescriptor_propagatesAuthorizationException() {
    when(capabilityDescriptor.requiresSettingsUpdatePermission()).thenReturn(true);
    doThrow(new AuthorizationException("denied")).when(securityHelper)
        .ensurePermitted(any(Permission.class));

    assertThrows(AuthorizationException.class, () -> underTest.ensurePermittedForDescriptor(capabilityDescriptor));
  }

  @Test
  public void ensurePermittedForType_propagatesAuthorizationException() {
    CapabilityType type = CapabilityType.capabilityType("rutauth");
    when(capabilityDescriptorRegistry.get(type)).thenReturn(capabilityDescriptor);
    when(capabilityDescriptor.requiresSettingsUpdatePermission()).thenReturn(true);
    doThrow(new AuthorizationException("denied")).when(securityHelper)
        .ensurePermitted(any(Permission.class));

    assertThrows(AuthorizationException.class, () -> underTest.ensurePermittedForType(type));
  }
}
