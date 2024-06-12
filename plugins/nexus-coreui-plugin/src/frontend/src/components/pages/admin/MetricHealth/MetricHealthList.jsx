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
import {isEmpty} from 'ramda';
import {
  Page,
  ContentBody,
  PageHeader,
  PageTitle,
  ListMachineUtils,
  HelpTile,
} from '@sonatype/nexus-ui-plugin';

import {
  faMedkit,
  faExclamationCircle,
  faCheckCircle,
} from '@fortawesome/free-solid-svg-icons';

import {
  NxTile,
  NxTable,
  NxFontAwesomeIcon,
} from '@sonatype/react-shared-components';

import UIStrings from '../../../../constants/UIStrings';

import MetricHealthListMachine from './MetricHealthListMachine';

import UpgradeAlert from './UpgradeAlert';

const {METRIC_HEALTH} = UIStrings;

export default function MetricHealthList({onEdit}) {
  const [state, send] = useMachine(MetricHealthListMachine, {devTools: true});
  const {data, error} = state.context;
  const isLoading = state.matches('loading');

  const nameSortDir = ListMachineUtils.getSortDirection('name', state.context);
  const errorSortDir = ListMachineUtils.getSortDirection(
    'error',
    state.context
  );
  const messageSortDir = ListMachineUtils.getSortDirection(
    'message',
    state.context
  );

  return (
    <Page className="nxrm-metric-health">
      <PageHeader>
        <PageTitle
          icon={faMedkit}
          text={METRIC_HEALTH.MENU.text}
          description={METRIC_HEALTH.MENU.description}
        />
      </PageHeader>
      <ContentBody className="nxrm-metric-health-list">
        <NxTile>
          <UpgradeAlert/>
          <NxTable>
            <NxTable.Head>
              <NxTable.Row>
                <NxTable.Cell hasIcon />
                <NxTable.Cell
                  onClick={() => send('SORT_BY_NAME')}
                  isSortable
                  sortDir={nameSortDir}
                >
                  {METRIC_HEALTH.NAME_HEADER}
                </NxTable.Cell>
                <NxTable.Cell
                  onClick={() => send('SORT_BY_ERROR')}
                  isSortable
                  sortDir={errorSortDir}
                >
                  {METRIC_HEALTH.ERROR_HEADER}
                </NxTable.Cell>
                <NxTable.Cell
                  onClick={() => send('SORT_BY_MESSAGE')}
                  isSortable
                  sortDir={messageSortDir}
                >
                  {METRIC_HEALTH.MESSAGE_HEADER}
                </NxTable.Cell>
                <NxTable.Cell chevron />
              </NxTable.Row>
            </NxTable.Head>
            <NxTable.Body
              isLoading={isLoading}
              error={error}
              emptyMessage={METRIC_HEALTH.EMPTY_NODE_LIST}
            >
              {data.map(({name, error, message, nodeId}) => (
                <NxTable.Row
                  key={nodeId}
                  onClick={() => onEdit(encodeURIComponent(nodeId))}
                  isClickable
                >
                  <NxTable.Cell hasIcon>
                    <NxFontAwesomeIcon
                      color={isEmpty(error) ? 'green' : 'red'}
                      icon={
                        isEmpty(error) ? faCheckCircle : faExclamationCircle
                      }
                    />
                  </NxTable.Cell>
                  <NxTable.Cell>{name}</NxTable.Cell>
                  <NxTable.Cell>{error}</NxTable.Cell>
                  <NxTable.Cell>
                    <p
                      dangerouslySetInnerHTML={{
                        __html: message,
                      }}
                    />
                  </NxTable.Cell>
                  <NxTable.Cell chevron />
                </NxTable.Row>
              ))}
            </NxTable.Body>
          </NxTable>
        </NxTile>
        <HelpTile
          header={METRIC_HEALTH.HELP.LABEL}
          body={METRIC_HEALTH.HELP.SUB_LABEL}
        />
      </ContentBody>
    </Page>
  );
}
