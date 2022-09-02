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
import {
  NxFontAwesomeIcon,
  NxTooltip,
  NxSmallThreatCounter,
  NxTextLink,
  NxP
} from '@sonatype/react-shared-components';
import {faBan, faExclamationCircle} from '@fortawesome/free-solid-svg-icons';
import UIStrings from '../../../../../constants/UIStrings';
import './IQServerColumns.scss';
import {useRepositoriesService} from '../RepositoriesContextProvider';

const {HEALTH_CHECK} = UIStrings.REPOSITORIES.LIST;

export default function IqPolicyViolationsCell({name}) {
  const [state] = useRepositoriesService();

  const {readFirewallRepositoryStatusError, firewallStatus} = state.context;

  const status = firewallStatus ? firewallStatus[name] : null;

  const isLoading = state.matches('readingFirewallStatus');

  const isLoadingError = !!readFirewallRepositoryStatusError;

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

  if (!status) {
    return (
      <NxTooltip title={HEALTH_CHECK.NOT_AVAILABLE_TOOLTIP_FS}>
        <NxFontAwesomeIcon icon={faBan} className="nxrm-unavailable-icon"/>
      </NxTooltip>
    );
  }

  const {
    criticalComponentCount,
    severeComponentCount,
    moderateComponentCount,
    quarantinedComponentCount,
    reportUrl,
    message,
    errorMessage
  } = status;

  if (errorMessage) {
    return (
      <NxP className="nxrm-rhc-loading-error">
        <NxFontAwesomeIcon icon={faExclamationCircle} />
        {errorMessage}
      </NxP>
    );
  }

  if (message) {
    return <NxP>{message}</NxP>;
  }

  return (
    <div className="nx-p nxrm-iq-policy-violations-counters">
      <NxSmallThreatCounter
        criticalCount={criticalComponentCount}
        severeCount={severeComponentCount}
        moderateCount={moderateComponentCount}
      />
      <NxTooltip title={HEALTH_CHECK.QUARANTINED_TOOLTIP}>
        <div>
          <NxFontAwesomeIcon icon={faBan} />
          {quarantinedComponentCount}
        </div>
      </NxTooltip>
      <NxTextLink href={reportUrl} external onClick={(e) => e.stopPropagation()} />
    </div>
  );
}
