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
import {ResponsiveBar} from "@nivo/bar";
import {
  NxH4,
  NxNavigationDropdown,
  NxStatefulNavigationDropdown,
  NxWarningAlert,
  NxErrorAlert
} from '@sonatype/react-shared-components';
import {faFilter} from '@fortawesome/free-solid-svg-icons';

import './UsageInsightsChart.scss';
import UIStrings from './../../../../constants/HistoricalUsageStrings'
import UsageInsightsChartMachine from './UsageInsightsChartMachine';
import {
  formatAsShortDate,
  getMaxValue,
  getValueTicks,
  getMetricDateTicks,
  KEY_EGRESS,
  KEY_STORAGE
} from './UsageInsightsUtils';

import HumanReadableUtils from "../../../../interface/HumanReadableUtils";
import {UsageInsightsChartTooltip} from './UsageInsightsChartTooltip';
const chartEgressColor = "var(--nx-color-chart-data-1)";
const chartStorageColor = "var(--nx-color-chart-data-4)";

const MAX_VALUE_TICKS = 10;

export function UsageInsightsChart() {
  const [state, send] = useMachine(UsageInsightsChartMachine, {
    devTools: true
  });
  const {combinedData, monthOptions, selectedMonth, isOpen, isPermissionError, loadError} = state.context;

  const sorted = React.useMemo(
      () => combinedData?.sort((a, b) => new Date(a.metricDate) - new Date(b.metricDate)) || [],
      [combinedData]
  );

  // Transform data to use full label names as keys for proper legend display
  const chartData = React.useMemo(
    () => sorted.map(item => ({
        metricDate: item.metricDate,
        [UIStrings.HISTORICAL_USAGE.CHART.LEGEND_EGRESS]: item[KEY_EGRESS],
        [UIStrings.HISTORICAL_USAGE.CHART.LEGEND_STORAGE]: item[KEY_STORAGE],
        _available: item._available
      }))
    ,
    [sorted]
  );
  const maxValue = React.useMemo(() => getMaxValue(sorted), [sorted]);
  const valueTicks = React.useMemo(() => getValueTicks(maxValue, MAX_VALUE_TICKS), [maxValue]);
  const dateTicks = React.useMemo(() => getMetricDateTicks(sorted), [sorted]);

  const handleMonthSelect = (option) => {
    send({type: 'SELECT_MONTH', month: option});
  };

  const handleToggleDropdown = () => {
    send({type: 'TOGGLE_DROPDOWN'});
  };

  const tooltip = (p) => <UsageInsightsChartTooltip data={p.data} />;

  if (isPermissionError) {
    return (
      <div className="usage-insights-chart">
        <NxWarningAlert>{UIStrings.HISTORICAL_USAGE.CHART.PERMISSION_ERROR}</NxWarningAlert>
      </div>
    );
  }

  if (loadError) {
    return (
      <div className="usage-insights-chart">
        <NxErrorAlert>{loadError}</NxErrorAlert>
      </div>
    );
  }

  return (
      <div className="usage-insights-chart">
        <div className="usage-insights-chart-header">
          <div className="usage-insights-chart-title">
            <div>{UIStrings.HISTORICAL_USAGE.CHART.TITLE}</div>
            <div>{UIStrings.HISTORICAL_USAGE.CHART.SUB_TITLE}</div>
          </div>
          <NxStatefulNavigationDropdown
              isOpen={isOpen}
              onToggleCollapse={handleToggleDropdown}
              icon={faFilter}
              title={selectedMonth ? selectedMonth.label : 'Select Month'}
              className="usage-insights-chart-filter usage-insights-month-dropdown">
            <NxNavigationDropdown.MenuHeader>
              <NxH4>
                {UIStrings.HISTORICAL_USAGE.CHART.FILTER_OPTION_HEADER}
              </NxH4>
            </NxNavigationDropdown.MenuHeader>
            <div className={"usage-insights-filter-month-options"}>
              {monthOptions.map(option => (
                  <button key={option.key} className="nx-dropdown-link" onClick={() => handleMonthSelect(option)}>
                    {option.label}
                  </button>
              ))}
            </div>
          </NxStatefulNavigationDropdown>
        </div>
        <div className="usage-insights-bar-chart">
          <ResponsiveBar
            data={chartData}
            keys={[UIStrings.HISTORICAL_USAGE.CHART.LEGEND_EGRESS, UIStrings.HISTORICAL_USAGE.CHART.LEGEND_STORAGE]}
            indexBy="metricDate"
            groupMode="stacked"
            margin={{top: 8, right: 16, bottom: 92, left: 110}}

            enableLabel={false}
            enableGridX={false}
            enableGridY={true}

            valueScale={{type: 'linear', min: 0, max: maxValue, nice: false}}
            indexScale={{type: "band"}}
            colors={({id}) => (id === UIStrings.HISTORICAL_USAGE.CHART.LEGEND_STORAGE ? chartStorageColor : chartEgressColor)}

            padding={0.3}
            innerPadding={0}
            gridYValues={valueTicks.length > 0 ? valueTicks : undefined}

            axisLeft={{
              legend: UIStrings.HISTORICAL_USAGE.CHART.AXIS_EGRESS_STORAGE,
              legendOffset: -95,
              legendPosition: "middle",
              tickValues: valueTicks,
              format: (v) => HumanReadableUtils.bytesToString(Number(v)),
            }}
            axisBottom={{
              legend: UIStrings.HISTORICAL_USAGE.CHART.AXIS_DAYS,
              legendPosition: "middle",
              legendOffset: 47,
              tickValues: dateTicks,
              tickPadding: 8,
              tickSize: 10,
              format: (v) => formatAsShortDate(v),
            }}

            theme={{
              text: {
                fontFamily: "Inter, system-ui, -apple-system, Segoe UI, Roboto, 'Helvetica Neue', Arial, sans-serif",
                fontSize: "var(--nx-font-size-s)",
                fill: "var(--nx-color-chart-text)"
              },
              grid: {
                line: {
                  stroke: "var(--nx-color-chart-line)",
                  strokeWidth: 1,
                  opacity: 0.1
                }
              },
              axis: {
                domain: {
                  line: {
                    stroke: "var(--nx-color-chart-line)",
                    strokeWidth: 1
                  }
                },
                ticks: {
                  line: {
                    stroke: "var(--nx-color-chart-line)"
                  },
                  text: {
                    fill: "var(--nx-color-chart-text)",
                    fontSize: "var(--nx-font-size-xs)"
                  }
                },
                legend: {
                  text: {
                    fill: "var(--nx-color-chart-text)",
                    fontSize: "var(--nx-font-size-s)"
                  }
                }
              }
            }}

            tooltip={sorted.length > 0 ? tooltip : undefined}

            legends={[
              {
                anchor: "bottom",
                direction: "row",
                translateY: 85,
                itemWidth: 150,
                itemHeight: 16,
                itemsSpacing: 12,
                symbolSize: 14,
                symbolShape: "square",
                itemTextColor: "var(--nx-color-chart-text)"
              },
            ]}
          />
        </div>
      </div>
  );
}
