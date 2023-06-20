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

import {
  NxCard,
  NxFontAwesomeIcon,
  NxH3,
  NxP,
  NxTextLink,
  NxTooltip,
  NxLoadingSpinner
} from '@sonatype/react-shared-components';

import UIStrings from '../../../../../constants/UIStrings';
import {faCheckCircle, faTimesCircle} from '@fortawesome/free-solid-svg-icons';

import './SupportZipHa.scss';

import NodeCardMachine from './NodeCardMachine';

const {SUPPORT_ZIP: LABELS} = UIStrings;

const NODE_UNAVAILABLE = 'NODE_UNAVAILABLE';

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

  const zipStatusLabels = {
    NOT_CREATED: LABELS.CREATE_SUPPORT_ZIP,
    COMPLETED: LABELS.DOWNLOAD_ZIP,
    CREATING: LABELS.CREATING_ZIP
  };

  const statusActionLabel = zipStatusLabels[node.status];

  const zipLastUpdatedHtml = () => {
    if (node.blobRef == null && !isNodeActive()) {
      return <NxP className="nxrm-p-zip-updated">{LABELS.NODE_UNAVAILABLE_CANNOT_CREATE}</NxP>;
    }
    if (zipNotCreated || node.blobRef === null) {
      return <NxP className="nxrm-p-zip-updated">{LABELS.NO_ZIP_CREATED}</NxP>;
    }

    const updatedDate = new Date(node.lastUpdated).toLocaleDateString();
    return (
      <NxP className="nxrm-p-zip-updated">
        {LABELS.ZIP_UPDATED}&nbsp;<b>{updatedDate}</b>
      </NxP>
    );
  };

  const isNodeActive = () => node.status !== NODE_UNAVAILABLE;

  return (
    <NxCard>
      <NxTooltip
        title={isNodeActive() ? LABELS.NODE_IS_ACTIVE : LABELS.NODE_IS_INACTIVE}
        placement="top-middle"
      >
        <NxCard.Header>
          <NxH3>
            {isNodeActive() ? (
              <NxFontAwesomeIcon icon={faCheckCircle} className="nxrm-node-green-checkmark" />
            ) : (
              <NxFontAwesomeIcon icon={faTimesCircle} />
            )}{' '}
            {node.hostname}
          </NxH3>
        </NxCard.Header>
      </NxTooltip>
      <NxCard.Content>{zipLastUpdatedHtml()}</NxCard.Content>
      <NxCard.Footer>
        {!isNodeActive() && !zipCreated && <NxCard.Text>{LABELS.OFFLINE}</NxCard.Text>}

        {zipNotCreated && <NxTextLink onClick={createZip}>{statusActionLabel}</NxTextLink>}

        {zipCreated && (
          <NxTextLink onClick={() => downloadZip(node)}>{statusActionLabel}</NxTextLink>
        )}

        {zipCreating && (
          <NxCard.Text>
            <NxLoadingSpinner>{statusActionLabel}</NxLoadingSpinner>
          </NxCard.Text>
        )}
      </NxCard.Footer>
    </NxCard>
  );
}
