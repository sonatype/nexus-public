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
import { SearchResultCard } from '../SearchResultCard';
import * as formatLogos from '../formatLogos';

/*
 * The shared formatter (NEXUS-54427) renders "Mon D, YYYY, HH:MM GMT±X" in the viewer's own
 * timezone, so the exact wall clock and offset depend on where the suite runs — jest.config.js
 * pins no TZ, and setting process.env.TZ inside a test file does not take effect. These matchers
 * therefore assert the *shape*: a calendar date, a 24-hour time, and a timezone label. Exact
 * per-zone output is pinned in shared/__tests__/dateFormat.test.ts, which passes an explicit zone.
 */
const DATE_TIME_TZ = /^[A-Z][a-z]{2} \d{1,2}, \d{4}, \d{2}:\d{2} GMT([+-]\d{1,2}(:\d{2})?)?$/;
const monthAndYear = (month, year) =>
  new RegExp(`^${month} \\d{1,2}, ${year}, \\d{2}:\\d{2} GMT`);

// Helper to wrap components with Radix Theme
function renderWithTheme(ui) {
  return render(<Theme>{ui}</Theme>);
}

// Sample test data
const mockResult = {
  id: 'test-123',
  name: 'my-component',
  format: 'maven2',
  repository: 'maven-central',
  group: 'com.example',
  version: '1.2.3',
  lastUpdated: '2026-01-14T12:00:00Z',
};

const mockResultNoGroup = {
  id: 'test-456',
  name: 'npm-package',
  format: 'npm',
  repository: 'npm-proxy',
  version: '2.0.0',
};

// Mirrors the search API for a scoped npm package: the scope lives in `group` and the bare package
// in `name` (transformResult maps both verbatim). Keeping `name` bare here is deliberate — a name
// like '@scope/package' would already carry the scope and mask a dropped-scope regression in
// guidePackageName.
const mockResultNpm = {
  id: 'test-789',
  name: 'package',
  format: 'npm',
  repository: 'npm-hosted',
  group: '@scope',
  version: '3.1.0',
  lastUpdated: '2026-01-10T08:30:00Z',
};

const mockResultDocker = {
  id: 'test-docker',
  name: 'nginx',
  format: 'docker',
  repository: 'docker-hub',
  version: 'latest',
  lastUpdated: '2026-01-15T16:45:00Z',
};

