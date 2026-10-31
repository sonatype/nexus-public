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

/**
 * Named capability decisions for the Browse page.
 *
 * Keeping them here (rather than as inline `type === 'group'` conditionals) gives
 * the rule one documented home shared by the delete action, the detail panel and
 * their tests.
 */

/**
 * Whether content may be deleted from a repository of this type at all.
 *
 * Deletion is a *content maintenance* operation: it needs the repository's
 * ContentMaintenanceFacet, which group repositories do not have — a delete
 * attempted against one fails server-side in
 * `MaintenanceServiceImpl.maintenanceFacet` with
 * `IllegalOperationException("Deleting from repository %s of type %s is not
 * supported")`.  Group members are deleted through their own hosted/proxy
 * repository instead.
 *
 * This mirrors Classic UI's authoritative gate in
 * `NX.coreui.mixin.ComponentUtils.updateDeleteButton`, which hides the Delete
 * button entirely unless `currentRepository.get('type') !== 'group'`.
 *
 * An unresolved type — still loading, or absent because the repository listing
 * could not be read — returns false: the action must never be offered
 * optimistically. Any other non-empty type returns true, which is what makes this
 * a `group` denylist rather than a `hosted`/`proxy` allowlist (see below). This is
 * a usability gate only — the server re-authorises every delete, and remains the
 * security boundary.
 *
 * The repository `Type` hierarchy also declares `virtual` (VirtualType), which is
 * vestigial: no recipe registers it, so the repositories API cannot report it for
 * any repository a user can create. The rule is kept as a `group` denylist rather
 * than a `hosted`/`proxy` allowlist so it stays byte-for-byte equivalent to the
 * Classic UI gate above; were `virtual` ever revived, both UIs would need the same
 * revisit rather than only this one silently diverging.
 *
 * @param repositoryType `type` from the repositories API ('hosted' | 'proxy' | 'group'),
 *                       or undefined while it is still resolving
 */
export function isDeletableRepositoryType(repositoryType: string | undefined): boolean {
  if (!repositoryType) {
    return false;
  }
  return repositoryType !== 'group';
}
