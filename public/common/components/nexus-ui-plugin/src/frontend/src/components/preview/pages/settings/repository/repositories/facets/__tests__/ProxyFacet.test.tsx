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

const mockGetValue = jest.fn().mockReturnValue(false);

jest.mock('@sonatype/nexus-ui-plugin', () => ({
  ExtJS: {
    state: () => ({
      getValue: mockGetValue,
    }),
  },
}));

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
  beforeEach(() => {
    mockGetValue.mockReturnValue(false);
  });

  afterEach(() => {
    jest.clearAllMocks();
  });

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

  it('shows correct placeholder for swift format', () => {
    const formData = {
      ...defaultFormData,
      proxy: { ...defaultFormData.proxy!, remoteUrl: '' },
    };
    renderFacet({ formData, format: 'swift' });

    expect(screen.getByText(/https:\/\/github\.com/)).toBeInTheDocument();
  });

  it('shows correct placeholder for cargo format', () => {
    const formData = {
      ...defaultFormData,
      proxy: { ...defaultFormData.proxy!, remoteUrl: '' },
    };
    renderFacet({ formData, format: 'cargo' });

    expect(screen.getByText(/https:\/\/index\.crates\.io/)).toBeInTheDocument();
  });

  it('shows correct placeholder for terraform format', () => {
    const formData = {
      ...defaultFormData,
      proxy: { ...defaultFormData.proxy!, remoteUrl: '' },
    };
    renderFacet({ formData, format: 'terraform' });

    expect(screen.getByText(/https:\/\/registry\.terraform\.io/)).toBeInTheDocument();
  });

  it('shows correct placeholder for composer format', () => {
    const formData = {
      ...defaultFormData,
      proxy: { ...defaultFormData.proxy!, remoteUrl: '' },
    };
    renderFacet({ formData, format: 'composer' });

    expect(screen.getByText(/https:\/\/repo\.packagist\.org/)).toBeInTheDocument();
  });

  it('shows correct placeholder for conan format', () => {
    const formData = {
      ...defaultFormData,
      proxy: { ...defaultFormData.proxy!, remoteUrl: '' },
    };
    renderFacet({ formData, format: 'conan' });

    expect(screen.getByText(/https:\/\/center\.conan\.io/)).toBeInTheDocument();
  });

  it('shows correct placeholder for pub format', () => {
    const formData = {
      ...defaultFormData,
      proxy: { ...defaultFormData.proxy!, remoteUrl: '' },
    };
    renderFacet({ formData, format: 'pub' });

    expect(screen.getByText(/https:\/\/pub\.dev/)).toBeInTheDocument();
  });

  it('shows correct placeholder for r format', () => {
    const formData = {
      ...defaultFormData,
      proxy: { ...defaultFormData.proxy!, remoteUrl: '' },
    };
    renderFacet({ formData, format: 'r' });

    expect(screen.getByText(/https:\/\/cran\.r-project\.org/)).toBeInTheDocument();
  });

  it('shows correct placeholder for rubygems format', () => {
    const formData = {
      ...defaultFormData,
      proxy: { ...defaultFormData.proxy!, remoteUrl: '' },
    };
    renderFacet({ formData, format: 'rubygems' });

    expect(screen.getByText(/https:\/\/rubygems\.org/)).toBeInTheDocument();
  });

  it('shows correct placeholder for huggingface format', () => {
    const formData = {
      ...defaultFormData,
      proxy: { ...defaultFormData.proxy!, remoteUrl: '' },
    };
    renderFacet({ formData, format: 'huggingface' });

    expect(screen.getByText(/https:\/\/huggingface\.co/)).toBeInTheDocument();
  });

  it('shows correct placeholder for docker format', () => {
    const formData = {
      ...defaultFormData,
      proxy: { ...defaultFormData.proxy!, remoteUrl: '' },
    };
    renderFacet({ formData, format: 'docker' });

    expect(screen.getByText(/https:\/\/registry-1\.docker\.io/)).toBeInTheDocument();
  });

  it('shows correct placeholder for yum format', () => {
    const formData = {
      ...defaultFormData,
      proxy: { ...defaultFormData.proxy!, remoteUrl: '' },
    };
    renderFacet({ formData, format: 'yum' });

    expect(screen.getByText(/https:\/\/mirror\.stream\.centos\.org/)).toBeInTheDocument();
  });

  it('shows correct placeholder for maven2 format', () => {
    const formData = {
      ...defaultFormData,
      proxy: { ...defaultFormData.proxy!, remoteUrl: '' },
    };
    renderFacet({ formData, format: 'maven2' });

    expect(screen.getByText(/https:\/\/repo1\.maven\.org\/maven2/)).toBeInTheDocument();
  });

  it('shows correct placeholder for nuget format', () => {
    const formData = {
      ...defaultFormData,
      proxy: { ...defaultFormData.proxy!, remoteUrl: '' },
    };
    renderFacet({ formData, format: 'nuget' });

    expect(screen.getByText(/https:\/\/api\.nuget\.org/)).toBeInTheDocument();
  });

  it('shows correct placeholder for raw format', () => {
    const formData = {
      ...defaultFormData,
      proxy: { ...defaultFormData.proxy!, remoteUrl: '' },
    };
    renderFacet({ formData, format: 'raw' });

    expect(screen.getByText(/https:\/\/example\.com\/files/)).toBeInTheDocument();
  });

  it('shows correct placeholder for ansiblegalaxy format', () => {
    const formData = {
      ...defaultFormData,
      proxy: { ...defaultFormData.proxy!, remoteUrl: '' },
    };
    renderFacet({ formData, format: 'ansiblegalaxy' });

    expect(screen.getByText(/https:\/\/galaxy\.ansible\.com/)).toBeInTheDocument();
  });

  it('shows default placeholder for unknown format', () => {
    const formData = {
      ...defaultFormData,
      proxy: { ...defaultFormData.proxy!, remoteUrl: '' },
    };
    renderFacet({ formData, format: 'unknown' });

    expect(screen.getByText(/https:\/\/example\.com\/repository/)).toBeInTheDocument();
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

  describe('NuGet remote URL help text', () => {
    const nugetFormData: RepositoryFormData = {
      name: 'nuget-proxy',
      format: 'nuget',
      type: 'proxy',
      proxy: {
        remoteUrl: '',
        contentMaxAge: 1440,
        metadataMaxAge: 1440,
      },
    };

    it('shows standard help text when chocolatey is disabled', () => {
      mockGetValue.mockReturnValue(false);
      renderFacet({ formData: nugetFormData, format: 'nuget' });
      expect(
        screen.getByText(/Location of the remote repository being proxied/i)
      ).toBeInTheDocument();
    });

    it('shows chocolatey help text when chocolatey is enabled', () => {
      mockGetValue.mockImplementation((key: string) =>
        key === 'nugetChocolateyEnabled' ? true : false
      );
      renderFacet({ formData: nugetFormData, format: 'nuget' });
      expect(
        screen.getByText(/Supports NuGet V2, NuGet V3, and Chocolatey/i)
      ).toBeInTheDocument();
    });
  });

  describe('NuGet symbol server fields', () => {
    // The Symbol Server URL and Allow Anonymous Symbol Access fields are rendered inside
    // ProxyFacet (not NugetFacet) because they must appear immediately after Remote Storage
    // per the Classic UI layout. They are double-gated: format must be 'nuget' AND the
    // nexus.nuget.symbol.server.enabled flag must be true. Backend routes (see
    // NugetProxyRecipe.addSymSrvRoute) are gated on the same flag, so hiding the fields
    // when the flag is off prevents operators from configuring an inert URL that the
    // backend won't consult.

    const enableSymbolFlag = () => {
      mockGetValue.mockImplementation((key: string) =>
        key === 'nexus.nuget.symbol.server.enabled' ? true : false
      );
    };

    it('does not render Symbol Server fields for non-nuget formats even with the flag on', () => {
      enableSymbolFlag();
      renderFacet({ format: 'maven2' });
      expect(screen.queryByText('Symbol Server URL')).not.toBeInTheDocument();
      expect(screen.queryByText('Allow Anonymous Symbol Access')).not.toBeInTheDocument();
    });

    it('does not render Symbol Server fields for nuget when the flag is off', () => {
      // Default mockGetValue returns false via the top-level beforeEach.
      const formData: RepositoryFormData = {
        ...defaultFormData,
        format: 'nuget',
        proxy: { remoteUrl: 'https://api.nuget.org/v3/index.json' },
      };
      renderFacet({ formData, format: 'nuget' });
      expect(screen.queryByText('Symbol Server URL')).not.toBeInTheDocument();
      expect(screen.queryByText('Allow Anonymous Symbol Access')).not.toBeInTheDocument();
    });

    it('renders Symbol Server URL and Allow Anonymous Symbol Access when nuget + flag on', () => {
      enableSymbolFlag();
      const formData: RepositoryFormData = {
        ...defaultFormData,
        format: 'nuget',
        proxy: { remoteUrl: 'https://api.nuget.org/v3/index.json' },
        nugetProxy: {
          queryCacheItemMaxAge: 3600,
          nugetVersion: 'V3',
          symbolServerUrl: '',
          allowAnonymousSymbolAccess: true,
        },
      };
      renderFacet({ formData, format: 'nuget' });
      expect(screen.getByText('Symbol Server URL')).toBeInTheDocument();
      expect(screen.getByText('Allow Anonymous Symbol Access')).toBeInTheDocument();
    });

    it('propagates Symbol Server URL edits to onNestedChange', () => {
      enableSymbolFlag();
      const mockOnNestedChange = jest.fn();
      const formData: RepositoryFormData = {
        ...defaultFormData,
        format: 'nuget',
        proxy: { remoteUrl: 'https://api.nuget.org/v3/index.json' },
        nugetProxy: {
          queryCacheItemMaxAge: 3600,
          nugetVersion: 'V3',
          symbolServerUrl: '',
          allowAnonymousSymbolAccess: true,
        },
      };
      renderFacet({ formData, format: 'nuget', onNestedChange: mockOnNestedChange });

      const input = screen.getByRole('textbox', { name: /Symbol Server URL/i });
      fireEvent.change(input, { target: { value: 'https://symbols.nuget.org/download/symbols' } });

      expect(mockOnNestedChange).toHaveBeenCalledWith('nugetProxy', {
        symbolServerUrl: 'https://symbols.nuget.org/download/symbols',
      });
    });
  });
});
