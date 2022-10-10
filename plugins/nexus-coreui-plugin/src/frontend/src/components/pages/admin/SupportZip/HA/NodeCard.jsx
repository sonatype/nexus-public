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
import React from "react";
import {useMachine} from "@xstate/react";

import {
  NxCard, NxFontAwesomeIcon, NxFormRow,
  NxH3, NxP, NxTextLink, NxTooltip
} from "@sonatype/react-shared-components";

import UIStrings from "../../../../../constants/UIStrings";
import {faCheckCircle} from "@fortawesome/free-solid-svg-icons";

import './SupportZipHa.scss';

import NodeCardMachine from "./NodeCardMachine";

const {SUPPORT_ZIP: LABELS} = UIStrings;

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
    if (zipNotCreated) {
      return <NxP className="nx-p-zip-updated">
        {LABELS.NO_ZIP_CREATED}
      </NxP>;
    }

    const updatedDate = new Date(node.lastUpdated).toLocaleDateString();
    return <NxP className="nx-p-zip-updated">
      {LABELS.ZIP_UPDATED}&nbsp;<b>{updatedDate}</b>
    </NxP>
  };

  return <NxCard.Container>
    <NxTooltip
        title={LABELS.NODE_IS_ACTIVE} open placement="top">
      <NxCard>
        <NxCard.Content>
          <NxFormRow className="nx-node-name-container">
            <>
              <NxFontAwesomeIcon icon={faCheckCircle} className="nx-node-green-checkmark"/>
              <NxH3>{node.hostname}</NxH3>
            </>
          </NxFormRow>

          {zipLastUpdatedHtml()}

        </NxCard.Content>
        <NxCard.Footer>
          {zipNotCreated
              &&
              <NxTextLink className="nx-underline" onClick={createZip}>
                {statusActionLabel}
              </NxTextLink>
          }

          {zipCreated
              &&
              <NxTextLink className="nx-underline" onClick={() => downloadZip(node)}>
                {statusActionLabel}
              </NxTextLink>
          }

          {zipCreating
              &&
              <NxCard.Text id="nx-zip-creating-inprogress">
                {statusActionLabel}
              </NxCard.Text>
          }
        </NxCard.Footer>
      </NxCard>
    </NxTooltip>

  </NxCard.Container>
}
