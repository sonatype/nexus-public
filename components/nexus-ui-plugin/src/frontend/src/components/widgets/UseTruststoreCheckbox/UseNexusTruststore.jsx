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
  NxFontAwesomeIcon,
  NxTooltip,
} from '@sonatype/react-shared-components';
import classNames from 'classnames';

import ExtJS from '../../../interface/ExtJS';
import Permissions from '../../../constants/Permissions';

import {faCertificate} from '@fortawesome/free-solid-svg-icons';

import ValidationUtils from '../../../interface/ValidationUtils';
import SslCertificateDetailsModal from '../SslCertificateDetailsModal/SslCertificateDetailsModal';

import UIStrings from '../../../constants/UIStrings';
import './UseNexusTruststore.scss';

const {
  USE_TRUST_STORE: {LABEL, DESCRIPTION, VIEW_CERTIFICATE, NOT_SECURE_URL},
} = UIStrings;

export default function UseNexusTruststore({remoteUrl, ...checkboxProps}) {
  const [showModal, setShowModal] = useState(false);
  const canCreate = ExtJS.checkPermission(Permissions.SSL_TRUSTSTORE.CREATE);
  const canUpdate = ExtJS.checkPermission(Permissions.SSL_TRUSTSTORE.UPDATE);
  const canRead = ExtJS.checkPermission(Permissions.SSL_TRUSTSTORE.READ);
  const canMarkAsChecked = canCreate && canUpdate;
  const hasSecureRemoteUrl = ValidationUtils.isSecureUrl(remoteUrl);
  const disabledCheckbox = !canMarkAsChecked || !hasSecureRemoteUrl;
  const disabledViewCertificate = !canRead || !hasSecureRemoteUrl;
  const viewCertificateClasses = classNames({
    disabled: disabledViewCertificate,
  });
  let message;

  if (!canMarkAsChecked) {
    message = UIStrings.PERMISSION_ERROR;
  } else if (!hasSecureRemoteUrl) {
    message = NOT_SECURE_URL;
  }

  const openModal = () => {
    if (!disabledViewCertificate) {
      setShowModal(true);
    }
  };
  const closeModal = () => setShowModal(false);

  return (
    <NxFieldset label={LABEL}>
      <div className="nxrm-use-nexus-trust-store">
        <NxTooltip title={disabledCheckbox && message}>
          <span>
            <NxCheckbox {...checkboxProps} disabled={disabledCheckbox}>
              {DESCRIPTION}
            </NxCheckbox>
          </span>
        </NxTooltip>
        <NxTooltip
          title={disabledViewCertificate && UIStrings.PERMISSION_ERROR}
        >
          <NxButton
            variant="tertiary"
            onClick={openModal}
            className={viewCertificateClasses}
            type="button"
          >
            <NxFontAwesomeIcon icon={faCertificate} />
            <span>{VIEW_CERTIFICATE}</span>
          </NxButton>
        </NxTooltip>
      </div>
      {showModal && (
        <SslCertificateDetailsModal
          remoteUrl={remoteUrl}
          onCancel={closeModal}
        />
      )}
    </NxFieldset>
  );
}
