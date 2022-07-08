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
import {NxFontAwesomeIcon, NxTooltip} from '@sonatype/react-shared-components';
import {ExtJS} from '@sonatype/nexus-ui-plugin';
import {faBan, faShieldAlt, faAward, faExclamationCircle} from '@fortawesome/free-solid-svg-icons';
import UIStrings from '../../../../../constants/UIStrings';

const {HEALTH_CHECK} = UIStrings.REPOSITORIES.LIST;

export default function RepositoryHealthCheck({name, health, current, openModal}) {
  const {enablingHealthCheckRepoName, enableHealthCheckError, readHealthCheckError} =
    current.context;

  const canUpdateHealthCheck = ExtJS.checkPermission('nexus:healthcheck:update');

  const isLoading =
    (current.matches('enablingHealthCheckSingleRepo') && enablingHealthCheckRepoName === name) ||
    (current.matches('enablingHealthCheckAllRepos') && !health?.enabled) ||
    current.matches('readingHealthCheck');

  const isLoadingError =
    (!!enableHealthCheckError && enablingHealthCheckRepoName === name) ||
    (!!enableHealthCheckError && !enablingHealthCheckRepoName) ||
    !!readHealthCheckError;

  const isDisabled =
    current.matches('enablingHealthCheckSingleRepo') && enablingHealthCheckRepoName !== name;

  const showModal = (e) => {
    if (isDisabled) {
      return;
    }
    e.stopPropagation();
    openModal();
  };

  if (isLoading) {
    return <p className="nx-p nxrm-rhc-loading-lbl">{HEALTH_CHECK.LOADING}</p>;
  }

  if (isLoadingError) {
    return (
      <p className="nx-p nxrm-rhc-loading-error">
        <NxFontAwesomeIcon icon={faExclamationCircle} />
        {HEALTH_CHECK.LOADING_ERROR}
      </p>
    );
  }

  if (!health || (!health.enabled && !canUpdateHealthCheck)) {
    return (
      <NxTooltip title={HEALTH_CHECK.NOT_AVAILABLE_TOOLTIP}>
        <NxFontAwesomeIcon icon={faBan} />
      </NxTooltip>
    );
  }

  if (!health.enabled) {
    return (
      <>
        <p
          className={`nx-p ${
            isDisabled ? 'nxrm-rhc-analyze-btn-disabled' : 'nxrm-rhc-analyze-btn-enabled'
          }`}
          onClick={showModal}
        >
          {HEALTH_CHECK.ANALYZE_BUTTON}
        </p>
      </>
    );
  }

  if (health.analyzing) {
    return <p className="nx-p nxrm-rhc-analyzing-lbl">{HEALTH_CHECK.ANALYZING}</p>;
  }

  if (health.enabled) {
    return (
      <div className="nx-p nxrm-health-check-indicators">
        <NxFontAwesomeIcon icon={faShieldAlt} />
        {health.securityIssueCount}
        <NxFontAwesomeIcon icon={faAward} />
        {health.licenseIssueCount}
      </div>
    );
  }
}
