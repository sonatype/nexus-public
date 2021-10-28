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
import {useMachine} from '@xstate/react';

import {
  NxButton,
  NxLoadWrapper,
  NxModal,
  NxWarningAlert,
  NxLoadError
} from '@sonatype/react-shared-components';

import SslCertificateDetail from './SslCertificateDetail';
import SslCertificateDetailsModalMachine from './SslCertificateDetailsModalMachine';
import DateUtils from '../../../interface/DateUtils';
import UIStrings from '../../../constants/UIStrings';

/**
 * @since 3.36
 * @param {string} hostUrl the host to retrieve the certificate from
 * @param {function} onCancel a function to fire when the window is closed
 * @returns {JSX.Element}
 */
export default function SslCertificateDetailsModal({hostUrl, onCancel}) {
  const url = new URL(hostUrl);
  const [current, send] = useMachine(SslCertificateDetailsModalMachine, {
    context: {
      host: url.hostname,
      port: url.port ? url.port : '443',
      onCancel
    },
    devTools: true
  });
  const {certificateDetails = {}, error = null, isInTruststore = null} = current.context;
  const isLoading = current.matches('loading') || current.matches('adding') || current.matches('removing');
  const isViewing = current.matches('viewing');
  const hasLoadError = current.matches('loadError');
  const hasAddCertificateError = current.matches('addCertificateError');
  const hasRemoveCertificateError = current.matches('removeCertificateError');

  const SSL_CERTIFICATE_DETAILS = UIStrings.SSL_CERTIFICATE_DETAILS;
  const issuedDate = DateUtils.timestampToString(certificateDetails?.issuedOn);
  const expiredDate = DateUtils.timestampToString(certificateDetails?.expiresOn);

  function retryHandler() {
    send({type: 'RETRY'});
  }

  function addToTruststore() {
    send({type: 'ADD_CERTIFICATE'});
  }

  function removeFromTruststore() {
    send({type: 'REMOVE_CERTIFICATE'});
  }

  return (
      <NxModal onCancel={onCancel}>
        <header className="nx-modal-header">
          <h2 className="nx-h2" id="modal-header-text">
            {SSL_CERTIFICATE_DETAILS.TITLE}
          </h2>
        </header>
        <div className="nx-modal-content">
          <NxLoadWrapper retryHandler={retryHandler} loading={isLoading} error={hasLoadError && error}>
            {() => <>
              {hasAddCertificateError &&
              <NxLoadError titleMessage={SSL_CERTIFICATE_DETAILS.ADD_ERROR}
                           error={error}
                           retryHandler={retryHandler}/>}

              {hasRemoveCertificateError &&
              <NxLoadError titleMessage={SSL_CERTIFICATE_DETAILS.REMOVE_ERROR}
                           error={error}
                           retryHandler={retryHandler}/>}

              {isViewing && <>
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
            </>}
          </NxLoadWrapper>
        </div>
        <footer className="nx-footer">
          <div className="nx-btn-bar">
            {isInTruststore &&
            <NxButton variant="primary"
                      type="button"
                      disabled={isLoading}
                      onClick={removeFromTruststore}>
              {SSL_CERTIFICATE_DETAILS.REMOVE_CERTIFICATE}
            </NxButton>}

            {isInTruststore === false && // isInTruststore will be null during loading, don't show this button then
            <NxButton variant="primary"
                      type="button"
                      disabled={isLoading}
                      onClick={addToTruststore}>
              {SSL_CERTIFICATE_DETAILS.ADD_CERTIFICATE}
            </NxButton>}

            <NxButton type="button" onClick={onCancel}>Close</NxButton>
          </div>
        </footer>
      </NxModal>
  );
}
