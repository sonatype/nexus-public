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
import '@testing-library/jest-dom';
import { Theme } from '@radix-ui/themes';
import { FeatureGate, withFeatureGate, withCloudExcluded } from '../FeatureGate';

jest.mock('../../../../interface/ExtJS', () => ({
  ExtJS: {
    state: jest.fn(),
    useState: jest.fn((init) => (typeof init === 'function' ? init() : init)),
    checkPermission: jest.fn().mockReturnValue(true),
  },
}));

// Control window.location to simulate production vs development
const originalLocation = window.location;

beforeEach(() => {
  delete window.location;
  window.location = {
    hostname: 'production.example.com',
    search: '',
    href: 'https://production.example.com/path',
  };
});

afterEach(() => {
  window.location = originalLocation;
});

describe('FeatureGate', () => {
  describe('with an enabled feature key', () => {
    it('renders children', () => {
      render(
        <FeatureGate featureKey="support.logs" featureName="Logs">
          <div data-testid="child-content">Child content</div>
        </FeatureGate>
      );

      expect(screen.getByTestId('child-content')).toBeInTheDocument();
    });

    it('does not render the not-available page', () => {
      render(
        <FeatureGate featureKey="support.logs" featureName="Logs">
          <div>Child content</div>
        </FeatureGate>
      );

      expect(screen.queryByText('Not available in preview')).not.toBeInTheDocument();
    });
  });

  describe('with a disabled feature key', () => {
    // Using security.oauth2 since security.anonymous is now enabled (NEXUS-51085)
    it('renders SettingsNotAvailablePage instead of children', () => {
      render(
        <FeatureGate featureKey="security.oauth2" featureName="OAuth2">
          <div data-testid="child-content">Child content</div>
        </FeatureGate>
      );

      expect(screen.queryByTestId('child-content')).not.toBeInTheDocument();
      expect(screen.getByText('Not available in preview')).toBeInTheDocument();
    });

    it('passes featureName to SettingsNotAvailablePage', () => {
      render(
        <FeatureGate featureKey="security.oauth2" featureName="OAuth2">
          <div>Child content</div>
        </FeatureGate>
      );

      expect(
        screen.getByText(/OAuth2 is still being prepared for the Nexus One UI/i)
      ).toBeInTheDocument();
    });
  });
});

describe('withFeatureGate', () => {
  function SamplePage() {
    return <div data-testid="sample-page">Sample page content</div>;
  }
  SamplePage.displayName = 'SamplePage';

  describe('with an enabled feature key', () => {
    it('renders the wrapped component', () => {
      const GatedPage = withFeatureGate(SamplePage, 'support.logs', 'Logs');
      render(<GatedPage />);

      expect(screen.getByTestId('sample-page')).toBeInTheDocument();
    });
  });

  describe('with a disabled feature key', () => {
    it('renders SettingsNotAvailablePage with the correct featureName', () => {
      const GatedPage = withFeatureGate(SamplePage, 'security.oauth2', 'OAuth2');
      render(<GatedPage />);

      expect(screen.queryByTestId('sample-page')).not.toBeInTheDocument();
      expect(
        screen.getByText(/OAuth2 is still being prepared for the Nexus One UI/i)
      ).toBeInTheDocument();
    });
  });

  describe('displayName', () => {
    it('sets displayName using the wrapped component displayName', () => {
      const GatedPage = withFeatureGate(SamplePage, 'support.logs', 'Logs');
      expect(GatedPage.displayName).toBe('FeatureGate(SamplePage)');
    });

    it('falls back to component name when displayName is not set', () => {
      function UnnamedComponent() {
        return <div />;
      }
      const GatedPage = withFeatureGate(UnnamedComponent, 'support.logs', 'Logs');
      expect(GatedPage.displayName).toBe('FeatureGate(UnnamedComponent)');
    });
  });
});

describe('withCloudExcluded', () => {
  const { ExtJS } = require('../../../../interface/ExtJS');

  function TestWrapper({ children }: { children: React.ReactNode }) {
    return <Theme>{children}</Theme>;
  }

  function Hello() {
    return <div>hello</div>;
  }

  beforeEach(() => {
    jest.clearAllMocks();
  });

  it('renders the wrapped component when not on cloud', () => {
    ExtJS.state.mockReturnValue({ getValue: jest.fn().mockReturnValue(false) });
    const Gated = withCloudExcluded(Hello, 'HTTP');
    render(<Gated />, { wrapper: TestWrapper });
    expect(screen.getByText('hello')).toBeInTheDocument();
  });

  it('renders SettingsNotAvailablePage when on cloud', () => {
    ExtJS.state.mockReturnValue({ getValue: jest.fn().mockReturnValue(true) });
    const Gated = withCloudExcluded(Hello, 'HTTP');
    render(<Gated />, { wrapper: TestWrapper });
    expect(screen.queryByText('hello')).not.toBeInTheDocument();
    expect(screen.getByText(/HTTP/)).toBeInTheDocument();
  });

  it('sets displayName on the returned component', () => {
    const Gated = withCloudExcluded(Hello, 'HTTP');
    expect(Gated.displayName).toBe('CloudExcluded(Hello)');
  });
});
