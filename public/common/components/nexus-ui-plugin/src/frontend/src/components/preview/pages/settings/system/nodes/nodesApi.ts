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

import { ExtAPIUtils } from '../../../../../../interface/ExtAPIUtils';
import { APIConstants } from '../../../../../../constants/APIConstants';
import { restClient } from '../../../../../../interface/api';
import { ExtJS } from '../../../../../../interface/ExtJS';
import { NodeInfo } from './types';

const { REST } = APIConstants;
const ACTIVE_NODES_URL = REST.INTERNAL.GET_SUPPORT_ZIP_ACTIVE_NODES;
const NODE_ACTION = 'node_NodeAccess';
const CLUSTERED_ENABLED_KEY = 'nexus.datastore.clustered.enabled';

interface ActiveNodeData {
  nodeId: string;
  hostname: string;
}

/**
 * Fetch cluster nodes, merging the ExtDirect node list (name + local flag)
 * with best-effort hostname resolution from the REST active-nodes endpoint.
 * Hostname resolution failures fall back to the node name.
 */
export async function fetchNodes(): Promise<NodeInfo[]> {
  const response = await ExtAPIUtils.extAPIRequest(NODE_ACTION, 'nodes');
  ExtAPIUtils.checkForError(response);
  const extNodes: Array<{ name: string; local: boolean }> =
    Array.isArray(response?.data?.result?.data) ? response.data.result.data : [];

  let hostnameByNodeId = new Map<string, string>();
  // The activenodes endpoint is only registered in clustered (HA) mode (backend
  // @ConditionalOnProperty nexus.datastore.clustered.enabled), so it returns 404
  // in single-node mode. Only attempt the hostname lookup when clustering is
  // enabled; otherwise nodes fall back to their id/name (the same result the 404
  // fallback produced, but without the pointless failed request).
  if (ExtJS.state().getValue(CLUSTERED_ENABLED_KEY)) {
    try {
      const activeNodes = await restClient.get<ActiveNodeData[]>(ACTIVE_NODES_URL);
      hostnameByNodeId = new Map((activeNodes ?? []).map((n) => [n.nodeId, n.hostname]));
    } catch (_err) {
      // Best-effort; fall back to node name below.
    }
  }

  return extNodes.map((node) => ({
    name: node.name,
    displayName: hostnameByNodeId.get(node.name) || node.name,
    local: node.local,
  }));
}
