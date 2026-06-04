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
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import { faInfoCircle } from '@fortawesome/free-solid-svg-icons';
import { UsageInsightsChart } from './UsageInsightsChart'

import {
  NxTable,
  NxTableHead,
  NxTableRow,
  NxTableCell,
  NxTableBody,
  NxTooltip,
  NxH2, NxP,
  NxInfoAlert,
  useToggle
} from '@sonatype/react-shared-components';

import HistoricalUsageMachine from './HistoricalUsageMachine';
import './Usage.scss';
import UIStrings from "../../../../constants/UIStrings";
import ExtJS from "../../../../interface/ExtJS";

export default function HistoricalUsage({columns}) {
  const [state] = useMachine(HistoricalUsageMachine, {
      devTools: true
    });
  const {data} = state.context;
  const [isOpen, dismiss] = useToggle(true);
  const isCloud = ExtJS.useState(() => ExtJS.state().getValue('isCloud', false));

  return (<>
        <NxH2>{UIStrings.HISTORICAL_USAGE.TITLE}</NxH2>
        {isCloud && <NxInfoAlert>{UIStrings.HISTORICAL_USAGE.USAGE_DATA_UPDATE_FREQUENCY}</NxInfoAlert>}
        <NxP>{UIStrings.HISTORICAL_USAGE.DESCRIPTION}</NxP>
        {isCloud && isOpen && <NxInfoAlert onClose={dismiss}>{UIStrings.HISTORICAL_USAGE.USAGE_DATA_STORAGE_EXPLANATION}</NxInfoAlert>}
        {isCloud && <UsageInsightsChart />}

        <NxTable className="historical-usage-table">
          <NxTableHead>
            <NxTableRow>
              {columns.map(column => (
                  <NxTableCell key={column.key}>
                    {column.Header()}
                    {column.tooltip && column.showTooltip !== false && (
                        <NxTooltip title={column.tooltip}>
                          <span><FontAwesomeIcon icon={faInfoCircle} /></span>
                        </NxTooltip>
                    )}
                  </NxTableCell>
              ))}
            </NxTableRow>
          </NxTableHead>
          <NxTableBody>
            {data && data.map((item, index) => (
                <NxTableRow key={index}>
                  {columns.map(column => (
                      <NxTableCell key={column.key}>
                        {column.Cell(item)}
                      </NxTableCell>
                  ))}
                </NxTableRow>
            ))}
          </NxTableBody>
        </NxTable>
      </>
  );
}
