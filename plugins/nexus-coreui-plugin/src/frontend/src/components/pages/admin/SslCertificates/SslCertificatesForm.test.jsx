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
import {render, screen, waitFor, within, waitForElementToBeRemoved, act} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {when} from 'jest-when';
import Axios from 'axios';

import {ExtJS, DateUtils, APIConstants} from '@sonatype/nexus-ui-plugin';
import TestUtils from '@sonatype/nexus-ui-plugin/src/frontend/src/interface/TestUtils';

import UIStrings from '../../../../constants/UIStrings';
import SslCertificatesForm from './SslCertificatesForm';
import {URLS} from './SslCertificatesHelper';
import {SSL_CERTIFICATES, SSL_CERTIFICATES_MAP} from './SslCertificates.testdata';

const {EXT} = APIConstants;

const XSS_STRING = TestUtils.XSS_STRING;
const {sslCertificatesUrl, singleSslCertificatesUrl, createSslCertificatesUrl} = URLS;

const {
  SSL_CERTIFICATES: {
    FORM: LABELS,
    ADD_FORM: {
      CAPTION: ADD_FORM_CAPTION,
      LOAD_BUTTON,
      PEM,
      SERVER,
      MODAL: {
        VIEW_BUTTON,
        CONTENT: MODAL_CONTENT
      }
    }
  },
  SETTINGS: {CANCEL_BUTTON_LABEL}
} = UIStrings;

jest.mock('axios', () => ({
  ...jest.requireActual('axios'),
  get: jest.fn(),
  put: jest.fn(),
  post: jest.fn(),
  delete: jest.fn(),
}));

jest.mock('@sonatype/nexus-ui-plugin', () => ({
  ...jest.requireActual('@sonatype/nexus-ui-plugin'),
  ExtJS: {
    requestConfirmation: jest.fn(),
    checkPermission: jest.fn(),
    showErrorMessage: jest.fn(),
    showSuccessMessage: jest.fn(),
  },
}));

const testId = SSL_CERTIFICATES[1].id;
const DATA = SSL_CERTIFICATES_MAP[testId];

const selectors = {
  ...TestUtils.selectors,
  subjectSection: () => screen.getByRole('heading', {name: LABELS.SECTIONS.SUBJECT}).closest('section'),
  subjectCommonName: () => within(selectors.subjectSection()).getByText(LABELS.COMMON_NAME.LABEL).nextSibling,
  subjectOrganization: () => within(selectors.subjectSection()).getByText(LABELS.ORGANIZATION.LABEL).nextSibling,
  subjectOrganizationalUnit: () => within(selectors.subjectSection()).getByText(LABELS.UNIT.LABEL).nextSibling,
  issuerSection: () => screen.getByRole('heading', {name: LABELS.SECTIONS.ISSUER}).closest('section'),
  issuerCommonName: () => within(selectors.issuerSection()).getByText(LABELS.COMMON_NAME.LABEL).nextSibling,
  issuerOrganization: () => within(selectors.issuerSection()).getByText(LABELS.ORGANIZATION.LABEL).nextSibling,
  issuerOrganizationalUnit: () => within(selectors.issuerSection()).getByText(LABELS.UNIT.LABEL).nextSibling,
  certificateSection: () => screen.getByRole('heading', {name: LABELS.SECTIONS.CERTIFICATE}).closest('section'),
  issuedOn: () => within(selectors.certificateSection()).getByText(LABELS.ISSUED_ON.LABEL).nextSibling,
  expiresOn: () => within(selectors.certificateSection()).getByText(LABELS.VALID_UNTIL.LABEL).nextSibling,
  fingerprint: () => within(selectors.certificateSection()).getByText(LABELS.FINGERPRINT.LABEL).nextSibling,
  warning: () => screen.getByText(LABELS.WARNING),
  cancelButton: () => screen.getByText(CANCEL_BUTTON_LABEL),
  deleteButton: () => screen.getByText(LABELS.BUTTONS.DELETE),
  addFormTitle: () => screen.queryByText(ADD_FORM_CAPTION),
  loadFromServerRadioButton: () => screen.getByLabelText(SERVER.RADIO_DESCRIPTION),
  loadFromPemRadioButton: () => screen.getByLabelText(PEM.RADIO_DESCRIPTION),
  remoteHostUrlInput: () => screen.getByLabelText(SERVER.LABEL),
  pemInput: () => screen.getByLabelText(PEM.LABEL),
  loadCertificateButton: () => screen.getByText(LOAD_BUTTON),
  alreadyExistsModal: () => screen.queryByRole('dialog'),
  viewExistingCertificateButton: () => within(selectors.alreadyExistsModal()).getByText(VIEW_BUTTON),
  alreadyExistsModalContent: () => within(selectors.alreadyExistsModal()).getByText(MODAL_CONTENT)
};

