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
import { render, screen, act, cleanup } from '@testing-library/react';
import { Theme } from '@radix-ui/themes';

import { HttpClientFacet } from '../HttpClientFacet';
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

      // onOriginChangeWarning should be called with true
      expect(mockOnOriginChangeWarning).toHaveBeenCalledWith(true);

      // onNestedChange should be called to reset auth (clears all auth type fields)
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

      // onOriginChangeWarning should be called with true (any URL change triggers warning)
      expect(mockOnOriginChangeWarning).toHaveBeenCalledWith(true);

      // onNestedChange should be called to reset auth
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

      // onOriginChangeWarning should NOT be called
      expect(mockOnOriginChangeWarning).not.toHaveBeenCalled();

      // onNestedChange should NOT be called
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
});
