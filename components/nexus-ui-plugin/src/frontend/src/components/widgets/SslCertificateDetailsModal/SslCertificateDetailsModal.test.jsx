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
import {fireEvent, render, screen, waitForElementToBeRemoved} from '@testing-library/react';
import '@testing-library/jest-dom/extend-expect';
import {when} from 'jest-when';
import axios from 'axios';

import SslCertificateDetailsModal from './SslCertificateDetailsModal';
import TestUtils from "../../../interface/TestUtils";
import DateUtils from "../../../interface/DateUtils";

jest.mock('axios', () => ({
  ...jest.requireActual('axios'),
  get: jest.fn(),
  put: jest.fn(),
  post: jest.fn(),
  delete: jest.fn()
}));

const selectors = {
  ...TestUtils.selectors,
  getSubjectCommonName: () => screen.getByText('Common Name'),
  getSubjectOrganization: () => screen.getByText('Organization'),
  getSubjectOrganizationalUnit: () => screen.getByText('Unit'),
  getIssuerCommonName: () => screen.getByText('Issuer Common Name'),
  getIssuerOrganization: () => screen.getByText('Issuer Organization'),
  getIssuerOrganizationalUnit: () => screen.getByText('Issuer Unit'),
  getIssuedOn: () => screen.getByText('Certificate Issued On'),
  getExpiresOn: () => screen.getByText('Valid Until'),
  getFingerprint: () => screen.getByText('Fingerprint'),

  getAlert: () => screen.getByRole('alert'),

  getAddButton: () => screen.queryByText('Add certificate to truststore'),
  getRemoveButton: () => screen.queryByText('Remove certificate from truststore'),
  getCloseButton: () => screen.getByText('Close')
};

