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
import userEvent from '@testing-library/user-event';
import {ExtJS} from '@sonatype/nexus-ui-plugin';

import IqServerConnected from './IqServerConnected';
import UIStrings from '../../../../constants/UIStrings';

const mockRouter = {
  stateService: {
    go: jest.fn()
  }
};

jest.mock('@uirouter/react', () => ({
  useRouter: () => mockRouter
}));

jest.mock('@sonatype/nexus-ui-plugin', () => {
  return {
    ...jest.requireActual('@sonatype/nexus-ui-plugin'),
    ExtJS: {
      state: jest.fn(),
      useUser: jest.fn(() => ({ name: 'test-user' }))
    }
  };
});

jest.mock('./IqServerConnectedMachine', () => {
  const {createMachine, assign} = require('xstate');

  return createMachine({
    id: 'IqServerConnectedMachine',
    initial: 'loading',
    context: {
      data: null,
      pristineData: null,
      iqServerUrl: '',
      error: null,
      connectionStatus: 'connected'
    },
    states: {
      loading: {
        always: {
          target: 'loaded',
          actions: 'initContext'
        }
      },
      loaded: {}
    }
  }, {
    actions: {
      initContext: assign({
        data: () => {
          const ExtJS = require('@sonatype/nexus-ui-plugin').ExtJS;
          const clmState = ExtJS.state().getValue('clm');
          return {
            lifecycle: clmState?.enabled || false,
            firewall: clmState?.hasFirewall || false
          };
        },
        pristineData: () => {
          const ExtJS = require('@sonatype/nexus-ui-plugin').ExtJS;
          const clmState = ExtJS.state().getValue('clm');
          return {
            lifecycle: clmState?.enabled || false,
            firewall: clmState?.hasFirewall || false
          };
        },
        iqServerUrl: () => {
          const ExtJS = require('@sonatype/nexus-ui-plugin').ExtJS;
          const clmState = ExtJS.state().getValue('clm');
          return clmState?.url || '';
        }
      })
    }
  });
});

const {IQ_SERVER} = UIStrings;

