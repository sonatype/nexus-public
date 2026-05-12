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
import { NxFontAwesomeIcon, NxH3 } from '@sonatype/react-shared-components';
import { faExclamationTriangle } from "@fortawesome/free-solid-svg-icons";

import UIStrings from '../../../constants/UIStrings';
import { ExtJS } from '@sonatype/nexus-ui-plugin';

export default function MaliciousRiskOnDiskComponentCount() {
  const { TITLE_PLURAL, TITLE_SINGULAR} = UIStrings.MALICIOUS_RISK.RISK_ON_DISK;
  const maliciousRiskOnDisk = ExtJS.useState(() => ExtJS.state().getValue('nexus.malware.count'));
  const testOverride = parseInt(localStorage.getItem('SONATYPE_TEST_MALWARE_BANNER') || '0', 10) || 0;
  const riskOnDiskCount = testOverride || (maliciousRiskOnDisk?.totalCount ?? 0);

  return (<div className={`malware-components-count ${riskOnDiskCount === 0 ? 'zero' : ''}`}>
    {riskOnDiskCount > 0 && <NxFontAwesomeIcon icon={faExclamationTriangle}/>}
    <NxH3 className="nxrm-malware-remediation-heading">
      <span className='nxrm-malware-components-count-value'>{riskOnDiskCount.toLocaleString()}</span>
      {riskOnDiskCount > 1 ? TITLE_PLURAL : TITLE_SINGULAR}
    </NxH3>
  </div>);
}
