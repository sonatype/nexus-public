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
import {when} from 'jest-when';
import userEvent from '@testing-library/user-event';
import {ExtJS} from '@sonatype/nexus-ui-plugin';
import UpgradeModal from './UpgradeModal';
import UIStrings from '../../../../constants/UIStrings';

const {UPGRADE_MODAL: {HEADER, ABOUT, BENEFITS}} = UIStrings;

jest.mock('@sonatype/nexus-ui-plugin', () => ({
  ...jest.requireActual('@sonatype/nexus-ui-plugin'),
  ExtJS: {
    state: jest.fn().mockReturnValue({
      getValue: jest.fn(),
    }),
    useState: jest.fn(),
    usePermission: jest.fn(),
    useUser: jest.fn(),
    urlOf: jest.fn()
  },
}));

describe('UpgradeModal', () => {

  beforeEach(() => {
    jest.spyOn(ExtJS, 'useState').mockReturnValue(false);
    jest.spyOn(ExtJS, 'usePermission').mockReturnValue(true);
    jest.spyOn(ExtJS, 'useUser').mockImplementation(() => 'admin');

    when(ExtJS.state().getValue)
        .calledWith('zero.downtime.marketing.modal')
        .mockReturnValue(true);

    when(ExtJS.state().getValue)
        .calledWith('nexus.zero.downtime.enabled')
        .mockReturnValue(true);
  });
  
  it('renders the modal when required values are set', () => {
    render(<UpgradeModal />);
    const getCloseButton = screen.getByRole('button', {name: 'Dismiss'});
    const modal = screen.getByRole('dialog');

    expect(modal).toHaveTextContent(HEADER.TITLE);
    expect(modal).toHaveTextContent(ABOUT.TITLE + ABOUT.DESCRIPTION);
    expect(modal).toHaveTextContent(BENEFITS.TITLE);
    expect(modal).toHaveTextContent(BENEFITS.LIST.ITEM1 + BENEFITS.LIST.ITEM2 + BENEFITS.LIST.ITEM3);
    expect(getCloseButton).toBeInTheDocument();

    userEvent.click(getCloseButton);

    expect(modal).not.toBeInTheDocument();
  });

  it('closes when clicking the X button in the header', () => {
    render(<UpgradeModal />);
    const getCloseButton = screen.getByRole('button', {name: 'Close'});
    const modal = screen.getByRole('dialog');

    expect(modal).toBeInTheDocument();
    expect(getCloseButton).toBeInTheDocument();

    userEvent.click(getCloseButton);

    expect(modal).not.toBeInTheDocument();
  });
});
