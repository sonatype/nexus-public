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
import '@testing-library/jest-dom';
import { Theme } from '@radix-ui/themes';

// Import the actual helpers from the shared constants module
import {
  HIDDEN_FACETS,
  EXCLUDED_ATTRIBUTE_KEYS,
  formatAttributeValue,
  shouldDisplayAttributeFacet,
} from '../../browse.constants';

// Test wrapper with Radix Theme
const renderWithTheme = (ui: React.ReactElement) => {
  return render(<Theme>{ui}</Theme>);
};

/**
 * Tests for shared attribute rendering helpers.
 * These tests import the actual implementation from browse.constants.ts
 * to ensure we're testing the real code, not a copy.
 */
describe('Attribute Rendering Helpers', () => {
  describe('formatAttributeValue helper', () => {
    it('should join array values with comma and space', () => {
      expect(formatAttributeValue(['a', 'b', 'c'])).toBe('a, b, c');
    });

    it('should return empty string for empty array', () => {
      expect(formatAttributeValue([])).toBe('');
    });

    it('should convert primitives to strings', () => {
      expect(formatAttributeValue('test')).toBe('test');
      expect(formatAttributeValue(123)).toBe('123');
      expect(formatAttributeValue(true)).toBe('true');
    });

    it('should JSON stringify objects in arrays', () => {
      expect(formatAttributeValue([{ key: 'value' }])).toBe('{"key":"value"}');
    });

    it('should handle null and undefined', () => {
      expect(formatAttributeValue(null)).toBe('');
      expect(formatAttributeValue(undefined)).toBe('');
    });

    it('should format totalSize with formatFileSizeFn when provided', () => {
      const mockFormatFileSize = (bytes: number) => `${bytes} bytes`;
      expect(formatAttributeValue(1024, 'totalSize', mockFormatFileSize)).toBe('1024 bytes');
    });

    it('should return raw number for totalSize without formatFileSizeFn', () => {
      expect(formatAttributeValue(1024, 'totalSize')).toBe('1024');
    });

    it('should JSON stringify objects', () => {
      expect(formatAttributeValue({ key: 'value' })).toBe('{"key":"value"}');
    });
  });

  describe('HIDDEN_FACETS constant', () => {
    it('should include npm_rev in hidden facets', () => {
      expect(HIDDEN_FACETS).toContain('npm_rev');
    });

    it('should include upstream_sonatype_filtered_versions in hidden facets', () => {
      expect(HIDDEN_FACETS).toContain('upstream_sonatype_filtered_versions');
    });

    it('should match Classic UI hidden facets', () => {
      // This test documents the feature parity requirement
      expect(HIDDEN_FACETS).toHaveLength(2);
    });
  });

  describe('EXCLUDED_ATTRIBUTE_KEYS constant', () => {
    it('should include checksum in excluded keys', () => {
      expect(EXCLUDED_ATTRIBUTE_KEYS).toContain('checksum');
    });

    it('should include content in excluded keys', () => {
      expect(EXCLUDED_ATTRIBUTE_KEYS).toContain('content');
    });
  });

  describe('shouldDisplayAttributeFacet helper', () => {
    it('should hide npm_rev facet', () => {
      expect(shouldDisplayAttributeFacet('npm_rev', { revision: '1.0' })).toBe(false);
    });

    it('should hide upstream_sonatype_filtered_versions facet', () => {
      expect(shouldDisplayAttributeFacet('upstream_sonatype_filtered_versions', { versions: [] })).toBe(false);
    });

    it('should hide checksum facet', () => {
      expect(shouldDisplayAttributeFacet('checksum', { sha1: 'abc' })).toBe(false);
    });

    it('should hide content facet', () => {
      expect(shouldDisplayAttributeFacet('content', { lastModified: '2026-01-01' })).toBe(false);
    });

    it('should display firewall facet', () => {
      expect(shouldDisplayAttributeFacet('firewall', { policyViolations: 5 })).toBe(true);
    });

    it('should display maven2 facet', () => {
      expect(shouldDisplayAttributeFacet('maven2', { groupId: 'com.test' })).toBe(true);
    });

    it('should display docker facet', () => {
      expect(shouldDisplayAttributeFacet('docker', { os: 'linux' })).toBe(true);
    });

    it('should not display primitive values as facets', () => {
      expect(shouldDisplayAttributeFacet('simpleString', 'value')).toBe(false);
      expect(shouldDisplayAttributeFacet('simpleNumber', 123)).toBe(false);
    });

    it('should not display null values', () => {
      expect(shouldDisplayAttributeFacet('nullFacet', null)).toBe(false);
    });
  });
});

