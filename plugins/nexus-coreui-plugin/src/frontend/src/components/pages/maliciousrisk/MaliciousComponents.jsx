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

import {NxGrid, NxH2, NxH3, NxTextLink} from "@sonatype/react-shared-components";

import UIStrings from "../../../constants/UIStrings";

const {
  MALICIOUS_COMPONENTS,
  AVERAGE_ATTACK,
  LEARN_MORE
} = UIStrings.MALICIOUS_RISK.COMPONENT_MALWARE;

export default function MaliciousComponents() {
  return (
      <NxGrid.Column className="nxrm-component-malware nx-grid-col--50">
        <NxGrid.ColumnSection className="attack-content-section">
          <NxGrid.Header>
            <NxH3>{AVERAGE_ATTACK.TEXT}</NxH3>
          </NxGrid.Header>
          <NxH2>{AVERAGE_ATTACK.DESCRIPTION}</NxH2>
          <p>{AVERAGE_ATTACK.SUB_TEXT}</p>
        </NxGrid.ColumnSection>
        <NxGrid.ColumnSection>
          <NxGrid.Header>
            <NxH3>{MALICIOUS_COMPONENTS.TEXT}</NxH3>
          </NxGrid.Header>
          <p>{MALICIOUS_COMPONENTS.DESCRIPTION}</p>
        </NxGrid.ColumnSection>
        <NxGrid.ColumnSection>
          <NxTextLink href={LEARN_MORE.URL} external>{LEARN_MORE.TEXT}</NxTextLink>
        </NxGrid.ColumnSection>
      </NxGrid.Column>
  )
}