describe('SearchResultCard', () => {
  describe('rendering', () => {
    it('renders name and version as distinct, individually legible elements', () => {
      renderWithTheme(<SearchResultCard result={mockResult} onSelect={jest.fn()} />);
      // Name and version are separate nodes, not one concatenated string, so each matches
      // on its own exact text (an exact match on the name alone would fail if the two were
      // still rendered as "my-component 1.2.3").
      expect(screen.getByText('my-component')).toBeInTheDocument();
      expect(screen.getByText('1.2.3')).toBeInTheDocument();
    });

    it('renders group when provided', () => {
      renderWithTheme(<SearchResultCard result={mockResult} onSelect={jest.fn()} />);
      expect(screen.getByText('com.example')).toBeInTheDocument();
    });

    it('does not render group when group is not provided', () => {
      renderWithTheme(<SearchResultCard result={mockResultNoGroup} onSelect={jest.fn()} />);
      expect(screen.queryByText('com.example')).not.toBeInTheDocument();
    });

    it('renders version', () => {
      renderWithTheme(<SearchResultCard result={mockResult} onSelect={jest.fn()} />);
      expect(screen.getByText('1.2.3')).toBeInTheDocument();
    });

    it('does not render an empty version badge for versionless formats', () => {
      // raw/apt/yum/gitlfs items can have no version; the version badge must be omitted rather than
      // rendering an empty gray pill next to the name. The format badge always renders, so a
      // versionless card has exactly one badge (format) versus two when a version is present.
      const versioned = renderWithTheme(<SearchResultCard result={mockResult} onSelect={jest.fn()} />);
      expect(versioned.container.querySelectorAll('.rt-Badge')).toHaveLength(2);

      const versionless = renderWithTheme(
        <SearchResultCard result={{ ...mockResult, version: '' }} onSelect={jest.fn()} />,
      );
      expect(versionless.container.querySelectorAll('.rt-Badge')).toHaveLength(1);
    });

    it('shows the format as a visible labeled badge', () => {
      renderWithTheme(<SearchResultCard result={mockResult} onSelect={jest.fn()} />);
      // "Format:" label sits to the LEFT of the ecosystem badge, and the format is visible text
      // (not only conveyed by the logo image).
      expect(screen.getByText('Format:')).toBeInTheDocument();
      expect(screen.getByText('maven2')).toBeInTheDocument();
    });

    it('renders repository name with a "Repository name:" label', () => {
      renderWithTheme(<SearchResultCard result={mockResult} onSelect={jest.fn()} />);
      expect(screen.getByText('Repository name:')).toBeInTheDocument();
      expect(screen.getByText('maven-central')).toBeInTheDocument();
    });

    it('renders last modified with date, time and timezone', () => {
      renderWithTheme(<SearchResultCard result={mockResult} onSelect={jest.fn()} />);
      expect(screen.getByText(DATE_TIME_TZ)).toBeInTheDocument();
      expect(screen.getByText(monthAndYear('Jan', 2026))).toBeInTheDocument();
    });

    it('renders "—" when lastUpdated is not provided', () => {
      renderWithTheme(<SearchResultCard result={mockResultNoGroup} onSelect={jest.fn()} />);
      expect(screen.getByText('—')).toBeInTheDocument();
    });

    it('renders "—" rather than "Invalid Date" when lastUpdated is unparseable', () => {
      renderWithTheme(
        <SearchResultCard result={{ ...mockResult, lastUpdated: 'not-a-date' }} onSelect={jest.fn()} />,
      );
      expect(screen.getByText('—')).toBeInTheDocument();
      expect(screen.queryByText(/Invalid Date/)).not.toBeInTheDocument();
    });

    it('renders the card without crashing when lastUpdated is malformed', () => {
      expect(() =>
        renderWithTheme(
          <SearchResultCard
            result={{ ...mockResult, lastUpdated: '2026-13-45T99:99:99Z' }}
            onSelect={jest.fn()}
          />,
        ),
      ).not.toThrow();
      expect(screen.getByText('my-component')).toBeInTheDocument();
    });
  });

  describe('ecosystem badge', () => {
    it('renders the decorative brand logo when the format has a logo', () => {
      const { container } = renderWithTheme(<SearchResultCard result={mockResult} onSelect={jest.fn()} />);
      const logoImg = container.querySelector('img[alt=""]');
      expect(logoImg).toBeInTheDocument();
      expect(logoImg).toHaveAttribute('src');
    });

    it('shows the npm format as visible badge text', () => {
      renderWithTheme(<SearchResultCard result={mockResultNpm} onSelect={jest.fn()} />);
      expect(screen.getByText('npm')).toBeInTheDocument();
    });

    it('shows the maven2 format as visible badge text', () => {
      renderWithTheme(<SearchResultCard result={mockResult} onSelect={jest.fn()} />);
      expect(screen.getByText('maven2')).toBeInTheDocument();
    });

    it('shows the docker format as visible badge text', () => {
      renderWithTheme(<SearchResultCard result={mockResultDocker} onSelect={jest.fn()} />);
      expect(screen.getByText('docker')).toBeInTheDocument();
    });
  });

  describe('interactions', () => {
    it('calls onSelect when card is clicked', () => {
      const onSelect = jest.fn();
      renderWithTheme(<SearchResultCard result={mockResult} onSelect={onSelect} />);
      fireEvent.click(screen.getByRole('button', { name: /View details for/ }));
      expect(onSelect).toHaveBeenCalledTimes(1);
      expect(onSelect).toHaveBeenCalledWith(mockResult);
    });

    it('exposes the details action as a native button so the platform handles Enter/Space', () => {
      // A native <button> is activated by Enter and Space by the browser itself, with no custom
      // keydown handler. Encoding the contract as "this is a real button" is more honest than
      // firing a synthetic keydown, whose default click action jsdom does not perform.
      const onSelect = jest.fn();
      renderWithTheme(<SearchResultCard result={mockResult} onSelect={onSelect} />);
      const control = screen.getByRole('button', { name: /View details for/ });
      expect(control.tagName).toBe('BUTTON');
      expect(control).toHaveAttribute('type', 'button');
      fireEvent.click(control);
      expect(onSelect).toHaveBeenCalledTimes(1);
      expect(onSelect).toHaveBeenCalledWith(mockResult);
    });

    it('does not call onSelect for other keys', () => {
      const onSelect = jest.fn();
      renderWithTheme(<SearchResultCard result={mockResult} onSelect={onSelect} />);
      const control = screen.getByRole('button', { name: /View details for/ });
      control.focus();
      fireEvent.keyDown(control, { key: 'Tab' });
      expect(onSelect).not.toHaveBeenCalled();
    });
  });

  describe('accessibility', () => {
    it('has correct aria-label', () => {
      renderWithTheme(<SearchResultCard result={mockResult} onSelect={jest.fn()} />);
      const card = screen.getByRole('button', { name: /View details for/ });
      expect(card).toHaveAttribute('aria-label', 'View details for my-component');
    });

    it('has button role', () => {
      renderWithTheme(<SearchResultCard result={mockResult} onSelect={jest.fn()} />);
      expect(screen.getByRole('button', { name: /View details for/ })).toBeInTheDocument();
    });

    it('is focusable as a native control, without a manual tabIndex', () => {
      renderWithTheme(<SearchResultCard result={mockResult} onSelect={jest.fn()} />);
      const control = screen.getByRole('button', { name: /View details for/ });
      control.focus();
      expect(control).toHaveFocus();
      // A native <button> is in the tab order by default; no author-supplied tabIndex is needed.
      expect(control).not.toHaveAttribute('tabIndex');
    });

    it('makes the title itself the accessible control, not a wrapper hiding the card text', () => {
      // role="button" on the whole Card marks its entire subtree presentational, so a screen
      // reader announces only the card's label and never the Group / Repository / Format / date.
      // The details control must therefore be a discrete element that does NOT contain that
      // descriptive text.
      renderWithTheme(<SearchResultCard result={mockResult} onSelect={jest.fn()} />);
      const control = screen.getByRole('button', { name: /View details for my-component/ });
      expect(control).toHaveTextContent('my-component');
      expect(control).not.toHaveTextContent('Format:');
      expect(control).not.toHaveTextContent('Repository name:');
    });

    it('does not nest the Guide link inside the details control (no nested-interactive)', () => {
      // An interactive <a> nested inside an interactive control violates WCAG 4.1.2; the Guide
      // link must live outside the details control.
      renderWithTheme(<SearchResultCard result={mockResult} onSelect={jest.fn()} />);
      const control = screen.getByRole('button', { name: /View details for my-component/ });
      const guideLink = screen
        .getAllByRole('link')
        .find((l) => l.getAttribute('href')?.includes('guide.sonatype.com'));
      expect(guideLink).toBeTruthy();
      expect(control).not.toContainElement(guideLink);
    });

    it('gives a long component name a break opportunity so it cannot overflow its column', () => {
      const longName = 'org.apache.some.really.long.artifact.identifier.that.will.not.fit';
      renderWithTheme(<SearchResultCard result={{ ...mockResult, name: longName }} onSelect={jest.fn()} />);
      expect(screen.getByText(longName)).toHaveStyle({ wordBreak: 'break-word' });
    });
  });

  describe('layout', () => {
    it('renders card with name, version, group, format badge, repository and last updated', () => {
      renderWithTheme(<SearchResultCard result={mockResult} onSelect={jest.fn()} />);
      expect(screen.getByText('my-component')).toBeInTheDocument();
      expect(screen.getByText('1.2.3')).toBeInTheDocument();
      expect(screen.getByText('com.example')).toBeInTheDocument();
      expect(screen.getByText('Format:')).toBeInTheDocument();
      expect(screen.getByText('maven2')).toBeInTheDocument();
      expect(screen.getByText('maven-central')).toBeInTheDocument();
      expect(screen.getByText('Last updated')).toBeInTheDocument();
      expect(screen.getByText(DATE_TIME_TZ)).toBeInTheDocument();
    });
  });

  describe('date formatting', () => {
    it('formats date correctly for different months', () => {
      const resultWithMarchDate = {
        ...mockResult,
        lastUpdated: '2026-03-15T10:00:00Z',
      };
      renderWithTheme(<SearchResultCard result={resultWithMarchDate} onSelect={jest.fn()} />);
      expect(screen.getByText(monthAndYear('Mar', 2026))).toBeInTheDocument();
    });

    it('formats date correctly for December', () => {
      const resultWithDecemberDate = {
        ...mockResult,
        lastUpdated: '2025-12-25T10:00:00Z',
      };
      renderWithTheme(<SearchResultCard result={resultWithDecemberDate} onSelect={jest.fn()} />);
      expect(screen.getByText(monthAndYear('Dec', 2025))).toBeInTheDocument();
    });
  });

  describe('memoization', () => {
    it('re-renders with the updated lastUpdated value when only lastUpdated changes', () => {
      const onSelect = jest.fn();
      const { rerender } = renderWithTheme(<SearchResultCard result={mockResult} onSelect={onSelect} />);
      expect(screen.getByText(monthAndYear('Jan', 2026))).toBeInTheDocument();

      const updatedResult = {
        ...mockResult,
        lastUpdated: '2026-06-01T09:15:00Z',
      };
      rerender(
        <Theme>
          <SearchResultCard result={updatedResult} onSelect={onSelect} />
        </Theme>
      );

      expect(screen.getByText(monthAndYear('Jun', 2026))).toBeInTheDocument();
      expect(screen.queryByText(monthAndYear('Jan', 2026))).not.toBeInTheDocument();
    });

    it('re-renders with the updated name when only name changes', () => {
      const onSelect = jest.fn();
      const { rerender } = renderWithTheme(<SearchResultCard result={mockResult} onSelect={onSelect} />);
      expect(screen.getByText('my-component')).toBeInTheDocument();

      const updatedResult = { ...mockResult, name: 'renamed-component' };
      rerender(
        <Theme>
          <SearchResultCard result={updatedResult} onSelect={onSelect} />
        </Theme>
      );

      expect(screen.getByText('renamed-component')).toBeInTheDocument();
      expect(screen.queryByText('my-component')).not.toBeInTheDocument();
    });

    it('re-renders with the updated format when only format changes', () => {
      const onSelect = jest.fn();
      const { rerender } = renderWithTheme(<SearchResultCard result={mockResult} onSelect={onSelect} />);
      expect(screen.getByText('maven2')).toBeInTheDocument();

      const updatedResult = { ...mockResult, format: 'npm' };
      rerender(
        <Theme>
          <SearchResultCard result={updatedResult} onSelect={onSelect} />
        </Theme>
      );

      expect(screen.getByText('npm')).toBeInTheDocument();
      expect(screen.queryByText('maven2')).not.toBeInTheDocument();
    });

    it('re-renders with the updated group when only group changes', () => {
      const onSelect = jest.fn();
      const { rerender } = renderWithTheme(<SearchResultCard result={mockResult} onSelect={onSelect} />);
      expect(screen.getByText('com.example')).toBeInTheDocument();

      const updatedResult = { ...mockResult, group: 'com.other' };
      rerender(
        <Theme>
          <SearchResultCard result={updatedResult} onSelect={onSelect} />
        </Theme>
      );

      expect(screen.getByText('com.other')).toBeInTheDocument();
      expect(screen.queryByText('com.example')).not.toBeInTheDocument();
    });

    it('re-renders with the updated repository when only repository changes', () => {
      const onSelect = jest.fn();
      const { rerender } = renderWithTheme(<SearchResultCard result={mockResult} onSelect={onSelect} />);
      expect(screen.getByText('maven-central')).toBeInTheDocument();

      const updatedResult = { ...mockResult, repository: 'maven-releases' };
      rerender(
        <Theme>
          <SearchResultCard result={updatedResult} onSelect={onSelect} />
        </Theme>
      );

      expect(screen.getByText('maven-releases')).toBeInTheDocument();
      expect(screen.queryByText('maven-central')).not.toBeInTheDocument();
    });

    it('re-renders with the updated version when only version changes', () => {
      const onSelect = jest.fn();
      const { rerender } = renderWithTheme(<SearchResultCard result={mockResult} onSelect={onSelect} />);
      expect(screen.getByText('1.2.3')).toBeInTheDocument();

      const updatedResult = { ...mockResult, version: '4.5.6' };
      rerender(
        <Theme>
          <SearchResultCard result={updatedResult} onSelect={onSelect} />
        </Theme>
      );

      expect(screen.getByText('4.5.6')).toBeInTheDocument();
      expect(screen.queryByText('1.2.3')).not.toBeInTheDocument();
    });

    it('skips the re-render when neither the result nor onSelect changes', () => {
      // getFormatLogo is called once per render of the card body, with the real
      // implementation left in place, so the spy's call count is the render count.
      const renderSpy = jest.spyOn(formatLogos, 'getFormatLogo');
      try {
        const onSelect = jest.fn();
        const { rerender } = renderWithTheme(
          <SearchResultCard result={mockResult} onSelect={onSelect} />
        );
        expect(renderSpy).toHaveBeenCalledTimes(1);

        rerender(
          <Theme>
            <SearchResultCard result={mockResult} onSelect={onSelect} />
          </Theme>
        );

        expect(renderSpy).toHaveBeenCalledTimes(1);
      } finally {
        renderSpy.mockRestore();
      }
    });

    it('passes the current result to onSelect after the result changes', () => {
      const onSelect = jest.fn();
      const { rerender } = renderWithTheme(
        <SearchResultCard result={mockResult} onSelect={onSelect} />
      );

      const updatedResult = { ...mockResult, version: '9.9.9' };
      rerender(
        <Theme>
          <SearchResultCard result={updatedResult} onSelect={onSelect} />
        </Theme>
      );
      fireEvent.click(screen.getByRole('button', { name: /View details for/ }));

      expect(onSelect).toHaveBeenCalledWith(updatedResult);
    });

    it('invokes the latest onSelect after the callback identity changes, not a stale one', () => {
      const firstOnSelect = jest.fn();
      const secondOnSelect = jest.fn();
      const { rerender } = renderWithTheme(<SearchResultCard result={mockResult} onSelect={firstOnSelect} />);

      rerender(
        <Theme>
          <SearchResultCard result={mockResult} onSelect={secondOnSelect} />
        </Theme>
      );

      fireEvent.click(screen.getByRole('button', { name: /View details for/ }));

      expect(secondOnSelect).toHaveBeenCalledTimes(1);
      expect(firstOnSelect).not.toHaveBeenCalled();
    });
  });

  describe('Guide deep research integration (y87u)', () => {
    it('shows deep research link when component has version', () => {
      renderWithTheme(<SearchResultCard result={mockResult} onSelect={jest.fn()} />);
      expect(screen.getByTestId('deep-research-link')).toBeInTheDocument();
    });

    it('renders the guide link as a text link, not icon-only', () => {
      renderWithTheme(<SearchResultCard result={mockResult} onSelect={jest.fn()} />);
      expect(screen.getByText('Research in Guide')).toBeInTheDocument();
    });

    it('opens the guide link in a new tab with rel="noopener noreferrer"', () => {
      renderWithTheme(<SearchResultCard result={mockResult} onSelect={jest.fn()} />);
      const link = screen
        .getAllByRole('link')
        .find((l) => l.getAttribute('href')?.includes('guide.sonatype.com'));
      expect(link).toBeTruthy();
      expect(link).toHaveAttribute('target', '_blank');
      expect(link).toHaveAttribute('rel', 'noopener noreferrer');
    });

    it('builds a maven guide link from the group:name coordinate', () => {
      // maven2 packages are group:artifact coordinates; the colon encodes to %3A in the path.
      renderWithTheme(<SearchResultCard result={mockResult} onSelect={jest.fn()} />);
      const link = screen
        .getAllByRole('link')
        .find((l) => l.getAttribute('href')?.includes('guide.sonatype.com'));
      expect(link).toHaveAttribute(
        'href',
        expect.stringContaining('/component/maven/com.example%3Amy-component/1.2.3'),
      );
    });

    it('builds an npm guide link that preserves a scoped package name', () => {
      // Scoped npm arrives split: group '@scope' + bare name 'package'. guidePackageName must
      // rejoin them as '@scope/package', and the slash must be encoded (%2F) — not dropped, which
      // would link to the wrong Guide page (/component/npm/package/...). Regression for NEXUS-54426.
      renderWithTheme(<SearchResultCard result={mockResultNpm} onSelect={jest.fn()} />);
      const link = screen
        .getAllByRole('link')
        .find((l) => l.getAttribute('href')?.includes('guide.sonatype.com'));
      expect(link).toHaveAttribute(
        'href',
        expect.stringContaining('/component/npm/%40scope%2Fpackage/3.1.0'),
      );
    });

    it('omits the guide link for an ecosystem Guide does not support', () => {
      // docker is not a Guide-supported ecosystem, so the isGuideSupported gate must drop the link
      // entirely (no empty wrapper, no broken href). Without this test, removing the gate would
      // keep the suite green.
      renderWithTheme(<SearchResultCard result={mockResultDocker} onSelect={jest.fn()} />);
      expect(screen.queryByText('Research in Guide')).not.toBeInTheDocument();
      expect(screen.queryByTestId('deep-research-link')).not.toBeInTheDocument();
    });

    it('deep research link does not trigger card onSelect', () => {
      // The link sits inside a wrapper that stops click propagation, so clicking it must not
      // bubble up to the card's onSelect. Guard first that the link exists (otherwise a missing
      // link would make the not-called assertion pass vacuously), then click it and assert the
      // card handler stayed silent.
      const onCardClick = jest.fn();
      renderWithTheme(<SearchResultCard result={mockResult} onSelect={onCardClick} />);
      const guideLinks = screen.getAllByRole('link').filter(l => l.getAttribute('href')?.includes('guide.sonatype.com'));
      expect(guideLinks.length).toBeGreaterThan(0);
      fireEvent.click(guideLinks[0]);
      expect(onCardClick).not.toHaveBeenCalled();
    });

    it('pressing Enter on the guide link does not trigger card onSelect', () => {
      // stopPropagation on the wrapper's onClick does not stop keydown, and a card-level keydown
      // handler would preventDefault() Enter and navigate to the detail page instead of following
      // the link. Guard the link exists first so the not-called assertion cannot pass vacuously.
      const onCardClick = jest.fn();
      renderWithTheme(<SearchResultCard result={mockResult} onSelect={onCardClick} />);
      const guideLink = screen
        .getAllByRole('link')
        .find((l) => l.getAttribute('href')?.includes('guide.sonatype.com'));
      expect(guideLink).toBeTruthy();
      fireEvent.keyDown(guideLink, { key: 'Enter' });
      expect(onCardClick).not.toHaveBeenCalled();
    });
  });
});
