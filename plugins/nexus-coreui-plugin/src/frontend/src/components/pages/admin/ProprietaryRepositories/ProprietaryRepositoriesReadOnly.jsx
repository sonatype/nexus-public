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
  NxInfoAlert,
  NxList,
  NxTile,
  NxH2,
} from '@sonatype/react-shared-components';

import UIStrings from '../../../../constants/UIStrings';
import ProprietaryRepositoriesMachine from "./ProprietaryRepositoriesMachine";

const {PROPRIETARY_REPOSITORIES: { CONFIGURATION: LABELS }, SETTINGS} = UIStrings;

export default function ProprietaryRepositoriesReadOnly() {
  const [current, send] = useMachine(ProprietaryRepositoriesMachine, {devTools: true});
  const {data: {enabledRepositories}, loadError} = current.context;
  const isLoading = current.matches('loading');

  function retry() {
    send({type: 'RETRY'});
  }

  return <>
    <NxInfoAlert>{SETTINGS.READ_ONLY.WARNING}</NxInfoAlert>
    <NxTile>
      <NxTile.Header>
        <NxTile.HeaderTitle>
          <NxH2>{LABELS.SELECTED_TITLE}</NxH2>
        </NxTile.HeaderTitle>
      </NxTile.Header>
      <NxTile.Content>
        <NxList
            emptyMessage={LABELS.EMPTY_LIST}
            isLoading={isLoading}
            error={loadError}
            retryHandler={retry}
        >
          {enabledRepositories?.map(name => (
              <NxList.Item key={name}>
                <NxList.Text>{name}</NxList.Text>
              </NxList.Item>
          ))}
        </NxList>
      </NxTile.Content>
    </NxTile>
  </>;
}
