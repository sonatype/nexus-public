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
  PageTitle
} from '@sonatype/nexus-ui-plugin';

import {
  NxH2,
  NxLoadWrapper,
  NxTile,
  NxTree
} from '@sonatype/react-shared-components';

import {faDatabase} from '@fortawesome/free-solid-svg-icons';
import {isEmpty} from 'ramda';
import UIStrings from '../../../../constants/UIStrings';
import BrowseTreeMachine from './BrowseTreeMachine';
import BrowseTreeChildren from './BrowseTreeChildren';
import './Browse.scss';

const {BROWSE} = UIStrings;

export default function BrowseTree({itemId}) {
  const [repoId, path] = decodeURIComponent(itemId).split(':');
  const [state, send] = useMachine(BrowseTreeMachine, {
    context: {
      repositoryName: repoId,
      initialOpenPath: path ?? ''
    },
    devTools: true
  });
  const {children, loadError} = state.context;
  const isLoading = state.matches('chooseInitialState') || state.matches('loading');

  return (
    <Page>
      <PageHeader>
        <PageTitle icon={faDatabase} {...BROWSE.MENU}/>
      </PageHeader>
      <ContentBody className="nxrm-browse-tree">
        <NxTile>
          <NxTile.Header>
            <NxTile.HeaderTitle>
              <NxH2>{BROWSE.TREE_TITLE} {repoId}</NxH2>
            </NxTile.HeaderTitle>
          </NxTile.Header>
          <NxTile.Content>
            <NxLoadWrapper retryHandler={() => send({type: 'RETRY'})} loading={isLoading} error={loadError}>
              {() => (
                isEmpty(children) ?
                <span>{BROWSE.TREE_EMPTY_MESSAGE}</span> :
                <NxTree>
                  <BrowseTreeChildren children={children}/>
                </NxTree>
              )}
            </NxLoadWrapper>
          </NxTile.Content>
        </NxTile>
      </ContentBody>
    </Page>
  )
};
