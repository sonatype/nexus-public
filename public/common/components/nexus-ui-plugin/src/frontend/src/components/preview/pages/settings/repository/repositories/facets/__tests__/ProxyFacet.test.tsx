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

import { ProxyFacet } from '../ProxyFacet';
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
};

function renderFacet(props: Partial<React.ComponentProps<typeof ProxyFacet>> = {}) {
  const defaultProps = {
    formData: defaultFormData,
    onChange: jest.fn(),
    onNestedChange: jest.fn(),
    errors: {},
    format: 'maven2',
  };
  return render(
    <Theme>
      <ProxyFacet {...defaultProps} {...props} />
    </Theme>
  );
}

describe('ProxyFacet', () => {
  it('renders Remote Storage field', () => {
    renderFacet();
    const remoteUrlInput = screen.getByDisplayValue('https://repo1.maven.org/maven2/');
    expect(remoteUrlInput).toBeInTheDocument();
    expect(remoteUrlInput).toHaveAttribute('name', 'proxy-remoteUrl');
  });

  it('renders Maximum Component Age field', () => {
    renderFacet();
    expect(screen.getByLabelText(/Maximum Component Age/i)).toBeInTheDocument();
  });

  it('renders Maximum Metadata Age field', () => {
    renderFacet();
    expect(screen.getByLabelText(/Maximum Metadata Age/i)).toBeInTheDocument();
  });

  it('calls onNestedChange when Remote URL is changed', () => {
    const mockOnNestedChange = jest.fn();
    renderFacet({ onNestedChange: mockOnNestedChange });

    const remoteUrlInput = screen.getByDisplayValue('https://repo1.maven.org/maven2/');
    fireEvent.change(remoteUrlInput, { target: { value: 'https://new-url.example.com/' } });

    expect(mockOnNestedChange).toHaveBeenCalledWith('proxy', {
      remoteUrl: 'https://new-url.example.com/',
    });
  });

  it('shows correct placeholder for npm format', () => {
    const formData = {
      ...defaultFormData,
      proxy: { ...defaultFormData.proxy!, remoteUrl: '' },
    };
    renderFacet({ formData, format: 'npm' });

    expect(screen.getByText(/https:\/\/registry\.npmjs\.org/)).toBeInTheDocument();
  });

  it('shows correct placeholder for pypi format', () => {
    const formData = {
      ...defaultFormData,
      proxy: { ...defaultFormData.proxy!, remoteUrl: '' },
    };
    renderFacet({ formData, format: 'pypi' });

    expect(screen.getByText(/https:\/\/pypi\.org/)).toBeInTheDocument();
  });

  it('renders Preserve Encoded Characters checkbox', () => {
    renderFacet();
    expect(screen.getByText('Preserve Encoded Characters')).toBeInTheDocument();
  });

  it('renders Preserve Encoded Characters checkbox unchecked by default', () => {
    renderFacet();
    const checkbox = screen.getByRole('checkbox', { name: /Preserve Encoded Characters/i });
    expect(checkbox).not.toBeChecked();
  });

  it('calls onNestedChange when Preserve Encoded Characters is toggled', () => {
    const mockOnNestedChange = jest.fn();
    renderFacet({ onNestedChange: mockOnNestedChange });

    const checkbox = screen.getByRole('checkbox', { name: /Preserve Encoded Characters/i });
    fireEvent.click(checkbox);

    expect(mockOnNestedChange).toHaveBeenCalledWith('proxy', { preserveEncodedCharacters: true });
  });

  describe('Maximum Component Age (issue 1)', () => {
    it('clamps values below -1 to -1', () => {
      const mockOnNestedChange = jest.fn();
      renderFacet({ onNestedChange: mockOnNestedChange });

      const input = screen.getByLabelText(/Maximum Component Age/i);
      fireEvent.change(input, { target: { value: '-5' } });

      expect(mockOnNestedChange).toHaveBeenCalledWith('proxy', { contentMaxAge: -1 });
    });

    it('accepts -1 as valid value without clamping', () => {
      const mockOnNestedChange = jest.fn();
      renderFacet({ onNestedChange: mockOnNestedChange });

      const input = screen.getByLabelText(/Maximum Component Age/i);
      fireEvent.change(input, { target: { value: '-1' } });

      expect(mockOnNestedChange).toHaveBeenCalledWith('proxy', { contentMaxAge: -1 });
    });

    it('accepts positive values without clamping', () => {
      const mockOnNestedChange = jest.fn();
      renderFacet({ onNestedChange: mockOnNestedChange });

      const input = screen.getByLabelText(/Maximum Component Age/i);
      // Use a value different from the default (1440) to ensure onChange fires
      fireEvent.change(input, { target: { value: '720' } });

      expect(mockOnNestedChange).toHaveBeenCalledWith('proxy', { contentMaxAge: 720 });
    });

    it('does not update when value is empty (allows user to clear field)', () => {
      const mockOnNestedChange = jest.fn();
      renderFacet({ onNestedChange: mockOnNestedChange });

      const input = screen.getByLabelText(/Maximum Component Age/i);
      fireEvent.change(input, { target: { value: '' } });

      expect(mockOnNestedChange).toHaveBeenCalledWith('proxy', { contentMaxAge: undefined });
    });
  });

  describe('Maximum Metadata Age (issue 2)', () => {
    it('allows field to be cleared (empty string)', () => {
      const mockOnNestedChange = jest.fn();
      renderFacet({ onNestedChange: mockOnNestedChange });

      const input = screen.getByLabelText(/Maximum Metadata Age/i);
      fireEvent.change(input, { target: { value: '' } });

      expect(mockOnNestedChange).toHaveBeenCalledWith('proxy', { metadataMaxAge: undefined });
    });

    it('accepts positive integer values', () => {
      const mockOnNestedChange = jest.fn();
      renderFacet({ onNestedChange: mockOnNestedChange });

      const input = screen.getByLabelText(/Maximum Metadata Age/i);
      // Use a value different from the default (1440) to ensure onChange fires
      fireEvent.change(input, { target: { value: '720' } });

      expect(mockOnNestedChange).toHaveBeenCalledWith('proxy', { metadataMaxAge: 720 });
    });

    it('accepts -1 (cache forever) — matches Classic UI semantics', () => {
      // Classic UI's validateTimeToLive allows any value >= -1 for both
      // contentMaxAge and metadataMaxAge. -1 means "cache forever". An earlier
      // tighter rule (>0) silently broke editing existing repos that had been
      // saved with -1.
      const mockOnNestedChange = jest.fn();
      renderFacet({ onNestedChange: mockOnNestedChange });

      const input = screen.getByLabelText(/Maximum Metadata Age/i);
      fireEvent.change(input, { target: { value: '-1' } });

      expect(mockOnNestedChange).toHaveBeenCalledWith('proxy', { metadataMaxAge: -1 });
    });

    it('clamps values below -1 up to -1', () => {
      const mockOnNestedChange = jest.fn();
      renderFacet({ onNestedChange: mockOnNestedChange });

      const input = screen.getByLabelText(/Maximum Metadata Age/i);
      fireEvent.change(input, { target: { value: '-5' } });

      expect(mockOnNestedChange).toHaveBeenCalledWith('proxy', { metadataMaxAge: -1 });
    });
  });
});
