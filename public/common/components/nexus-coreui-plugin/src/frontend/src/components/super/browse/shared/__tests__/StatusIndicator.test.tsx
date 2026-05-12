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
import { StatusIndicator } from '../StatusIndicator';

const renderWithTheme = (component: React.ReactElement) => {
  return render(<Theme>{component}</Theme>);
};

describe('StatusIndicator', () => {
  describe('online status', () => {
    it('renders indicator when online', () => {
      renderWithTheme(<StatusIndicator status={{ online: true }} />);
      const indicator = screen.getByTestId('status-indicator');
      expect(indicator).toBeInTheDocument();
      const circle = screen.getByTestId('status-indicator-circle');
      expect(circle).toBeInTheDocument();
    });

    it('shows "Online" label when showLabel is true', () => {
      renderWithTheme(<StatusIndicator status={{ online: true }} showLabel />);
      expect(screen.getByText('Online')).toBeInTheDocument();
    });
  });

  describe('offline status', () => {
    it('renders indicator when offline', () => {
      renderWithTheme(<StatusIndicator status={{ online: false }} />);
      const indicator = screen.getByTestId('status-indicator');
      expect(indicator).toBeInTheDocument();
    });

    it('shows "Offline" label when showLabel is true', () => {
      renderWithTheme(<StatusIndicator status={{ online: false }} showLabel />);
      expect(screen.getByText('Offline')).toBeInTheDocument();
    });
  });

  describe('without showLabel', () => {
    it('does not show label by default', () => {
      renderWithTheme(<StatusIndicator status={{ online: true }} />);
      expect(screen.queryByText('Online')).not.toBeInTheDocument();
      expect(screen.queryByText('Offline')).not.toBeInTheDocument();
    });

    it('does not show label when showLabel is false', () => {
      renderWithTheme(<StatusIndicator status={{ online: true }} showLabel={false} />);
      expect(screen.queryByText('Online')).not.toBeInTheDocument();
    });
  });

  describe('with description', () => {
    it('renders indicator with description', () => {
      renderWithTheme(
        <StatusIndicator status={{ online: false, description: 'Connection timeout' }} />
      );
      // Status indicator should be present
      const indicator = screen.getByTestId('status-indicator');
      expect(indicator).toBeInTheDocument();
    });
  });
});

