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

import DockerEcrAuthConfiguration from './DockerEcrAuthConfiguration';
import UIStrings from '../../../../../constants/UIStrings';

const {ECR} = UIStrings.REPOSITORIES.EDITOR.DOCKER;

describe('DockerEcrAuthConfiguration', () => {
  const mockSend = jest.fn();

  const ECR_URL = 'https://123456789012.dkr.ecr.us-east-1.amazonaws.com';

  const createParentMachine = ({
    sessionToken = null,
    format = 'docker',
    type = 'proxy',
    remoteUrl = ECR_URL,
    authType = 'username'
  } = {}) => [
    {
      context: {
        data: {
          format,
          type,
          proxy: {remoteUrl},
          httpClient: {authentication: {type: authType}},
          dockerProxy: {
            ecrAuth: {sessionToken}
          }
        },
        pristineData: {
          dockerProxy: {
            ecrAuth: {sessionToken: null}
          }
        }
      }
    },
    mockSend
  ];

  beforeEach(() => jest.clearAllMocks());

  it('renders nothing when the remote URL is not an ECR registry', () => {
    const {container} = render(
      <DockerEcrAuthConfiguration
        parentMachine={createParentMachine({remoteUrl: 'https://registry-1.docker.io'})}
      />
    );

    expect(container).toBeEmptyDOMElement();
  });

  it('renders nothing when the auth type is not username', () => {
    const {container} = render(
      <DockerEcrAuthConfiguration parentMachine={createParentMachine({authType: 'ntlm'})} />
    );

    expect(container).toBeEmptyDOMElement();
  });

  it('renders nothing when there is no auth type selected', () => {
    const {container} = render(
      <DockerEcrAuthConfiguration parentMachine={createParentMachine({authType: ''})} />
    );

    expect(container).toBeEmptyDOMElement();
  });

  it('renders nothing when the format is not docker', () => {
    const {container} = render(
      <DockerEcrAuthConfiguration parentMachine={createParentMachine({format: 'maven2'})} />
    );

    expect(container).toBeEmptyDOMElement();
  });

  it('renders the caption, session token field and its sublabel', () => {
    render(<DockerEcrAuthConfiguration parentMachine={createParentMachine()} />);

    expect(screen.getByText(ECR.CAPTION)).toBeInTheDocument();
    expect(screen.getByText(ECR.SESSION_TOKEN.LABEL)).toBeInTheDocument();
    expect(screen.getByText(ECR.SESSION_TOKEN.SUBLABEL)).toBeInTheDocument();
    expect(screen.getByPlaceholderText(ECR.SESSION_TOKEN.PLACEHOLDER)).toBeInTheDocument();
  });

  it('does NOT show the expiry warning when the session token is blank', () => {
    render(<DockerEcrAuthConfiguration parentMachine={createParentMachine({sessionToken: null})} />);

    expect(screen.queryByText(ECR.EXPIRY_WARNING)).not.toBeInTheDocument();
  });

  it('shows the expiry warning when a session token is present', () => {
    render(<DockerEcrAuthConfiguration parentMachine={createParentMachine({sessionToken: 'some-token'})} />);

    expect(screen.getByText(ECR.EXPIRY_WARNING)).toBeInTheDocument();
  });

  it('dispatches UPDATE for the session token on change', () => {
    render(<DockerEcrAuthConfiguration parentMachine={createParentMachine()} />);

    const input = screen.getByPlaceholderText(ECR.SESSION_TOKEN.PLACEHOLDER);
    fireEvent.change(input, {target: {value: 'ASIA-session-token'}});

    expect(mockSend).toHaveBeenCalledWith(
      expect.objectContaining({
        type: 'UPDATE',
        name: 'dockerProxy.ecrAuth.sessionToken'
      })
    );
  });

  it('renders the session token field as a password input', () => {
    render(<DockerEcrAuthConfiguration parentMachine={createParentMachine()} />);

    const input = screen.getByPlaceholderText(ECR.SESSION_TOKEN.PLACEHOLDER);
    expect(input).toHaveAttribute('type', 'password');
  });
});
