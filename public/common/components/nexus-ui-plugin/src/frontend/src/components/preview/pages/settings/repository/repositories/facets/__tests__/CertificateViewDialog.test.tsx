/*
 * Sonatype Nexus (TM) Open Source Version
 * Copyright (c) 2008-present Sonatype, Inc.
 * All rights reserved. Includes the third-party code listed at http://links.sonatype.com/products/nexus/oss/attributions.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse Public License Version 1.0,
 * which accompanies this distribution and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Sonatype Nexus (TM) Professional Version is available from Sonatype, Inc. "Sonatype" and "Sonatype Nexus" are trademarks
 * of Sonatype, Inc. Apache Maven is a trademark of the Apache Software Foundation. All other trademarks are the property of their respective owners.
 */

import React from 'react';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { Theme } from '@radix-ui/themes';
import Axios from 'axios';

import { CertificateViewDialog } from '../CertificateViewDialog';

jest.mock('axios');
jest.mock('@sonatype/nexus-ui-plugin', () => ({
  ExtJS: {
    checkPermission: jest.fn().mockReturnValue(true),
  },
}));

const mockedAxios = Axios as jest.Mocked<typeof Axios>;

const MOCK_CERT = {
  id: 'cert-id-123',
  subjectCommonName: 'repo1.maven.org',
  subjectOrganization: 'Apache Software Foundation',
  subjectOrganizationalUnit: 'Infrastructure',
  issuerCommonName: 'DigiCert TLS RSA SHA256 2020 CA1',
  issuerOrganization: 'DigiCert Inc',
  issuerOrganizationalUnit: undefined,
  fingerprint: 'AA:BB:CC:DD:EE:FF',
  issuedOn: 1700000000000,
  expiresOn: 1900000000000,
  pem: '-----BEGIN CERTIFICATE-----\nMIIFake\n-----END CERTIFICATE-----',
};

function renderDialog(props: Partial<React.ComponentProps<typeof CertificateViewDialog>> = {}) {
  const defaultProps = {
    remoteUrl: 'https://repo1.maven.org/maven2/',
    onClose: jest.fn(),
  };
  return render(
    <Theme>
      <CertificateViewDialog {...defaultProps} {...props} />
    </Theme>
  );
}

beforeEach(() => {
  jest.clearAllMocks();
  const { ExtJS } = jest.requireMock('@sonatype/nexus-ui-plugin');
  ExtJS.checkPermission.mockReturnValue(true);
});

