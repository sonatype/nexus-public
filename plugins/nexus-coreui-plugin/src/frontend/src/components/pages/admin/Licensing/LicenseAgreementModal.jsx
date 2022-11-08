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
  NxButton,
  NxFooter,
  NxButtonBar,
  NxModal,
  NxH2,
  NxTextLink,
} from '@sonatype/react-shared-components';

import {ExtJS} from '@sonatype/nexus-ui-plugin';

import UIStrings from '../../../../constants/UIStrings';

const {AGREEMENT: {CAPTION, BUTTONS: {ACCEPT, DECLINE, DOWNLOAD}}} = UIStrings.LICENSING;

export default function LicenseAgreementModal({onAccept, onDecline}) {
  const licenseUrl = ExtJS.proLicenseUrl();
  return (
      <NxModal
          onCancel={onDecline}
          aria-labelledby="license-agreement-modal"
          variant="wide"
      >
        <NxModal.Header>
          <NxH2 id="license-agreement-modal">{CAPTION}</NxH2>
        </NxModal.Header>
        <NxModal.Content>
          <iframe
              className="agreement-iframe"
              title={CAPTION}
              src={licenseUrl}
          />
        </NxModal.Content>
        <NxFooter>
          <NxButtonBar>
            <NxTextLink external href={licenseUrl}>{DOWNLOAD}</NxTextLink>
            <NxButton onClick={onDecline}>{DECLINE}</NxButton>
            <NxButton variant="primary" onClick={onAccept}>{ACCEPT}</NxButton>
          </NxButtonBar>
        </NxFooter>
      </NxModal>
  );
}
