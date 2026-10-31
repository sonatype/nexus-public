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

import { SystemAlert } from '../SystemAlert';

const renderInTheme = (ui: React.ReactElement) => render(<Theme>{ui}</Theme>);

describe('SystemAlert', () => {
  it('renders the title and message', () => {
    renderInTheme(
      <SystemAlert title="Recovery Mode Enabled" message="Repair tasks are blocked." />
    );

    expect(screen.getByText('Recovery Mode Enabled')).toBeInTheDocument();
    expect(screen.getByText('Repair tasks are blocked.')).toBeInTheDocument();
  });

  it('does not render a CTA when no action is provided', () => {
    renderInTheme(<SystemAlert title="Title" message="Message" />);
    expect(screen.queryByRole('button')).not.toBeInTheDocument();
  });

  it('renders a configurable CTA and fires onClick', () => {
    const onClick = jest.fn();
    renderInTheme(
      <SystemAlert
        title="Title"
        message="Message"
        action={{ label: 'View Details', onClick }}
      />
    );

    const cta = screen.getByRole('button', { name: 'View Details' });
    fireEvent.click(cta);
    expect(onClick).toHaveBeenCalledTimes(1);
  });

  it('navigates via href when the CTA has no onClick', () => {
    renderInTheme(
      <SystemAlert
        title="Title"
        message="Message"
        action={{ label: 'View Details', href: 'preview/admin/settings/support/recoverymode' }}
      />
    );

    fireEvent.click(screen.getByRole('button', { name: 'View Details' }));
    expect(window.location.hash).toContain('preview/admin/settings/support/recoverymode');
  });

  it('is not dismissable by default (no dismiss button)', () => {
    renderInTheme(<SystemAlert title="Title" message="Message" />);
    expect(screen.queryByRole('button', { name: 'Dismiss alert' })).not.toBeInTheDocument();
  });

  it('renders a dismiss button when dismissable and hides itself on click', () => {
    const onDismiss = jest.fn();
    renderInTheme(
      <SystemAlert title="Title" message="Message" dismissable onDismiss={onDismiss} />
    );

    const dismiss = screen.getByRole('button', { name: 'Dismiss alert' });
    fireEvent.click(dismiss);

    expect(onDismiss).toHaveBeenCalledTimes(1);
    expect(screen.queryByText('Title')).not.toBeInTheDocument();
  });

  it('applies the tier modifier class', () => {
    renderInTheme(<SystemAlert title="Title" message="Message" tier="warning" />);
    expect(screen.getByTestId('nxrm-system-alert')).toHaveClass('nxrm-system-alert--warning');
  });

  it.each(['info', 'warning', 'error', 'success'] as const)(
    'renders the %s tier with the correct role/aria-live and icon',
    (tier) => {
      renderInTheme(<SystemAlert title="Title" message="Message" tier={tier} />);
      const alert = screen.getByTestId('nxrm-system-alert');
      expect(alert).toHaveClass(`nxrm-system-alert--${tier}`);
      // error tier is assertive; the others are polite status regions
      expect(alert).toHaveAttribute('role', tier === 'error' ? 'alert' : 'status');
      expect(alert).toHaveAttribute('aria-live', tier === 'error' ? 'assertive' : 'polite');
    }
  );

  it('does nothing when the CTA action has neither onClick nor href', () => {
    const hashBefore = window.location.hash;
    renderInTheme(
      <SystemAlert title="Title" message="Message" action={{ label: 'Noop' }} />
    );
    fireEvent.click(screen.getByRole('button', { name: 'Noop' }));
    // No navigation and no crash.
    expect(window.location.hash).toBe(hashBefore);
  });

  it('dismisses without an onDismiss handler', () => {
    renderInTheme(<SystemAlert title="Title" message="Message" dismissable />);
    fireEvent.click(screen.getByRole('button', { name: 'Dismiss alert' }));
    expect(screen.queryByText('Title')).not.toBeInTheDocument();
  });
});

describe('SystemAlert title optionality', () => {
  it('renders the message with no title element when title is omitted', () => {
    renderInTheme(<SystemAlert tier="warning" message="Message only text" />);
    expect(screen.getByText('Message only text')).toBeInTheDocument();
    expect(document.querySelector('.nxrm-system-alert__title')).toBeNull();
  });

  it('renders the title element when title is provided', () => {
    renderInTheme(<SystemAlert tier="warning" title="A Title" message="Body" />);
    expect(screen.getByText('A Title')).toBeInTheDocument();
    expect(document.querySelector('.nxrm-system-alert__title')).not.toBeNull();
  });
});
