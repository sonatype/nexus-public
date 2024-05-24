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
import React, {useEffect} from 'react';

import {ExtJS} from '@sonatype/nexus-ui-plugin';
import {useMachine} from '@xstate/react';
import UIStrings from '../../../../constants/UIStrings';
import {NxModal, NxButton, NxButtonBar, NxFooter, NxH2, NxH3, NxCloseButton} from '@sonatype/react-shared-components';

import UpgradeModalMachine from './UpgradeModalMachine';
import './MarketingUpgradeModal.scss'

const {UPGRADE_MODAL: {HEADER, ABOUT, BENEFITS}} = UIStrings;

export default function UpgradeModal() {
  const [state, send] = useMachine(UpgradeModalMachine, {
    devTools: true
  });

  const zeroDowntimeMarketingModalClosed = ExtJS.useState(() => ExtJS.state().getValue('zeroDowntimeMarketingModalClosed'));
  const onboarding = !ExtJS.useState(() => ExtJS.state().getValue('onboarding.required'));
  const showModalFlag = ExtJS.state().getValue('zero.downtime.marketing.modal');
  const isHA = ExtJS.state().getValue('nexus.datastore.clustered.enabled');
  const hasUser = ExtJS.useUser() ?? false;
  const hasPermissions = ExtJS.usePermission(() => ExtJS.checkPermission('nexus:*'), [hasUser])
  const modalCloseHandler = () => send({ type: 'CLOSE_AND_SAVE' });

  useEffect(() => {
    if (showModalFlag && onboarding && isHA && hasPermissions && hasUser && zeroDowntimeMarketingModalClosed === false) {
      send({ type: 'OPEN_MODAL' });
    } else {
      send({ type: 'CLOSE_MODAL' });
    }
  }, [zeroDowntimeMarketingModalClosed, hasPermissions, onboarding]);

  return <>
    {state.matches('open') && <NxModal onCancel={modalCloseHandler} aria-labelledby="zero-downtime-upgrade-modal">
      <header className="nx-modal-header">
        <NxH2 id="zero-downtime-upgrade-modal">{HEADER.TITLE}</NxH2>
        <NxCloseButton onClick={modalCloseHandler}></NxCloseButton>
      </header>
      <div className="nx-modal-content">
        <img className="modal-logo"
                 src="/static/rapture/resources/images/sonatype-repository-logo.svg"
                 alt="Sonatype Nexus Repository logo"/>
        <NxH3>{ABOUT.TITLE}</NxH3>
        <p>{ABOUT.DESCRIPTION}</p>
        <NxH3>{BENEFITS.TITLE}</NxH3>
        <ul>
          <li>{BENEFITS.LIST.ITEM1}</li>
          <li>{BENEFITS.LIST.ITEM2}</li>
          <li>{BENEFITS.LIST.ITEM3}</li>
        </ul>
        <p>{BENEFITS.NOTES}</p>
      </div>
      <NxFooter>
        <NxButtonBar>
          <NxButton onClick={modalCloseHandler}>Dismiss</NxButton>
        </NxButtonBar>
      </NxFooter>
    </NxModal>}
  </>
}
