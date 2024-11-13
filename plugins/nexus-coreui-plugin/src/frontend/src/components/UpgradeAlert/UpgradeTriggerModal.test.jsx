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
import {render} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import axios from 'axios';
import UpgradeTriggerModal from './UpgradeTriggerModal';

jest.mock('axios', () => ({
  post: jest.fn()
}));

describe('UpgradeTriggerModal', () => {
  it('renders the modal when showModal is true with the text', () => {
    const {getByText} = render(<UpgradeTriggerModal showModal={true} />);

    expect(getByText('Proceed with upgrade?')).toBeInTheDocument();
    expect(getByText('We highly recommend backing up your database before proceeding.')).toBeInTheDocument();
  });

  it('calls setShowModal with false when the cancel button is clicked', () => {
    const setShowModal = jest.fn();
    const {getByText} = render(<UpgradeTriggerModal showModal={true} setShowModal={setShowModal} />);

    userEvent.click(getByText('Cancel'));

    expect(setShowModal).toHaveBeenCalledWith(false);
    expect(axios.post).toHaveBeenCalledTimes(0);
  });

  it('calls put request when the save button is clicked', () => {
    const setShowModal = jest.fn();
    const {getByText, getByRole} = render(<UpgradeTriggerModal showModal={true} setShowModal={setShowModal} />);

    expect(getByText('Continue')).toBeDisabled();

    userEvent.click(getByRole('checkbox'));
    userEvent.click(getByText('Continue'));

    expect(getByText('Continue')).not.toBeDisabled();
    expect(setShowModal).toHaveBeenCalledWith(false);
    expect(axios.post).toHaveBeenCalledTimes(1);
    expect(axios.post).toHaveBeenCalledWith('service/rest/v1/clustered/upgrade-database-schema');
  });
});
