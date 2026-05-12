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

jest.mock('axios');

jest.mock('@fortawesome/react-fontawesome', () => ({
  FontAwesomeIcon: (props) => React.createElement('span', { 'data-testid': `fa-icon`, className: 'fa-icon' }),
}));

jest.mock('@sonatype/nexus-ui-plugin', () => {
  const actual = jest.requireActual('@sonatype/nexus-ui-plugin');
  return {
    ...actual,
    HistoricalUsage: function MockHistoricalUsage() {
      return React.createElement('div', null,
        React.createElement('h2', null, 'Historical Usage')
      );
    },
  };
});

const selectors = {
  ...TestUtils.selectors,
  contactCompany: () => screen.getByText(DETAILS.COMPANY.LABEL).nextSibling,
  contactName: () => screen.getByText(DETAILS.NAME.LABEL).nextSibling,
  contactEmail: () => screen.getByText(DETAILS.EMAIL.LABEL).nextSibling,
  effectiveDate: () => screen.getByText(DETAILS.EFFECTIVE_DATE.LABEL).nextSibling,
  expirationDate: () => screen.getByText(DETAILS.EXPIRATION_DATE.LABEL).nextSibling,
  licenseType: () => screen.getByText(DETAILS.LICENSE_TYPES.LABEL),
  fingerprint: () => screen.getByText(DETAILS.FINGERPRINT.LABEL).nextSibling,
  maxRepoRequests: () => screen.getByText(DETAILS.REQUESTS_PER_MONTH.LABEL).nextSibling,
  maxRepoComponents: () => screen.getByText(DETAILS.TOTAL_COMPONENTS.LABEL).nextSibling,
  detailsSection: () => screen.queryByRole('heading', {name: LICENSING.SECTIONS.DETAILS, level: 2}),
  installSection: () => screen.queryByRole('heading', {name: LICENSING.SECTIONS.INSTALL, level: 2}),
  licensedUsageSection: () => screen.queryByRole('heading', {name: LICENSING.SECTIONS.USAGE, level: 2}),
  uploadInput: () => screen.queryByLabelText(INSTALL.LABEL, {hidden: true}),
  uploadButton: () => screen.queryByText(INSTALL.BUTTONS.UPLOAD),
  readOnlyWarning: () => screen.queryByText(UIStrings.SETTINGS.READ_ONLY.WARNING),
  agreement: {
    modal: () => screen.queryByText(AGREEMENT.CAPTION),
    acceptButton: () => screen.getByText(AGREEMENT.BUTTONS.ACCEPT),
    declineButton: () => screen.getByText(AGREEMENT.BUTTONS.DECLINE),
  },
  tabs: {
    queryLicenseTab: () => screen.queryByText(LICENSING.TABS.LICENSE, {selector: '.nx-tab'}),
    queryUsageTab: () => screen.queryByText(LICENSING.TABS.USAGE, {selector: '.nx-tab'})
  },

  historicalUsage: {
    queryTitle: () => screen.queryByRole('heading', {name: LICENSING.HISTORICAL_USAGE.TITLE})
  }
};

const DATA = {
  contactCompany: 'Test Sonatype Inc',
  contactEmail: 'test@sonatype.com',
  contactName: 'Ember Deboer',
  effectiveDate: '2022-07-14T14:20:40.108+00:00',
  expirationDate: '2023-08-02T00:00:00.000+00:00',
  features: 'SonatypeCLM, NexusProfessional, Firewall',
  fingerprint: '53274cced19cs2e5208s73801g4a9160a960684',
  licenseType: 'Sonatype IQ Server, Nexus Repository Pro, Sonatype Repository Firewall',
  licensedUsers: '1001',
  maxRepoRequests: '60000',
  maxRepoComponents: '200000'
};

