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
import {act} from 'react-dom/test-utils';
import TestUtils from '@sonatype/nexus-ui-plugin/src/frontend/src/interface/TestUtils';
import {waitFor, waitForElementToBeRemoved} from '@testing-library/react';
import userEvent from '@testing-library/user-event';

import UIStrings from '../../../../constants/UIStrings';
import AnalyzeApplicationModal from './AnalyzeApplicationModal';

const component = {
  'componentName': 'foobar',
  'repositoryName': 'hosted-repo'
};

jest.mock('axios', () => ({
  ...jest.requireActual('axios'), // Use most functions from actual axios
  get: jest.fn(() => Promise.resolve()),
  post: jest.fn(() => Promise.resolve())
}));

jest.mock('@sonatype/nexus-ui-plugin', () => {
  return {
    ...jest.requireActual('@sonatype/nexus-ui-plugin'),
    ExtJS: {
      showSuccessMessage: jest.fn(),
      showErrorMessage: jest.fn(),
    }
  }
});

const onAnalyzedMock = jest.fn(() => {});

const onCancelMock = jest.fn(() => {});

describe('AnalyzeApplicationModal', () => {
  const render = () => TestUtils
      .render(<AnalyzeApplicationModal componentModel={component} onCancel={onCancelMock} onAnalyzed={onAnalyzedMock} />, ({getByLabelText, getByText}) => ({
        email: () => getByLabelText(UIStrings.ANALYZE_APPLICATION.EMAIL.LABEL),
        password: () => getByLabelText(UIStrings.ANALYZE_APPLICATION.PASSWORD.LABEL),
        packages: () => getByLabelText(UIStrings.ANALYZE_APPLICATION.PACKAGES.LABEL),
        reportName: () => getByLabelText(UIStrings.ANALYZE_APPLICATION.REPORT.LABEL),
        analyzeButton: () => getByText(UIStrings.ANALYZE_APPLICATION.BUTTONS.ANALYZE),
        cancelButton: () => getByText(UIStrings.ANALYZE_APPLICATION.BUTTONS.CANCEL),
        selectedAsset: () => getByLabelText(UIStrings.ANALYZE_APPLICATION.SELECT_ASSET.LABEL),
      }));

  Axios.get.mockReturnValue(Promise.resolve({
    data: {
      emailAddress: 'test@sonatype.com',
      reportLabel: 'foo-1.0.0.jar',
      assetMap: {
        'foo': 'foo-app',
        'bar': 'bar-app'
      },
      selectedAsset: 'foo',
      tosAccepted: false
    }
  }));

  it('renders correctly', async() => {
    let {loadingMask, email, password, packages, reportName, cancelButton, analyzeButton, selectedAsset } = render();

    await waitForElementToBeRemoved(loadingMask());

    expect(email()).toHaveValue('test@sonatype.com');
    expect(password()).toHaveValue('');
    expect(packages()).toHaveValue('');
    expect(reportName()).toHaveValue('foo-1.0.0.jar');
    expect(cancelButton()).not.toHaveClass('disabled');
    expect(analyzeButton()).toHaveClass('disabled');
    expect(selectedAsset()).toHaveValue('foo');
  });

  it('form values are updated', async() => {
    let {loadingMask, email, password, packages, reportName, selectedAsset, cancelButton, analyzeButton } = render();

    await waitForElementToBeRemoved(loadingMask());

    await TestUtils.changeField(email, 'foo@bar.com');
    await TestUtils.changeField(password, 'foobar');
    await TestUtils.changeField(packages, 'packages');
    await TestUtils.changeField(reportName, 'foo-2.0.0.jar');
    await TestUtils.changeField(selectedAsset, 'bar');

    expect(email()).toHaveValue('foo@bar.com');
    expect(password()).toHaveValue('foobar');
    expect(packages()).toHaveValue('packages');
    expect(reportName()).toHaveValue('foo-2.0.0.jar');
    expect(selectedAsset()).toHaveValue('bar');
    expect(cancelButton()).not.toHaveClass('disabled');
    expect(analyzeButton()).not.toHaveClass('disabled');
  });

  it('analyze cancelled', async() => {
    let {loadingMask, email, password, analyzeButton, cancelButton } = render();

    await waitForElementToBeRemoved(loadingMask());

    expect(analyzeButton()).toHaveClass('disabled');
    await TestUtils.changeField(email, 'foo@bar.com');
    await TestUtils.changeField(password, 'foobar');
    expect(analyzeButton()).not.toHaveClass('disabled');

    await act(async () => userEvent.click(cancelButton()));

    expect(Axios.post).toHaveBeenCalledTimes(0);

    await waitFor( () => expect(onCancelMock).toHaveBeenCalled());
  });

  it('analyze submitted', async() => {
    let {loadingMask, email, password, packages, analyzeButton } = render();

    await waitForElementToBeRemoved(loadingMask());

    await TestUtils.changeField(email, 'foo@bar.com');
    await TestUtils.changeField(password, 'foobar');
    await TestUtils.changeField(packages, 'packageA');
    expect(analyzeButton()).not.toHaveClass('disabled');

    userEvent.click(analyzeButton());

    expect(Axios.post).toHaveBeenCalledTimes(1);
    expect(Axios.post).toHaveBeenCalledWith(
        'service/rest/internal/ui/ahc',
        {
          repositoryName: 'hosted-repo',
          assetId: 'foo',
          emailAddress: 'foo@bar.com',
          reportPassword: 'foobar',
          reportLabel: 'foo-1.0.0.jar',
          proprietaryPackages: 'packageA'
        }
    );

    await waitFor( () => expect(onAnalyzedMock).toHaveBeenCalled());
  });
});
