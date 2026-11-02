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
import {render, screen} from '@testing-library/react';
import Axios from 'axios';

import FirewallConfiguration from './FirewallConfiguration';

jest.mock('axios');

const buildParentMachine = ({xetEnabled = false, firewallMode = 'DISABLED', format = 'maven2', type = 'proxy'} = {}) => [
  {
    context: {
      data: {
        format,
        type,
        xet: {enabled: xetEnabled},
        firewall: {mode: firewallMode}
      },
      pristineData: {}
    }
  },
  jest.fn()
];

describe('FirewallConfiguration', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    Axios.get.mockResolvedValue({data: []});
  });

  it('renders the Firewall section heading and mode selector', () => {
    render(<FirewallConfiguration parentMachine={buildParentMachine()} />);

    // Both CAPTION (h2) and LABEL (fieldset legend) render "Sonatype Nexus Firewall".
    expect(screen.getAllByText('Sonatype Nexus Firewall').length).toBeGreaterThanOrEqual(1);
    expect(screen.getByRole('combobox')).toBeInTheDocument();
  });

  it('does not show XET warning when XET is disabled', () => {
    render(<FirewallConfiguration parentMachine={buildParentMachine({xetEnabled: false})} />);

    expect(screen.queryByText(/Firewall is disabled while XET is enabled/i)).not.toBeInTheDocument();
  });

  it('shows XET warning when XET is enabled', () => {
    render(<FirewallConfiguration parentMachine={buildParentMachine({xetEnabled: true})} />);

    expect(screen.getByText(/Firewall is disabled while XET is enabled/i)).toBeInTheDocument();
  });

  it('disables the mode selector when XET is enabled', () => {
    render(<FirewallConfiguration parentMachine={buildParentMachine({xetEnabled: true})} />);

    expect(screen.getByRole('combobox')).toBeDisabled();
  });

  it('enables the mode selector when XET is disabled', () => {
    render(<FirewallConfiguration parentMachine={buildParentMachine({xetEnabled: false})} />);

    expect(screen.getByRole('combobox')).not.toBeDisabled();
  });

  it('does not set the disabled attribute on the DISABLED option itself when XET is on', () => {
    render(<FirewallConfiguration parentMachine={buildParentMachine({xetEnabled: true})} />);

    // The select element is disabled as a whole; the DISABLED option does not carry its
    // own disabled attribute (unlike AUDIT/QUARANTINE/PCCS) so it remains the valid fallback.
    const disabledOption = screen.getByRole('option', {name: /Disabled/i});
    expect(disabledOption).not.toHaveAttribute('disabled');
  });

  it('has AUDIT option disabled when XET is enabled', () => {
    render(<FirewallConfiguration parentMachine={buildParentMachine({xetEnabled: true})} />);

    const auditOption = screen.getByRole('option', {name: /Audit Only/i});
    expect(auditOption).toBeDisabled();
  });

  it('has QUARANTINE option disabled when XET is enabled', () => {
    render(<FirewallConfiguration parentMachine={buildParentMachine({xetEnabled: true})} />);

    const quarantineOption = screen.getByRole('option', {name: /Quarantine/i});
    expect(quarantineOption).toBeDisabled();
  });

  it('shows PCCS option for a PCCS-capable format and disables it when XET is enabled', async () => {
    Axios.get.mockResolvedValue({data: [{format: 'npm', pccsModeSupported: true}]});

    render(<FirewallConfiguration parentMachine={buildParentMachine({xetEnabled: true, format: 'npm'})} />);

    const pccsOption = await screen.findByRole('option', {name: /PCCS/i});
    expect(pccsOption).toBeDisabled();
  });

  it('does not show PCCS option for a non-PCCS format', async () => {
    Axios.get.mockResolvedValue({data: [{format: 'npm', pccsModeSupported: true}]});

    render(<FirewallConfiguration parentMachine={buildParentMachine({format: 'maven2'})} />);

    await screen.findByRole('combobox');
    expect(screen.queryByRole('option', {name: /PCCS/i})).not.toBeInTheDocument();
  });
});
