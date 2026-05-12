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

import { NistStepper } from '../NistStepper';

function renderWithTheme(ui: React.ReactElement) {
  return render(<Theme>{ui}</Theme>);
}

describe('NistStepper', () => {
  it('returns null when currentPhase is null', () => {
    const { container } = renderWithTheme(<NistStepper currentPhase={null} />);
    expect(container.querySelector('[role="navigation"]')).toBeNull();
  });

  it('renders all 6 phase labels', () => {
    renderWithTheme(<NistStepper currentPhase="ALERT" />);

    expect(screen.getByText('Alert')).toBeInTheDocument();
    expect(screen.getByText('Triage')).toBeInTheDocument();
    expect(screen.getByText('Containment')).toBeInTheDocument();
    expect(screen.getByText('Eradication')).toBeInTheDocument();
    expect(screen.getByText('Recovery')).toBeInTheDocument();
    expect(screen.getByText('Post Incident')).toBeInTheDocument();
  });

  it('has navigation role and aria-label', () => {
    renderWithTheme(<NistStepper currentPhase="TRIAGE" />);
    expect(screen.getByRole('navigation', { name: 'Incident response phases' })).toBeInTheDocument();
  });

  it('highlights the current phase with bold text', () => {
    renderWithTheme(<NistStepper currentPhase="CONTAINMENT" />);

    const containment = screen.getByText('Containment');
    expect(containment.className).toMatch(/weight-bold/);
  });

  it('shows completed phases before the current phase', () => {
    renderWithTheme(<NistStepper currentPhase="ERADICATION" />);

    const nav = screen.getByRole('navigation');
    const alertText = screen.getByText('Alert');
    const triageText = screen.getByText('Triage');
    const containmentText = screen.getByText('Containment');

    expect(alertText).toHaveStyle({ color: 'var(--green-9)' });
    expect(triageText).toHaveStyle({ color: 'var(--green-9)' });
    expect(containmentText).toHaveStyle({ color: 'var(--green-9)' });
  });

  it('shows future phases in gray', () => {
    renderWithTheme(<NistStepper currentPhase="TRIAGE" />);

    const containment = screen.getByText('Containment');
    const eradication = screen.getByText('Eradication');

    expect(containment).toHaveStyle({ color: 'var(--gray-8)' });
    expect(eradication).toHaveStyle({ color: 'var(--gray-8)' });
  });

  it('marks current phase in blue', () => {
    renderWithTheme(<NistStepper currentPhase="RECOVERY" />);

    const recovery = screen.getByText('Recovery');
    expect(recovery).toHaveStyle({ color: 'var(--blue-9)' });
  });

  it('handles POST_INCIDENT as the last phase', () => {
    renderWithTheme(<NistStepper currentPhase="POST_INCIDENT" />);

    const postIncident = screen.getByText('Post Incident');
    expect(postIncident).toHaveStyle({ color: 'var(--blue-9)' });

    const recovery = screen.getByText('Recovery');
    expect(recovery).toHaveStyle({ color: 'var(--green-9)' });
  });
});
