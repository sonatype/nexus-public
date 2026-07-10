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
import { render, screen, fireEvent } from '@testing-library/react';
import { Theme } from '@radix-ui/themes';

import { SupportZipNodeCard } from '../SupportZipNodeCard';
import { NodeInfo } from '../types';

jest.mock('../../../../../../../interface/ExtJS', () => ({
  ExtJS: {
    urlOf: jest.fn((url: string) => `http://localhost:8081/${url}`),
  },
}));

function TestWrapper({ children }: { children: React.ReactNode }) {
  return <Theme>{children}</Theme>;
}

const baseNode: NodeInfo = {
  nodeId: 'node-a',
  hostname: 'host-a',
  status: 'NOT_CREATED',
};

function renderCard(overrides: Partial<NodeInfo> = {}, props: Partial<{
  isBlobStoreConfigured: boolean;
  disabled: boolean;
}> = {}) {
  const onGenerate = jest.fn();
  const utils = render(
    <SupportZipNodeCard
      node={{ ...baseNode, ...overrides }}
      isBlobStoreConfigured={props.isBlobStoreConfigured ?? true}
      onGenerate={onGenerate}
      disabled={props.disabled}
    />,
    { wrapper: TestWrapper }
  );
  return { ...utils, onGenerate };
}

describe('SupportZipNodeCard', () => {
  it('renders the hostname in the header', () => {
    renderCard();
    expect(screen.getByText('host-a')).toBeInTheDocument();
    expect(screen.getByTestId('support-zip-node-card-node-a')).toBeInTheDocument();
  });

  it('falls back to nodeId when no hostname is provided', () => {
    renderCard({ hostname: '' });
    expect(screen.getByText('node-a')).toBeInTheDocument();
  });

  describe('NODE_UNAVAILABLE status', () => {
    it('renders unavailable message and disables the generate button', () => {
      const { onGenerate } = renderCard({ status: 'NODE_UNAVAILABLE' });

      expect(screen.getByText(/Node is unavailable/)).toBeInTheDocument();
      const button = screen.getByTestId('support-zip-node-card-generate-node-a');
      expect(button).toBeDisabled();

      fireEvent.click(button);
      expect(onGenerate).not.toHaveBeenCalled();
    });
  });

  describe('NOT_CREATED status', () => {
    it('renders empty-state message and an enabled Generate button', () => {
      const { onGenerate } = renderCard({ status: 'NOT_CREATED' });

      expect(screen.getByText('No ZIP created yet.')).toBeInTheDocument();
      const button = screen.getByTestId('support-zip-node-card-generate-node-a');
      expect(button).toBeEnabled();
      expect(button.textContent).toContain('Generate new ZIP');

      fireEvent.click(button);
      expect(onGenerate).toHaveBeenCalledWith(expect.objectContaining({ nodeId: 'node-a' }));
    });

    it('disables the Generate button when no blob store is configured', () => {
      renderCard({ status: 'NOT_CREATED' }, { isBlobStoreConfigured: false });

      expect(screen.getByText(/No blob store configured/)).toBeInTheDocument();
      expect(screen.getByTestId('support-zip-node-card-generate-node-a')).toBeDisabled();
    });

    it('disables the Generate button when the parent passes disabled=true', () => {
      renderCard({ status: 'NOT_CREATED' }, { disabled: true });
      expect(screen.getByTestId('support-zip-node-card-generate-node-a')).toBeDisabled();
    });
  });

  describe('CREATING status', () => {
    it('renders the spinner with aria-busy and disables the Generate button', () => {
      renderCard({ status: 'CREATING' });

      expect(screen.getByText('Creating ZIP...')).toBeInTheDocument();
      expect(screen.getByTestId('support-zip-node-card-generate-node-a')).toBeDisabled();
    });
  });

  describe('COMPLETED status', () => {
    it('renders the download link only when COMPLETED, using wonderland/download/{blobRef}', () => {
      renderCard({
        status: 'COMPLETED',
        blobRef: 'support-host-a.zip',
        lastUpdated: 1700000000000,
      });

      const link = screen.getByTestId('support-zip-node-card-download-node-a') as HTMLAnchorElement;
      expect(link).toBeInTheDocument();
      expect(link.getAttribute('href')).toBe(
        'http://localhost:8081/service/rest/wonderland/download/support-host-a.zip'
      );
      expect(link.hasAttribute('download')).toBe(true);
    });

    it('does NOT render the download link in non-COMPLETED states', () => {
      const { rerender } = renderCard({ status: 'NOT_CREATED' });
      expect(screen.queryByTestId('support-zip-node-card-download-node-a')).not.toBeInTheDocument();

      rerender(
        <SupportZipNodeCard
          node={{ ...baseNode, status: 'CREATING' }}
          isBlobStoreConfigured={true}
          onGenerate={jest.fn()}
        />
      );
      expect(screen.queryByTestId('support-zip-node-card-download-node-a')).not.toBeInTheDocument();

      rerender(
        <SupportZipNodeCard
          node={{ ...baseNode, status: 'FAILED' }}
          isBlobStoreConfigured={true}
          onGenerate={jest.fn()}
        />
      );
      expect(screen.queryByTestId('support-zip-node-card-download-node-a')).not.toBeInTheDocument();
    });
  });

  describe('FAILED status', () => {
    it('renders the failure message and a Retry affordance on the action button', () => {
      const { onGenerate } = renderCard({ status: 'FAILED' });

      expect(screen.getByText(/Generation failed/)).toBeInTheDocument();
      const button = screen.getByTestId('support-zip-node-card-generate-node-a');
      expect(button).toBeEnabled();
      expect(button.textContent).toContain('Retry');

      fireEvent.click(button);
      expect(onGenerate).toHaveBeenCalled();
    });
  });
});
