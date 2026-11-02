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
import { EditionPill } from '../EditionPill';

describe('EditionPill', () => {
  it('renders the Pro edition name as a blue Radix Badge', () => {
    render(<EditionPill tier="pro" />);

    const pill = screen.getByTestId('edition-pill-pro');
    expect(pill).toHaveTextContent('Pro');
    expect(pill).toHaveClass('rt-Badge');
    expect(pill).toHaveAttribute('data-accent-color', 'blue');
  });

  it('renders the Enterprise edition name as a purple Radix Badge', () => {
    render(<EditionPill tier="enterprise" />);

    const pill = screen.getByTestId('edition-pill-enterprise');
    expect(pill).toHaveTextContent('Enterprise');
    expect(pill).toHaveAttribute('data-accent-color', 'purple');
  });
});
