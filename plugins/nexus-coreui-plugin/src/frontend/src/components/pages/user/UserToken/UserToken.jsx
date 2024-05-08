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
import {useMachine, useService} from '@xstate/react';
import {faKey, faLock} from '@fortawesome/free-solid-svg-icons';
import {isNil} from 'ramda';

import {
  ContentBody,
  DateUtils,
  Page,
  PageHeader,
  PageTitle,
  Section,
  SectionFooter
} from '@sonatype/nexus-ui-plugin';

import {
  NxButton,
  NxButtonBar,
  NxFontAwesomeIcon,
  NxH2,
  NxErrorAlert,
  NxInfoAlert,
  NxLoadWrapper,
  NxReadOnly,
  useToggle
} from '@sonatype/react-shared-components';

import UIStrings from '../../../../constants/UIStrings';
import UserTokenMachine from './UserTokenMachine';
import UserTokenDetails from './UserTokenDetails';
import './UserToken.scss';

const {
  USER_TOKEN: {
    MENU,
    CAPTION,
    LABELS, MESSAGES,
    BUTTONS,
    USER_TOKEN_STATUS
  }
} = UIStrings;

export default function UserToken() {
  const service = useMachine(UserTokenMachine, {devTools: true})[2];

  return <UserTokenForm service={service}/>;
}

export function UserTokenForm({service}) {
  const [state, send] = useService(service);

  const {data, error, token} = state.context;
  const isLoading = !(state.matches('loaded') || state.matches('showToken'));
  const expirationTimestamp = data?.expirationTimeTimestamp;
  const isExpired = !isNil(error);
  const [isOpen, dismiss] = useToggle(true);
  const showUserTokenStatus = !isNil(expirationTimestamp) || isExpired;

  function onAccessTokenClick() {
    send('ACCESS');
  }

  function onResetTokenClick() {
    send('RESET');
  }

  function onGenerateTokenClick() {
    send('GENERATE');
  }
  function onCloseClick() {
    send('HIDE');
  }

  function retry() {
    send('RETRY');
  }

  return <Page>
    <PageHeader><PageTitle icon={faKey} {...MENU}/></PageHeader>
    <ContentBody className='nxrm-usertoken-current'>
      {state.matches('showToken') && <UserTokenDetails userToken={token} onCloseClick={onCloseClick}/>}
      <Section>
        <NxLoadWrapper loading={isLoading} retryHandler={retry}>
          <NxH2>{CAPTION}</NxH2>
          <NxInfoAlert>{LABELS.NOTE}</NxInfoAlert>
          {showUserTokenStatus &&
            <NxReadOnly>
              <NxReadOnly.Label>{USER_TOKEN_STATUS.TEXT}</NxReadOnly.Label>
              <NxReadOnly.Data className="nx-sub-label">{USER_TOKEN_STATUS.DESCRIPTION}</NxReadOnly.Data>
              <NxReadOnly.Data>
                {USER_TOKEN_STATUS.TIMESTAMP_TEXT(isExpired)}
                {isExpired && isOpen && <NxErrorAlert onClose={dismiss}>{error}</NxErrorAlert>}
                {!isExpired && DateUtils.prettyDateTime(new Date(parseInt(expirationTimestamp)))}
              </NxReadOnly.Data>
            </NxReadOnly>
          }
          <SectionFooter>
            <NxButtonBar>
              <NxButton onClick={onResetTokenClick}>
                <NxFontAwesomeIcon icon={faLock}/>
                <span>{BUTTONS.RESET}</span>
              </NxButton>
              {isExpired && <NxButton variant='primary' onClick={onGenerateTokenClick}>
                <span>{BUTTONS.GENERATE}</span>
              </NxButton>}
              {!isExpired && <NxButton variant='primary' onClick={onAccessTokenClick}>
                <span>{BUTTONS.ACCESS}</span>
              </NxButton>}
            </NxButtonBar>
          </SectionFooter>
        </NxLoadWrapper>
      </Section>
    </ContentBody>
  </Page>;
}