describe('Licensing', () => {
  const renderComponent = async () => {
    await act(async () => {
      render(<Licensing/>);
    });
  }

  const expectLicenseDetails = data => {
    const {
      contactCompany,
      contactName,
      contactEmail,
      effectiveDate,
      expirationDate,
      licenseType,
      fingerprint
    } = selectors;

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
    expect(fingerprint()).toHaveTextContent(data.fingerprint);
  };

  beforeEach(() => {
    when(Axios.get).calledWith(licenseUrl).mockResolvedValue({
      data: DATA
    });
    when(Axios.post).calledWith(licenseUrl, expect.anything(), expect.anything()).mockResolvedValue({
      data: DATA
    });
    jest.spyOn(ExtJS, 'proLicenseUrl').mockImplementation(() => 'http://localhost:8081/PRO-LICENSE.html');
    jest.spyOn(ExtJS, 'useUser').mockImplementation(() => ({name: 'test-user'}));
    jest.spyOn(ExtJS, 'checkPermission').mockImplementation((permission) => {
      console.debug('checkPermission => true', permission);
      return true;
    });
  });

  describe('licensing page', () => {
    it('renders the resolved data', async () => {
      const {detailsSection, installSection} = selectors;

      await renderComponent();

      expect(detailsSection()).toBeInTheDocument();
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
      const {detailsSection, licensedUsageSection, uploadButton, uploadInput, agreement: {modal, acceptButton, declineButton}} = selectors;

      when(Axios.get).calledWith(licenseUrl).mockRejectedValue({message: 'Error'});

      await renderComponent();

      expect(detailsSection()).not.toBeInTheDocument();
      expect(licensedUsageSection()).not.toBeInTheDocument();
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

    it('shows DESCRIPTION_EXISTING when license is installed', async function() {
      await renderComponent();

      expect(screen.getByText(INSTALL.DESCRIPTION_EXISTING)).toBeInTheDocument();
      expect(screen.queryByText(INSTALL.DESCRIPTION)).not.toBeInTheDocument();
    });

    it('shows DESCRIPTION when no license is installed', async function() {
      when(Axios.get).calledWith(licenseUrl).mockRejectedValue({message: 'Error'});

      await renderComponent();

      expect(screen.getByText(INSTALL.DESCRIPTION)).toBeInTheDocument();
      expect(screen.queryByText(INSTALL.DESCRIPTION_EXISTING)).not.toBeInTheDocument();
    });
  });

  describe('historical usage tab', () => {
    it('shows the historical usage tab', async function() {
      await renderComponent();

      expect(selectors.tabs.queryLicenseTab()).toBeInTheDocument();
      expect(selectors.tabs.queryUsageTab()).toBeInTheDocument();
    });

    it ('hides the historical usage tab', async function() {
      when(ExtJS.checkPermission).calledWith(Permissions.METRICS.READ).mockReturnValue(false);

      await renderComponent();

      expect(selectors.tabs.queryLicenseTab()).toBeInTheDocument();
      expect(selectors.tabs.queryUsageTab()).not.toBeInTheDocument();
    });

    it('shows the historical usage tab content when clicked', async function() {
      givenMockHistoricalUsageData();

      await renderComponent();

      await userEvent.click(selectors.tabs.queryUsageTab());
      await waitFor(() => expect(selectors.historicalUsage.queryTitle()).toBeInTheDocument());

      await userEvent.click(selectors.tabs.queryLicenseTab());
      await waitFor(() => expect(selectors.historicalUsage.queryTitle()).not.toBeInTheDocument());
    });

    function givenMockHistoricalUsageData() {
      const mockData = [
        {
          metricDate: '2024-11-01T00:00:00.000',
          componentCount: 1000,
          percentageChangeComponent: 10,
          requestCount: 2000,
          percentageChangeRequest: -5,
          peakStorage: 1073741824,
          responseSize: 536870912,
        }
      ];
      when(Axios.get).calledWith('service/rest/v1/monthly-metrics').mockResolvedValue({ data: mockData });
    }
  });
});
