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
let cdnDefaultForNewRepositories = false;

jest.mock('@sonatype/nexus-ui-plugin', () => ({
  ...jest.requireActual('@sonatype/nexus-ui-plugin'),
  ExtAPIUtils: {
    useExtMachine: () => [
      {
        context: {
          data: [
            {name: 'default', globalDistributionEnabled: true},
            {name: 'plain', globalDistributionEnabled: false}
          ],
          error: null
        },
        matches: () => false
      },
      jest.fn()
    ]
  },
  ExtJS: {
    isEnterpriseEdition: () => hasEnterprise,
    state: () => ({
      getValue: (key, defaultValue) => {
        if (key === 'cdnDefaultForNewRepositories') {
          return cdnDefaultForNewRepositories;
        }
        return defaultValue;
      }
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
    cdnDefaultForNewRepositories = false;
  });

  it('should not render the global distribution checkbox without an enterprise edition', () => {
    render(<GenericStorageConfiguration parentMachine={createParentMachine()} />);
    expect(globalDistributionCheckbox()).not.toBeInTheDocument();
  });

  it('should render the checkbox unchecked for an enterprise edition when the CDN default is false', () => {
    hasEnterprise = true;
    cdnDefaultForNewRepositories = false;

    render(<GenericStorageConfiguration parentMachine={createParentMachine()} />);

    expect(screen.getByText(EDITOR.GLOBAL_DISTRIBUTION_LABEL)).toBeInTheDocument();
    expect(globalDistributionCheckbox()).not.toBeChecked();
  });

  it('should render the checkbox for a group repository', () => {
    hasEnterprise = true;

    render(<GenericStorageConfiguration parentMachine={createParentMachine({type: 'group'})} />);

    expect(globalDistributionCheckbox()).toBeInTheDocument();
  });

  it('should check the checkbox when the repository already has globalDistribution=true', () => {
    hasEnterprise = true;
    // Simulate edit mode: parent already has the field set; the CDN default seed must NOT
    // overwrite an explicit value.
    render(
      <GenericStorageConfiguration
        parentMachine={createParentMachine({
          storage: {blobStoreName: 'default', strictContentTypeValidation: true, globalDistribution: true}
        })}
      />
    );

    expect(globalDistributionCheckbox()).toBeChecked();
  });


  it('should not render the global distribution checkbox when the blob store cannot distribute', () => {
    hasEnterprise = true;

    render(
      <GenericStorageConfiguration parentMachine={createParentMachine({storage: {blobStoreName: 'plain'}})} />
    );

    expect(globalDistributionCheckbox()).not.toBeInTheDocument();
  });

  it('should clear global distribution when the blob store cannot distribute', () => {
    hasEnterprise = true;

    render(
      <GenericStorageConfiguration
        parentMachine={createParentMachine({storage: {blobStoreName: 'plain', globalDistribution: true}})}
      />
    );

    expect(mockSendParent).toHaveBeenCalledWith({
      type: 'UPDATE',
      name: 'storage.globalDistribution',
      value: false
    });
  });

  it('should dispatch UPDATE with the CDN default on mount when the field is absent and enterprise is on', () => {
    hasEnterprise = true;
    cdnDefaultForNewRepositories = true;

    render(<GenericStorageConfiguration parentMachine={createParentMachine()} />);

    expect(mockSendParent).toHaveBeenCalledWith({
      type: 'UPDATE',
      name: 'storage.globalDistribution',
      value: true
    });
  });

  it('should NOT dispatch UPDATE on mount when the parent already carries an explicit value', () => {
    hasEnterprise = true;
    cdnDefaultForNewRepositories = true;

    render(
      <GenericStorageConfiguration
        parentMachine={createParentMachine({
          storage: {blobStoreName: 'default', strictContentTypeValidation: true, globalDistribution: false}
        })}
      />
    );

    // The blob-store default dispatch may still fire; we're asserting no globalDistribution UPDATE happened.
    const globalDistCalls = mockSendParent.mock.calls.filter(
      ([msg]) => msg?.name === 'storage.globalDistribution'
    );
    expect(globalDistCalls).toHaveLength(0);
  });

  it('should NOT dispatch UPDATE on mount when not enterprise', () => {
    hasEnterprise = false;
    cdnDefaultForNewRepositories = true;

    render(<GenericStorageConfiguration parentMachine={createParentMachine()} />);

    const globalDistCalls = mockSendParent.mock.calls.filter(
      ([msg]) => msg?.name === 'storage.globalDistribution'
    );
    expect(globalDistCalls).toHaveLength(0);
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
