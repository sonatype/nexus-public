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
import { render, screen } from '@testing-library/react';
import { Theme } from '@radix-ui/themes';

import { HttpClientFacet, createAuthTypeChangeHandler, parseEcrUrl } from '../HttpClientFacet';
import { RepositoryFormData } from '../../types';

const defaultFormData: RepositoryFormData = {
  name: 'test-repo',
  format: 'maven2',
  type: 'proxy',
  proxy: {
    remoteUrl: 'https://repo1.maven.org/maven2/',
    contentMaxAge: 1440,
    metadataMaxAge: 1440,
  },
  httpClient: {
    blocked: false,
    autoBlock: true,
    connection: null,
    authentication: null,
  },
};

function renderFacet(props: Partial<React.ComponentProps<typeof HttpClientFacet>> = {}) {
  const defaultProps = {
    formData: defaultFormData,
    onChange: jest.fn(),
    onNestedChange: jest.fn(),
    onOriginChangeWarning: jest.fn(),
    errors: {},
  };
  return render(
    <Theme>
      <HttpClientFacet {...defaultProps} {...props} />
    </Theme>
  );
}

describe('HttpClientFacet', () => {
  describe('URL change warning', () => {
    it('triggers warning callback when host changes in edit mode with auth configured', () => {
      const mockOnNestedChange = jest.fn();
      const mockOnOriginChangeWarning = jest.fn();
      const formDataWithAuth: RepositoryFormData = {
        ...defaultFormData,
        httpClient: {
          blocked: false,
          autoBlock: true,
          connection: null,
          authentication: {
            type: 'username',
            username: 'testuser',
            password: 'testpass',
          },
        },
        proxy: {
          remoteUrl: 'https://new-host.example.com/maven2/',
          contentMaxAge: 1440,
          metadataMaxAge: 1440,
        },
      };

      renderFacet({
        formData: formDataWithAuth,
        onNestedChange: mockOnNestedChange,
        onOriginChangeWarning: mockOnOriginChangeWarning,
        isEdit: true,
        originalRemoteUrl: 'https://repo1.maven.org/maven2/',
        hadAuthOnLoad: true,
      });

      expect(mockOnOriginChangeWarning).toHaveBeenCalledWith(true);

      expect(mockOnNestedChange).toHaveBeenCalledWith('httpClient', {
        authentication: {
          type: 'username',
          username: '',
          password: '',
          bearerToken: '',
          ntlmHost: '',
          ntlmDomain: '',
          preemptive: false,
        },
      });
    });

    it('triggers warning when URL changes (including path) in edit mode with auth configured', () => {
      const mockOnNestedChange = jest.fn();
      const mockOnOriginChangeWarning = jest.fn();
      const formDataWithPathChange: RepositoryFormData = {
        ...defaultFormData,
        httpClient: {
          blocked: false,
          autoBlock: true,
          connection: null,
          authentication: {
            type: 'username',
            username: 'testuser',
            password: 'testpass',
          },
        },
        proxy: {
          remoteUrl: 'https://repo1.maven.org/maven2/changed-path/',
          contentMaxAge: 1440,
          metadataMaxAge: 1440,
        },
      };

      renderFacet({
        formData: formDataWithPathChange,
        onNestedChange: mockOnNestedChange,
        onOriginChangeWarning: mockOnOriginChangeWarning,
        isEdit: true,
        originalRemoteUrl: 'https://repo1.maven.org/maven2/',
        hadAuthOnLoad: true,
      });

      expect(mockOnOriginChangeWarning).toHaveBeenCalledWith(true);

      expect(mockOnNestedChange).toHaveBeenCalledWith('httpClient', {
        authentication: {
          type: 'username',
          username: '',
          password: '',
          bearerToken: '',
          ntlmHost: '',
          ntlmDomain: '',
          preemptive: false,
        },
      });
    });

    it('does not trigger warning in edit mode without auth configured', () => {
      const mockOnNestedChange = jest.fn();
      const mockOnOriginChangeWarning = jest.fn();
      const formDataNoAuth: RepositoryFormData = {
        ...defaultFormData,
        httpClient: {
          blocked: false,
          autoBlock: true,
          connection: null,
          authentication: null,
        },
        proxy: {
          remoteUrl: 'https://new-host.example.com/maven2/',
          contentMaxAge: 1440,
          metadataMaxAge: 1440,
        },
      };

      renderFacet({
        formData: formDataNoAuth,
        onNestedChange: mockOnNestedChange,
        onOriginChangeWarning: mockOnOriginChangeWarning,
        isEdit: true,
        originalRemoteUrl: 'https://repo1.maven.org/maven2/',
      });

      expect(mockOnOriginChangeWarning).not.toHaveBeenCalled();
      expect(mockOnNestedChange).not.toHaveBeenCalled();
    });

    it('does not trigger warning when not in edit mode', () => {
      const mockOnNestedChange = jest.fn();
      const mockOnOriginChangeWarning = jest.fn();
      const formDataWithAuth: RepositoryFormData = {
        ...defaultFormData,
        httpClient: {
          blocked: false,
          autoBlock: true,
          connection: null,
          authentication: {
            type: 'username',
            username: 'testuser',
            password: 'testpass',
          },
        },
        proxy: {
          remoteUrl: 'https://new-host.example.com/maven2/',
          contentMaxAge: 1440,
          metadataMaxAge: 1440,
        },
      };

      renderFacet({
        formData: formDataWithAuth,
        onNestedChange: mockOnNestedChange,
        onOriginChangeWarning: mockOnOriginChangeWarning,
        isEdit: false,
        originalRemoteUrl: 'https://repo1.maven.org/maven2/',
      });

      expect(mockOnOriginChangeWarning).not.toHaveBeenCalled();
      expect(mockOnNestedChange).not.toHaveBeenCalled();
    });

    it('does not retrigger on subsequent renders (one-shot behavior)', () => {
      const mockOnNestedChange = jest.fn();
      const mockOnOriginChangeWarning = jest.fn();
      const formDataWithAuth: RepositoryFormData = {
        ...defaultFormData,
        httpClient: {
          blocked: false,
          autoBlock: true,
          connection: null,
          authentication: {
            type: 'username',
            username: 'testuser',
            password: 'testpass',
          },
        },
        proxy: {
          remoteUrl: 'https://new-host.example.com/maven2/',
          contentMaxAge: 1440,
          metadataMaxAge: 1440,
        },
      };

      const { rerender } = renderFacet({
        formData: formDataWithAuth,
        onNestedChange: mockOnNestedChange,
        onOriginChangeWarning: mockOnOriginChangeWarning,
        isEdit: true,
        originalRemoteUrl: 'https://repo1.maven.org/maven2/',
        hadAuthOnLoad: true,
      });

      expect(mockOnOriginChangeWarning).toHaveBeenCalledTimes(1);
      mockOnOriginChangeWarning.mockClear();
      mockOnNestedChange.mockClear();

      rerender(
        <Theme>
          <HttpClientFacet
            formData={{
              ...formDataWithAuth,
              proxy: { remoteUrl: 'https://another-host.example.com/', contentMaxAge: 1440, metadataMaxAge: 1440 },
            }}
            onChange={jest.fn()}
            onNestedChange={mockOnNestedChange}
            onOriginChangeWarning={mockOnOriginChangeWarning}
            errors={{}}
            isEdit={true}
            originalRemoteUrl="https://repo1.maven.org/maven2/"
            hadAuthOnLoad={true}
          />
        </Theme>
      );

      expect(mockOnOriginChangeWarning).not.toHaveBeenCalled();
      expect(mockOnNestedChange).not.toHaveBeenCalled();
    });

    it('clears warning when URL reverts to original origin', () => {
      const mockOnNestedChange = jest.fn();
      const mockOnOriginChangeWarning = jest.fn();
      const formDataWithAuth: RepositoryFormData = {
        ...defaultFormData,
        httpClient: {
          blocked: false,
          autoBlock: true,
          connection: null,
          authentication: {
            type: 'username',
            username: 'testuser',
            password: 'testpass',
          },
        },
        proxy: {
          remoteUrl: 'https://new-host.example.com/maven2/',
          contentMaxAge: 1440,
          metadataMaxAge: 1440,
        },
      };

      const { rerender } = renderFacet({
        formData: formDataWithAuth,
        onNestedChange: mockOnNestedChange,
        onOriginChangeWarning: mockOnOriginChangeWarning,
        isEdit: true,
        originalRemoteUrl: 'https://repo1.maven.org/maven2/',
        hadAuthOnLoad: true,
      });

      expect(mockOnOriginChangeWarning).toHaveBeenCalledWith(true);
      mockOnOriginChangeWarning.mockClear();

      rerender(
        <Theme>
          <HttpClientFacet
            formData={{
              ...formDataWithAuth,
              proxy: { remoteUrl: 'https://repo1.maven.org/maven2/', contentMaxAge: 1440, metadataMaxAge: 1440 },
            }}
            onChange={jest.fn()}
            onNestedChange={mockOnNestedChange}
            onOriginChangeWarning={mockOnOriginChangeWarning}
            errors={{}}
            isEdit={true}
            originalRemoteUrl="https://repo1.maven.org/maven2/"
            hadAuthOnLoad={true}
          />
        </Theme>
      );

      expect(mockOnOriginChangeWarning).toHaveBeenCalledWith(false);
    });
  });

  // Regression guard for NEXUS-53319: parseEcrUrl was called but never defined, causing a
  // ReferenceError that white-screened the docker proxy repository form (introduced by NEXUS-51703).
  describe('parseEcrUrl', () => {
    it('parses a valid ECR URL with https scheme', () => {
      const result = parseEcrUrl('https://123456789012.dkr.ecr.us-east-1.amazonaws.com');
      expect(result).toEqual({ registryId: '123456789012', awsRegion: 'us-east-1' });
    });

    it('parses a valid ECR URL with a path suffix', () => {
      const result = parseEcrUrl('https://123456789012.dkr.ecr.eu-west-2.amazonaws.com/my-repo');
      expect(result).toEqual({ registryId: '123456789012', awsRegion: 'eu-west-2' });
    });

    it('parses a valid ECR URL with a port', () => {
      const result = parseEcrUrl('https://123456789012.dkr.ecr.us-west-2.amazonaws.com:443');
      expect(result).toEqual({ registryId: '123456789012', awsRegion: 'us-west-2' });
    });

    it('parses a valid ECR URL without a scheme', () => {
      const result = parseEcrUrl('123456789012.dkr.ecr.ap-southeast-1.amazonaws.com');
      expect(result).toEqual({ registryId: '123456789012', awsRegion: 'ap-southeast-1' });
    });

    it('returns null for a non-ECR URL', () => {
      expect(parseEcrUrl('https://registry-1.docker.io')).toBeNull();
    });

    it('returns null for an empty string', () => {
      expect(parseEcrUrl('')).toBeNull();
    });

    it('returns null for a URL with fewer than 12 account-id digits', () => {
      expect(parseEcrUrl('https://12345.dkr.ecr.us-east-1.amazonaws.com')).toBeNull();
    });
  });

  describe('ECR docker proxy form — auth type dropdown hidden for ECR URL', () => {
    it('hides the auth type dropdown when the remote URL is an ECR registry URL', () => {
      renderFacet({
        format: 'docker',
        formData: {
          ...defaultFormData,
          proxy: {
            remoteUrl: 'https://123456789012.dkr.ecr.us-east-1.amazonaws.com',
            contentMaxAge: 1440,
            metadataMaxAge: 1440,
          },
        },
      });
      expect(screen.queryByRole('combobox', { name: /authentication type/i })).not.toBeInTheDocument();
    });

    it('shows the auth type dropdown for a non-ECR docker proxy URL', () => {
      renderFacet({
        format: 'docker',
        formData: {
          ...defaultFormData,
          proxy: {
            remoteUrl: 'https://registry-1.docker.io',
            contentMaxAge: 1440,
            metadataMaxAge: 1440,
          },
        },
      });
      // Radix Select renders a button not a native combobox — verify the label is present
      expect(screen.getByText(/authentication type/i)).toBeInTheDocument();
    });
  });

  // The Authentication Type select is a Radix Select, whose options are not rendered into
  // jsdom, so the type-change behavior is exercised through the exported pure handler factory.
  describe('auth type handler', () => {
    it('clears HTTP auth when None is selected', () => {
      const onNestedChange = jest.fn();
      const handler = createAuthTypeChangeHandler(onNestedChange, {
        existingAuth: { type: 'username', username: 'user', password: 'pass' },
      });

      handler('');

      expect(onNestedChange).toHaveBeenCalledWith('httpClient', { authentication: null });
    });

    it('sets the auth type and preserves existing credentials when a type is selected', () => {
      const onNestedChange = jest.fn();
      const handler = createAuthTypeChangeHandler(onNestedChange, {
        existingAuth: { type: 'username', username: 'existingUser', password: 'existingPass' },
      });

      handler('username');

      expect(onNestedChange).toHaveBeenCalledWith('httpClient', {
        authentication: { type: 'username', username: 'existingUser', password: 'existingPass' },
      });
    });

    it('initializes with empty credentials when no existing auth', () => {
      const onNestedChange = jest.fn();
      const handler = createAuthTypeChangeHandler(onNestedChange, {
        existingAuth: null,
      });

      handler('ntlm');

      expect(onNestedChange).toHaveBeenCalledWith('httpClient', {
        authentication: { type: 'ntlm', username: '', password: '' },
      });
    });
  });
});
