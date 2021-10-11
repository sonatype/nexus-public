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
import React from "react";

import {NxButton, NxInfoAlert, NxLoadWrapper, NxModal, NxWarningAlert} from '@sonatype/react-shared-components';
import DateUtils from "../../../interface/DateUtils";
import UIStrings from "../../../constants/UIStrings";

/**
 * @since 3.next
 * @param {function} retryHandler - a function to reload the certificate details if a failure occurs
 * @param {object} certificateDetails - an object containing the certificate details
 * @param {?number} certificateDetails.issuedOn - a timestamp for when the certificate was issued
 * @param {?number} certificateDetails.expiresOn - a timestamp for when the certificate was expired
 * @param {?string} certificateDetails.subjectCommonName
 * @param {?string} certificateDetails.subjectOrganization
 * @param {?string} certificateDetails.subjectOrganizationalUnit
 * @param {?string} certificateDetails.issuerCommonName
 * @param {?string} certificateDetails.issuerOrganization
 * @param {?string} certificateDetails.issuerOrganizationalUnit
 * @param {?string} certificateDetails.fingerprint
 * @param {?string} error - an error message indicating why the certificate could not be loaded
 * @param {function} isLoading - a boolean indicating whether the certificate details are loading
 * @returns {JSX.Element}
 */
export default function SslCertificateDetailsModal({
                                                     retryHandler,
                                                     onCancel,
                                                     certificateDetails,
                                                     error,
                                                     isLoading
                                                   }) {
  const SSL_CERTIFICATE_DETAILS = UIStrings.SSL_CERTIFICATE_DETAILS;
  const issuedDate = DateUtils.timestampToString(certificateDetails?.issuedOn);
  const expiredDate = DateUtils.timestampToString(certificateDetails?.expiresOn);
  return (
      <NxModal onCancel={onCancel}>
        <header className="nx-modal-header">
          <h2 className="nx-h2" id="modal-header-text">
            {SSL_CERTIFICATE_DETAILS.TITLE}
          </h2>
        </header>
        <div className="nx-modal-content">
          <NxLoadWrapper retryHandler={retryHandler} loading={isLoading} error={error}>
            {() => <>
              <NxWarningAlert>{SSL_CERTIFICATE_DETAILS.WARNING}</NxWarningAlert>
              <dl className="nx-read-only">
                <SslCertificateDetail label={SSL_CERTIFICATE_DETAILS.NAME}
                                      value={certificateDetails.subjectCommonName}/>
                <SslCertificateDetail label={SSL_CERTIFICATE_DETAILS.ORG}
                                      value={certificateDetails.subjectOrganization}/>
                <SslCertificateDetail label={SSL_CERTIFICATE_DETAILS.UNIT}
                                      value={certificateDetails.subjectOrganizationalUnit}/>
                <SslCertificateDetail label={SSL_CERTIFICATE_DETAILS.ISSUER_NAME}
                                      value={certificateDetails.issuerCommonName}/>
                <SslCertificateDetail label={SSL_CERTIFICATE_DETAILS.ISSUER_ORG}
                                      value={certificateDetails.issuerOrganization}/>
                <SslCertificateDetail label={SSL_CERTIFICATE_DETAILS.ISSUER_UNIT}
                                      value={certificateDetails.issuerOrganizationalUnit}/>
                <SslCertificateDetail label={SSL_CERTIFICATE_DETAILS.ISSUE_DATE}
                                      value={issuedDate}/>
                <SslCertificateDetail label={SSL_CERTIFICATE_DETAILS.EXPIRE_DATE}
                                      value={expiredDate}/>
                <SslCertificateDetail label={SSL_CERTIFICATE_DETAILS.FINGERPRINT}
                                      value={certificateDetails.fingerprint}/>
              </dl>
            </>}
          </NxLoadWrapper>
        </div>
        <footer className="nx-footer">
          <div className="nx-btn-bar">
            <NxButton onClick={onCancel}>Close</NxButton>
          </div>
        </footer>
      </NxModal>
  );
}

function SslCertificateDetail({label, value}) {
  if (!value) {
    return null;
  }
  return <>
    <dt className="nx-read-only__label">{label}</dt>
    <dd className="nx-read-only__data">{value}</dd>
  </>
}