describe('IqServerConnected', () => {
  const mockClmState = (overrides = {}) => {
    const defaultState = {
      enabled: true,
      hasFirewall: false,
      url: 'http://localhost:8070'
    };
    ExtJS.state.mockReturnValue({
      getValue: jest.fn(() => ({...defaultState, ...overrides}))
    });
  };

  beforeEach(() => {
    mockRouter.stateService.go.mockClear();
  });

  it('renders the page title and description', () => {
    mockClmState();
    render(<IqServerConnected />);

    expect(screen.getByText(IQ_SERVER.CONNECTED.TITLE)).toBeInTheDocument();
    expect(screen.getByText(IQ_SERVER.CONNECTED.SUBTITLE)).toBeInTheDocument();
  });

  it('displays connection status', () => {
    mockClmState();
    render(<IqServerConnected />);

    expect(screen.getByText(IQ_SERVER.CONNECTED.STATUS)).toBeInTheDocument();
  });

  it('shows IQ Server URL when available', () => {
    const testUrl = 'http://test.iq.server:8070';
    mockClmState({url: testUrl});
    render(<IqServerConnected />);

    expect(screen.getByText(testUrl)).toBeInTheDocument();
  });

  it('does not show URL when not available', () => {
    mockClmState({url: ''});
    const {container} = render(<IqServerConnected />);

    const urlElement = container.querySelector('.nxrm-iq-tile-url');
    expect(urlElement).not.toBeInTheDocument();
  });

  it('shows Lifecycle as enabled when available', () => {
    mockClmState({enabled: true});
    render(<IqServerConnected />);

    expect(screen.getByText(IQ_SERVER.CONNECTED.LIFECYCLE.ENABLED)).toBeInTheDocument();
  });

  it('shows Lifecycle as not available when disabled', () => {
    mockClmState({enabled: false});
    render(<IqServerConnected />);

    const lifecycleTile = screen.getByLabelText('Sonatype Lifecycle - Not Available');
    expect(lifecycleTile).toBeInTheDocument();
  });

  it('shows Firewall as enabled when available', () => {
    mockClmState({hasFirewall: true});
    render(<IqServerConnected />);

    const firewallTile = screen.getByLabelText('Repository Firewall - Enabled');
    expect(firewallTile).toBeInTheDocument();
  });

  it('shows Firewall as not available when disabled', () => {
    mockClmState({hasFirewall: false});
    render(<IqServerConnected />);

    expect(screen.getByText(IQ_SERVER.CONNECTED.FIREWALL.NOT_AVAILABLE)).toBeInTheDocument();
  });

  it('shows Firewall explore link when not available', () => {
    mockClmState({hasFirewall: false});
    render(<IqServerConnected />);

    const exploreLink = screen.getByText(IQ_SERVER.CONNECTED.FIREWALL.EXPLORE_LINK);
    expect(exploreLink).toBeInTheDocument();
    expect(exploreLink.closest('a')).toHaveAttribute('href', 'https://www.sonatype.com/products/firewall');
  });

  it('does not show Firewall explore link when available', () => {
    mockClmState({hasFirewall: true});
    render(<IqServerConnected />);

    expect(screen.queryByText(IQ_SERVER.CONNECTED.FIREWALL.EXPLORE_LINK)).not.toBeInTheDocument();
  });

  it('navigates to connection settings when button is clicked', async () => {
    mockClmState();
    render(<IqServerConnected />);

    const connectionSettingsButton = screen.getByText(IQ_SERVER.CONNECTED.CONNECTION_SETTINGS_BUTTON);
    await userEvent.click(connectionSettingsButton);

    expect(mockRouter.stateService.go).toHaveBeenCalledWith('admin.iq');
  });

  it('applies enabled class to Lifecycle tile when enabled', () => {
    mockClmState({enabled: true});
    render(<IqServerConnected />);

    const lifecycleTile = screen.getByLabelText('Sonatype Lifecycle - Enabled');
    expect(lifecycleTile).toBeInTheDocument();
    expect(lifecycleTile).toHaveClass('nxrm-iq-tile', 'enabled');
  });

  it('applies disabled class to Lifecycle tile when disabled', () => {
    mockClmState({enabled: false});
    render(<IqServerConnected />);

    const lifecycleTile = screen.getByLabelText('Sonatype Lifecycle - Not Available');
    expect(lifecycleTile).toBeInTheDocument();
    expect(lifecycleTile).toHaveClass('nxrm-iq-tile', 'disabled');
  });

  it('applies enabled class to Firewall tile when enabled', () => {
    mockClmState({hasFirewall: true});
    render(<IqServerConnected />);

    const firewallTile = screen.getByLabelText('Repository Firewall - Enabled');
    expect(firewallTile).toBeInTheDocument();
    expect(firewallTile).toHaveClass('nxrm-iq-tile', 'enabled');
  });

  it('applies disabled class to Firewall tile when disabled', () => {
    mockClmState({hasFirewall: false});
    render(<IqServerConnected />);

    const firewallTile = screen.getByLabelText('Repository Firewall - Not Available');
    expect(firewallTile).toBeInTheDocument();
    expect(firewallTile).toHaveClass('nxrm-iq-tile', 'disabled');
  });

  it('renders both Lifecycle and Firewall tiles', () => {
    mockClmState();
    render(<IqServerConnected />);

    expect(screen.getByLabelText('Sonatype Lifecycle - Enabled')).toBeInTheDocument();
    expect(screen.getByLabelText('Repository Firewall - Not Available')).toBeInTheDocument();
  });

  it('shows Firewall description', () => {
    mockClmState();
    render(<IqServerConnected />);

    expect(screen.getByText(IQ_SERVER.CONNECTED.FIREWALL.DESCRIPTION)).toBeInTheDocument();
  });

  it('navigates to Sonatype Lifecycle page when enabled Lifecycle tile is clicked', async () => {
    mockClmState({enabled: true});
    render(<IqServerConnected />);

    const lifecycleTile = screen.getByLabelText('Sonatype Lifecycle - Enabled');
    await userEvent.click(lifecycleTile);

    expect(mockRouter.stateService.go).toHaveBeenCalledWith('admin.sonatypelifecycle');
  });

  it('does not navigate when disabled Lifecycle tile is clicked', async () => {
    mockClmState({enabled: false});
    render(<IqServerConnected />);

    const lifecycleTile = screen.getByLabelText('Sonatype Lifecycle - Not Available');
    await userEvent.click(lifecycleTile);

    expect(mockRouter.stateService.go).not.toHaveBeenCalled();
  });

  it('navigates to Sonatype Lifecycle page when enabled Lifecycle tile is activated with Enter key', async () => {
    mockClmState({enabled: true});
    render(<IqServerConnected />);

    const lifecycleTile = screen.getByLabelText('Sonatype Lifecycle - Enabled');
    lifecycleTile.focus();
    await userEvent.type(lifecycleTile, '{Enter}');

    expect(mockRouter.stateService.go).toHaveBeenCalledWith('admin.sonatypelifecycle');
  });

  it('navigates to Sonatype Lifecycle page when enabled Lifecycle tile is activated with Space key', async () => {
    mockClmState({enabled: true});
    render(<IqServerConnected />);

    const lifecycleTile = screen.getByLabelText('Sonatype Lifecycle - Enabled');
    lifecycleTile.focus();
    await userEvent.type(lifecycleTile, ' ');

    expect(mockRouter.stateService.go).toHaveBeenCalledWith('admin.sonatypelifecycle');
  });

  it('does not navigate when disabled Lifecycle tile is activated with Enter key', async () => {
    mockClmState({enabled: false});
    render(<IqServerConnected />);

    const lifecycleTile = screen.getByLabelText('Sonatype Lifecycle - Not Available');
    lifecycleTile.focus();
    await userEvent.type(lifecycleTile, '{Enter}');

    expect(mockRouter.stateService.go).not.toHaveBeenCalled();
  });

  it('sets cursor to pointer for enabled Lifecycle tile', () => {
    mockClmState({enabled: true});
    render(<IqServerConnected />);

    const lifecycleTile = screen.getByLabelText('Sonatype Lifecycle - Enabled');
    expect(lifecycleTile).toHaveStyle({cursor: 'pointer'});
  });

  it('sets cursor to default for disabled Lifecycle tile', () => {
    mockClmState({enabled: false});
    render(<IqServerConnected />);

    const lifecycleTile = screen.getByLabelText('Sonatype Lifecycle - Not Available');
    expect(lifecycleTile).toHaveStyle({cursor: 'default'});
  });

  it('sets tabIndex to 0 for enabled Lifecycle tile', () => {
    mockClmState({enabled: true});
    render(<IqServerConnected />);

    const lifecycleTile = screen.getByLabelText('Sonatype Lifecycle - Enabled');
    expect(lifecycleTile).toHaveAttribute('tabIndex', '0');
  });

  it('sets tabIndex to -1 for disabled Lifecycle tile', () => {
    mockClmState({enabled: false});
    render(<IqServerConnected />);

    const lifecycleTile = screen.getByLabelText('Sonatype Lifecycle - Not Available');
    expect(lifecycleTile).toHaveAttribute('tabIndex', '-1');
  });
});