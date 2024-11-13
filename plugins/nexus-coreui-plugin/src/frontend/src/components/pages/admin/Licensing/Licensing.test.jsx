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
import Axios from 'axios';
import {when} from 'jest-when';
import {act} from 'react-dom/test-utils';
import userEvent from '@testing-library/user-event';
import {render, screen, waitFor} from '@testing-library/react';

import {
  ExtJS,
  APIConstants,
  DateUtils,
  Permissions,
} from '@sonatype/nexus-ui-plugin';
import TestUtils from '@sonatype/nexus-ui-plugin/src/frontend/src/interface/TestUtils';

import Licensing from './Licensing';
import UIStrings from '../../../../constants/UIStrings';

const {LICENSING: {DETAILS, INSTALL, AGREEMENT}, LICENSING} = UIStrings;

const {REST: {PUBLIC: {LICENSE: licenseUrl}}} = APIConstants;
const XSS_STRING = TestUtils.XSS_STRING;

jest.mock('axios', () => ({
  ...jest.requireActual('axios'),
  get: jest.fn(),
  post: jest.fn(),
}));

jest.mock('@sonatype/nexus-ui-plugin', () => ({
  ...jest.requireActual('@sonatype/nexus-ui-plugin'),
  ExtJS: {
    checkPermission: jest.fn(),
    showErrorMessage: jest.fn(),
    showSuccessMessage: jest.fn(),
    proLicenseUrl: jest.fn().mockReturnValue('http://localhost:8081/PRO-LICENSE.html'),
  },
}));

const selectors = {
  ...TestUtils.selectors,
  contactCompany: () => screen.getByText(DETAILS.COMPANY.LABEL).nextSibling,
  contactName: () => screen.getByText(DETAILS.NAME.LABEL).nextSibling,
  contactEmail: () => screen.getByText(DETAILS.EMAIL.LABEL).nextSibling,
  effectiveDate: () => screen.getByText(DETAILS.EFFECTIVE_DATE.LABEL).nextSibling,
  expirationDate: () => screen.getByText(DETAILS.EXPIRATION_DATE.LABEL).nextSibling,
  licenseType: () => screen.getByText(DETAILS.LICENSE_TYPES.LABEL),
  licensedUsers: () => screen.getByText(DETAILS.NUMBER_OF_USERS.LABEL).nextSibling,
  fingerprint: () => screen.getByText(DETAILS.FINGERPRINT.LABEL).nextSibling,
  detailsSection: () => screen.queryByRole('heading', {name: LICENSING.SECTIONS.DETAILS, level: 2}),
  installSection: () => screen.queryByRole('heading', {name: LICENSING.SECTIONS.INSTALL, level: 2}),
  uploadInput: () => screen.queryByLabelText(INSTALL.LABEL, {hidden: true}),
  uploadButton: () => screen.queryByText(INSTALL.BUTTONS.UPLOAD),
  readOnlyWarning: () => screen.queryByText(UIStrings.SETTINGS.READ_ONLY.WARNING),
  agreement: {
    modal: () => screen.queryByText(AGREEMENT.CAPTION),
    acceptButton: () => screen.getByText(AGREEMENT.BUTTONS.ACCEPT),
    declineButton: () => screen.getByText(AGREEMENT.BUTTONS.DECLINE),
  },
};

const DATA = {
  contactCompany: 'Test Sonatype Inc',
  contactEmail: 'test@sonatype.com',
  contactName: 'Ember Deboer',
  effectiveDate: '2022-07-14T14:20:40.108+00:00',
  expirationDate: '2023-08-02T00:00:00.000+00:00',
  features: 'SonatypeCLM, NexusProfessional, Firewall',
  fingerprint: '53274cced19cs2e5208s73801g4a9160a960684',
  licenseType: 'Nexus IQ Server, Nexus Repository Pro, Nexus Firewall',
  licensedUsers: '1001',
};

