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
import ComingSoonPage from '../ComingSoonPage';

// Mock window.history.back
const mockHistoryBack = jest.fn();
Object.defineProperty(window, 'history', {
  value: { back: mockHistoryBack },
  writable: true,
});

// Mock window.location
const mockLocation = {
  hash: '',
};
Object.defineProperty(window, 'location', {
  value: mockLocation,
  writable: true,
});

describe('ComingSoonPage', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    mockLocation.hash = '';
  });

  describe('rendering', () => {
    it('renders Coming Soon title', () => {
      render(<ComingSoonPage />);
      expect(screen.getByText('Coming Soon')).toBeInTheDocument();
    });

    it('renders default feature name', () => {
      render(<ComingSoonPage />);
      expect(screen.getByText('This Feature')).toBeInTheDocument();
    });

    it('renders custom feature name', () => {
      render(<ComingSoonPage featureName="Custom Feature" />);
      expect(screen.getByText('Custom Feature')).toBeInTheDocument();
    });

    it('renders default description', () => {
      render(<ComingSoonPage />);
      expect(
        screen.getByText("We're working hard to bring this feature to the new Nexus One UI.")
      ).toBeInTheDocument();
    });

    it('renders custom description', () => {
      render(<ComingSoonPage description="Custom description text" />);
      expect(screen.getByText('Custom description text')).toBeInTheDocument();
    });

    it('renders migration notice', () => {
      render(<ComingSoonPage />);
      expect(
        screen.getByText(/This page is part of the Nexus One UI rollout/i)
      ).toBeInTheDocument();
    });

    it('renders Go to Dashboard button', () => {
      render(<ComingSoonPage />);
      expect(screen.getByRole('button', { name: /Go to Dashboard/i })).toBeInTheDocument();
    });
  });

  describe('back button', () => {
    it('shows back button by default', () => {
      render(<ComingSoonPage />);
      expect(screen.getByRole('button', { name: /Go Back/i })).toBeInTheDocument();
    });

    it('hides back button when showBackButton is false', () => {
      render(<ComingSoonPage showBackButton={false} />);
      expect(screen.queryByRole('button', { name: /Go Back/i })).not.toBeInTheDocument();
    });

    it('calls history.back when Go Back is clicked', () => {
      render(<ComingSoonPage />);
      fireEvent.click(screen.getByRole('button', { name: /Go Back/i }));
      expect(mockHistoryBack).toHaveBeenCalled();
    });
  });

  describe('navigation', () => {
    it('navigates to default URL when Go to Dashboard is clicked', () => {
      render(<ComingSoonPage defaultUrl="#custom/url" />);
      fireEvent.click(screen.getByRole('button', { name: /Go to Dashboard/i }));
      expect(mockLocation.hash).toBe('custom/url');
    });

    it('uses browse/welcome as default URL', () => {
      render(<ComingSoonPage />);
      fireEvent.click(screen.getByRole('button', { name: /Go to Dashboard/i }));
      expect(mockLocation.hash).toBe('browse/welcome');
    });
  });
});


