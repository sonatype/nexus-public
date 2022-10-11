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
import React, {useRef, useEffect, useState} from 'react';
import {NxH2, NxButtonBar, NxButton} from '@sonatype/react-shared-components';
import UIStrings from '../../../../../constants/UIStrings';
import {canReadHealthCheckDetail} from './IQServerHelpers';

import './IQServerColumns.scss';

const {CAPTION, HELP_BUTTON, DETAILS_BUTTON} = UIStrings.REPOSITORIES.LIST.HEALTH_CHECK.SUMMARY;

const HELP_URL = 'http://links.sonatype.com/products/nexus/rhc/manual-remediation-with-rhc';
export default function HealthCheckSummary({healthCheckData, baseTopPosition, close}) {
  const {summaryUrl, detailUrl, iframeHeight, iframeWidth} = healthCheckData;

  const summaryRef = useRef();

  const [topPosition, setTopPosition] = useState("-100%");

  const canReadDetails = canReadHealthCheckDetail();

  useEffect(() => {
    const repoListTopPosition = document
      .querySelector('.nxrm-content-body')
      .getBoundingClientRect().y;
    const initialTopPosition = baseTopPosition - repoListTopPosition;
    const maxSummaryY =
      window.innerHeight - (repoListTopPosition + summaryRef.current.offsetHeight) - 28;
    const position = initialTopPosition < maxSummaryY ? initialTopPosition : maxSummaryY;
    setTopPosition(position);
  }, []);

  const openInNewTab = (e, url) => {
    e.stopPropagation();
    window.open(url, '_blank');
  };

  return (
    <div
      className="nxrm-health-check-summary"
      style={{
        top: topPosition
      }}
      onMouseLeave={close}
      ref={summaryRef}
      data-testid="nxrm-health-check-summary"
    >
      <NxH2>{CAPTION}</NxH2>
      <iframe
        title={CAPTION}
        src={summaryUrl}
        style={{
          height: iframeHeight,
          width: iframeWidth
        }}
      />
      <NxButtonBar>
        <NxButton variant="tertiary" onClick={(e) => openInNewTab(e, HELP_URL)}>
          {HELP_BUTTON}
        </NxButton>
        <NxButton onClick={(e) => openInNewTab(e, detailUrl)} disabled={!canReadDetails}>
          {DETAILS_BUTTON}
        </NxButton>
      </NxButtonBar>
    </div>
  );
}
