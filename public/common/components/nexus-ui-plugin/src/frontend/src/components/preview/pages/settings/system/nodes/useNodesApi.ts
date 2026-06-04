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
 * Nodes API Hook
 *
 * Migration Status:
 * - fetchNodes: ExtDirect (REST API is Pro-only and limited)
 *
 * BACKEND BLOCKER: Nodes REST API is Pro-only and only returns node IDs.
 * Full node info requires ExtDirect.
 */

import { useCallback } from 'react';
import { ExtAPIUtils } from '../../../../../../interface/ExtAPIUtils';
import { NodeInfo } from './types';

// ExtDirect API action for nodes (no REST available)
const NODE_ACTION = 'node_NodeAccess';

/**
 * Custom hook for Nodes API operations
 */
export function useNodesApi() {
  /**
   * Fetch all nodes in the cluster using ExtDirect
   * NOTE: REST API is Pro-only and returns limited info
   */
  const fetchNodes = useCallback(async (): Promise<NodeInfo[]> => {
    try {
      const response = await ExtAPIUtils.extAPIRequest(NODE_ACTION, 'nodes');
      ExtAPIUtils.checkForError(response);
      const data = response?.data?.result?.data;
      return Array.isArray(data) ? data : [];
    } catch (err: unknown) {
      const message = err instanceof Error ? err.message : 'Failed to load nodes';
      console.error('Failed to fetch nodes:', err);
      throw new Error(message);
    }
  }, []);

  return {
    fetchNodes,
  };
}

export default useNodesApi;
