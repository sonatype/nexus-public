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
import React from 'react';
import {useActor} from '@xstate/react';

import {NxFormGroup, NxFormSelect} from '@sonatype/react-shared-components';

import UIStrings from '../../../../constants/UIStrings';

const {ACTIONS, LOAD_ERROR} = UIStrings.SYSTEM_INFORMATION;

export default function NodeSelector({service}) {
  const [state, send] = useActor(service);

  const isLoading = state.matches('loading');
  const loadError = state.matches('loadError') ? LOAD_ERROR : null;

  const {selectedNodeId, nodeIds} = state.context;

  const handleSelectNode = (value) => {
    send({type: 'SELECT_NODE', value});
  };

  return (
    <div className="nxrm-sys-info-node-selector">
      <NxFormGroup label={ACTIONS.nodeSelector}>
        <NxFormSelect
          onChange={handleSelectNode}
          value={selectedNodeId}
          disabled={isLoading || loadError}
        >
          {nodeIds.map((nodeId) => (
            <option value={nodeId} key={nodeId}>
              {nodeId}
            </option>
          ))}
        </NxFormSelect>
      </NxFormGroup>
    </div>
  );
}
