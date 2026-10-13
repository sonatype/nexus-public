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
import {fireEvent, render, screen} from '@testing-library/react';

import GenericStorageConfiguration from './GenericStorageConfiguration';
import UIStrings from '../../../../../constants/UIStrings';

const {EDITOR} = UIStrings.REPOSITORIES;

let hasEnterprise = false;

jest.mock('@sonatype/nexus-ui-plugin', () => ({
  ...jest.requireActual('@sonatype/nexus-ui-plugin'),
  ExtAPIUtils: {
    useExtMachine: () => [
      {
        context: {data: [{name: 'default'}], error: null},
        matches: () => false
      },
      jest.fn()
    ]
  },
  ExtJS: {
    state: () => ({
      getValue: (key, defaultValue) => (key === 'hasEnterprise' ? hasEnterprise : defaultValue)
    })
  }
}));

describe('GenericStorageConfiguration', () => {
  const mockSendParent = jest.fn();

  const createParentMachine = (data = {}) => [
    {
      context: {
        data: {
          format: 'maven2',
          type: 'hosted',
          storage: {blobStoreName: 'default', strictContentTypeValidation: true},
          ...data
        },
        pristineData: {}
      }
    },
    mockSendParent
  ];

  const globalDistributionCheckbox = () =>
    screen.queryByRole('checkbox', {name: EDITOR.GLOBAL_DISTRIBUTION_DESCR});

  beforeEach(() => {
    jest.clearAllMocks();
    hasEnterprise = false;
  });

  it('should not render the global distribution checkbox without an enterprise edition', () => {
    render(<GenericStorageConfiguration parentMachine={createParentMachine()} />);

    expect(globalDistributionCheckbox()).not.toBeInTheDocument();
  });

  it('should render the global distribution checkbox for an enterprise edition', () => {
    hasEnterprise = true;

    render(<GenericStorageConfiguration parentMachine={createParentMachine()} />);

    expect(screen.getByText(EDITOR.GLOBAL_DISTRIBUTION_LABEL)).toBeInTheDocument();
    expect(globalDistributionCheckbox()).not.toBeChecked();
  });

  it('should render the global distribution checkbox for a group repository', () => {
    hasEnterprise = true;

    render(<GenericStorageConfiguration parentMachine={createParentMachine({type: 'group'})} />);

    expect(globalDistributionCheckbox()).toBeInTheDocument();
  });

  it('should check the global distribution checkbox when enabled on the repository', () => {
    hasEnterprise = true;

    render(
      <GenericStorageConfiguration parentMachine={createParentMachine({storage: {globalDistribution: true}})} />
    );

    expect(globalDistributionCheckbox()).toBeChecked();
  });

  it('should update global distribution when toggled', () => {
    hasEnterprise = true;

    render(<GenericStorageConfiguration parentMachine={createParentMachine()} />);
    fireEvent.click(globalDistributionCheckbox());

    expect(mockSendParent).toHaveBeenCalledWith({
      type: 'UPDATE',
      name: 'storage.globalDistribution',
      value: true
    });
  });
});
