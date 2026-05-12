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
import { FormatBadge } from '../FormatBadge';

const renderWithTheme = (component: React.ReactElement) => {
  return render(<Theme>{component}</Theme>);
};

describe('FormatBadge', () => {
  describe('display names', () => {
    it('displays "maven" for maven2 format', () => {
      renderWithTheme(<FormatBadge format="maven2" />);
      expect(screen.getByText('maven')).toBeInTheDocument();
    });

    it('displays "git-lfs" for gitlfs format', () => {
      renderWithTheme(<FormatBadge format="gitlfs" />);
      expect(screen.getByText('git-lfs')).toBeInTheDocument();
    });

    it('displays "hugging-face" for huggingface format', () => {
      renderWithTheme(<FormatBadge format="huggingface" />);
      expect(screen.getByText('hugging-face')).toBeInTheDocument();
    });

    it('displays format name as-is when no override exists', () => {
      renderWithTheme(<FormatBadge format="npm" />);
      expect(screen.getByText('npm')).toBeInTheDocument();
    });
  });

  describe('case insensitivity', () => {
    it('handles uppercase format names', () => {
      renderWithTheme(<FormatBadge format="MAVEN2" />);
      expect(screen.getByText('maven')).toBeInTheDocument();
    });

    it('handles mixed case format names', () => {
      renderWithTheme(<FormatBadge format="Docker" />);
      expect(screen.getByText('docker')).toBeInTheDocument();
    });
  });

  describe('known formats', () => {
    const knownFormats = [
      'npm',
      'nuget',
      'pypi',
      'rubygems',
      'go',
      'cargo',
      'composer',
      'cocoapods',
      'conan',
      'conda',
      'r',
      'docker',
      'helm',
      'apt',
      'yum',
      'raw',
      'p2',
      'terraform',
      'swift',
    ];

    knownFormats.forEach((format) => {
      it(`renders badge for ${format}`, () => {
        renderWithTheme(<FormatBadge format={format} />);
        expect(screen.getByText(format)).toBeInTheDocument();
      });
    });
  });

  describe('unknown formats', () => {
    it('renders badge for unknown format with gray color', () => {
      renderWithTheme(<FormatBadge format="unknown-format" />);
      expect(screen.getByText('unknown-format')).toBeInTheDocument();
    });
  });
});

