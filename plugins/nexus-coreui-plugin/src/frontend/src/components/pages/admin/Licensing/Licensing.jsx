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
  ContentBody,
  Page,
  PageHeader,
  PageTitle,
} from '@sonatype/nexus-ui-plugin';

import {faWallet} from '@fortawesome/free-solid-svg-icons';

import LicenseDetails from './LicenseDetails';
import InstallLicense from './InstallLicense';

import Machine from './LicenseMachine';

import UIStrings from '../../../../constants/UIStrings';

import './Licensing.scss';

const {LICENSING: {MENU}} = UIStrings;

export default function Licensing() {
  const [state, , service] = useMachine(Machine, {devTools: true});

  const {data, loadError} = state.context;
  const showDetails = !loadError && data.contactCompany;

  return <Page>
    <PageHeader>
      <PageTitle
          icon={faWallet}
          text={MENU.text}
          description={MENU.description}
      />
    </PageHeader>
    <ContentBody className="nxrm-licensing">
      {showDetails && <LicenseDetails service={service}/>}
      <InstallLicense service={service}/>
    </ContentBody>
  </Page>;
}