describe('SslCertificatesForm', function() {
  const onDone = jest.fn();
  const CONFIRM = Promise.resolve();

  const renderAndWaitForLoad = async (itemId) => {
    const {debug} = render(<SslCertificatesForm itemId={itemId || ''} onDone={onDone}/>);
    await waitForElementToBeRemoved(selectors.queryLoadingMask());

    return debug;
  }

  beforeEach(() => {
    when(Axios.get).calledWith(sslCertificatesUrl).mockResolvedValue({
      data: SSL_CERTIFICATES,
    });
    ExtJS.checkPermission.mockReturnValue(true);
  });

  it('renders the resolved data', async function() {
    const {subjectCommonName, subjectOrganization, subjectOrganizationalUnit, issuerCommonName, issuerOrganization,
      issuerOrganizationalUnit, issuedOn, expiresOn, fingerprint, warning, cancelButton, deleteButton} = selectors;

    await renderAndWaitForLoad(testId);

    expect(subjectCommonName()).toHaveTextContent(DATA.subjectCommonName);
    expect(subjectOrganization()).toHaveTextContent(DATA.subjectOrganization);
    expect(subjectOrganizationalUnit()).toHaveTextContent(DATA.subjectOrganizationalUnit);

    expect(issuerCommonName()).toHaveTextContent(DATA.issuerCommonName);
    expect(issuerOrganization()).toHaveTextContent(DATA.issuerOrganization);
    expect(issuerOrganizationalUnit()).toHaveTextContent(DATA.issuerOrganizationalUnit);

    expect(issuedOn()).toHaveTextContent(DateUtils.timestampToString(DATA.issuedOn));
    expect(expiresOn()).toHaveTextContent(DateUtils.timestampToString(DATA.expiresOn));
    expect(fingerprint()).toHaveTextContent(DATA.fingerprint);

    expect(warning()).toBeInTheDocument();
    expect(cancelButton()).toBeInTheDocument();
    expect(deleteButton()).toBeInTheDocument();

    expect(deleteButton()).not.toHaveClass('disabled');
  });

  it('renders the resolved data with XSS', async function() {
    const {subjectCommonName, subjectOrganization, subjectOrganizationalUnit, issuerCommonName,
      issuerOrganization, issuerOrganizationalUnit, fingerprint} = selectors;

    const DATA_WITH_XSS = [{
      ...DATA,
      subjectCommonName: XSS_STRING,
      subjectOrganization: XSS_STRING,
      subjectOrganizationalUnit: XSS_STRING,
      issuerCommonName: XSS_STRING,
      issuerOrganization: XSS_STRING,
      issuerOrganizationalUnit: XSS_STRING,
      issuedOn: XSS_STRING,
      expiresOn: XSS_STRING,
      fingerprint: XSS_STRING,
    }];

    when(Axios.get).calledWith(sslCertificatesUrl).mockResolvedValue({
      data: DATA_WITH_XSS,
    });

    await renderAndWaitForLoad(testId);

    expect(subjectCommonName()).toHaveTextContent(XSS_STRING);
    expect(subjectOrganization()).toHaveTextContent(XSS_STRING);
    expect(subjectOrganizationalUnit()).toHaveTextContent(XSS_STRING);

    expect(issuerCommonName()).toHaveTextContent(XSS_STRING);
    expect(issuerOrganization()).toHaveTextContent(XSS_STRING);
    expect(issuerOrganizationalUnit()).toHaveTextContent(XSS_STRING);

    expect(fingerprint()).toHaveTextContent(XSS_STRING);
  });

  it('renders load error message', async function() {
    const message = 'Load error message!';

    Axios.get.mockReturnValue(Promise.reject({message}));

    await renderAndWaitForLoad(testId);

    expect(screen.getByRole('alert')).toHaveTextContent(message);
  });

  it('requests confirmation when delete is requested', async function() {
    const {deleteButton} = selectors;
    Axios.delete.mockReturnValue(Promise.resolve(null));

    await renderAndWaitForLoad(testId);

    ExtJS.requestConfirmation.mockReturnValue(CONFIRM);
    userEvent.click(deleteButton());

    await waitFor(() => expect(Axios.delete).toBeCalledWith(singleSslCertificatesUrl(testId)));
    expect(onDone).toBeCalled();
    expect(ExtJS.showSuccessMessage)
        .toHaveBeenCalledWith(UIStrings.SSL_CERTIFICATES.MESSAGES.DELETE_SUCCESS(DATA.subjectCommonName));
  });

  it('disables the delete button when not enough permissions', async function() {
    const {deleteButton} = selectors;

    when(ExtJS.checkPermission).calledWith('nexus:ssl-truststore:delete').mockReturnValue(false);

    await renderAndWaitForLoad(testId);

    expect(deleteButton()).toHaveClass('disabled');
  });

  it('fires onDone when cancelled', async function() {
    const {cancelButton} = selectors;

    await renderAndWaitForLoad(testId);

    userEvent.click(cancelButton());

    await waitFor(() => expect(onDone).toBeCalled());
  });

  it('uses proper urls', function() {
    expect(sslCertificatesUrl).toBe('service/rest/v1/security/ssl/truststore');
    expect(singleSslCertificatesUrl(testId)).toBe('service/rest/v1/security/ssl/truststore/F6%3ADB%3A65%3AA4%3A0D%3A38%3A75%3A86%3A90%3A96%3A29%3A5F%3A36%3A44%3A7F%3A3D%3A98%3A4B%3A3A%3A5N');
    expect(singleSslCertificatesUrl('G33:$($%?)')).toBe('service/rest/v1/security/ssl/truststore/G33%3A%24(%24%25%3F)');
    expect(createSslCertificatesUrl).toBe('service/rest/v1/security/ssl/truststore');
  });
});

