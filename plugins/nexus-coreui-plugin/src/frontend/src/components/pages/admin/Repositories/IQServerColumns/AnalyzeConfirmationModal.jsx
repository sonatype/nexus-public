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

import {
  NxModal,
  NxButton,
  NxH2,
  NxFooter,
  NxP,
  NxButtonBar
} from '@sonatype/react-shared-components';

import UIStrings from '../../../../../constants/UIStrings';
import {useRepositoriesService} from '../RepositoriesContextProvider';
import AnalyzeButton from './AnalyzeButton';

const {HEALTH_CHECK} = UIStrings.REPOSITORIES.LIST;
const {CANCEL_BUTTON_LABEL} = UIStrings.SETTINGS;

export default function AnalyzeConfirmationModal({close, name}) {  
  const [, send] = useRepositoriesService();

  const enableHealthCheck = (name) => {
    close();
    if (name) {
      send({type: 'ENABLE_HELTH_CHECK_SINGLE_REPO', repoName: name});
    } else {
      send({type: 'ENABLE_HELTH_CHECK_ALL_REPOS'});
    }
  };

  return (
    <NxModal
      className="nxrm-enable-health-check-modal"
      onCancel={close}
      aria-labelledby="modal-header-text"
      onClick={(e) => e.stopPropagation()} // allows NxSegmentedButton dropdown to work properly, otherwise it closes immediately
    >
      <NxModal.Header>
        <NxH2 id="modal-header-text">{HEALTH_CHECK.MODAL_HEADER}</NxH2>
      </NxModal.Header>
      <NxModal.Content>
        <NxP>{HEALTH_CHECK.MODAL_CONTENT(name)}</NxP>
      </NxModal.Content>
      <NxFooter>
        <NxButtonBar>
          <NxButton onClick={close}>{CANCEL_BUTTON_LABEL}</NxButton>
          <AnalyzeButton enableHealthCheck={enableHealthCheck} name={name} />
        </NxButtonBar>
      </NxFooter>
    </NxModal>
  );
}
