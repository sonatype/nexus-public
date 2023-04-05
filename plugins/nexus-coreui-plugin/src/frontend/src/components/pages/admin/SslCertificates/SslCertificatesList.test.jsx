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
import {render, screen, waitForElementToBeRemoved} from '@testing-library/react';
import {sort, prop, descend, ascend} from 'ramda';
import userEvent from '@testing-library/user-event';
import {ExtJS, APIConstants} from '@sonatype/nexus-ui-plugin';
import TestUtils from '@sonatype/nexus-ui-plugin/src/frontend/src/interface/TestUtils';
import {when} from 'jest-when';
import Axios from 'axios';

import SslCertificatesList from './SslCertificatesList';
import {URLS} from './SslCertificatesHelper';
import UIStrings from '../../../../constants/UIStrings';
import {SSL_CERTIFICATES} from './SslCertificates.testdata';

const {SORT_DIRECTIONS: {DESC, ASC}} = APIConstants;
const {sslCertificatesUrl} = URLS;
const {SSL_CERTIFICATES: {LIST: LABELS}} = UIStrings;
const XSS_STRING = TestUtils.XSS_STRING;

jest.mock('axios', () => ({
  get: jest.fn()
}));

jest.mock('@sonatype/nexus-ui-plugin', () => {
  return {
    ...jest.requireActual('@sonatype/nexus-ui-plugin'),
    ExtJS: {
      checkPermission: jest.fn().mockReturnValue(true),
    }
  }
});

const selectors = {
  ...TestUtils.selectors,
  ...TestUtils.tableSelectors,
  emptyMessage: () => screen.getByText(LABELS.EMPTY_LIST),
  filter: () => screen.queryByPlaceholderText(UIStrings.FILTER),
  createButton: () => screen.getByText(LABELS.CREATE_BUTTON),
};

const FIELDS = {
  SUBJECT_COMMON_NAME: 'subjectCommonName',
  SUBJECT_ORGANIZATION: 'subjectOrganization',
  ISSUER_ORGANIZATION: 'issuerOrganization',
  FINGERPRINT: 'fingerprint',
};

const sortSslCertificates = (field, order = ASC) => sort((order === ASC ? ascend : descend)(prop(field)), SSL_CERTIFICATES);

describe('SslCertificatesList', function() {

  const renderAndWaitForLoad = async () => {
    render(<SslCertificatesList/>);
    await waitForElementToBeRemoved(selectors.queryLoadingMask());
  }

  beforeEach(() => {
    when(Axios.get).calledWith(sslCertificatesUrl).mockResolvedValue({
      data: SSL_CERTIFICATES
    });
  });

  it('renders the resolved empty data', async function() {
    when(Axios.get).calledWith(sslCertificatesUrl).mockResolvedValue({
      data: []
    });
    await renderAndWaitForLoad();

    expect(selectors.createButton()).not.toHaveClass('disabled');
    expect(selectors.emptyMessage()).toBeInTheDocument();
  });

  it('renders the resolved data', async function() {
    await renderAndWaitForLoad();

    TestUtils.expectTableHeaders(Object.values(LABELS.COLUMNS));
    const certificates = sortSslCertificates(FIELDS.SUBJECT_COMMON_NAME);

    TestUtils.expectTableRows(certificates, Object.values(FIELDS));
  });

  it('renders the resolved data with XSS', async function() {
    const XSS_ROWS = [{
      ...SSL_CERTIFICATES[0],
      subjectCommonName: XSS_STRING,
      subjectOrganization: XSS_STRING,
      issuerOrganization: XSS_STRING,
      fingerprint: XSS_STRING,
    }];

    when(Axios.get).calledWith(sslCertificatesUrl).mockResolvedValue({
      data: XSS_ROWS
    });

    await renderAndWaitForLoad();

    TestUtils.expectTableHeaders(Object.values(LABELS.COLUMNS));
    TestUtils.expectTableRows(XSS_ROWS, Object.values(FIELDS));
  });

  it('renders an error message', async function() {
    const message = 'Error Message !';
    const {tableAlert} = selectors;
    Axios.get.mockReturnValue(Promise.reject({message}));

    await renderAndWaitForLoad();

    expect(tableAlert()).toHaveTextContent(message);
  });

  it('sorts the rows by each columns', async function () {
    const {headerCell} = selectors;
    await renderAndWaitForLoad();

    let certificates = sortSslCertificates(FIELDS.SUBJECT_COMMON_NAME);
    TestUtils.expectProperRowsOrder(certificates, FIELDS.SUBJECT_COMMON_NAME);

    userEvent.click(headerCell(LABELS.COLUMNS.NAME));
    certificates = sortSslCertificates(FIELDS.SUBJECT_COMMON_NAME, DESC);
    TestUtils.expectProperRowsOrder(certificates, FIELDS.SUBJECT_COMMON_NAME);

    userEvent.click(headerCell(LABELS.COLUMNS.ISSUED_BY));
    certificates = sortSslCertificates(FIELDS.ISSUER_ORGANIZATION);
    TestUtils.expectProperRowsOrder(certificates, FIELDS.SUBJECT_COMMON_NAME);

    userEvent.click(headerCell(LABELS.COLUMNS.ISSUED_BY));
    certificates = sortSslCertificates(FIELDS.ISSUER_ORGANIZATION, DESC);
    TestUtils.expectProperRowsOrder(certificates, FIELDS.SUBJECT_COMMON_NAME);

    userEvent.click(headerCell(LABELS.COLUMNS.FINGERPRINT));
    certificates = sortSslCertificates(FIELDS.FINGERPRINT);
    TestUtils.expectProperRowsOrder(certificates, FIELDS.SUBJECT_COMMON_NAME);

    userEvent.click(headerCell(LABELS.COLUMNS.FINGERPRINT));
    certificates = sortSslCertificates(FIELDS.FINGERPRINT, DESC);
    TestUtils.expectProperRowsOrder(certificates, FIELDS.SUBJECT_COMMON_NAME);
  });

  it('filters by each columns', async function() {
    const {filter} = selectors;

    await renderAndWaitForLoad();

    await TestUtils.expectProperFilteredItemsCount(filter, '', SSL_CERTIFICATES.length);
    await TestUtils.expectProperFilteredItemsCount(filter, 'nuget', 1);
    await TestUtils.expectProperFilteredItemsCount(filter, 'micro', 1);
    await TestUtils.expectProperFilteredItemsCount(filter, 'amazon', 1);
    await TestUtils.expectProperFilteredItemsCount(filter, '6C', 1);
  });

  it('disables the create button when not enough permissions', async function() {
    ExtJS.checkPermission.mockReturnValue(false);

    await renderAndWaitForLoad();

    expect(selectors.createButton()).toHaveClass('disabled');
  });
});
