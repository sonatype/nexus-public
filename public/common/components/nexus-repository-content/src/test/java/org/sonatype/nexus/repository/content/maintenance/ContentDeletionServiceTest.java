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
package org.sonatype.nexus.repository.content.maintenance;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.sonatype.nexus.common.app.softdelete.SoftDeleteContext;
import org.sonatype.nexus.common.app.softdelete.SoftDeleteFormatGate;
import org.sonatype.nexus.common.app.softdelete.SoftDeletedEntityService;
import org.sonatype.nexus.common.entity.EntityId;
import org.sonatype.nexus.repository.Format;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.Type;
import org.sonatype.nexus.repository.content.Asset;
import org.sonatype.nexus.repository.content.Component;
import org.sonatype.nexus.repository.content.store.InternalIds;
import org.sonatype.nexus.repository.content.store.SoftDeleteSchemaState;
import org.sonatype.nexus.repository.types.HostedType;
import org.sonatype.nexus.security.SecurityHelper;

import org.apache.shiro.authz.AuthorizationException;
import org.apache.shiro.subject.Subject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ContentDeletionServiceTest
{
  private static final String REPO = "my-repo";

  @Mock
  private MaintenanceService maintenanceService;

  @Mock
  private SecurityHelper securityHelper;

  @Mock
  private SoftDeletedEntityService componentSoftDeleteService;

  @Mock
  private SoftDeletedEntityService assetSoftDeleteService;

  @Mock
  private SoftDeleteSchemaState softDeleteSchemaState;

  @Mock
  private Repository repository;

  @Mock
  private Type type;

  @Mock
  private Format format;

  @Mock
  private Subject subject;

  @Mock
  private Component component;

  @Mock
  private Asset asset;

  @BeforeEach
  void setUp() {
    lenient().when(repository.getName()).thenReturn(REPO);
    lenient().when(repository.getType()).thenReturn(type);
    lenient().when(repository.getFormat()).thenReturn(format);
    lenient().when(format.getValue()).thenReturn("maven2");
    lenient().when(securityHelper.subject()).thenReturn(subject);
    lenient().when(subject.getPrincipal()).thenReturn("admin");
    lenient().when(componentSoftDeleteService.getEntityType()).thenReturn("component");
    lenient().when(assetSoftDeleteService.getEntityType()).thenReturn("asset");
  }

  private ContentDeletionService service(
      final boolean softDeleteEnabled,
      final List<SoftDeletedEntityService> services)
  {
    // Allowlist the setUp format (maven2) so "flag on" exercises the soft-delete path under the
    // fail-closed gate (an empty allowlist would hard-delete everything).
    return service(softDeleteEnabled, "maven2", services);
  }

  private ContentDeletionService service(
      final boolean softDeleteEnabled,
      final String formatsAllowlist,
      final List<SoftDeletedEntityService> services)
  {
    // Columns are assumed present in these tests, so schema-availability tracks the flag.
    lenient().when(softDeleteSchemaState.isSoftDeleteAvailable()).thenReturn(softDeleteEnabled);
    return new ContentDeletionService(maintenanceService, securityHelper, services,
        new SoftDeleteFormatGate(softDeleteEnabled, formatsAllowlist), softDeleteSchemaState);
  }

  // ---- component ----

  @Test
  void hostedComponentWithFlagOnIsSoftDeleted() {
    when(type.getValue()).thenReturn(HostedType.NAME);
    when(maintenanceService.canDeleteComponent(repository, component)).thenReturn(true);
    EntityId externalId = mock(EntityId.class);
    when(externalId.getValue()).thenReturn("abc");

    Set<String> result;
    try (MockedStatic<InternalIds> ids = mockStatic(InternalIds.class)) {
      ids.when(() -> InternalIds.internalComponentId(component)).thenReturn(5);
      ids.when(() -> InternalIds.toExternalId(5)).thenReturn(externalId);

      result = service(true, List.of(componentSoftDeleteService)).deleteComponent(repository, component);
    }

    assertTrue(result.isEmpty());
    verify(componentSoftDeleteService)
        .softDelete(eq(ContentDeletionService.encodeIdentifier(REPO, "abc")), any(SoftDeleteContext.class));
    verify(maintenanceService, never()).deleteComponent(repository, component);
  }

  @Test
  void hostedComponentWithFlagOffIsHardDeleted() {
    service(false, List.of(componentSoftDeleteService)).deleteComponent(repository, component);

    verify(maintenanceService).deleteComponent(repository, component);
    verify(componentSoftDeleteService, never()).softDelete(any(), any());
  }

  @Test
  void proxyComponentWithFlagOnIsHardDeleted() {
    when(type.getValue()).thenReturn("proxy");

    service(true, List.of(componentSoftDeleteService)).deleteComponent(repository, component);

    verify(maintenanceService).deleteComponent(repository, component);
    verify(componentSoftDeleteService, never()).softDelete(any(), any());
  }

  @Test
  void componentWithFlagOnButNoServiceIsHardDeleted() {
    service(true, Collections.emptyList()).deleteComponent(repository, component);

    verify(maintenanceService).deleteComponent(repository, component);
  }

  @Test
  void hostedComponentWithFlagOnButFormatNotAllowlistedIsHardDeleted() {
    when(type.getValue()).thenReturn(HostedType.NAME);
    when(format.getValue()).thenReturn("npm");

    // Soft-delete enabled, but only maven2 is allow-listed -> an npm repo hard-deletes (phased rollout).
    service(true, "maven2", List.of(componentSoftDeleteService)).deleteComponent(repository, component);

    verify(maintenanceService).deleteComponent(repository, component);
    verify(componentSoftDeleteService, never()).softDelete(any(), any());
  }

  @Test
  void hostedComponentWithFlagOnButSchemaNotMigratedIsHardDeleted() {
    // ZDU window: flag on and format allow-listed, but the deleted_on columns don't exist yet.
    // Must fall back to hard-delete rather than attempt a soft-delete write that would fail.
    when(softDeleteSchemaState.isSoftDeleteAvailable()).thenReturn(false);
    ContentDeletionService svc = new ContentDeletionService(maintenanceService, securityHelper,
        List.of(componentSoftDeleteService), new SoftDeleteFormatGate(true, "maven2"), softDeleteSchemaState);

    svc.deleteComponent(repository, component);

    verify(maintenanceService).deleteComponent(repository, component);
    verify(componentSoftDeleteService, never()).softDelete(any(), any());
  }

  @Test
  void hostedComponentWithFlagOnAndFormatAllowlistedIsSoftDeleted() {
    when(type.getValue()).thenReturn(HostedType.NAME);
    when(format.getValue()).thenReturn("maven2");
    when(maintenanceService.canDeleteComponent(repository, component)).thenReturn(true);
    EntityId externalId = mock(EntityId.class);
    when(externalId.getValue()).thenReturn("abc");

    try (MockedStatic<InternalIds> ids = mockStatic(InternalIds.class)) {
      ids.when(() -> InternalIds.internalComponentId(component)).thenReturn(5);
      ids.when(() -> InternalIds.toExternalId(5)).thenReturn(externalId);

      service(true, "npm,maven2", List.of(componentSoftDeleteService)).deleteComponent(repository, component);
    }

    verify(componentSoftDeleteService)
        .softDelete(eq(ContentDeletionService.encodeIdentifier(REPO, "abc")), any(SoftDeleteContext.class));
    verify(maintenanceService, never()).deleteComponent(repository, component);
  }

  @Test
  void componentSoftDeleteEnforcesDeletePermission() {
    when(type.getValue()).thenReturn(HostedType.NAME);
    when(maintenanceService.canDeleteComponent(repository, component)).thenReturn(false);

    assertThrows(AuthorizationException.class,
        () -> service(true, List.of(componentSoftDeleteService)).deleteComponent(repository, component));

    verify(componentSoftDeleteService, never()).softDelete(any(), any());
    verify(maintenanceService, never()).deleteComponent(repository, component);
  }

  // ---- asset ----

  @Test
  void hostedAssetWithFlagOnIsSoftDeleted() {
    when(type.getValue()).thenReturn(HostedType.NAME);
    when(maintenanceService.canDeleteAsset(repository, asset)).thenReturn(true);
    EntityId externalId = mock(EntityId.class);
    when(externalId.getValue()).thenReturn("def");

    Set<String> result;
    try (MockedStatic<InternalIds> ids = mockStatic(InternalIds.class)) {
      ids.when(() -> InternalIds.internalAssetId(asset)).thenReturn(7);
      ids.when(() -> InternalIds.toExternalId(7)).thenReturn(externalId);

      result = service(true, List.of(assetSoftDeleteService)).deleteAsset(repository, asset);
    }

    assertTrue(result.isEmpty());
    verify(assetSoftDeleteService)
        .softDelete(eq(ContentDeletionService.encodeIdentifier(REPO, "def")), any(SoftDeleteContext.class));
    verify(maintenanceService, never()).deleteAsset(repository, asset);
  }

  @Test
  void assetWithFlagOffIsHardDeleted() {
    service(false, List.of(assetSoftDeleteService)).deleteAsset(repository, asset);

    verify(maintenanceService).deleteAsset(repository, asset);
    verify(assetSoftDeleteService, never()).softDelete(any(), any());
  }

  @Test
  void assetSoftDeleteEnforcesDeletePermission() {
    when(type.getValue()).thenReturn(HostedType.NAME);
    when(maintenanceService.canDeleteAsset(repository, asset)).thenReturn(false);

    assertThrows(AuthorizationException.class,
        () -> service(true, List.of(assetSoftDeleteService)).deleteAsset(repository, asset));

    verify(assetSoftDeleteService, never()).softDelete(any(), any());
    verify(maintenanceService, never()).deleteAsset(repository, asset);
  }

  // ---- client-protocol entry points (softDelete*IfEligible) ----

  @Test
  void softDeleteAssetIfEligible_softDeletesAndReturnsTrueWhenEligible() {
    when(type.getValue()).thenReturn(HostedType.NAME);
    when(maintenanceService.canDeleteAsset(repository, asset)).thenReturn(true);
    EntityId externalId = mock(EntityId.class);
    when(externalId.getValue()).thenReturn("def");

    boolean result;
    try (MockedStatic<InternalIds> ids = mockStatic(InternalIds.class)) {
      ids.when(() -> InternalIds.internalAssetId(asset)).thenReturn(7);
      ids.when(() -> InternalIds.toExternalId(7)).thenReturn(externalId);

      result = service(true, List.of(assetSoftDeleteService)).softDeleteAssetIfEligible(repository, asset);
    }

    assertTrue(result);
    verify(assetSoftDeleteService)
        .softDelete(eq(ContentDeletionService.encodeIdentifier(REPO, "def")), any(SoftDeleteContext.class));
    // must NOT hard-delete: the caller falls back to its own hard delete only when this returns false
    verify(maintenanceService, never()).deleteAsset(repository, asset);
  }

  @Test
  void softDeleteAssetIfEligible_returnsFalseAndDoesNothingWhenFlagOff() {
    boolean result = service(false, List.of(assetSoftDeleteService)).softDeleteAssetIfEligible(repository, asset);

    assertFalse(result);
    verify(assetSoftDeleteService, never()).softDelete(any(), any());
    verify(maintenanceService, never()).deleteAsset(repository, asset);
  }

  @Test
  void softDeleteAssetIfEligible_returnsFalseWhenProxy() {
    when(type.getValue()).thenReturn("proxy");

    boolean result = service(true, List.of(assetSoftDeleteService)).softDeleteAssetIfEligible(repository, asset);

    assertFalse(result);
    verify(assetSoftDeleteService, never()).softDelete(any(), any());
  }

  @Test
  void softDeleteAssetIfEligible_returnsFalseWhenFormatNotAllowlisted() {
    when(type.getValue()).thenReturn(HostedType.NAME);
    when(format.getValue()).thenReturn("npm");

    boolean result =
        service(true, "maven2", List.of(assetSoftDeleteService)).softDeleteAssetIfEligible(repository, asset);

    assertFalse(result);
    verify(assetSoftDeleteService, never()).softDelete(any(), any());
  }

  @Test
  void softDeleteAssetIfEligible_enforcesDeletePermission() {
    when(type.getValue()).thenReturn(HostedType.NAME);
    when(maintenanceService.canDeleteAsset(repository, asset)).thenReturn(false);

    assertThrows(AuthorizationException.class,
        () -> service(true, List.of(assetSoftDeleteService)).softDeleteAssetIfEligible(repository, asset));

    verify(assetSoftDeleteService, never()).softDelete(any(), any());
  }

  @Test
  void softDeleteComponentIfEligible_softDeletesAndReturnsTrueWhenEligible() {
    when(type.getValue()).thenReturn(HostedType.NAME);
    when(maintenanceService.canDeleteComponent(repository, component)).thenReturn(true);
    EntityId externalId = mock(EntityId.class);
    when(externalId.getValue()).thenReturn("abc");

    boolean result;
    try (MockedStatic<InternalIds> ids = mockStatic(InternalIds.class)) {
      ids.when(() -> InternalIds.internalComponentId(component)).thenReturn(5);
      ids.when(() -> InternalIds.toExternalId(5)).thenReturn(externalId);

      result = service(true, List.of(componentSoftDeleteService)).softDeleteComponentIfEligible(repository, component);
    }

    assertTrue(result);
    verify(componentSoftDeleteService)
        .softDelete(eq(ContentDeletionService.encodeIdentifier(REPO, "abc")), any(SoftDeleteContext.class));
    verify(maintenanceService, never()).deleteComponent(repository, component);
  }

  @Test
  void softDeleteComponentIfEligible_returnsFalseWhenFlagOff() {
    boolean result =
        service(false, List.of(componentSoftDeleteService)).softDeleteComponentIfEligible(repository, component);

    assertFalse(result);
    verify(componentSoftDeleteService, never()).softDelete(any(), any());
    verify(maintenanceService, never()).deleteComponent(repository, component);
  }
}
