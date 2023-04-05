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
  NxLoadWrapper,
  NxButton
} from '@sonatype/react-shared-components';

import UIStrings from '../../../../constants/UIStrings';
import {Page, PageHeader, PageTitle, ExtJS} from '@sonatype/nexus-ui-plugin';
import './InsightFrontend.scss';
import {faBinoculars} from '@fortawesome/free-solid-svg-icons';
import InsightFrontendMachine from './InsightFrontendMachine';
import DownloadsByRepositoryName from './DownloadsByRepositoryName';
import DownloadsByUsername from './DownloadsByUsername';
import DownloadsByIpAddress from './DownloadsByIpAddress';

export default function InsightFrontend() {
  const [state] = useMachine(InsightFrontendMachine, {devTools: true});
  const isLoading = state.matches('loading');
  const {downloadsByDay, downloadsByDayNonVulnerable, totalDownloads} = state.context;

  const isProEdition = ExtJS.isProEdition();
  let findAndFixLog4jUrl = 'https://links.sonatype.com/products/nexus/log4j/find-and-fix/oss';
  let resourceCentreUrl = 'https://links.sonatype.com/products/nexus/log4j/resource-center/oss';
  let lifecycleUrl = 'https://links.sonatype.com/products/nexus/log4j/lifecycle/oss';
  let firewallUrl = 'https://links.sonatype.com/products/nexus/log4j/firewall/oss';

  if (isProEdition) {
    findAndFixLog4jUrl = 'https://links.sonatype.com/products/nexus/log4j/find-and-fix/pro';
    resourceCentreUrl = 'https://links.sonatype.com/products/nexus/log4j/resource-center/pro';
    lifecycleUrl = 'https://links.sonatype.com/products/nexus/log4j/lifecycle/pro';
    firewallUrl = 'https://links.sonatype.com/products/nexus/log4j/firewall/pro';
  }

  function createChartView() {
    return downloadsByDay.length > 0 || downloadsByDayNonVulnerable.length > 0 ?
        <div>
        <div className="chart-header">
          <h3 className="nx-h3">Log4j Consumption</h3>
          <span>This chart shows a breakdown of your organisation's Maven log4j component consumption</span>
        </div>
        <div className="chart-container">
          <ResponsiveLine data={
            [
              {
                id: 'log4shell',
                data: downloadsByDay.map((download) => {
                  return {
                    x: new Date(download.day),
                    y: download.downloadCount
                  }
                })
              }
              ,
              {
                id: 'non-log4shell',
                data: downloadsByDayNonVulnerable.map((download) => {
                  return {
                    x: new Date(download.day),
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
                            '#FF6685',
                            '#008FCC'
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
        </div> </div> : <div className="chart-container empty-chart">No Data Available</div>
  }

  function retry() {
    // Do nothing for now
  }

  function openFirewallPage() {
    window.open(firewallUrl, '_blank');
  }

  return (
      <Page className="insight-frontend-root">
        <PageHeader>
          <PageTitle icon={faBinoculars} {...UIStrings.LOG4J_VISUALIZER.MENU} />
        </PageHeader>
        <NxLoadWrapper loading={isLoading} retryHandler={retry}>
          {() => <>
            <div className="insight-frontend-frame">
              <div className="insight-frontend-container">
                <div className="nx-page-title">
                  <hgroup className="nx-page-title__headings">
                    <h2>
                      Overview
                    </h2>
                  </hgroup>
                  <div className="nx-page-title__description insight-frontend-title">
                    <p>
                      Log4j is the most popular logging framework used by Java software.
                      It has been reported to contain serious vulnerabilities,
                      including <a href="https://nvd.nist.gov/vuln/detail/CVE-2021-44228"
                         target="_blank"
                         rel="noopener noreferrer">CVE-2021-44228</a> (also known as "log4shell").
                      The Organizational Insights below provide visibility into log4j consumption through
                      this Nexus Repository instance and specifically show downloads impacted by CVE-2021-44228.
                    </p>
                    <div className="overview-content-section">
                      <div className="overview-content-section-img">
                        <img width="300" src="/static/rapture/resources/images/firewall_illustration_final.png"/>
                      </div>
                      <div>
                        <h2>Reduce Risk and Keep Innovating</h2>
                        <p>
                          Want to start blocking malicious and suspicious open-source components from entering your software development lifecycle?

                          Nexus Firewall’s AI uses behavioral analysis scans over 600,000 new component releases every month
                          to detect and block malicious attacks. Nexus Firewall discovered and blocked over 97,000 new malicious attacks since 2021.
                        </p>
                        <NxButton
                            className="overview-content-section-item-btn"
                            variant="primary"
                            onClick={openFirewallPage}
                        >
                          Explore Nexus Firewall
                        </NxButton>
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            </div>
            <div className="insight-frontend-frame">
              <div className="insight-frontend-container">
                <div className="nx-page-title">
                  <hgroup className="nx-page-title__headings">
                    <h2>
                      Our Recommendations
                    </h2>
                  </hgroup>
                </div>
                <div className="nx-tile-content">
                  <div className="nx-grid-row">
                    <section className="nx-grid-col nx-grid-col--50">
                      <h4>
                        Short term
                      </h4>
                      <ol className="recommendations-container">
                        <li>
                          <strong>Upgrade your log4j dependencies</strong> to a non-vulnerable version.
                        </li>
                        <li>Refer to the guidance in Sonatype’s <a href={findAndFixLog4jUrl} target="_blank"
                                                                   rel="noopener noreferrer">Find and Fix Log4j
                          announcement</a>.
                        </li>
                        <li><strong>Don’t delete vulnerable log4j versions</strong> from your repositories except as a
                          last resort. Fixing critical problems can be harder when missing dependencies as that can cause
                          builds to break.
                        </li>
                        <li>Stay up to date with the <a href={resourceCentreUrl} target="_blank"
                                                        rel="noopener noreferrer">latest Log4j development</a>.
                        </li>
                      </ol>
                    </section>
                    <section className="nx-grid-col nx-grid-col--50">
                      <h4>
                        Long term
                      </h4>
                      <ol className="recommendations-container">
                        <li>Consider <strong>reducing anonymous access to your repositories</strong> so that you can more
                          easily understand who is consuming vulnerable dependencies.
                        </li>
                        <li><strong>Block vulnerable open source components and malicious attacks</strong> from being
                          downloaded into your repositories using <a href={firewallUrl} target="_blank"
                                                                     rel="noopener noreferrer">Nexus Firewall</a>.
                        </li>
                        <li><strong>Reduce remediation time</strong> by using <a href={lifecycleUrl} target="_blank"
                                                                                 rel="noopener noreferrer">Nexus
                          Lifecycle</a> for continuous application monitoring.
                        </li>
                      </ol>
                    </section>
                  </div>
                  <br/>
                </div>
              </div>
            </div>
            <div className="insight-frontend-frame">
              <div className="insight-frontend-container">
                <div className="nx-page-title">
                  <hgroup className="nx-page-title__headings">
                    <h2>
                      Organization Insights
                    </h2>
                  </hgroup>
                  <div className="nx-page-title__description insight-frontend-title">
                    <div className="total-downloads-affected-count">
                      <strong>
                        Total Downloads Affected by Log4shell : <span className="total-downloads-affected-by-log4j-shell">{totalDownloads}</span>
                      </strong>
                    </div>
                    <p>
                      This data is not live and is refreshed whenever the Statistics - Recalculate
                      vulnerabilities statistics task runs. Note that the Log4j Visualizer only
                      captures information about the log4j component in Maven and only identifies those impacted by
                      <a href="https://nvd.nist.gov/vuln/detail/CVE-2021-44228"
                         target="_blank"
                         rel="noopener noreferrer"> CVE-2021-44228</a>. It does not currently identify or track other log4j
                      vulnerabilities. You must run the <em>Statistics - Recalculate vulnerabilities
                      statistics</em> task to refresh the data
                      below. <a
                        href="https://help.sonatype.com/repomanager3/nexus-repository-administration/capabilities/log4j-visualizer"
                        target="_blank" rel="noopener noreferrer">Learn more about the Log4J Visualizer</a>.
                    </p>
                  </div>
                </div>
                <hr/>
                <div className="insight-frontend-content">
                  <div className="insight-frontend-content-item">
                    {createChartView()}
                    <DownloadsByRepositoryName downloadsByRepositoryName={state.context.downloadsByRepositoryName}/>
                  </div>
                  <div className="insight-frontend-content-item">
                    <DownloadsByUsername downloadsByUsername={state.context.downloadsByUsername}/>

                    <DownloadsByIpAddress downloadsByIpAddress={state.context.downloadsByIpAddress}/>

                  </div>
                  <p>
                    Sonatype is providing this Log4j Visualizer for a limited time to all Nexus Repository OSS and PRO
                    users due to the urgent threat that the log4j vulnerability poses to the global software community.
                    All access and use of the Log4j Visualizer is governed by the terms of your agreement with Sonatype or,
                    in the absence of such, <a href="https://www.sonatype.com/campaign/software-evaluation-agreement" target="_blank" rel="noopener noreferrer">these terms</a>.
                    We may update or remove this feature completely in future versions.
                    <a href="https://www.sonatype.com/products/firewall" target="_blank" rel="noopener noreferrer"> Get more information on log4J</a>.
                  </p>
                </div>
              </div>
            </div>
          </>
          }
        </NxLoadWrapper>
      </Page>
  );
}
