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
import React, {useState, useEffect} from 'react';

import {faDownload, faGlobe} from '@fortawesome/free-solid-svg-icons';
import Axios from 'axios';
import {
  Button,
  ContentBody,
  NxFontAwesomeIcon,
  NxLoadWrapper,
  Page,
  PageActions,
  PageHeader,
  PageTitle,
  Utils
} from 'nexus-ui-plugin';

import UIStrings from '../../../../constants/UIStrings';
import SystemInformationBody from './SystemInformationBody';
import './SystemInformation.scss';

const INITIAL_VALUE = {};

/**
 * @since 3.24
 */
export default function SystemInformation() {
  const [systemInformation, setSystemInformation] = useState(INITIAL_VALUE);
  const isLoaded = systemInformation !== INITIAL_VALUE;
  const isLoading = !isLoaded;

  useEffect(() => {
    if (isLoaded) {
      return;
    }

    Axios.get('/service/rest/atlas/system-information').then((result) => {
      setSystemInformation(result.data);
    });
  });

  function downloadSystemInformation() {
    window.open(Utils.urlFromPath('/service/rest/atlas/system-information'), '_blank');
  }

  return <Page>
    <PageHeader>
      <PageTitle icon={faGlobe} {...UIStrings.SYSTEM_INFORMATION.MENU}/>
      <PageActions>
        <Button variant="primary" onClick={() => downloadSystemInformation()} disabled={!isLoaded}>
          <NxFontAwesomeIcon icon={faDownload}/>
          <span>{UIStrings.SYSTEM_INFORMATION.ACTIONS.download}</span>
        </Button>
      </PageActions>
    </PageHeader>
    <ContentBody className="nxrm-system-information">
      <NxLoadWrapper loading={isLoading}>
        <SystemInformationBody systemInformation={systemInformation} />
      </NxLoadWrapper>
    </ContentBody>
  </Page>;
}
