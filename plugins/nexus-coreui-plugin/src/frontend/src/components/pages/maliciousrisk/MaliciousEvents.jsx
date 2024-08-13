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
import {NxFontAwesomeIcon, NxGrid, NxH3, NxMeter, NxTextLink, NxTooltip} from "@sonatype/react-shared-components";
import {faExclamationTriangle, faInfoCircle} from "@fortawesome/free-solid-svg-icons";
import React from "react";
import UIStrings from "../../../constants/UIStrings";

const {
  UNPROTECTED_MALWARE,
  HOW_TO_PROTECT,
  PROXY_PROTECTION
} = UIStrings.MALICIOUS_RISK.MALICIOUS_EVENTS;

export default function MaliciousEvents({totalMaliciousRiskCount, totalProxyRepositoryCount}) {
  return (
      <NxGrid.Column className="nxrm-component-malicious nx-grid-col--50">
        <NxGrid.ColumnSection>
          <NxGrid.Header>
            <NxH3>
              <NxFontAwesomeIcon icon={faExclamationTriangle}/>
              <span>{UNPROTECTED_MALWARE.TEXT}</span>
            </NxH3>
          </NxGrid.Header>
          <p>{totalMaliciousRiskCount} {UNPROTECTED_MALWARE.DESCRIPTION}</p>
        </NxGrid.ColumnSection>
        <NxGrid.ColumnSection>
          <NxGrid.Header>
            <NxH3>{PROXY_PROTECTION.TITLE}</NxH3>
          </NxGrid.Header>
          <NxMeter data-testid="meter"
                   value={0}
                   max={totalProxyRepositoryCount}>
            {`0 / ${totalProxyRepositoryCount} total`}
          </NxMeter>
          <div className="nxrm-label-container">
            <span className="nxrm-label-text-main"> 0 / {totalProxyRepositoryCount} total</span>
            <span className="nxrm-label-text-info"> {PROXY_PROTECTION.DESCRIPTION}
              <NxTooltip
                  title={PROXY_PROTECTION.TOOLTIP}>
                <NxFontAwesomeIcon icon={faInfoCircle}/>
              </NxTooltip>
            </span>
          </div>
        </NxGrid.ColumnSection>
        <NxGrid.ColumnSection>
          <NxTextLink href={HOW_TO_PROTECT.URL} external>{HOW_TO_PROTECT.TEXT}</NxTextLink>
        </NxGrid.ColumnSection>
      </NxGrid.Column>
  );
}
