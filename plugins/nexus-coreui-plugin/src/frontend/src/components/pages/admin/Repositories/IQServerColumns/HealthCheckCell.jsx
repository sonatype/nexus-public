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
import {NxFontAwesomeIcon, NxTooltip, NxP} from '@sonatype/react-shared-components';
import {faBan, faShieldAlt, faAward, faExclamationCircle} from '@fortawesome/free-solid-svg-icons';
import UIStrings from '../../../../../constants/UIStrings';
import './IQServerColumns.scss';
import {useRepositoriesService} from '../RepositoriesContextProvider';
import {canUpdateHealthCheck} from './IQServerHelpers';

const {HEALTH_CHECK} = UIStrings.REPOSITORIES.LIST;

export default function HealthCheckCell({name, openModal}) {
  const [state] = useRepositoriesService();

  const {enablingHealthCheckRepoName, enableHealthCheckError, readHealthCheckError, healthCheck} =
    state.context;

  const health = healthCheck ? healthCheck[name] : null;

  const isLoading =
    (state.matches('enablingHealthCheckSingleRepo') && enablingHealthCheckRepoName === name) ||
    (state.matches('enablingHealthCheckAllRepos') && !health?.enabled) ||
    state.matches('readingHealthCheck');

  const isLoadingError =
    (!!enableHealthCheckError && enablingHealthCheckRepoName === name) ||
    (!!enableHealthCheckError && !enablingHealthCheckRepoName) ||
    !!readHealthCheckError;

  const isDisabled =
    state.matches('enablingHealthCheckSingleRepo') && enablingHealthCheckRepoName !== name;

  const showModal = (e) => {
    if (isDisabled) {
      return;
    }
    e.stopPropagation();
    openModal();
  };

  if (isLoading) {
    return <NxP className="nxrm-rhc-loading-lbl">{HEALTH_CHECK.LOADING}</NxP>;
  }

  if (isLoadingError) {
    return (
      <NxP className="nxrm-rhc-loading-error">
        <NxFontAwesomeIcon icon={faExclamationCircle} />
        {HEALTH_CHECK.LOADING_ERROR}
      </NxP>
    );
  }

  if (!health || (!health.enabled && !canUpdateHealthCheck())) {
    return (
      <NxTooltip title={HEALTH_CHECK.NOT_AVAILABLE_TOOLTIP_HC}>
        <NxFontAwesomeIcon icon={faBan} className="nxrm-unavailable-icon" />
      </NxTooltip>
    );
  }

  if (!health.enabled) {
    return (
      <NxP
        className={isDisabled ? 'nxrm-rhc-analyze-btn-disabled' : 'nxrm-rhc-analyze-btn-enabled'}
        onClick={showModal}
      >
        {HEALTH_CHECK.ANALYZE_BUTTON}
      </NxP>
    );
  }

  if (health.analyzing) {
    return <NxP className="nxrm-rhc-analyzing-lbl">{HEALTH_CHECK.ANALYZING}</NxP>;
  }

  if (health.enabled) {
    return (
      <NxP className="nxrm-health-check-indicators">
        <NxFontAwesomeIcon icon={faShieldAlt} />
        {health.securityIssueCount}
        <NxFontAwesomeIcon icon={faAward} />
        {health.licenseIssueCount}
      </NxP>
    );
  }
}
