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

import org.sonatype.nexus.common.app.trash.TrashContext;
import org.sonatype.nexus.common.app.trash.TrashFormatGate;
import org.sonatype.nexus.common.app.trash.TrashedEntityService;
import org.sonatype.nexus.common.entity.EntityId;
import org.sonatype.nexus.repository.Format;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.Type;
import org.sonatype.nexus.repository.content.Asset;
import org.sonatype.nexus.repository.content.Component;
import org.sonatype.nexus.repository.content.store.InternalIds;
import org.sonatype.nexus.repository.content.store.TrashSchemaState;
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
  private TrashedEntityService componentTrashService;

  @Mock
  private TrashedEntityService assetTrashService;

  @Mock
  private TrashSchemaState trashSchemaState;

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
    lenient().when(componentTrashService.getEntityType()).thenReturn("component");
    lenient().when(assetTrashService.getEntityType()).thenReturn("asset");
  }

  private ContentDeletionService service(
      final boolean trashEnabled,
      final List<TrashedEntityService> services)
  {
    // Allowlist the setUp format (maven2) so "flag on" exercises the trash path under the
    // fail-closed gate (an empty allowlist would hard-delete everything).
    return service(trashEnabled, "maven2", services);
  }

  private ContentDeletionService service(
      final boolean trashEnabled,
      final String formatsAllowlist,
      final List<TrashedEntityService> services)
  {
    // Columns are assumed present in these tests, so schema-availability tracks the flag.
    lenient().when(trashSchemaState.isTrashAvailable()).thenReturn(trashEnabled);
    return new ContentDeletionService(maintenanceService, securityHelper, services,
        new TrashFormatGate(trashEnabled, formatsAllowlist), trashSchemaState);
  }

  // ---- component ----

  @Test
  void hostedComponentWithFlagOnIsTrashed() {
    when(type.getValue()).thenReturn(HostedType.NAME);
    when(maintenanceService.canDeleteComponent(repository, component)).thenReturn(true);
    EntityId externalId = mock(EntityId.class);
    when(externalId.getValue()).thenReturn("abc");

    Set<String> result;
    try (MockedStatic<InternalIds> ids = mockStatic(InternalIds.class)) {
      ids.when(() -> InternalIds.internalComponentId(component)).thenReturn(5);
      ids.when(() -> InternalIds.toExternalId(5)).thenReturn(externalId);

      result = service(true, List.of(componentTrashService)).deleteComponent(repository, component);
    }

    assertTrue(result.isEmpty());
    verify(componentTrashService)
        .trash(eq(ContentDeletionService.encodeIdentifier(REPO, "abc")), any(TrashContext.class));
    verify(maintenanceService, never()).deleteComponent(repository, component);
  }

  @Test
  void hostedComponentWithFlagOffIsHardDeleted() {
    service(false, List.of(componentTrashService)).deleteComponent(repository, component);

    verify(maintenanceService).deleteComponent(repository, component);
    verify(componentTrashService, never()).trash(any(), any());
  }

  @Test
  void proxyComponentWithFlagOnIsHardDeleted() {
    when(type.getValue()).thenReturn("proxy");

    service(true, List.of(componentTrashService)).deleteComponent(repository, component);

    verify(maintenanceService).deleteComponent(repository, component);
    verify(componentTrashService, never()).trash(any(), any());
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

    // Trash enabled, but only maven2 is allow-listed -> an npm repo hard-deletes (phased rollout).
    service(true, "maven2", List.of(componentTrashService)).deleteComponent(repository, component);

    verify(maintenanceService).deleteComponent(repository, component);
    verify(componentTrashService, never()).trash(any(), any());
  }

  @Test
  void hostedComponentWithFlagOnButSchemaNotMigratedIsHardDeleted() {
    // ZDU window: flag on and format allow-listed, but the deleted_on columns don't exist yet.
    // Must fall back to hard-delete rather than attempt a trash write that would fail.
    when(trashSchemaState.isTrashAvailable()).thenReturn(false);
    ContentDeletionService svc = new ContentDeletionService(maintenanceService, securityHelper,
        List.of(componentTrashService), new TrashFormatGate(true, "maven2"), trashSchemaState);

    svc.deleteComponent(repository, component);

    verify(maintenanceService).deleteComponent(repository, component);
    verify(componentTrashService, never()).trash(any(), any());
  }

  @Test
  void hostedComponentWithFlagOnAndFormatAllowlistedIsTrashed() {
    when(type.getValue()).thenReturn(HostedType.NAME);
    when(format.getValue()).thenReturn("maven2");
    when(maintenanceService.canDeleteComponent(repository, component)).thenReturn(true);
    EntityId externalId = mock(EntityId.class);
    when(externalId.getValue()).thenReturn("abc");

    try (MockedStatic<InternalIds> ids = mockStatic(InternalIds.class)) {
      ids.when(() -> InternalIds.internalComponentId(component)).thenReturn(5);
      ids.when(() -> InternalIds.toExternalId(5)).thenReturn(externalId);

      service(true, "npm,maven2", List.of(componentTrashService)).deleteComponent(repository, component);
    }

    verify(componentTrashService)
        .trash(eq(ContentDeletionService.encodeIdentifier(REPO, "abc")), any(TrashContext.class));
    verify(maintenanceService, never()).deleteComponent(repository, component);
  }

  @Test
  void componentTrashEnforcesDeletePermission() {
    when(type.getValue()).thenReturn(HostedType.NAME);
    when(maintenanceService.canDeleteComponent(repository, component)).thenReturn(false);

    assertThrows(AuthorizationException.class,
        () -> service(true, List.of(componentTrashService)).deleteComponent(repository, component));

    verify(componentTrashService, never()).trash(any(), any());
    verify(maintenanceService, never()).deleteComponent(repository, component);
  }

  // ---- asset ----

  @Test
  void hostedAssetWithFlagOnIsTrashed() {
    when(type.getValue()).thenReturn(HostedType.NAME);
    when(maintenanceService.canDeleteAsset(repository, asset)).thenReturn(true);
    EntityId externalId = mock(EntityId.class);
    when(externalId.getValue()).thenReturn("def");

    Set<String> result;
    try (MockedStatic<InternalIds> ids = mockStatic(InternalIds.class)) {
      ids.when(() -> InternalIds.internalAssetId(asset)).thenReturn(7);
      ids.when(() -> InternalIds.toExternalId(7)).thenReturn(externalId);

      result = service(true, List.of(assetTrashService)).deleteAsset(repository, asset);
    }

    assertTrue(result.isEmpty());
    verify(assetTrashService)
        .trash(eq(ContentDeletionService.encodeIdentifier(REPO, "def")), any(TrashContext.class));
    verify(maintenanceService, never()).deleteAsset(repository, asset);
  }

  @Test
  void assetWithFlagOffIsHardDeleted() {
    service(false, List.of(assetTrashService)).deleteAsset(repository, asset);

    verify(maintenanceService).deleteAsset(repository, asset);
    verify(assetTrashService, never()).trash(any(), any());
  }

  @Test
  void assetTrashEnforcesDeletePermission() {
    when(type.getValue()).thenReturn(HostedType.NAME);
    when(maintenanceService.canDeleteAsset(repository, asset)).thenReturn(false);

    assertThrows(AuthorizationException.class,
        () -> service(true, List.of(assetTrashService)).deleteAsset(repository, asset));

    verify(assetTrashService, never()).trash(any(), any());
    verify(maintenanceService, never()).deleteAsset(repository, asset);
  }

  // ---- client-protocol entry points (trash*IfEligible) ----

  @Test
  void trashAssetIfEligible_trashsAndReturnsTrueWhenEligible() {
    when(type.getValue()).thenReturn(HostedType.NAME);
    when(maintenanceService.canDeleteAsset(repository, asset)).thenReturn(true);
    EntityId externalId = mock(EntityId.class);
    when(externalId.getValue()).thenReturn("def");

    boolean result;
    try (MockedStatic<InternalIds> ids = mockStatic(InternalIds.class)) {
      ids.when(() -> InternalIds.internalAssetId(asset)).thenReturn(7);
      ids.when(() -> InternalIds.toExternalId(7)).thenReturn(externalId);

      result = service(true, List.of(assetTrashService)).trashAssetIfEligible(repository, asset);
    }

    assertTrue(result);
    verify(assetTrashService)
        .trash(eq(ContentDeletionService.encodeIdentifier(REPO, "def")), any(TrashContext.class));
    // must NOT hard-delete: the caller falls back to its own hard delete only when this returns false
    verify(maintenanceService, never()).deleteAsset(repository, asset);
  }

  @Test
  void trashAssetIfEligible_returnsFalseAndDoesNothingWhenFlagOff() {
    boolean result = service(false, List.of(assetTrashService)).trashAssetIfEligible(repository, asset);

    assertFalse(result);
    verify(assetTrashService, never()).trash(any(), any());
    verify(maintenanceService, never()).deleteAsset(repository, asset);
  }

  @Test
  void trashAssetIfEligible_returnsFalseWhenProxy() {
    when(type.getValue()).thenReturn("proxy");

    boolean result = service(true, List.of(assetTrashService)).trashAssetIfEligible(repository, asset);

    assertFalse(result);
    verify(assetTrashService, never()).trash(any(), any());
  }

  @Test
  void trashAssetIfEligible_returnsFalseWhenFormatNotAllowlisted() {
    when(type.getValue()).thenReturn(HostedType.NAME);
    when(format.getValue()).thenReturn("npm");

    boolean result =
        service(true, "maven2", List.of(assetTrashService)).trashAssetIfEligible(repository, asset);

    assertFalse(result);
    verify(assetTrashService, never()).trash(any(), any());
  }

  @Test
  void trashAssetIfEligible_enforcesDeletePermission() {
    when(type.getValue()).thenReturn(HostedType.NAME);
    when(maintenanceService.canDeleteAsset(repository, asset)).thenReturn(false);

    assertThrows(AuthorizationException.class,
        () -> service(true, List.of(assetTrashService)).trashAssetIfEligible(repository, asset));

    verify(assetTrashService, never()).trash(any(), any());
  }

  @Test
  void trashComponentIfEligible_trashsAndReturnsTrueWhenEligible() {
    when(type.getValue()).thenReturn(HostedType.NAME);
    when(maintenanceService.canDeleteComponent(repository, component)).thenReturn(true);
    EntityId externalId = mock(EntityId.class);
    when(externalId.getValue()).thenReturn("abc");

    boolean result;
    try (MockedStatic<InternalIds> ids = mockStatic(InternalIds.class)) {
      ids.when(() -> InternalIds.internalComponentId(component)).thenReturn(5);
      ids.when(() -> InternalIds.toExternalId(5)).thenReturn(externalId);

      result = service(true, List.of(componentTrashService)).trashComponentIfEligible(repository, component);
    }

    assertTrue(result);
    verify(componentTrashService)
        .trash(eq(ContentDeletionService.encodeIdentifier(REPO, "abc")), any(TrashContext.class));
    verify(maintenanceService, never()).deleteComponent(repository, component);
  }

  @Test
  void trashComponentIfEligible_returnsFalseWhenFlagOff() {
    boolean result =
        service(false, List.of(componentTrashService)).trashComponentIfEligible(repository, component);

    assertFalse(result);
    verify(componentTrashService, never()).trash(any(), any());
    verify(maintenanceService, never()).deleteComponent(repository, component);
  }
}
