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
import {formatAsShortDate, KEY_EGRESS, KEY_STORAGE} from './UsageInsightsUtils';
import HumanReadableUtils from '../../../../interface/HumanReadableUtils';
import UIStrings from './../../../../constants/HistoricalUsageStrings';

export function UsageInsightsChartTooltip({data}) {
  const entry = data;
  const date = entry?.metricDate;
  const available = entry?._available || {};
  const egress = entry?.[UIStrings.HISTORICAL_USAGE.CHART.LEGEND_EGRESS];
  const storage = entry?.[UIStrings.HISTORICAL_USAGE.CHART.LEGEND_STORAGE];

  const displayValue = (value, key) =>
      available[key] ? HumanReadableUtils.bytesToString(value) : UIStrings.HISTORICAL_USAGE.CHART.DATA_NOT_AVAILABLE;

  return (
      <div className="usage-insights-chart-tooltip">
        <div className="tooltip-title">
          {date ? formatAsShortDate(date, true) : 'N/A'}
        </div>

        <div className="tooltip-content">
          <div className="tooltip-content-item">
            <span className="tooltip-item-symbol tooltip-peak-storage"/>
            <span>{UIStrings.HISTORICAL_USAGE.CHART.LEGEND_STORAGE}</span>
            <span>{displayValue(storage, KEY_STORAGE)}</span>
          </div>
          <div className="tooltip-content-item">
            <span className="tooltip-item-symbol tooltip-total-egress"/>
            <span>{UIStrings.HISTORICAL_USAGE.CHART.LEGEND_EGRESS}</span>
            <span>{displayValue(egress, KEY_EGRESS)}</span>
          </div>
        </div>
      </div>
  );
}
