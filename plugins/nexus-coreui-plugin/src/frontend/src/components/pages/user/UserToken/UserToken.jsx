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
import {useMachine, useService} from '@xstate/react';
import {faKey, faLock} from '@fortawesome/free-solid-svg-icons';

import {
  ContentBody,
  Page,
  PageHeader,
  PageTitle,
  Section,
  SectionFooter
} from '@sonatype/nexus-ui-plugin';

import {
  NxButton,
  NxFontAwesomeIcon,
  NxLoadWrapper,
} from '@sonatype/react-shared-components';

import UIStrings from '../../../../constants/UIStrings';
import UserTokenMachine from './UserTokenMachine';
import UserTokenDetails from './UserTokenDetails';
import './UserToken.scss';

export default function UserToken() {
  const service = useMachine(UserTokenMachine, {devTools: true})[2];

  return <UserTokenForm service={service}/>;
}

export function UserTokenForm({service}) {
  const [current, send] = useService(service);

  const context = current.context;
  const isLoading = !(current.matches('idle') || current.matches('showToken'));

  function onAccessTokenClick() {
    send('ACCESS');
  }

  function onResetTokenClick() {
    send('RESET');
  }

  function onCloseClick() {
    send('HIDE');
  }

  function retry() {
    send('RETRY');
  }

  return <Page>
    <PageHeader><PageTitle icon={faKey} {...UIStrings.USER_TOKEN.MENU}/></PageHeader>
    <ContentBody className='nxrm-usertoken-current'>
      {current.matches('showToken') && <UserTokenDetails userToken={context.token} onCloseClick={onCloseClick}/>}
      <Section>
        <NxLoadWrapper loading={isLoading} retryHandler={retry}>
          <p>{UIStrings.USER_TOKEN.LABELS.ACCESS_NOTE}</p>
          <p>{UIStrings.USER_TOKEN.LABELS.RESET_NOTE}</p>
          <SectionFooter>
            <NxButton variant='primary' onClick={onAccessTokenClick}>
              <span>{UIStrings.USER_TOKEN.BUTTONS.ACCESS}</span>
            </NxButton>
            <NxButton onClick={onResetTokenClick}>
              <NxFontAwesomeIcon icon={faLock}/>
              <span>{UIStrings.USER_TOKEN.BUTTONS.RESET}</span>
            </NxButton>
          </SectionFooter>
        </NxLoadWrapper>
      </Section>
    </ContentBody>
  </Page>;
}
