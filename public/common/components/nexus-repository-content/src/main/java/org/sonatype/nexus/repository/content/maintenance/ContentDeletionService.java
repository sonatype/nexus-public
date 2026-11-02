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
import org.sonatype.nexus.common.app.trash.TrashIdentifiers;
import org.sonatype.nexus.common.app.trash.TrashedEntityService;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.content.Asset;
import org.sonatype.nexus.repository.content.Component;
import org.sonatype.nexus.repository.content.store.InternalIds;
import org.sonatype.nexus.repository.content.store.TrashSchemaState;
import org.sonatype.nexus.repository.types.HostedType;
import org.sonatype.nexus.security.SecurityHelper;

import org.apache.shiro.authz.AuthorizationException;
import org.apache.shiro.subject.Subject;
import org.springframework.beans.factory.annotation.Autowired;

import static java.util.Objects.requireNonNull;

/**
 * Single decision point for user-requested component and asset deletion.
 *
 * <p>
 * Centralizes the trash-vs-hard-delete decision <em>and</em> the content {@code DELETE}
 * authorization so the REST API and both UIs behave identically (NEXUS-49147 / NEXUS-51733).
 * Previously each transport re-implemented the decision inline and the trash branch called the
 * trash service directly, bypassing the content-granular delete permission that
 * {@link MaintenanceService} enforces on the hard-delete path. Routing both paths through here closes
 * that bypass: the soft path performs the same {@code canDelete} check the hard path does.
 * </p>
 *
 * <p>
 * Only human-facing transports route through this service. System- and policy-driven deletions
 * (cleanup policies, scheduled tasks, format/maintenance code) continue to call
 * {@link MaintenanceService} directly and always hard-delete; only human-initiated deletes are
 * eligible for trash.
 * </p>
 */
@org.springframework.stereotype.Component
public class ContentDeletionService
{
  private final MaintenanceService maintenanceService;

  private final SecurityHelper securityHelper;

  /**
   * The component {@link TrashedEntityService}, or {@code null} when the trash plugin is not
   * present. When {@code null}, deletion always falls back to hard-delete.
   */
  private final TrashedEntityService componentTrashService;

  /**
   * The asset {@link TrashedEntityService}, or {@code null} when the trash plugin is not
   * present. When {@code null}, deletion always falls back to hard-delete.
   */
  private final TrashedEntityService assetTrashService;

  private final TrashFormatGate trashFormatGate;

  private final TrashSchemaState trashSchemaState;

  @Autowired
  public ContentDeletionService(
      final MaintenanceService maintenanceService,
      final SecurityHelper securityHelper,
      final List<TrashedEntityService> trashedEntityServices,
      final TrashFormatGate trashFormatGate,
      final TrashSchemaState trashSchemaState)
  {
    this.maintenanceService = requireNonNull(maintenanceService);
    this.securityHelper = requireNonNull(securityHelper);
    this.componentTrashService = findService(trashedEntityServices, "component");
    this.assetTrashService = findService(trashedEntityServices, "asset");
    this.trashFormatGate = requireNonNull(trashFormatGate);
    this.trashSchemaState = requireNonNull(trashSchemaState);
  }

  /**
   * Delete a component as requested by a user: trash when the feature is enabled and the
   * repository is hosted, otherwise hard-delete. The content {@code DELETE} permission is enforced on
   * both paths.
   *
   * @return the set of asset paths removed; empty for a trash
   */
  public Set<String> deleteComponent(final Repository repository, final Component component) {
    requireNonNull(repository);
    requireNonNull(component);

    if (shouldMoveToTrash(repository, componentTrashService)) {
      if (!maintenanceService.canDeleteComponent(repository, component)) {
        throw new AuthorizationException();
      }
      String externalId = InternalIds.toExternalId(InternalIds.internalComponentId(component)).getValue();
      componentTrashService.trash(encodeIdentifier(repository.getName(), externalId), trashContext());
      return Collections.emptySet();
    }
    return maintenanceService.deleteComponent(repository, component);
  }