/**
 * Integration tests for collapsible section accessibility.
 * Tests the real AttributeSection component from AssetDetailPage.tsx.
 */
describe('Collapsible Section Accessibility', () => {
  // Import the real AttributeSection component
  const { AttributeSection } = require('../AssetDetailPage');

  it('should render title', () => {
    renderWithTheme(<AttributeSection title="firewall" attributes={{ policyViolations: 5 }} />);
    expect(screen.getByText('firewall')).toBeInTheDocument();
  });

  it('should be collapsed by default when defaultOpen is false', () => {
    renderWithTheme(<AttributeSection title="firewall" attributes={{ policyViolations: 5 }} defaultOpen={false} />);
    // The attribute value should not be visible when collapsed
    expect(screen.queryByText('5')).not.toBeInTheDocument();
  });

  it('should be expanded by default when defaultOpen is true', () => {
    renderWithTheme(<AttributeSection title="firewall" attributes={{ policyViolations: 5 }} defaultOpen={true} />);
    // The attribute value should be visible when expanded
    expect(screen.getByText('5')).toBeInTheDocument();
  });

  it('should toggle when header is clicked', () => {
    renderWithTheme(<AttributeSection title="firewall" attributes={{ policyViolations: 5 }} />);

    const header = screen.getByRole('button', { name: 'firewall' });
    expect(screen.queryByText('5')).not.toBeInTheDocument(); // collapsed by default

    fireEvent.click(header);
    expect(screen.getByText('5')).toBeInTheDocument(); // expanded

    fireEvent.click(header);
    expect(screen.queryByText('5')).not.toBeInTheDocument(); // collapsed again
  });

  it('should toggle with Enter key', () => {
    renderWithTheme(<AttributeSection title="firewall" attributes={{ policyViolations: 5 }} />);

    const header = screen.getByRole('button', { name: 'firewall' });
    expect(screen.queryByText('5')).not.toBeInTheDocument();

    fireEvent.keyDown(header, { key: 'Enter' });
    expect(screen.getByText('5')).toBeInTheDocument();
  });

  it('should toggle with Space key', () => {
    renderWithTheme(<AttributeSection title="firewall" attributes={{ policyViolations: 5 }} />);

    const header = screen.getByRole('button', { name: 'firewall' });
    expect(screen.queryByText('5')).not.toBeInTheDocument();

    fireEvent.keyDown(header, { key: ' ' });
    expect(screen.getByText('5')).toBeInTheDocument();
  });

  it('should have aria-expanded attribute', () => {
    renderWithTheme(<AttributeSection title="firewall" attributes={{ policyViolations: 5 }} />);

    const header = screen.getByRole('button', { name: 'firewall' });
    expect(header).toHaveAttribute('aria-expanded', 'false');

    fireEvent.click(header);
    expect(header).toHaveAttribute('aria-expanded', 'true');
  });

  it('should have role="button"', () => {
    renderWithTheme(<AttributeSection title="firewall" attributes={{ policyViolations: 5 }} />);
    expect(screen.getByRole('button', { name: 'firewall' })).toBeInTheDocument();
  });

  it('should be focusable via tabIndex', () => {
    renderWithTheme(<AttributeSection title="firewall" attributes={{ policyViolations: 5 }} />);
    const header = screen.getByRole('button', { name: 'firewall' });
    expect(header).toHaveAttribute('tabIndex', '0');
  });

  it('should return null when attributes are empty', () => {
    const { queryByRole } = renderWithTheme(<AttributeSection title="empty" attributes={{}} />);
    // When attributes is empty, AttributeSection returns null, so no button should be found
    expect(queryByRole('button')).not.toBeInTheDocument();
  });
});
