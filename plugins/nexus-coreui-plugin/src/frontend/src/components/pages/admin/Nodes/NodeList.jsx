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
import {NxCard, NxH2, NxH3, NxLoadWrapper} from '@sonatype/react-shared-components';
import {faArchive} from '@fortawesome/free-solid-svg-icons';
import {
  Page,
  PageHeader,
  PageTitle,
  PageActions,
  ContentBody,
  HelpTile
} from '@sonatype/nexus-ui-plugin';
import UIStrings from '../../../../constants/UIStrings';
import NodeListMachine from './NodeListMachine';
import FreezeAction from './FreezeAction/FreezeAction';
import './NodeList.scss';

const {
  NODES: {MENU, HELP}
} = UIStrings;

export default function NodeList() {
  const [state] = useMachine(NodeListMachine, {
    devTools: true
  });

  const {data, error} = state.context;

  const isLoading = state.matches('loading');

  return (
    <Page className="nxrm-nodes">
      <PageHeader>
        <PageTitle icon={faArchive} {...MENU} />
        <PageActions>
          <FreezeAction />
        </PageActions>
      </PageHeader>

      <ContentBody>
        <NxH2>{MENU.text}</NxH2>
        <NxLoadWrapper loading={isLoading} error={error} retryHandler={() => _}>
          <NxCard.Container>
            {data.map((node) => (
              <NxCard key={node.nodeId}>
                <NxCard.Header>
                  <NxH3>{node.hostname}</NxH3>
                </NxCard.Header>
              </NxCard>
            ))}
          </NxCard.Container>
        </NxLoadWrapper>

        <HelpTile header={HELP.TITLE} body={HELP.TEXT} />
      </ContentBody>
    </Page>
  );
}
