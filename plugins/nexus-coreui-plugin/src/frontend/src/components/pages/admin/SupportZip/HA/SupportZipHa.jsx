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

import UIStrings from "../../../../../constants/UIStrings";

const {SUPPORT_ZIP: LABELS} = UIStrings;

import {
  NxCard, NxPageTitle, NxH2
} from "@sonatype/react-shared-components";

import {
  Page, PageHeader, PageTitle, ContentBody, ExtJS
} from "@sonatype/nexus-ui-plugin";

import {faArchive} from "@fortawesome/free-solid-svg-icons";

import {useMachine} from '@xstate/react';
import SupportZipHaMachine from "./SupportZipHaMachine";

import SupportZipMachine from "../SupportZipMachine";

import NodeCard from "./NodeCard";
import SupportZipHaModalForm from "./SupportZipHaModalForm";

import './SupportZipHa.scss';

export default function SupportZipHa() {
  const [nxrmNodesState, sendToNxrmNodes] = useMachine(SupportZipHaMachine, {devTools: true});
  const {nxrmNodes, selectedNode, showCreateZipModal} = nxrmNodesState.context;

  const [supportZipState, sendToSupportZip] = useMachine(SupportZipMachine, {devTools: true});
  const {params} = supportZipState.context;

  function showCreateZipModalForm(node) {
    sendToNxrmNodes({
      type: 'SHOW_SUPPORT_ZIP_FORM_MODAL',
      data: {
        selectedNode: node
      }
    });
  }

  function closeSupportZipFormModal() {
    sendToNxrmNodes({
      type: 'HIDE_SUPPORT_ZIP_FORM_MODAL'
    })
  }

  function setSupportFormParams({target}) {
    sendToSupportZip({
      type: 'UPDATE',
      params: {
        ...params,
        [target.id]: target.checked
      }
    });
  }

  function downloadZip(node) {
    sendToNxrmNodes({
      type: 'DOWNLOAD_ZIP',
      data: {
        node: node
      }
    });
  }

  function submitCreateNodeSupportZip() {
    closeSupportZipFormModal();

    sendToNxrmNodes({
      type: 'CREATE_SUPPORT_ZIP_FOR_NODE',
      data: {
        params: params,
        node: selectedNode
      }
    });
  }

  return <Page className="nxrm-support-zip-ha">
    <PageHeader>
      <PageTitle icon={faArchive} {...LABELS.MENU_HA}/>
    </PageHeader>

    <ContentBody>
      <NxPageTitle>
        <NxH2>
          {LABELS.AVAILABLE_NODES}
        </NxH2>
      </NxPageTitle>

      <NxCard.Container className="nxrm-nodes-container">
        {nxrmNodes.map(({...node}) =>
            <NodeCard
                key={node.nodeId}
                initial={node}
                createZip={() => showCreateZipModalForm(node)}
                downloadZip={downloadZip}/>
        )}
      </NxCard.Container>
    </ContentBody>

    {showCreateZipModal
        && <SupportZipHaModalForm
            formParams={params}
            setFormParams={setSupportFormParams}
            submitHandler={submitCreateNodeSupportZip}
            cancelHandler={closeSupportZipFormModal}/>
    }
  </Page>
}