describe('SslCertificatesAddForm', function () {
  const onDone = jest.fn();

  const renderAddForm = async () => {
    const result = render(<SslCertificatesForm itemId="" onDone={onDone} />);
    await waitFor(() => expect(selectors.addFormTitle()).toBeInTheDocument());
    return result;
  };

  const loadFromServerExtReqBody = (remoteHostUrl) => ({
    action: EXT.SSL.ACTION,
    method: EXT.SSL.METHODS.RETRIEVE_FROM_HOST,
    data: [remoteHostUrl, null, null],
    type: 'rpc',
    tid: 1
  });

  const loadFromPemExtReqBody = (pem) => ({
    action: EXT.SSL.ACTION,
    method: EXT.SSL.METHODS.DETAILS,
    data: [pem],
    type: 'rpc',
    tid: 1
  });

  const loadFromServerExtErrorRespBody = (errorMsg) => ({
    action: EXT.SSL.ACTION,
    method: EXT.SSL.METHODS.RETRIEVE_FROM_HOST,
    result: {
      message: errorMsg,
      authenticationRequired: false,
      success: false,
      data: []
    },
    type: 'rpc',
    tid: 1
  });

  const loadFromServerExtRespBody = (certificate) => ({
    action: EXT.SSL.ACTION,
    method: EXT.SSL.METHODS.RETRIEVE_FROM_HOST,
    result: {
      success: true,
      data: certificate
    },
    type: 'rpc',
    tid: 1,
  });

  const mockResponse = (response = {}, remoteHost = '') =>{
    when(Axios.post).calledWith(EXT.URL, loadFromServerExtReqBody(remoteHost)).mockResolvedValue(response);
  }

  beforeEach(() => {
    ExtJS.checkPermission.mockReturnValue(true);
  });

  it('renders correct initial state', async function () {
    await renderAddForm();

    const {
      loadFromServerRadioButton,
      loadFromPemRadioButton,
      remoteHostUrlInput,
      pemInput
    } = selectors;

    expect(loadFromServerRadioButton()).toBeChecked();
    expect(loadFromPemRadioButton()).not.toBeChecked();
    expect(remoteHostUrlInput()).toBeEnabled();
    expect(pemInput()).toBeDisabled();
  });

  it('loads certificate from server', async function () {
    await renderAddForm();

    const {remoteHostUrlInput, loadCertificateButton} = selectors;

    const remoteHost = 'foo.bar';

    await TestUtils.changeField(remoteHostUrlInput, remoteHost);

    await act(async () => userEvent.click(loadCertificateButton()));

    expect(Axios.post).toBeCalledWith(EXT.URL, loadFromServerExtReqBody(remoteHost));
  });

  it('loads certificate from pem', async function () {
    await renderAddForm();

    const {
      pemInput,
      remoteHostUrlInput,
      loadFromPemRadioButton,
      loadCertificateButton
    } = selectors;

    const pemContent = 'CORRECTPEMCONTENT';

    await waitFor(() => expect(pemInput()).toBeDisabled());

    userEvent.click(loadFromPemRadioButton());

    expect(remoteHostUrlInput()).toBeDisabled();
    expect(pemInput()).toBeEnabled();

    await TestUtils.changeField(pemInput, pemContent);

    mockResponse();

    userEvent.click(loadCertificateButton());

    await waitFor(() => {
      expect(Axios.post).toBeCalledWith(EXT.URL, loadFromPemExtReqBody(pemContent));
    });
  });

  it('shows add form with error toast on load from server error', async function () {
    const remoteHost = 'bad.host';
    const errorMessage = 'Could not retrieve an SSL certificate';

    const response = {
      data: loadFromServerExtErrorRespBody(errorMessage)
    }

    mockResponse(response, remoteHost);

    await renderAddForm();

    const {remoteHostUrlInput, loadCertificateButton} = selectors;

    await TestUtils.changeField(remoteHostUrlInput, remoteHost);

    await act(async () => userEvent.click(loadCertificateButton()));

    expect(Axios.post).toBeCalledWith(EXT.URL, loadFromServerExtReqBody(remoteHost));
    expect(remoteHostUrlInput()).toBeVisible();
    expect(screen.getByRole('alert')).toHaveTextContent(errorMessage);
  });

  it('fires onDone when cancelled', async function() {
    const {cancelButton} = selectors;

    await renderAddForm();

    userEvent.click(cancelButton());

    await waitFor(() => expect(onDone).toBeCalled());
  });

  it('shows modal when trying to add existing certificate', async function () {
    const remoteHost = 'foo.bar';

    const certificate = SSL_CERTIFICATES[3];

    const response = {data: loadFromServerExtRespBody(certificate)};

    when(Axios.post).calledWith(EXT.URL, loadFromServerExtReqBody(remoteHost)).mockResolvedValue(response);

    await renderAddForm();

    const {
      remoteHostUrlInput,
      loadCertificateButton,
      alreadyExistsModal,
      alreadyExistsModalContent,
      viewExistingCertificateButton
    } = selectors;

    await TestUtils.changeField(remoteHostUrlInput, remoteHost);

    userEvent.click(loadCertificateButton());

    await waitFor(() => expect(Axios.post).toBeCalledWith(EXT.URL, loadFromServerExtReqBody(remoteHost)));

    expect(alreadyExistsModal()).toBeVisible();

    expect(alreadyExistsModalContent()).toBeVisible();

    expect(viewExistingCertificateButton()).toBeVisible();
  });
});
