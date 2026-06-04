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

import { MetricCards } from '../MetricCards';
import type { MaliciousFinding } from '../types';

function renderWithTheme(ui: React.ReactElement) {
  return render(<Theme>{ui}</Theme>);
}

function makeFinding(overrides: Partial<MaliciousFinding> = {}): MaliciousFinding {
  return {
    id: 1,
    repositoryName: 'npm-proxy',
    assetId: 'asset-1',
    path: '/some/path',
    format: 'npm',
    recordedTime: null,
    deletedTime: null,
    deletedBy: null,
    deletionMethod: null,
    acknowledgedAt: null,
    acknowledgedBy: null,
    acknowledgedReason: null,
    firstDetectedAt: '2026-03-15T10:00:00Z',
    hash: null,
    createdBy: null,
    createdByIp: null,
    componentName: 'evil-package',
    componentVersion: '1.0.0',
    componentFormat: 'npm',
    threatLevel: 10,
    threatSummary: 'Malware detected',
    threatReference: null,
    policyName: null,
    ...overrides,
  };
}

describe('MetricCards', () => {
  it('renders all four metric cards', () => {
    renderWithTheme(
      <MetricCards
        activeFindings={[makeFinding()]}
        malwareCount={5}
        countsByRepo={{ 'npm-proxy': 1 }}
      />
    );

    expect(screen.getByText('Pending Findings')).toBeInTheDocument();
    expect(screen.getByText('Affected Repos')).toBeInTheDocument();
    expect(screen.getByText('RHC Malware')).toBeInTheDocument();
    expect(screen.getByText('First Detected')).toBeInTheDocument();
  });

  it('displays correct pending findings count', () => {
    const findings = [
      makeFinding({ id: 1 }),
      makeFinding({ id: 2 }),
      makeFinding({ id: 3, deletedTime: '2026-03-20T10:00:00Z' }),
    ];

    renderWithTheme(
      <MetricCards activeFindings={findings} malwareCount={10} countsByRepo={{ 'npm-proxy': 3 }} />
    );

    expect(screen.getByText('2')).toBeInTheDocument();
  });

  it('displays correct affected repos count', () => {
    renderWithTheme(
      <MetricCards
        activeFindings={[makeFinding()]}
        malwareCount={3}
        countsByRepo={{ 'npm-proxy': 1, 'maven-central': 2 }}
      />
    );

    expect(screen.getByText('2')).toBeInTheDocument();
  });

  it('displays malware count', () => {
    renderWithTheme(
      <MetricCards activeFindings={[]} malwareCount={42} countsByRepo={{}} />
    );

    expect(screen.getByText('42')).toBeInTheDocument();
  });

  it('displays N/A when no firstDetectedAt dates exist', () => {
    renderWithTheme(
      <MetricCards
        activeFindings={[makeFinding({ firstDetectedAt: null })]}
        malwareCount={1}
        countsByRepo={{ repo: 1 }}
      />
    );

    expect(screen.getByText('N/A')).toBeInTheDocument();
  });

  it('displays oldest first detected date', () => {
    const findings = [
      makeFinding({ id: 1, firstDetectedAt: '2026-03-20T10:00:00Z' }),
      makeFinding({ id: 2, firstDetectedAt: '2026-03-10T10:00:00Z' }),
      makeFinding({ id: 3, firstDetectedAt: '2026-03-25T10:00:00Z' }),
    ];

    renderWithTheme(
      <MetricCards activeFindings={findings} malwareCount={3} countsByRepo={{ repo: 3 }} />
    );

    const expectedDate = new Date('2026-03-10T10:00:00Z').toLocaleDateString();
    expect(screen.getByText(expectedDate)).toBeInTheDocument();
  });
});