describe('SslCertificateDetailsModal', () => {
  const certificateDetails = {
    'expiresOn': 1654300799000,
    'fingerprint': 'C2:56:90:5E:91:65:A5:D1:6E:DC:98:65:CD:8D:34:32:B2:B1:45:40',
    'id': 'C2:56:90:5E:91:65:A5:D1:6E:DC:98:65:CD:8D:34:32:B2:B1:45:40',
    'issuedOn': 1622764800000,
    'issuerCommonName': 'issuer common name',
    'issuerOrganization': 'issuer organization',
    'issuerOrganizationalUnit': 'issuer unit',
    'pem': '-----BEGIN CERTIFICATE-----\ncertificate_text\n-----END CERTIFICATE-----\n',
    'serialNumber': '8987777501424561459122707745365601310',
    'subjectCommonName': 'subject common name',
    'subjectOrganization': 'subject organization',
    'subjectOrganizationalUnit': 'subject organizational unit'
  };

  const onCancel = jest.fn();

  function mockCertificateDetails(response, hostname, port) {
    if (!hostname) {
      hostname = 'localhost';
    }
    if (!port) {
      port = '443';
    }
    when(axios.get).calledWith('/service/rest/v1/security/ssl?host=' + hostname + '&port=' + port).mockReturnValue({
      data: response
    });
  }

  function mockTruststore(truststore) {
    when(axios.get).calledWith('/service/rest/v1/security/ssl/truststore').mockResolvedValue({
      data: truststore
    });
  }

  it('renders correctly when loading', async function() {
    const UNRESOLVED = new Promise(() => {});
    when(axios.get).calledWith('/service/rest/v1/security/ssl?host=localhost&port=443').mockReturnValue(UNRESOLVED);

    render(<SslCertificateDetailsModal hostUrl="https://localhost" onCancel={onCancel}/>);

    expect(selectors.queryLoadingMask()).toBeInTheDocument();
    expect(selectors.getAddButton()).not.toBeInTheDocument();
    expect(selectors.getRemoveButton()).not.toBeInTheDocument();
    expect(selectors.getCloseButton()).toBeInTheDocument();

    fireEvent.click(selectors.getCloseButton());

    expect(onCancel).toBeCalled();
  });

  it('renders the certificate details', async function() {
    mockCertificateDetails(certificateDetails);
    mockTruststore([]);

    render(<SslCertificateDetailsModal hostUrl="https://localhost" onCancel={onCancel}/>);

    await waitForElementToBeRemoved(selectors.queryLoadingMask());

    expect(selectors.getSubjectCommonName().nextSibling).toHaveTextContent(certificateDetails.subjectCommonName);
    expect(selectors.getSubjectOrganization().nextSibling).toHaveTextContent(certificateDetails.subjectOrganization);
    expect(selectors.getSubjectOrganizationalUnit().nextSibling)
        .toHaveTextContent(certificateDetails.subjectOrganizationalUnit);
    expect(selectors.getIssuerCommonName().nextSibling).toHaveTextContent(certificateDetails.issuerCommonName);
    expect(selectors.getIssuerOrganization().nextSibling).toHaveTextContent(certificateDetails.issuerOrganization);
    expect(selectors.getIssuerOrganizationalUnit().nextSibling)
        .toHaveTextContent(certificateDetails.issuerOrganizationalUnit);
    expect(selectors.getIssuedOn().nextSibling)
        .toHaveTextContent(DateUtils.timestampToString(certificateDetails.issuedOn));
    expect(selectors.getExpiresOn().nextSibling)
        .toHaveTextContent(DateUtils.timestampToString(certificateDetails.expiresOn));
    expect(selectors.getFingerprint().nextSibling).toHaveTextContent(certificateDetails.fingerprint);
  });

  it('renders the certificate details when non-default port used', async function() {
    mockCertificateDetails(certificateDetails, 'localhost', '8443');
    mockTruststore([]);

    render(<SslCertificateDetailsModal hostUrl="https://localhost:8443" onCancel={onCancel}/>);

    await waitForElementToBeRemoved(selectors.queryLoadingMask());

    expect(selectors.getSubjectCommonName().nextSibling).toHaveTextContent(certificateDetails.subjectCommonName);
    expect(selectors.getSubjectOrganization().nextSibling).toHaveTextContent(certificateDetails.subjectOrganization);
    expect(selectors.getSubjectOrganizationalUnit().nextSibling)
        .toHaveTextContent(certificateDetails.subjectOrganizationalUnit);
    expect(selectors.getIssuerCommonName().nextSibling).toHaveTextContent(certificateDetails.issuerCommonName);
    expect(selectors.getIssuerOrganization().nextSibling).toHaveTextContent(certificateDetails.issuerOrganization);
    expect(selectors.getIssuerOrganizationalUnit().nextSibling)
        .toHaveTextContent(certificateDetails.issuerOrganizationalUnit);
    expect(selectors.getIssuedOn().nextSibling)
        .toHaveTextContent(DateUtils.timestampToString(certificateDetails.issuedOn));
    expect(selectors.getExpiresOn().nextSibling)
        .toHaveTextContent(DateUtils.timestampToString(certificateDetails.expiresOn));
    expect(selectors.getFingerprint().nextSibling).toHaveTextContent(certificateDetails.fingerprint);
  });

  it('adds the certificate to the truststore', async function() {
    mockCertificateDetails(certificateDetails);
    mockTruststore([]);

    render(<SslCertificateDetailsModal hostUrl="https://localhost" onCancel={onCancel}/>);

    await waitForElementToBeRemoved(selectors.queryLoadingMask());

    expect(selectors.getAddButton()).toBeEnabled();
    expect(onCancel).not.toBeCalled();

    when(axios.post).calledWith('/service/rest/v1/security/ssl/truststore', certificateDetails.pem).mockResolvedValue();

    fireEvent.click(selectors.getAddButton());

    await waitForElementToBeRemoved(selectors.queryLoadingMask());

    expect(onCancel).toBeCalled();
  });

  it('removes the certificate from the truststore', async function() {
    mockCertificateDetails(certificateDetails);
    mockTruststore([certificateDetails]);

    render(<SslCertificateDetailsModal hostUrl="https://localhost" onCancel={onCancel}/>);

    await waitForElementToBeRemoved(selectors.queryLoadingMask());

    expect(selectors.getRemoveButton()).toBeEnabled();
    expect(onCancel).not.toBeCalled();

    when(axios.delete)
        .calledWith('/service/rest/v1/security/ssl/truststore/' + certificateDetails.id)
        .mockResolvedValue();

    fireEvent.click(selectors.getRemoveButton());

    await waitForElementToBeRemoved(selectors.queryLoadingMask());

    expect(onCancel).toBeCalled();
  });

  it('closes when close is clicked', async function() {
    mockCertificateDetails(certificateDetails);
    mockTruststore([]);

    render(<SslCertificateDetailsModal hostUrl="https://localhost" onCancel={onCancel}/>);

    fireEvent.click(selectors.getCloseButton());

    await waitForElementToBeRemoved(selectors.queryLoadingMask());

    expect(onCancel).toBeCalled();
  });

  it('renders an error when the load fails', async function() {
    when(axios.get).calledWith('/service/rest/v1/security/ssl?host=localhost&port=443').mockRejectedValue('error');
    mockTruststore([]);

    render(<SslCertificateDetailsModal hostUrl="https://localhost" onCancel={onCancel}/>);

    await waitForElementToBeRemoved(selectors.queryLoadingMask());

    expect(screen.getByText('An error occurred loading data. error')).toBeInTheDocument();
  });

  it('renders an error when adding the certificate fails', async function() {
    mockCertificateDetails(certificateDetails);
    mockTruststore([]);

    render(<SslCertificateDetailsModal hostUrl="https://localhost" onCancel={onCancel}/>);

    await waitForElementToBeRemoved(selectors.queryLoadingMask());

    when(axios.post)
        .calledWith('/service/rest/v1/security/ssl/truststore', certificateDetails.pem)
        .mockRejectedValue('error');

    fireEvent.click(selectors.getAddButton());

    await waitForElementToBeRemoved(selectors.queryLoadingMask());

    expect(screen.getByText('An error occurred while attempting to add the certificate to the truststore. error'))
        .toBeInTheDocument();
  });

  it('renders an error when removing the certificate fails', async function() {
    mockCertificateDetails(certificateDetails);
    mockTruststore([certificateDetails]);

    render(<SslCertificateDetailsModal hostUrl="https://localhost" onCancel={onCancel}/>);

    await waitForElementToBeRemoved(selectors.queryLoadingMask());

    when(axios.delete)
        .calledWith('/service/rest/v1/security/ssl/truststore/' + certificateDetails.id)
        .mockRejectedValue('error');

    fireEvent.click(selectors.getRemoveButton());

    await waitForElementToBeRemoved(selectors.queryLoadingMask());

    expect(screen.getByText('An error occurred while attempting to remove the certificate from the truststore. error'))
        .toBeInTheDocument();
  });
});
