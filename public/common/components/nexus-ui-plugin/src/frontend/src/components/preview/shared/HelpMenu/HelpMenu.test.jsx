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
import {render, screen, fireEvent} from '@testing-library/react';
import '@testing-library/jest-dom';

const EDITION_ID_PRO = 'nexus-professional-edition';
const EDITION_ID_PRO_PLUS = 'nexus-professional-plus-edition';
const EDITION_ID_ENTERPRISE = 'nexus-enterprise-edition';

let mockIsCloud = false;
let mockVersion = '3.90.0-01';
let mockEditionId = EDITION_ID_PRO;
let mockStateThrows = false;
const mockAboutModal = jest.fn();

jest.mock('../../../../interface/ExtJS', () => ({
  ExtJS: {
    state: () => {
      if (mockStateThrows) throw new Error('state unavailable during logout');
      return {
        getValue: (key) => {
          if (key === 'status') return {version: mockVersion, edition: 'PROPLUS'};
          if (key === 'editionId') return mockEditionId;
          return undefined;
        },
      };
    },
    useState: (init) => (typeof init === 'function' ? init() : init),
  },
}));

jest.mock('../hooks', () => ({
  useIsCloud: () => mockIsCloud,
}));

jest.mock('../AboutModal', () => ({
  AboutModal: (props) => {
    mockAboutModal(props);
    return props.open ? <div data-testid="about-modal-stub" /> : null;
  },
}));

jest.mock('../Tooltip', () => ({
  Tooltip: ({children}) => <>{children}</>,
  usePortalContainer: () => null,
}));

/*
 * jsdom does not drive Radix DropdownMenu's open/close reliably (pointer-capture),
 * so the menu content never mounts on a trigger click. Substitute inline renderers
 * for each DropdownMenu part - this keeps the assertions focused on the menu's
 * own structure rather than Radix's open-state machinery.
 */
jest.mock('@radix-ui/themes', () => {
  const React = require('react');
  const Root = ({children}) => <div>{children}</div>;
  const Trigger = ({children}) => <>{children}</>;
  const Content = ({children}) => <div role="menu">{children}</div>;
  const Item = ({children, onSelect, asChild, ...rest}) => {
    if (asChild) return React.cloneElement(children, {role: 'menuitem', ...rest});
    return (
      <button type="button" role="menuitem" onClick={onSelect} {...rest}>
        {children}
      </button>
    );
  };
  const Separator = () => <hr />;
  const Label = ({children}) => <div>{children}</div>;
  return {
    DropdownMenu: {Root, Trigger, Content, Item, Separator, Label},
    Flex: ({children}) => <span>{children}</span>,
    IconButton: ({children, ...rest}) => (
      <button type="button" {...rest}>
        {children}
      </button>
    ),
    Text: ({children}) => <span>{children}</span>,
  };
});

jest.mock('lucide-react', () => ({
  HelpCircle: () => <span data-testid="icon-help" />,
  ExternalLink: () => <span data-testid="icon-external" />,
}));

import HelpMenu from './HelpMenu';

const NEXUS_BASE = 'https://links.sonatype.com/products/nexus';
const NXRM3_BASE = 'https://links.sonatype.com/products/nxrm3';

function getLinkByText(text) {
  return screen.getByText(text).closest('a');
}

