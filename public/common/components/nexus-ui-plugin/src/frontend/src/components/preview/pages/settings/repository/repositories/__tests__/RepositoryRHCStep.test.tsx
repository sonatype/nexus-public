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
import userEvent from '@testing-library/user-event';
import { RepositoryRHCStep } from '../RepositoryRHCStep';

jest.mock('../useRepositoriesApi', () => ({
  useRepositoriesApi: () => ({
    enableHealthCheck: jest.fn(),
  }),
}));

describe('RepositoryRHCStep', () => {
  it('renders with switch in deferred mode', () => {
    const onChoice = jest.fn();
    render(<RepositoryRHCStep mode="deferred" value="none" onChoice={onChoice} />);

    expect(screen.getByRole('switch')).toBeInTheDocument();
    expect(screen.getByText('Repository Health Check')).toBeInTheDocument();
  });

  it('calls onChoice with "enable" when switch is toggled on', async () => {
    const onChoice = jest.fn();

    render(<RepositoryRHCStep mode="deferred" value="none" onChoice={onChoice} />);

    const switchElement = screen.getByRole('switch');
    expect(switchElement).not.toBeChecked();

    await userEvent.click(switchElement);

    expect(onChoice).toHaveBeenCalledWith('enable');
  });

  it('calls onChoice with "none" when switch is toggled off', async () => {
    const onChoice = jest.fn();

    render(<RepositoryRHCStep mode="deferred" value="enable" onChoice={onChoice} />);

    const switchElement = screen.getByRole('switch');
    expect(switchElement).toBeChecked();

    await userEvent.click(switchElement);

    expect(onChoice).toHaveBeenCalledWith('none');
  });

  it('displays checked state correctly based on value prop', () => {
    const { rerender } = render(<RepositoryRHCStep mode="deferred" value="none" onChoice={jest.fn()} />);

    expect(screen.getByRole('switch')).not.toBeChecked();

    rerender(<RepositoryRHCStep mode="deferred" value="enable" onChoice={jest.fn()} />);

    expect(screen.getByRole('switch')).toBeChecked();
  });
});
