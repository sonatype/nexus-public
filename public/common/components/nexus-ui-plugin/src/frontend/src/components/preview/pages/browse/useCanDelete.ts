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

import { useEffect, useRef, useState } from 'react';

import { ExtJS } from '../../../../interface/ExtJS';

import { canDeleteAsset, canDeleteComponent, canDeleteFolder } from './browse.api';
import type { ComponentXO } from './browse.types';
import type { ComponentData } from './detail/DetailPanel';
import type { BrowseNode } from './tree/browse-tree.types';

/**
 * Reactively resolves whether the current user can delete the selected browse node.
 *
 * Dispatches on node type and calls the corresponding @DirectMethod on
 * coreui_Component — the same server API Classic UI's
 * mixin/ComponentUtils.updateDeleteButton relies on. The server evaluates the
 * concrete repository/format/asset (including content-selector grants), so users
 * with specific-scope delete permissions are correctly recognised.
 *
 * Do NOT replace with a client-side ExtJS.checkPermission wildcard check.
 * Requesting a wildcard permission against a specific-scope grant returns false
 * under Shiro's WildcardPermission matching, regressing legitimate partial-
 * permission deleters (NEXUS-53861).
 *
 * Returns false while resolving, on error, or for unauthenticated users. Cached
 * per node so re-selection / tab switches don't re-fire the network call.
 *
 * @param selectedNode        Currently selected browse tree node, or null
 * @param selectedRepository  Currently selected repository name, or undefined
 * @param componentData       Component metadata (required for component nodes;
 *                            the server signature takes a JSON-stringified XO)
 */
export function useCanDelete(
  selectedNode: BrowseNode | null,
  selectedRepository: string | undefined,
  componentData: ComponentData | null | undefined,
): boolean {
  const user = ExtJS.useUser();
  const authenticated = Boolean(user?.authenticated);
  const cacheRef = useRef<Map<string, boolean>>(new Map());
  const [canDelete, setCanDelete] = useState<boolean>(false);

  useEffect(() => {
    if (!authenticated || !selectedNode || !selectedRepository) {
      setCanDelete(false);
      return;
    }

    // Server signature for canDeleteComponent needs the full XO. Wait for the
    // detail-load machine to populate it before firing the preflight.
    if (selectedNode.type === 'component' && !componentData) {
      setCanDelete(false);
      return;
    }

    const cacheKey = `${selectedNode.type}:${selectedNode.id}:${selectedRepository}`;
    const cached = cacheRef.current.get(cacheKey);
    if (cached !== undefined) {
      setCanDelete(cached);
      return;
    }

    setCanDelete(false);

    let permissionPromise: Promise<boolean>;
    switch (selectedNode.type) {
      case 'asset':
        if (!selectedNode.assetId) return;
        permissionPromise = canDeleteAsset(selectedNode.assetId, selectedRepository);
        break;
      case 'component': {
        // Use the raw browse-node componentId (the internal id, e.g. "4f1bbcdd") for the preflight,
        // NOT componentData.id. componentData comes from the REST v1 API, whose id is the
        // base64(repository:internalId) RepositoryItemIDXO form; the ExtDirect coreui_Component
        // .canDeleteComponent server method expects the raw internal id (same value ExtDirect
        // readComponent returns) and does a numeric parse on it, so the base64 form throws
        // NumberFormatException server-side → preflight false → the Delete button is wrongly hidden
        // (NEXUS-54171).
        //
        // If the raw componentId is absent we cannot run the preflight correctly — the only other
        // id we hold is componentData.id (the base64 form), which is guaranteed to mis-parse and
        // return false. So treat the node as not-deletable rather than sending a known-bad id.
        // BrowsePage only populates componentData when componentId is present (it gates the detail
        // fetch on selectedNode.componentId), so this guard is defensive and should not trigger in
        // practice; it exists to keep the invariant explicit instead of silently reproducing
        // NEXUS-54171 if the wiring ever changes.
        if (!selectedNode.componentId) {
          setCanDelete(false);
          return;
        }
        // Guarded above — componentData is present here. Widen ComponentData
        // to the server's ComponentXO shape (nullable fields default to empty).
        const xo: ComponentXO = {
          id: selectedNode.componentId,
          repositoryName: componentData!.repositoryName,
          format: componentData!.format,
          group: componentData!.group ?? undefined,
          name: componentData!.name,
          version: componentData!.version ?? '',
        };
        permissionPromise = canDeleteComponent(xo);
        break;
      }
      case 'folder':
        permissionPromise = canDeleteFolder(selectedNode.id, selectedRepository);
        break;
      default:
        return;
    }

    let cancelled = false;
    permissionPromise.then((allowed) => {
      if (cancelled) return;
      cacheRef.current.set(cacheKey, allowed);
      setCanDelete(allowed);
    });

    return () => {
      cancelled = true;
    };
  }, [selectedNode, selectedRepository, componentData, authenticated]);

  return canDelete;
}
