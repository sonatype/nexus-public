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
import {render, screen} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {ExtJS} from '@sonatype/nexus-ui-plugin';

import GenericProxyConfiguration from './GenericProxyConfiguration';
import UIStrings from '../../../../../constants/UIStrings';

jest.mock('@sonatype/nexus-ui-plugin', () => ({
  ...jest.requireActual('@sonatype/nexus-ui-plugin'),
  ExtJS: {
    state: jest.fn()
  },
  UseNexusTruststore: () => null
}));

jest.mock('./DockerIndexConfiguration', () => ({
  __esModule: true,
  default: () => null
}));

jest.mock('./DockerForeignLayerConfiguration', () => ({
  __esModule: true,
  default: () => null
}));

const {EDITOR} = UIStrings.REPOSITORIES;

describe('GenericProxyConfiguration', () => {
  const mockSend = jest.fn();

  const createParentMachine = (preserveEncodedCharacters = false) => [
    {
      context: {
        data: {
          format: 'maven2',
          type: 'proxy',
          replication: {preemptivePullEnabled: false},
          proxy: {
            remoteUrl: 'https://repo.example.com',
            contentMaxAge: -1,
            metadataMaxAge: 1440,
            preserveEncodedCharacters
          },
          httpClient: {
            blocked: false,
            autoBlock: true,
            connection: null,
            authentication: null
          }
        }
      }
    },
    mockSend
  ];

  beforeEach(() => {
    jest.clearAllMocks();
    ExtJS.state.mockReturnValue({
      getValue: jest.fn().mockReturnValue(false)
    });
  });

  it('renders the Preserve Encoded Characters checkbox', () => {
    render(<GenericProxyConfiguration parentMachine={createParentMachine()} />);

    expect(screen.getByText(EDITOR.PRESERVE_ENCODED_CHARS_LABEL)).toBeInTheDocument();
  });

  it('renders the Preserve Encoded Characters sublabel', () => {
    render(<GenericProxyConfiguration parentMachine={createParentMachine()} />);

    expect(screen.getByText(EDITOR.PRESERVE_ENCODED_CHARS_SUBLABEL)).toBeInTheDocument();
  });

  it('renders checkbox as unchecked by default', () => {
    render(<GenericProxyConfiguration parentMachine={createParentMachine(false)} />);

    const label = screen.getByText(EDITOR.PRESERVE_ENCODED_CHARS_LABEL);
    const checkbox = label.closest('.nx-form-group').querySelector('input[type="checkbox"]');
    expect(checkbox).not.toBeChecked();
  });

  it('dispatches UPDATE when Preserve Encoded Characters checkbox is toggled', () => {
    render(<GenericProxyConfiguration parentMachine={createParentMachine(false)} />);

    const label = screen.getByText(EDITOR.PRESERVE_ENCODED_CHARS_LABEL);
    const formGroup = label.closest('.nx-form-group');
    const checkbox = formGroup.querySelector('input[type="checkbox"]');

    userEvent.click(checkbox);

    expect(mockSend).toHaveBeenCalledWith({
      type: 'UPDATE',
      name: 'proxy.preserveEncodedCharacters',
      value: true
    });
  });
});
