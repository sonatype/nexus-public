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
import {render} from '@testing-library/react';
import {Theme} from '@radix-ui/themes';

import {FormatIcon} from '../FormatIcon';

const TestWrapper = ({children}: {children: React.ReactNode}) => <Theme>{children}</Theme>;

describe('FormatIcon', () => {
  describe('SVG rendering', () => {
    it('should render SVG component when format has an SVG entry and useBrandLogo is true', () => {
      render(
        <TestWrapper>
          <FormatIcon format="maven2" useBrandLogo={true} />
        </TestWrapper>
      );

      // SVG icons from @icons-pack/react-simple-icons render as <svg> elements
      const svg = document.querySelector('svg.format-icon-tile__logo');
      expect(svg).toBeInTheDocument();
    });

    it('should render SVG for npm format', () => {
      render(
        <TestWrapper>
          <FormatIcon format="npm" useBrandLogo={true} />
        </TestWrapper>
      );

      const svg = document.querySelector('svg.format-icon-tile__logo');
      expect(svg).toBeInTheDocument();
    });

    it('should render SVG for docker format', () => {
      render(
        <TestWrapper>
          <FormatIcon format="docker" useBrandLogo={true} />
        </TestWrapper>
      );

      const svg = document.querySelector('svg.format-icon-tile__logo');
      expect(svg).toBeInTheDocument();
    });
  });

  describe('Image fallback', () => {
    it('should render img when format has only an image entry (ansiblegalaxy)', () => {
      // Note: This test relies on Jest's file transform returning a truthy string mock for PNG imports.
      // If the Jest config changes to return undefined for PNGs, this test would fail to cover the image path.
      render(
        <TestWrapper>
          <FormatIcon format="ansiblegalaxy" useBrandLogo={true} />
        </TestWrapper>
      );

      // Image fallback is decorative (aria-hidden) - use DOM query
      const img = document.querySelector('img.format-icon-tile__logo');
      expect(img).toBeInTheDocument();
      expect(img).toHaveAttribute('alt', '');
      expect(img).toHaveAttribute('aria-hidden', 'true');
    });
  });

  describe('Decorative icon accessibility', () => {
    it('should mark brand SVG as decorative with aria-hidden', () => {
      render(
        <TestWrapper>
          <FormatIcon format="maven2" useBrandLogo={true} />
        </TestWrapper>
      );

      const svg = document.querySelector('svg.format-icon-tile__logo');
      expect(svg).toHaveAttribute('aria-hidden', 'true');
    });

    it('should mark image fallback as decorative with aria-hidden', () => {
      render(
        <TestWrapper>
          <FormatIcon format="ansiblegalaxy" useBrandLogo={true} />
        </TestWrapper>
      );

      const img = document.querySelector('img.format-icon-tile__logo');
      expect(img).toHaveAttribute('aria-hidden', 'true');
    });

    it('should mark lucide fallback as decorative with aria-hidden', () => {
      render(
        <TestWrapper>
          <FormatIcon format="raw" useBrandLogo={true} />
        </TestWrapper>
      );

      // raw has no brand SVG/image, so it renders the lucide fallback.
      // All three render paths (brand SVG, image, lucide) must be consistently decorative
      // because format labels are always shown as visible text in usage sites.
      const svg = document.querySelector('svg.format-icon-tile__fallback');
      expect(svg).toHaveAttribute('aria-hidden', 'true');
    });
  });

  describe('Lucide fallback', () => {
    it('should render lucide fallback when useBrandLogo is false', () => {
      render(
        <TestWrapper>
          <FormatIcon format="maven2" useBrandLogo={false} />
        </TestWrapper>
      );

      // Lucide icons render as SVGs without the format-icon-tile__logo class
      const svg = document.querySelector('svg.format-icon-tile__fallback');
      expect(svg).toBeInTheDocument();
      // Should NOT have the brand logo class
      expect(document.querySelector('svg.format-icon-tile__logo')).not.toBeInTheDocument();
    });

    it('should render Folder icon for raw format (no brand identity)', () => {
      render(
        <TestWrapper>
          <FormatIcon format="raw" useBrandLogo={true} />
        </TestWrapper>
      );

      // raw has no SVG entry, falls through to lucide fallback
      const svg = document.querySelector('svg.format-icon-tile__fallback');
      expect(svg).toBeInTheDocument();
    });

    it('should render lucide fallback for ansiblegalaxy when useBrandLogo is false', () => {
      // ansiblegalaxy is in FORMAT_IMAGES but not FORMAT_SVGS. When useBrandLogo=false,
      // FORMAT_IMAGES is skipped and execution falls through to the FORMAT_ICONS lucide
      // fallback (Boxes). This path is production-reachable but was previously untested.
      render(
        <TestWrapper>
          <FormatIcon format="ansiblegalaxy" useBrandLogo={false} />
        </TestWrapper>
      );

      const fallback = document.querySelector('svg.format-icon-tile__fallback');
      expect(fallback).toBeInTheDocument();
      // Should NOT render the image or brand SVG paths
      expect(document.querySelector('img.format-icon-tile__logo')).not.toBeInTheDocument();
      expect(document.querySelector('svg.format-icon-tile__logo')).not.toBeInTheDocument();
    });
  });

  describe('Unknown format', () => {
    it('should render default Package icon for unknown format', () => {
      render(
        <TestWrapper>
          <FormatIcon format="unknown-format-xyz" useBrandLogo={true} />
        </TestWrapper>
      );

      // Unknown format falls through to DEFAULT_FORMAT_ICON (Package)
      const svg = document.querySelector('svg.format-icon-tile__fallback');
      expect(svg).toBeInTheDocument();
    });
  });

  describe('Type badge', () => {
    it('should render type badge when type is provided', () => {
      const {container} = render(
        <TestWrapper>
          <FormatIcon format="maven2" type="hosted" useBrandLogo={true} />
        </TestWrapper>
      );

      const typeBadge = container.querySelector('.format-icon-tile__type-badge--hosted');
      expect(typeBadge).toBeInTheDocument();
    });

    it('should render proxy type badge', () => {
      const {container} = render(
        <TestWrapper>
          <FormatIcon format="npm" type="proxy" useBrandLogo={true} />
        </TestWrapper>
      );

      const typeBadge = container.querySelector('.format-icon-tile__type-badge--proxy');
      expect(typeBadge).toBeInTheDocument();
    });

    it('should render group type badge', () => {
      const {container} = render(
        <TestWrapper>
          <FormatIcon format="docker" type="group" useBrandLogo={true} />
        </TestWrapper>
      );

      const typeBadge = container.querySelector('.format-icon-tile__type-badge--group');
      expect(typeBadge).toBeInTheDocument();
    });

    it('should not render type badge when type is undefined', () => {
      const {container} = render(
        <TestWrapper>
          <FormatIcon format="maven2" useBrandLogo={true} />
        </TestWrapper>
      );

      const typeBadge = container.querySelector('[class*="format-icon-tile__type-badge"]');
      expect(typeBadge).not.toBeInTheDocument();
    });
  });

  describe('Sizing', () => {
    it('should apply default size of 32', () => {
      const {container} = render(
        <TestWrapper>
          <FormatIcon format="maven2" />
        </TestWrapper>
      );

      const tile = container.querySelector('.format-icon-tile');
      expect(tile).toHaveStyle({width: '32px', height: '32px'});
    });

    it('should apply custom size', () => {
      const {container} = render(
        <TestWrapper>
          <FormatIcon format="maven2" size={48} />
        </TestWrapper>
      );

      const tile = container.querySelector('.format-icon-tile');
      expect(tile).toHaveStyle({width: '48px', height: '48px'});
    });
  });

  describe('ClassName', () => {
    it('should apply custom className', () => {
      const {container} = render(
        <TestWrapper>
          <FormatIcon format="maven2" className="custom-class" />
        </TestWrapper>
      );

      const tile = container.querySelector('.format-icon-tile.custom-class');
      expect(tile).toBeInTheDocument();
    });
  });
});
