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
import React, {useState, useEffect, useMemo} from 'react';
import {useRouter} from '@uirouter/react';
import {useMachine} from '@xstate/react';
import {faChevronRight} from '@fortawesome/free-solid-svg-icons';
import {FontAwesomeIcon} from '@fortawesome/react-fontawesome';
import {
  ContentBody,
  ExtJS,
  Page,
  PageHeader,
  Section
} from '@sonatype/nexus-ui-plugin';
import {
  NxH1,
  NxH2,
  NxH3,
  NxP,
  NxTextLink,
  NxTile
} from '@sonatype/react-shared-components';

import lifecycleLogoDark from '../../../../../../art/logos/lifecycle_dark.svg';
import lifecycleLogoLight from '../../../../../../art/logos/lifecycle_light.svg';

import UIStrings from '../../../../constants/UIStrings';
import {ROUTE_NAMES} from '../../../../routerConfig/routeNames/routeNames';
import {GlobalEvaluationSettingsMachine} from '@sonatype/nexus-ui-plugin';

import './IqServer.scss';

function useDarkMode() {
  const htmlEl = document.documentElement;
  const [isDarkMode, setDarkMode] = useState(htmlEl.classList.contains('nx-html--dark-mode'));

  useEffect(() => {
    const observer = new MutationObserver(() => {
      setDarkMode(htmlEl.classList.contains('nx-html--dark-mode'));
    });

    observer.observe(htmlEl, {attributes: true, attributeFilter: ['class']});

    return () => observer.disconnect();
  }, []);

  return isDarkMode;
}

export default function SonatypeLifecycle() {
  const router = useRouter();
  const isDarkMode = useDarkMode();
  const lifecycleLogo = isDarkMode ? lifecycleLogoDark : lifecycleLogoLight;
  const [state] = useMachine(GlobalEvaluationSettingsMachine, {devTools: true});

  const loading = state.matches('loading');
  const globalSettings = state.context.data;

  if (!ExtJS.useUser()) {
    return null;
  }

  function navigateToIqServer(e) {
    e.preventDefault();
    router.stateService.go(ROUTE_NAMES.ADMIN.IQ.CONNECTED);
  }

  function navigateToHostedReposEval() {
    router.stateService.go(ROUTE_NAMES.ADMIN.IQ.HOSTED_REPOS_EVAL.ROOT);
  }

  const settingsDescription = useMemo(() => {
    if (loading) {
      return UIStrings.LOADING;
    }

    if (!globalSettings) {
      return UIStrings.SONATYPE_LIFECYCLE.GLOBAL_EVALUATION_SETTINGS.description;
    }

    const {activityTimeFrame, artifactLatestVersions, policyEvaluationStage, monitoredRepoCount, totalRepoCount, numberOfCustomRepositories} = globalSettings;
    const globalCount = (monitoredRepoCount !== null && monitoredRepoCount !== undefined && numberOfCustomRepositories !== null && numberOfCustomRepositories !== undefined)
      ? Math.max(0, monitoredRepoCount - numberOfCustomRepositories)
      : monitoredRepoCount;
    const globalEvalText = (globalCount !== null && globalCount !== undefined && totalRepoCount !== null && totalRepoCount !== undefined)
      ? `${globalCount}/${totalRepoCount}`
      : 'N/A';
    const customEvalText = (numberOfCustomRepositories !== null && numberOfCustomRepositories !== undefined)
      ? numberOfCustomRepositories
      : 'N/A';
    // Format policyEvaluationStage from 'STAGE_RELEASE' → 'Stage Release' for display
    const stageLabel = policyEvaluationStage
      ? policyEvaluationStage.split('_').map(w => w.charAt(0) + w.slice(1).toLowerCase()).join(' ')
      : policyEvaluationStage;
    // Always show Activity Time Frame and Latest Deployed Versions together when
    // both are configured (the form saves both since CLM-41306). Legacy data with
    // only one set is still rendered correctly — the missing piece is skipped.
    // When both are missing (corrupt/legacy data) the depth section is omitted
    // entirely to avoid rendering a leading " | " separator.
    const depthParts = [];
    if (activityTimeFrame) {
      depthParts.push(`Last ${activityTimeFrame} Days`);
    }
    if (artifactLatestVersions > 0) {
      depthParts.push(`${artifactLatestVersions} Latest Deployed Versions`);
    }
    const depthSummary = depthParts.length > 0 ? `${depthParts.join(' | ')} | ` : '';
    return `${depthSummary}${stageLabel} | Global Evaluation: ${globalEvalText} | Custom Evaluation: ${customEvalText}`;
  }, [loading, globalSettings]);

  return <Page>
    <PageHeader>
      <div className="sonatype-lifecycle-header">
        <div className="breadcrumb-header">
          <NxTextLink href="#" onClick={navigateToIqServer}>
            {UIStrings.IQ_SERVER.MENU.text}
          </NxTextLink>
          <span className="breadcrumb-separator"> / </span>
          <span className="breadcrumb-current">{UIStrings.SONATYPE_LIFECYCLE.MENU.text}</span>
        </div>
        <div className="lifecycle-title">
          <img
            className="lifecycle-icon"
            src={lifecycleLogo}
            alt="Lifecycle"
          />
          <NxH1>{UIStrings.SONATYPE_LIFECYCLE.MENU.text}</NxH1>
        </div>
      </div>
    </PageHeader>
    <ContentBody className="nxrm-sonatype-lifecycle">
      <Section>
        <NxH2>{UIStrings.SONATYPE_LIFECYCLE.HOSTED_REPOSITORIES_EVALUATION.title}</NxH2>
        <NxP>{UIStrings.SONATYPE_LIFECYCLE.HOSTED_REPOSITORIES_EVALUATION.description}</NxP>

        <NxTile className="lifecycle-card" onClick={navigateToHostedReposEval} style={{cursor: 'pointer'}}>
          <NxTile.Content className="lifecycle-card-content">
            <div className="card-text">
              <NxH3>{UIStrings.SONATYPE_LIFECYCLE.GLOBAL_EVALUATION_SETTINGS.title}</NxH3>
              <NxP>{settingsDescription}</NxP>
            </div>
            <div className="card-arrow">
              <FontAwesomeIcon icon={faChevronRight} />
            </div>
          </NxTile.Content>
        </NxTile>
      </Section>
    </ContentBody>
  </Page>;
}
