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
import {Button, NxLoadWrapper, Page, PageHeader, PageTitle, Section, SectionFooter, ContentBody} from 'nexus-ui-plugin';
import {NxFontAwesomeIcon, NxModal} from '@sonatype/react-shared-components';
import {faKey, faLock} from '@fortawesome/free-solid-svg-icons';

import NuGetApiTokenMachine from './NuGetApiTokenMachine';
import NuGetApiTokenModal from './NuGetApiTokenModal';
import UIStrings from '../../../../constants/UIStrings';

export default function NuGetApiToken() {
  const [state, send] = useMachine(NuGetApiTokenMachine, {devTools: true});

  const isLoading = !(state.matches('idle') || state.matches('showToken'));
  const showNugetModal = state.matches('showToken');

  function handleAccessKey() {
    send('ACCESS');
  }

  function handleResetKey() {
    send('RESET');
  }

  function handleCloseKey() {
    send('HIDE');
  }

  return <Page>
    <PageHeader><PageTitle icon={faKey} {...UIStrings.NUGET_API_KEY.MENU}/></PageHeader>
    <ContentBody>
      <Section>
        <NxLoadWrapper isLoading={isLoading}>
          <p> {UIStrings.NUGET_API_KEY.INSTRUCTIONS} </p>
          <SectionFooter>
            <Button variant='primary' onClick={handleAccessKey}>
              <NxFontAwesomeIcon icon={faLock}/>
              <span>{UIStrings.NUGET_API_KEY.ACCESS.BUTTON}</span>
            </Button>
            <Button onClick={handleResetKey}>
              <NxFontAwesomeIcon icon={faLock}/>
              <span>{UIStrings.NUGET_API_KEY.RESET.BUTTON}</span>
            </Button>
          </SectionFooter>

          {showNugetModal && <NuGetApiTokenModal apiKey={state.context.token.apiKey} onCloseClick={handleCloseKey}/>}
        </NxLoadWrapper>
      </Section>
    </ContentBody>
  </Page>;
}
