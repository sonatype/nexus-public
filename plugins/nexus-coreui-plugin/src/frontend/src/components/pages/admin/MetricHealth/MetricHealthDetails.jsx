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
  faCheckCircle,
  faExclamationCircle,
  faMedkit,
} from '@fortawesome/free-solid-svg-icons';

import {
  ContentBody,
  ListMachineUtils,
  Page,
  PageTitle,
} from '@sonatype/nexus-ui-plugin';

import {
  NxFontAwesomeIcon,
  NxTable,
  NxTile,
  NxH2,
  NxBackButton,
} from '@sonatype/react-shared-components';

import MetricHealthMachine from './MetricHealthMachine';

import UIStrings from '../../../../constants/UIStrings';
import {isClustered} from './MetricHealthHelper';

const {METRIC_HEALTH} = UIStrings;

export default function MetricHealthDetails({itemId, onDone}) {
  const [state, send] = useMachine(MetricHealthMachine, {
    devTools: true,
    context: {itemId},
  });
  const isLoading = state.matches('loading');
  const {data, error, name} = state.context;

  const nameSortDir = ListMachineUtils.getSortDirection('name', state.context);
  const messageSortDir = ListMachineUtils.getSortDirection(
    'message',
    state.context
  );
  const errorSortDir = ListMachineUtils.getSortDirection(
    'error',
    state.context
  );

  return (
    <Page>
      <div>
        <PageTitle
          icon={faMedkit}
          text={METRIC_HEALTH.MENU.text}
          description={METRIC_HEALTH.MENU.detailsDescription}
        />
        {isClustered() && (
          <NxBackButton
            text={METRIC_HEALTH.BACK_BUTTON}
            href="#admin/support/status"
          />
        )}
      </div>
      <ContentBody className="nxrm-metric-health">
        <NxTile>
          {isClustered() && <NxH2>{name}</NxH2>}
          <NxTable>
            <NxTable.Head>
              <NxTable.Row>
                <NxTable.Cell hasIcon />
                <NxTable.Cell
                  onClick={() => send({type: 'SORT_BY_NAME'})}
                  isSortable
                  sortDir={nameSortDir}
                >
                  {METRIC_HEALTH.NAME_HEADER}
                </NxTable.Cell>
                <NxTable.Cell
                  onClick={() => send({type: 'SORT_BY_MESSAGE'})}
                  isSortable
                  sortDir={messageSortDir}
                >
                  {METRIC_HEALTH.MESSAGE_HEADER}
                </NxTable.Cell>
                <NxTable.Cell
                  onClick={() => send({type: 'SORT_BY_ERROR'})}
                  isSortable
                  sortDir={errorSortDir}
                >
                  {METRIC_HEALTH.ERROR_HEADER}
                </NxTable.Cell>
              </NxTable.Row>
            </NxTable.Head>
            <NxTable.Body
              isLoading={isLoading}
              error={error}
              emptyMessage={METRIC_HEALTH.EMPTY_NODE}
            >
              {data.map((metric) => (
                <NxTable.Row key={metric.name}>
                  <NxTable.Cell hasIcon>
                    <NxFontAwesomeIcon
                      color={metric.healthy ? 'green' : 'red'}
                      icon={
                        metric.healthy ? faCheckCircle : faExclamationCircle
                      }
                    />
                  </NxTable.Cell>
                  <NxTable.Cell>{metric.name}</NxTable.Cell>
                  <NxTable.Cell>
                    <span dangerouslySetInnerHTML={{__html: metric.message}} />
                  </NxTable.Cell>
                  <NxTable.Cell>
                    {metric.error ? metric.error.message : ''}
                  </NxTable.Cell>
                </NxTable.Row>
              ))}
            </NxTable.Body>
          </NxTable>
        </NxTile>
      </ContentBody>
    </Page>
  );
}
