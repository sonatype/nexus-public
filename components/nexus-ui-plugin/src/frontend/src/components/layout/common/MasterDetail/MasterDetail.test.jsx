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

import MasterDetail from './MasterDetail';
import Master from './Master';
import Detail from './Detail';

import ExtJS from '../../../../interface/ExtJS';

jest.mock('../../../../interface/ExtJS', () => class {
  static useHistory = jest.fn();
});

describe('MasterDetail', () => {
  it('renders the master view when at the root', () => {
    ExtJS.useHistory.mockReturnValue({location: {pathname: ''}});
    const {queryByTestId} = render(
        <MasterDetail path="admin">
          <Master><TestMaster /></Master>
          <Detail><TestDetail /></Detail>
        </MasterDetail>
    );

    expect(queryByTestId('master')).toBeInTheDocument();
    expect(queryByTestId('detail')).not.toBeInTheDocument();
  });

  it('renders the detail view when on a child path', () => {
    ExtJS.useHistory.mockReturnValue({location: {pathname: ':itemId'}});
    const {queryByTestId} = render(
        <MasterDetail path="admin">
          <Master><TestMaster /></Master>
          <Detail><TestDetail /></Detail>
        </MasterDetail>
    );

    expect(queryByTestId('master')).not.toBeInTheDocument();
    expect(queryByTestId('detail')).toBeInTheDocument();
  });

  it('renders the detail view when onCreate is called', () => {
    ExtJS.useHistory.mockReturnValue({location: {pathname: ''}});
    const {getByTestId, queryByTestId} = render(
        <MasterDetail path="admin">
          <Master><TestMaster /></Master>
          <Detail><TestDetail /></Detail>
        </MasterDetail>
    );

    expect(queryByTestId('master')).toBeInTheDocument();
    userEvent.click(getByTestId('create'));

    expect(queryByTestId('master')).not.toBeInTheDocument();
    expect(queryByTestId('detail')).toBeInTheDocument();
  });
});

// These components remove the warnings about the Master/Detail props not being used and provide hooks for validation
function TestMaster({onCreate, onEdit}) {
  return <div data-testid="master"><button data-testid="create" onClick={onCreate} /></div>;
}

function TestDetail({itemId, onDone}) {
  return <div data-testid="detail">${itemId}</div>;
}