describe('CertificateViewDialog', () => {
  describe('loading state', () => {
    it('shows loading indicator while fetching', () => {
      mockedAxios.get.mockReturnValue(new Promise(() => {}));
      renderDialog();
      expect(screen.getByText('Retrieving certificate…')).toBeInTheDocument();
    });
  });

  describe('non-HTTPS URL guard', () => {
    it('shows error for non-HTTPS URLs without making an API call', async () => {
      renderDialog({ remoteUrl: 'http://insecure.example.com/' });
      await waitFor(() => {
        expect(screen.getByText('Certificate inspection is only available for HTTPS URLs.')).toBeInTheDocument();
      });
      expect(mockedAxios.get).not.toHaveBeenCalled();
    });
  });

  describe('successful load', () => {
    beforeEach(() => {
      mockedAxios.get.mockImplementation((url: string) => {
        if (url.includes('/ssl/truststore')) {
          return Promise.resolve({ data: [] });
        }
        return Promise.resolve({ data: MOCK_CERT });
      });
    });

    it('renders certificate subject and issuer details', async () => {
      renderDialog();
      await waitFor(() => expect(screen.getByText('repo1.maven.org')).toBeInTheDocument());
      expect(screen.getByText('Apache Software Foundation')).toBeInTheDocument();
      expect(screen.getByText('DigiCert TLS RSA SHA256 2020 CA1')).toBeInTheDocument();
    });

    it('renders the fingerprint', async () => {
      renderDialog();
      await waitFor(() => expect(screen.getByText('AA:BB:CC:DD:EE:FF')).toBeInTheDocument());
    });

    it('shows "Add certificate to truststore" when cert is not already trusted', async () => {
      renderDialog();
      await waitFor(() => expect(screen.getByText('Add certificate to truststore')).toBeInTheDocument());
    });

    it('shows Remove button when cert is already trusted', async () => {
      mockedAxios.get.mockImplementation((url: string) => {
        if (url.includes('/ssl/truststore')) {
          return Promise.resolve({ data: [MOCK_CERT] });
        }
        return Promise.resolve({ data: MOCK_CERT });
      });
      renderDialog();
      await waitFor(() => expect(screen.getByText('Remove certificate from truststore')).toBeInTheDocument());
      expect(screen.queryByText('Add certificate to truststore')).not.toBeInTheDocument();
    });

    it('resolves cert id from trust store when fingerprints match but id is absent on fetched cert', async () => {
      const certWithoutId = { ...MOCK_CERT, id: undefined };
      mockedAxios.get.mockImplementation((url: string) => {
        if (url.includes('/ssl/truststore')) {
          return Promise.resolve({ data: [MOCK_CERT] });
        }
        return Promise.resolve({ data: certWithoutId });
      });
      renderDialog();
      await waitFor(() => expect(screen.getByText('Remove certificate from truststore')).toBeInTheDocument());
    });
  });

  describe('error state', () => {
    it('shows error message and Retry button on API failure', async () => {
      mockedAxios.get.mockRejectedValue({ message: 'Network Error' });
      renderDialog();
      await waitFor(() => expect(screen.getByText('Network Error')).toBeInTheDocument());
      expect(screen.getByText('Retry')).toBeInTheDocument();
    });

    it('retries the API call when Retry is clicked', async () => {
      mockedAxios.get
        .mockRejectedValueOnce({ message: 'Network Error' })
        .mockImplementation((url: string) => {
          if (url.includes('/ssl/truststore')) return Promise.resolve({ data: [] });
          return Promise.resolve({ data: MOCK_CERT });
        });

      renderDialog();
      await waitFor(() => expect(screen.getByText('Retry')).toBeInTheDocument());
      fireEvent.click(screen.getByText('Retry'));
      await waitFor(() => expect(screen.getByText('repo1.maven.org')).toBeInTheDocument());
    });

    it('uses server error message when available', async () => {
      mockedAxios.get.mockRejectedValue({ response: { data: { message: 'SSL handshake failed' } } });
      renderDialog();
      await waitFor(() => expect(screen.getByText('SSL handshake failed')).toBeInTheDocument());
    });
  });

  describe('trust store actions', () => {
    beforeEach(() => {
      mockedAxios.get.mockImplementation((url: string) => {
        if (url.includes('/ssl/truststore')) return Promise.resolve({ data: [] });
        return Promise.resolve({ data: MOCK_CERT });
      });
    });

    it('calls POST to add cert to trust store', async () => {
      mockedAxios.post = jest.fn().mockResolvedValue({});
      renderDialog();
      await waitFor(() => expect(screen.getByText('Add certificate to truststore')).toBeInTheDocument());
      fireEvent.click(screen.getByText('Add certificate to truststore'));
      await waitFor(() => expect(mockedAxios.post).toHaveBeenCalledWith(
        '/service/rest/v1/security/ssl/truststore',
        MOCK_CERT.pem,
        { headers: { 'Content-Type': 'text/plain' } }
      ));
      // After adding, the Remove button should appear instead
      expect(screen.getByText('Remove certificate from truststore')).toBeInTheDocument();
    });

    it('shows action error when adding cert fails', async () => {
      mockedAxios.post = jest.fn().mockRejectedValue({ message: 'Forbidden' });
      renderDialog();
      await waitFor(() => fireEvent.click(screen.getByText('Add certificate to truststore')));
      await waitFor(() => expect(screen.getByText('Forbidden')).toBeInTheDocument());
    });

    it('calls DELETE to remove cert from trust store', async () => {
      mockedAxios.get.mockImplementation((url: string) => {
        if (url.includes('/ssl/truststore')) return Promise.resolve({ data: [MOCK_CERT] });
        return Promise.resolve({ data: MOCK_CERT });
      });
      mockedAxios.delete = jest.fn().mockResolvedValue({});
      renderDialog();
      await waitFor(() => expect(screen.getByText('Remove certificate from truststore')).toBeInTheDocument());
      fireEvent.click(screen.getByText('Remove certificate from truststore'));
      await waitFor(() => expect(mockedAxios.delete).toHaveBeenCalledWith(
        `/service/rest/v1/security/ssl/truststore/${encodeURIComponent(MOCK_CERT.id)}`
      ));
      expect(screen.getByText('Add certificate to truststore')).toBeInTheDocument();
    });

    it('disables Add button when user lacks create permission', async () => {
      const { ExtJS } = jest.requireMock('@sonatype/nexus-ui-plugin');
      ExtJS.checkPermission.mockImplementation((perm: string) => perm !== 'nexus:ssl-truststore:create');
      renderDialog();
      await waitFor(() => expect(screen.getByRole('button', { name: /Add certificate to truststore/i })).toBeInTheDocument());
      expect(screen.getByRole('button', { name: /Add certificate to truststore/i })).toBeDisabled();
    });
  });

  describe('close behaviour', () => {
    it('calls onClose when Close button is clicked', async () => {
      mockedAxios.get.mockImplementation((url: string) => {
        if (url.includes('/ssl/truststore')) return Promise.resolve({ data: [] });
        return Promise.resolve({ data: MOCK_CERT });
      });
      const onClose = jest.fn();
      renderDialog({ onClose });
      await waitFor(() => expect(screen.getByText('Close')).toBeInTheDocument());
      fireEvent.click(screen.getByText('Close'));
      expect(onClose).toHaveBeenCalled();
    });
  });
});
