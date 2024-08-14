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
  NxErrorStatusIndicator,
  NxFontAwesomeIcon,
  NxGrid,
  NxH2,
  NxH3, NxTile,
  NxTooltip
} from "@sonatype/react-shared-components";
import {faInfoCircle} from "@fortawesome/free-solid-svg-icons";

import UIStrings from "../../../constants/UIStrings";

const {
  COMPONENTS_IN_HIGH_RISK_ECOSYSTEMS: {
    TEXT,
    REPOSITORIES_PROTECTED,
    PUBLIC_MALICIOUS_COMPONENT,
    TOOLTIP
  }} = UIStrings.MALICIOUS_RISK;

export default function MaliciousHighRiskEcosystems({countByEcosystem}) {

  return (
      <>
        <NxH2>{TEXT}</NxH2>
        <NxTile className="nxrm-high-risk-ecosystems">
          <NxTile.Content>
            <NxGrid.Row>
              {countByEcosystem?.map(({ecosystem, count}) => (
                <NxGrid.Column key={ecosystem} className="nx-grid-col--33" data-testid={ecosystem}>
                  <NxGrid.Header>
                    <NxH2>
                      {ecosystem}
                      <NxErrorStatusIndicator>{REPOSITORIES_PROTECTED}</NxErrorStatusIndicator>
                    </NxH2>
                  </NxGrid.Header>
                  <div className="nxrm-public-malicious-components">
                    <NxH3>
                      {PUBLIC_MALICIOUS_COMPONENT}
                      <NxTooltip title={TOOLTIP}>
                        <NxFontAwesomeIcon icon={faInfoCircle}/>
                      </NxTooltip>
                    </NxH3>
                    <NxH2>{count}</NxH2>
                  </div>
                </NxGrid.Column>
              ))}
            </NxGrid.Row>
          </NxTile.Content>
        </NxTile>
      </>
  )
}