  /**
   * Delete an asset as requested by a user: trash when the feature is enabled and the repository
   * is hosted, otherwise hard-delete. The content {@code DELETE} permission is enforced on both paths.
   *
   * @return the set of asset paths removed; empty for a trash
   */
  public Set<String> deleteAsset(final Repository repository, final Asset asset) {
    requireNonNull(repository);
    requireNonNull(asset);

    if (shouldMoveToTrash(repository, assetTrashService)) {
      if (!maintenanceService.canDeleteAsset(repository, asset)) {
        throw new AuthorizationException();
      }
      String externalId = InternalIds.toExternalId(InternalIds.internalAssetId(asset)).getValue();
      assetTrashService.trash(encodeIdentifier(repository.getName(), externalId), trashContext());
      return Collections.emptySet();
    }
    return maintenanceService.deleteAsset(repository, asset);
  }

  /**
   * Trashs {@code component} <em>only if</em> the repository is eligible (trash enabled,
   * hosted, format allow-listed, plugin present), enforcing the content {@code DELETE} permission.
   *
   * <p>
   * This is the entry point for <em>format client-protocol</em> delete paths (e.g. {@code npm unpublish},
   * {@code nuget delete}, docker/OCI registry {@code DELETE}). Unlike {@link #deleteComponent}, it does
   * <strong>not</strong> perform the hard delete itself: when the repository is not eligible it returns
   * {@code false} so the caller runs its own existing (hard) delete unchanged — guaranteeing zero
   * behavior change when trash is off.
   * </p>
   *
   * @return {@code true} if the component was trashed (caller should treat the delete as done);
   *         {@code false} if not eligible (caller must perform its normal hard delete)
   */
  public boolean trashComponentIfEligible(final Repository repository, final Component component) {
    requireNonNull(repository);
    requireNonNull(component);

    if (!shouldMoveToTrash(repository, componentTrashService)) {
      return false;
    }
    if (!maintenanceService.canDeleteComponent(repository, component)) {
      throw new AuthorizationException();
    }
    String externalId = InternalIds.toExternalId(InternalIds.internalComponentId(component)).getValue();
    componentTrashService.trash(encodeIdentifier(repository.getName(), externalId), trashContext());
    return true;
  }

  /**
   * Trashs {@code asset} <em>only if</em> the repository is eligible (trash enabled, hosted,
   * format allow-listed, plugin present), enforcing the content {@code DELETE} permission.
   *
   * <p>
   * Companion to {@link #trashComponentIfEligible} for asset-granular client-protocol deletes. When
   * not eligible it returns {@code false} so the caller runs its own existing (hard) delete unchanged.
   * </p>
   *
   * @return {@code true} if the asset was trashed; {@code false} if not eligible (caller must
   *         perform its normal hard delete)
   */
  public boolean trashAssetIfEligible(final Repository repository, final Asset asset) {
    requireNonNull(repository);
    requireNonNull(asset);

    if (!shouldMoveToTrash(repository, assetTrashService)) {
      return false;
    }
    if (!maintenanceService.canDeleteAsset(repository, asset)) {
      throw new AuthorizationException();
    }
    String externalId = InternalIds.toExternalId(InternalIds.internalAssetId(asset)).getValue();
    assetTrashService.trash(encodeIdentifier(repository.getName(), externalId), trashContext());
    return true;
  }

  private boolean shouldMoveToTrash(final Repository repository, final TrashedEntityService service) {
    // isTrashAvailable() requires the flag AND that the deleted_on columns actually exist. Gating on
    // schema (not just the flag) keeps a prematurely-enabled flag safe under ZDU: a new-version node can run
    // with the flag on before the deferred migration adds the columns; hard-delete until they exist rather
    // than attempting a trash write that would fail on the missing column.
    return trashSchemaState.isTrashAvailable()
        && service != null
        && HostedType.NAME.equals(repository.getType().getValue())
        && trashFormatGate.isEnabledForFormat(repository.getFormat().getValue());
  }

  private TrashContext trashContext() {
    Subject subject = securityHelper.subject();
    String deletedBy = subject != null && subject.getPrincipal() != null
        ? subject.getPrincipal().toString()
        : "system";
    return TrashContext.of(deletedBy);
  }

  private static TrashedEntityService findService(
      final List<TrashedEntityService> services,
      final String entityType)
  {
    return requireNonNull(services).stream()
        .filter(service -> entityType.equals(service.getEntityType()))
        .findFirst()
        .orElse(null);
  }

  /**
   * Encodes the trash identifier as base64-url("repositoryName:externalId"), matching the format
   * the trash services parse. Delegates to the shared SPI codec so the public and private sides
   * cannot drift (NEXUS-51733).
   */
  static String encodeIdentifier(final String repositoryName, final String externalId) {
    return TrashIdentifiers.encode(repositoryName, externalId);
  }
}