describe('HelpMenu', () => {
  beforeEach(() => {
    mockAboutModal.mockClear();
    mockIsCloud = false;
    mockVersion = '3.90.0-01';
    mockEditionId = EDITION_ID_PRO;
    mockStateThrows = false;
  });

  describe('items rendered on both distributions', () => {
    it.each([[false, 'self-hosted'], [true, 'cloud']])(
      'renders About, Documentation, Release Notes, Knowledge Base, Guides, Community, Issue Tracker and Support on %s (isCloud=%s)',
      (isCloud) => {
        mockIsCloud = isCloud;
        render(<HelpMenu />);

        expect(screen.getByText('About')).toBeInTheDocument();
        expect(screen.getByText('Documentation')).toBeInTheDocument();
        expect(screen.getByText('Release Notes')).toBeInTheDocument();
        expect(screen.getByText('Knowledge Base')).toBeInTheDocument();
        expect(screen.getByText('Sonatype Guides')).toBeInTheDocument();
        expect(screen.getByText('Community')).toBeInTheDocument();
        expect(screen.getByText('Issue Tracker')).toBeInTheDocument();
        expect(screen.getByText('Support')).toBeInTheDocument();
      }
    );
  });

  describe('About', () => {
    it('keeps the About modal closed until About is selected', () => {
      render(<HelpMenu />);
      expect(mockAboutModal).toHaveBeenCalledWith(
        expect.objectContaining({open: false})
      );
      expect(screen.queryByTestId('about-modal-stub')).not.toBeInTheDocument();
    });

    it('opens the native About modal when About is selected', () => {
      render(<HelpMenu />);
      fireEvent.click(screen.getByText('About'));
      expect(screen.getByTestId('about-modal-stub')).toBeInTheDocument();
      expect(mockAboutModal).toHaveBeenLastCalledWith(
        expect.objectContaining({open: true})
      );
    });

    it('passes the edition and status details through to the About modal', () => {
      mockVersion = '3.90.0-01';
      render(<HelpMenu />);
      expect(mockAboutModal).toHaveBeenCalledWith(
        expect.objectContaining({
          isCloud: false,
          version: '3.90.0-01',
          edition: 'Pro',
        })
      );
    });
  });

  describe('edition display name', () => {
    it.each([
      [EDITION_ID_PRO_PLUS, 'Pro+'],
      [EDITION_ID_ENTERPRISE, 'Enterprise'],
      [EDITION_ID_PRO, 'Pro'],
    ])('maps edition id %s to display name %s', (editionId, displayName) => {
      mockEditionId = editionId;
      render(<HelpMenu />);
      expect(mockAboutModal).toHaveBeenCalledWith(
        expect.objectContaining({edition: displayName})
      );
    });

    it('does not pass the raw backend short name for Pro+', () => {
      mockEditionId = EDITION_ID_PRO_PLUS;
      render(<HelpMenu />);
      expect(mockAboutModal).not.toHaveBeenCalledWith(
        expect.objectContaining({edition: 'PROPLUS'})
      );
    });

    it('falls back to the default display name when no edition id is published', () => {
      mockEditionId = undefined;
      render(<HelpMenu />);
      expect(mockAboutModal).toHaveBeenCalledWith(
        expect.objectContaining({edition: 'Core'})
      );
    });
  });

  describe('version label', () => {
    it('renders the version label on self-hosted', () => {
      mockIsCloud = false;
      mockVersion = '3.90.0-01';
      render(<HelpMenu />);
      expect(screen.getByText(/Version: 3\.90\.0-01/)).toBeInTheDocument();
    });

    it('omits the version label on cloud', () => {
      mockIsCloud = true;
      render(<HelpMenu />);
      expect(screen.queryByText(/Version:/)).not.toBeInTheDocument();
    });

    it('renders an empty version gracefully on self-hosted', () => {
      mockIsCloud = false;
      mockVersion = '';
      render(<HelpMenu />);
      expect(screen.getByText(/Version:/)).toBeInTheDocument();
    });

    it('renders without crashing when ExtJS.state() throws (e.g. during logout)', () => {
      mockIsCloud = false;
      mockStateThrows = true;
      expect(() => render(<HelpMenu />)).not.toThrow();
      expect(screen.getByText(/Version:/)).toBeInTheDocument();
    });
  });

  describe('Documentation link', () => {
    it('uses /docs/{major.minor} on self-hosted', () => {
      mockIsCloud = false;
      mockVersion = '3.90.0-01';
      render(<HelpMenu />);

      const link = getLinkByText('Documentation');
      expect(link).toHaveAttribute('href', expect.stringMatching(new RegExp(`^${NEXUS_BASE}/docs/3\\.90\\?`)));
      expect(link).toHaveAttribute('target', '_blank');
      expect(link).toHaveAttribute('rel', 'noopener noreferrer');
    });

    it('uses unversioned /docs on cloud', () => {
      mockIsCloud = true;
      mockVersion = '3.90.0-01';
      render(<HelpMenu />);

      const link = getLinkByText('Documentation');
      expect(link.getAttribute('href')).toMatch(new RegExp(`^${NEXUS_BASE}/docs\\?`));
      expect(link.getAttribute('href')).not.toMatch(/\/docs\/3\.90/);
    });

    it('falls back to the unversioned /docs path when the version is unparseable on self-hosted', () => {
      mockIsCloud = false;
      mockVersion = 'garbage';
      render(<HelpMenu />);

      const link = getLinkByText('Documentation');
      expect(link.getAttribute('href')).toMatch(new RegExp(`^${NEXUS_BASE}/docs\\?`));
      expect(link.getAttribute('href')).not.toMatch(/\/docs\//);
    });
  });

  describe('link targets', () => {
    it.each([
      ['Release Notes', `${NXRM3_BASE}/release-notes`, 'menu-release-notes'],
      ['Knowledge Base', `${NEXUS_BASE}/kb`, 'menu-knowledge'],
      ['Sonatype Guides', `${NXRM3_BASE}/guides`, 'menu-guides'],
      ['Community', `${NEXUS_BASE}/community`, 'menu-community'],
      ['Issue Tracker', `${NEXUS_BASE}/issues`, 'menu-issuetracker'],
      ['Support', `${NEXUS_BASE}/support`, 'menu-support'],
    ])('%s points at %s with campaign %s', (label, baseUrl, campaign) => {
      render(<HelpMenu />);
      const link = getLinkByText(label);
      expect(link).toBeInTheDocument();
      expect(link.getAttribute('href')).toMatch(new RegExp(`^${baseUrl}\\?`));
      expect(link.getAttribute('href')).toContain(`utm_campaign=${campaign}`);
      expect(link).toHaveAttribute('target', '_blank');
      expect(link).toHaveAttribute('rel', 'noopener noreferrer');
    });

    it('carries the UTM medium and source on every external link', () => {
      render(<HelpMenu />);
      const labels = [
        'Documentation',
        'Release Notes',
        'Knowledge Base',
        'Sonatype Guides',
        'Community',
        'Issue Tracker',
        'Support',
      ];
      for (const label of labels) {
        const href = getLinkByText(label).getAttribute('href');
        expect(href).toContain('utm_medium=product');
        expect(href).toContain('utm_source=nexus_repo');
      }
    });
  });

  describe('accessibility', () => {
    it('exposes the trigger with an aria-label', () => {
      render(<HelpMenu />);
      expect(screen.getByLabelText('Help & Documentation')).toBeInTheDocument();
    });
  });
});