describe('Licensing', () => {
  const renderComponent = async () => {
    await act(async () => {
      render(<Licensing/>);
    });
  }

  const expectLicenseDetails = data => {
    const {contactCompany, contactName, contactEmail, effectiveDate, expirationDate, licenseType,
      licensedUsers, fingerprint} = selectors;

    expect(contactCompany()).toHaveTextContent(data.contactCompany);
    expect(contactName()).toHaveTextContent(data.contactName);
    expect(contactEmail()).toHaveTextContent(data.contactEmail);
    expect(effectiveDate()).toHaveTextContent(DateUtils.prettyDate(data.effectiveDate));
    expect(expirationDate()).toHaveTextContent(DateUtils.prettyDate(data.expirationDate));
    let licenseTypeValue = licenseType().nextSibling;
    data.licenseType.split(',').map(type => type.trim()).forEach(type => {
      expect(licenseTypeValue).toHaveTextContent(type);
      licenseTypeValue = licenseTypeValue.nextSibling;
    });
    expect(licensedUsers()).toHaveTextContent(data.licensedUsers);
    expect(fingerprint()).toHaveTextContent(data.fingerprint);
  };

  beforeEach(() => {
    when(Axios.get).calledWith(licenseUrl).mockResolvedValue({
      data: DATA
    });
    when(Axios.post).calledWith(licenseUrl, expect.anything(), expect.anything()).mockResolvedValue({
      data: DATA
    });
    ExtJS.checkPermission.mockReturnValue(true);
  });

  it('renders the resolved data', async () => {
    const {detailsSection, installSection} = selectors;

    await renderComponent();

    expect(detailsSection()).toBeInTheDocument();
    expect(installSection()).toBeInTheDocument();

    expectLicenseDetails(DATA);
  });

  it('renders the resolved data with XSS', async () => {
    const DATA_WITH_XSS = {
      contactCompany: XSS_STRING,
      contactName: XSS_STRING,
      contactEmail: XSS_STRING,
      effectiveDate: XSS_STRING,
      expirationDate: XSS_STRING,
      licenseType: XSS_STRING,
      licensedUsers: [XSS_STRING, XSS_STRING, XSS_STRING].join(', '),
      fingerprint: XSS_STRING,
    }

    when(Axios.get).calledWith(licenseUrl).mockResolvedValue({
      data: DATA_WITH_XSS
    });

    await renderComponent();

    expectLicenseDetails(DATA_WITH_XSS);
  });

  it('renders when error', async function() {
    const {detailsSection, installSection} = selectors;

    when(Axios.get).calledWith(licenseUrl).mockRejectedValue({message: 'Error'});

    await renderComponent();

    expect(detailsSection()).not.toBeInTheDocument();
    expect(installSection()).toBeInTheDocument();
  });

  it('renders warning alert in the Read-Only mode', async function() {
    const {installSection, readOnlyWarning, uploadButton, uploadInput} = selectors;

    when(ExtJS.checkPermission).calledWith(Permissions.LICENSING.CREATE).mockReturnValue(false);

    await renderComponent();

    expectLicenseDetails(DATA);
    expect(installSection()).toBeInTheDocument();
    expect(readOnlyWarning()).toBeInTheDocument();
    expect(uploadButton()).not.toBeInTheDocument();
    expect(uploadInput()).not.toBeInTheDocument();
  });

  it('installs license', async function() {
    const {detailsSection, uploadButton, uploadInput, agreement: {modal, acceptButton, declineButton}} = selectors;

    when(Axios.get).calledWith(licenseUrl).mockRejectedValue({message: 'Error'});

    await renderComponent();

    expect(detailsSection()).not.toBeInTheDocument();
    expect(uploadInput()).not.toBeDisabled();
    expect(uploadButton()).toBeDisabled();

    const file = new File([new ArrayBuffer(1)], 'file.lic');
    userEvent.upload(uploadInput(), file);
    expect(uploadButton()).not.toHaveClass('disabled');

    userEvent.click(uploadButton());
    expect(modal()).toBeVisible();

    userEvent.click(declineButton());
    expect(modal()).not.toBeInTheDocument();

    userEvent.click(uploadButton());
    expect(modal()).toBeVisible();

    when(Axios.get).calledWith(licenseUrl).mockResolvedValue({data: DATA});

    userEvent.click(acceptButton());
    expect(modal()).not.toBeInTheDocument();
  });

  it('uses proper url', function() {
    expect(licenseUrl).toBe('service/rest/v1/system/license');
  });
});
