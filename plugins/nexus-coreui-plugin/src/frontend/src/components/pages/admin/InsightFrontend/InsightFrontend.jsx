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
import {ResponsiveLine} from '@nivo/line';

import {
  NxFilterInput,
  NxLoadWrapper,
  NxStatefulInfoAlert,
  NxTable
} from '@sonatype/react-shared-components';

import {
  Page
} from '@sonatype/nexus-ui-plugin';
import './InsightFrontend.scss';
import InsightFrontendMachine, {
  FILTER_BY_REPOSITORY_NAME,
  FILTER_BY_IP_ADDRESS,
  FILTER_BY_USERNAME
} from './InsightFrontendMachine';
import ExtJS from "@sonatype/nexus-ui-plugin/src/frontend/src/interface/ExtJS";

export default function InsightFrontend() {
  const [current, send] = useMachine(InsightFrontendMachine, {devTools: true});
  const {
    downloadsByRepositoryName,
    downloadsByIpAddress,
    downloadsByUsername,
    downloadsByDay,
    downloadsByDayNonVulnerable,
    totalDownloads,
  } = current.context.data;
  const {
    filterByRepositoryNameValue,
    filterByIpAddressValue,
    filterByUsernameValue
  } = current.context.filters;

  const edition = ExtJS.state().getEdition();
  let findAndFixLog4jUrl = 'https://links.sonatype.com/products/nexus/log4j/find-and-fix/oss';
  let resourceCentreUrl = 'https://links.sonatype.com/products/nexus/log4j/resource-center/oss';
  let lifecycleUrl = 'https://links.sonatype.com/products/nexus/log4j/lifecycle/oss';
  let firewallUrl = 'https://links.sonatype.com/products/nexus/log4j/firewall/oss';
  let mailAddress = 'mailto:nexus-feedback@sonatype.com?subject=OSS Log4j Visualizer';

  if (edition === 'PRO') {
    findAndFixLog4jUrl = 'https://links.sonatype.com/products/nexus/log4j/find-and-fix/pro';
    resourceCentreUrl = 'https://links.sonatype.com/products/nexus/log4j/resource-center/pro';
    lifecycleUrl = 'https://links.sonatype.com/products/nexus/log4j/lifecycle/pro';
    firewallUrl = 'https://links.sonatype.com/products/nexus/log4j/firewall/pro';
    mailAddress = 'mailto:nexus-feedback@sonatype.com?subject=PRO Log4j Visualizer';
  }

  function onFilterByRepositoryName(value) {
    send({
      type: 'FILTER_TABLE_BY',
      value: {
        filterType: FILTER_BY_REPOSITORY_NAME,
        filterValue: value
      }
    });
  }

  function onFilterByIpAddress(value) {
    send({
      type: 'FILTER_TABLE_BY',
      value: {
        filterType: FILTER_BY_IP_ADDRESS,
        filterValue: value
      }
    });
  }

  function onFilterByUsername(value) {
    send({
      type: 'FILTER_TABLE_BY',
      value: {
        filterType: FILTER_BY_USERNAME,
        filterValue: value
      }
    });
  }

  function createChartView() {
    return downloadsByDay.length > 0 || downloadsByDayNonVulnerable.length > 0 ?
        <div className="chart-container">
          <ResponsiveLine data={
            [
              {
                id: 'log4shell',
                data: downloadsByDay.map((download) => {
                  return {
                    x: new Date(download.day.year, download.day.monthValue - 1, download.day.dayOfMonth),
                    y: download.downloadCount
                  }
                })
              }
              ,
              {
                id: 'non-log4shell',
                data: downloadsByDayNonVulnerable.map((download) => {
                  return {
                    x: new Date(download.day.year, download.day.monthValue - 1, download.day.dayOfMonth),
                    y: download.downloadCount
                  }
                })
              }
            ]
          }
                          margin={{top: 50, right: 110, bottom: 50, left: 60}}
                          yScale={{
                            type: 'linear',
                            stacked: true
                          }}
                          xScale={{
                            type: 'time',
                            format: '%Y-%m-%d',
                            precision: 'day',
                            useUTC: false
                          }}
                          axisLeft={{
                            legend: 'Number of Downloads',
                            legendPosition: 'middle',
                            legendOffset: -40
                          }}
                          axisBottom={{
                            legend: 'Dates',
                            format: '%b-%d',
                            legendOffset: 40,
                            legendPosition: 'middle'
                          }}
                          colors={[
                            '#EF889A',
                            '#46A2E1'
                          ]}
                          xFormat="time:%Y-%m-%d"
                          enablePoints={false}
                          useMesh={true}
                          lineWidth={3}
                          crosshairType={'cross'}
                          legends={[
                            {
                              anchor: 'top-right',
                              direction: 'column',
                              justify: false,
                              translateX: 100,
                              translateY: 0,
                              itemsSpacing: 0,
                              itemDirection: 'left-to-right',
                              itemWidth: 80,
                              itemHeight: 20,
                              symbolSize: 14,
                              symbolShape: 'circle'
                            }
                          ]}
          />
        </div> : <div className="chart-container empty-chart">No Data Available</div>
  }

  function retry() {
    // Do nothing for now
  }

  return (
      <Page>
        <NxLoadWrapper loading={current.matches('loading')} retryHandler={retry}>
          {() => <>
            <NxStatefulInfoAlert>
              Sonatype is providing this Log4j Visualizer for a limited time to all Nexus Repository OSS and
              PRO users due to the urgent threat that the log4j vulnerability poses to the global software community.
              All access and use of the Log4j Visualizer is governed by the terms of your agreement with Sonatype or,
              in the absence of such, <a href="https://www.sonatype.com/campaign/software-evaluation-agreement" target="_blank" rel="noopener noreferrer">these terms</a>.
              We may update or remove this feature completely in future versions.
            </NxStatefulInfoAlert>
            <div className="nx-page-title">
              <hgroup className="nx-page-title__headings">
                <h1 className="nx-h1">
                  Organization Insights
                </h1>
                <p className="nx-page-title__sub-title">
                  Total Downloads Affected by Log4shell : <strong>{totalDownloads}</strong>
                </p>
              </hgroup>
              <div className="nx-page-title__description">
                <p className="nx-p">
                  Log4j is the most popular logging framework used by Java software. It has been reported to contain serious vulnerabilities, including <a href="https://nvd.nist.gov/vuln/detail/CVE-2021-44228" target="_blank" rel="noopener noreferrer">CVE-2021-44228</a> (also known as
                  "log4shell"). The data below provides insight into log4j consumption through this Nexus Repository instance and specifically shows downloads impacted by
                  CVE-2021-44228. This data is not live and is refreshed whenever the Statistics - Recalculate vulnerabilities statistics task runs. Note that the Log4j Visualizer only
                  captures information about the log4j component in Maven and only identifies those impacted by CVE-2021-44228. It does not currently identify or track other log4j
                  vulnerabilities. You must run the <em>Statistics - Recalculate vulnerabilities statistics</em> task to refresh the data
                  below. <a href="https://help.sonatype.com/repomanager3/nexus-repository-administration/capabilities/log4j-visualizer" target="_blank" rel="noopener noreferrer">Learn more</a>.
                </p>
              </div>
            </div>
            <div className="nx-page-title">
              <hgroup className="nx-page-title__headings">
                <h1 className="nx-h1">
                  Recommendations
                </h1>
              </hgroup>
              <div className="nx-page-title__description">
                <h4>
                  Short term
                </h4>
                <ol className="recommendations-container">
                  <li>Encourage development teams to <strong>upgrade their log4j dependencies</strong> to a non-vulnerable version.</li>
                  <li>Refer to the guidance in Sonatype’s <a href={findAndFixLog4jUrl} target="_blank" rel="noopener noreferrer">Find and Fix Log4j announcement</a>.</li>
                  <li><strong>Don’t delete vulnerable log4j versions</strong> from your repositories except as a last resort. Fixing critical problems can be harder when missing dependencies as that can cause builds to break.</li>
                  <li>Stay up to date with the <a href={resourceCentreUrl} target="_blank" rel="noopener noreferrer">latest Log4j development</a>.</li>
                </ol>
                <h4>
                  Long term
                </h4>
                <ol className="recommendations-container">
                  <li>Consider <strong>reducing anonymous access to your repositories</strong> so that you can more easily understand who is consuming vulnerable dependencies.</li>
                  <li><strong>Block vulnerable open source components and malicious attacks</strong> from being downloaded into your repositories using <a href={firewallUrl} target="_blank" rel="noopener noreferrer">Nexus Firewall</a>.</li>
                  <li><strong>Reduce remediation time</strong> by using <a href={lifecycleUrl} target="_blank" rel="noopener noreferrer">Nexus Lifecycle</a> for continuous application monitoring.</li>
                </ol>
              </div>
            </div>
            <div className="insight-frontend-content">
              <div className="insight-frontend-content-item">


                <div className="nx-scrollable nx-table-container">
                  <NxTable className="nx-table">
                    <NxTable.Head>
                      <NxTable.Row>
                        <NxTable.Cell>Repository</NxTable.Cell>
                        <NxTable.Cell>Downloads</NxTable.Cell>
                      </NxTable.Row>
                      <NxTable.Row isFilterHeader>
                        <NxTable.Cell>
                          <NxFilterInput placeholder="Type a name"
                                         id="repositoryFilter"
                                         onChange={onFilterByRepositoryName}
                                         value={filterByRepositoryNameValue}
                          />
                        </NxTable.Cell>
                      </NxTable.Row>
                    </NxTable.Head>
                    <NxTable.Body emptyMessage="No data">
                      {
                        downloadsByRepositoryName.map((row, index) => (
                            <NxTable.Row key={index}>
                              <NxTable.Cell>{row.identifier}</NxTable.Cell>
                              <NxTable.Cell>{row.downloadCount}</NxTable.Cell>
                            </NxTable.Row>
                        ))
                      }
                    </NxTable.Body>
                  </NxTable>
                </div>
                {createChartView()}
              </div>
              <div className="insight-frontend-content-item">

                <div className="nx-scrollable nx-table-container">
                  <NxTable className="nx-table">
                    <NxTable.Head>
                      <NxTable.Row>
                        <NxTable.Cell>Username</NxTable.Cell>
                        <NxTable.Cell>Downloads</NxTable.Cell>
                      </NxTable.Row>
                      <NxTable.Row isFilterHeader>
                        <NxTable.Cell>
                          <NxFilterInput placeholder="Type a name"
                                         id="usernameFilter"
                                         onChange={onFilterByUsername}
                                         value={filterByUsernameValue}
                          />
                        </NxTable.Cell>
                      </NxTable.Row>
                    </NxTable.Head>
                    <NxTable.Body emptyMessage="No data">
                      {downloadsByUsername.map((row, index) =>
                          <NxTable.Row key={index}>
                            <NxTable.Cell>{row.identifier}</NxTable.Cell>
                            <NxTable.Cell>{row.downloadCount}</NxTable.Cell>
                          </NxTable.Row>
                      )}
                    </NxTable.Body>
                  </NxTable>
                </div>

                <div className="nx-scrollable nx-table-container">
                  <NxTable className="nx-table">
                    <NxTable.Head>
                      <NxTable.Row>
                        <NxTable.Cell>IP address</NxTable.Cell>
                        <NxTable.Cell>Downloads</NxTable.Cell>
                      </NxTable.Row>
                      <NxTable.Row isFilterHeader>
                        <NxTable.Cell>
                          <NxFilterInput placeholder="Type a name"
                                         id="ipAddressFilter"
                                         onChange={onFilterByIpAddress}
                                         value={filterByIpAddressValue}
                          />
                        </NxTable.Cell>
                      </NxTable.Row>
                    </NxTable.Head>
                    <NxTable.Body emptyMessage="No data">

                      {downloadsByIpAddress.map((row, index) =>
                          <NxTable.Row key={index}>
                            <NxTable.Cell>{row.identifier}</NxTable.Cell>
                            <NxTable.Cell>{row.downloadCount}</NxTable.Cell>
                          </NxTable.Row>
                      )}
                    </NxTable.Body>
                  </NxTable>
                </div>

              </div>
              <div>
                <h1>Feedback</h1>
                <p>Interested in knowing other vulnerabilities in your repositories? Let us know what would you like to see. <a href={mailAddress}>Contact us</a></p>
              </div>
            </div>
          </>
          }
        </NxLoadWrapper>
      </Page>
  );
}
