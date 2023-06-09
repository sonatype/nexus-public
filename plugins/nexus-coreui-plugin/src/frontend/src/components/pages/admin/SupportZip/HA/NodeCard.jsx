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
import {useMachine} from '@xstate/react';
import {faCheckCircle, faTimesCircle} from '@fortawesome/free-solid-svg-icons';

import {
  NxCard,
  NxFontAwesomeIcon,
  NxH3,
  NxTooltip,
  NxLoadingSpinner,
  NxStatefulSegmentedButton
} from '@sonatype/react-shared-components';

import UIStrings from '../../../../../constants/UIStrings';
import NodeCardMachine from './NodeCardMachine';
import './SupportZipHa.scss';

const {
  CREATING_ZIP,
  NODE_UNAVAILABLE_CANNOT_CREATE,
  NO_ZIP_CREATED,
  ZIP_UPDATED,
  NODE_IS_ACTIVE,
  NODE_IS_INACTIVE,
  OFFLINE,
  DOWNLOAD_ZIP,
  CREATE_SUPPORT_ZIP
} = UIStrings.SUPPORT_ZIP;

export default function NodeCard({initial, createZip, downloadZip}) {
  const [current] = useMachine(NodeCardMachine, {
    context: {
      node: initial
    },
    devTools: true
  });

  const {node} = current.context;

  const zipNotCreated = node.status === 'NOT_CREATED';
  const zipCreated = node.status === 'COMPLETED';
  const zipCreating = node.status === 'CREATING';
  const isNodeActive = node.status !== 'NODE_UNAVAILABLE';

  const nodeCardText = () => {
    if (zipCreating) {
      return <NxLoadingSpinner>{CREATING_ZIP}</NxLoadingSpinner>;
    }
    if (node.blobRef == null && !isNodeActive) {
      return NODE_UNAVAILABLE_CANNOT_CREATE;
    }
    if (zipNotCreated || node.blobRef === null) {
      return NO_ZIP_CREATED;
    }
    const updatedDate = new Date(node.lastUpdated).toLocaleDateString();
    return (
      <>
        {ZIP_UPDATED} <b>{updatedDate}</b>
      </>
    );
  };

  return (
    <NxCard>
      <NxTooltip title={isNodeActive ? NODE_IS_ACTIVE : NODE_IS_INACTIVE} placement="top-middle">
        <NxCard.Header>
          <NxH3>
            {isNodeActive ? (
              <NxFontAwesomeIcon icon={faCheckCircle} className="nxrm-node-green-checkmark" />
            ) : (
              <NxFontAwesomeIcon icon={faTimesCircle} />
            )}{' '}
            {node.hostname}
          </NxH3>
        </NxCard.Header>
      </NxTooltip>

      <NxCard.Content>
        <NxCard.Text>{nodeCardText()}</NxCard.Text>
      </NxCard.Content>

      <NxCard.Footer>
        {!isNodeActive && !zipCreated && <NxCard.Text>{OFFLINE}</NxCard.Text>}

        {/* workaround RSC Segmented Button bug */}
        <div onClick={(e) => e.stopPropagation()}>
          <NxStatefulSegmentedButton
            variant="primary"
            onClick={zipCreated ? () => downloadZip(node) : createZip}
            buttonContent={zipCreated ? DOWNLOAD_ZIP : CREATE_SUPPORT_ZIP}
            disabled={zipCreating}
          >
            {zipCreated ? (
              <button className="nx-dropdown-button" onClick={createZip}>
                {CREATE_SUPPORT_ZIP}
              </button>
            ) : (
              <button className="disabled nx-dropdown-button">{DOWNLOAD_ZIP}</button>
            )}
          </NxStatefulSegmentedButton>
        </div>
      </NxCard.Footer>
    </NxCard>
  );
}
