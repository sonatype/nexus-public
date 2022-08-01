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
import React, {useState} from 'react';

import {
  NxFieldset,
  NxCheckbox,
  NxButton,
  NxFontAwesomeIcon
} from '@sonatype/react-shared-components';

import {faCertificate} from '@fortawesome/free-solid-svg-icons';

import ValidationUtils from '../../../interface/ValidationUtils';
import SslCertificateDetailsModal from '../SslCertificateDetailsModal/SslCertificateDetailsModal';

import UIStrings from '../../../constants/UIStrings';
import './UseNexusTruststore.scss';

const {LABEL, DESCRIPTION, VIEW_CERTIFICATE} = UIStrings.USE_TRUST_STORE;

export default function UseNexusTruststore({remoteUrl, ...checkboxPorps}) {
  const [showModal, setShowModal] = useState(false);

  const hasSecureRemoteUrl = ValidationUtils.isSecureUrl(remoteUrl);

  const openModal = () => setShowModal(true);
  const closeModal = () => setShowModal(false);

  return (
    <NxFieldset label={LABEL}>
      <div className="nxrm-use-nexus-trust-store">
        <NxCheckbox {...checkboxPorps} disabled={!hasSecureRemoteUrl}>
          {DESCRIPTION}
        </NxCheckbox>
        <NxButton
          variant="tertiary"
          disabled={!hasSecureRemoteUrl}
          onClick={openModal}
          type="button"
        >
          <NxFontAwesomeIcon icon={faCertificate} />
          <span>{VIEW_CERTIFICATE}</span>
        </NxButton>
      </div>
      {showModal && <SslCertificateDetailsModal remoteUrl={remoteUrl} onCancel={closeModal} />}
    </NxFieldset>
  );
}
