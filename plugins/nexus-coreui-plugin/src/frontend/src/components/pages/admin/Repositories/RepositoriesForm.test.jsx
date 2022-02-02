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
import {
  fireEvent,
  render,
  screen,
  waitFor,
  waitForElementToBeRemoved,
  getAllByRole,
  getByRole
} from '@testing-library/react';
import {when} from 'jest-when';
import '@testing-library/jest-dom/extend-expect';
import TestUtils from '@sonatype/nexus-ui-plugin/src/frontend/src/interface/TestUtils';

import axios from 'axios';
import UIStrings from '../../../../constants/UIStrings';

import RepositoriesForm from './RepositoriesForm';
import {
  basePubUrl,
  recipesUrl,
  blobStoresUrl,
  repositoryUrl
} from './RepositoriesFormMachine';

import {repositoriesUrl} from './GroupMembersSelectorMachine';

jest.mock('axios', () => ({
  ...jest.requireActual('axios'),
  get: jest.fn(),
  put: jest.fn(),
  post: jest.fn(),
  delete: jest.fn()
}));

const {EDITOR} = UIStrings.REPOSITORIES;

describe('RepositoriesForm', () => {
  const selectors = {
    ...TestUtils.selectors,
    getCreateButton: () =>
      screen.getByText(EDITOR.SAVE_BUTTON_LABEL, {selector: 'button'}),
    getCancelButton: () => screen.queryByText('Cancel'),
    getFormatSelect: () => screen.getByLabelText('Format'),
    getTypeSelect: () => screen.getByLabelText('Type'),
    getBlobStoreSelect: () => screen.getByLabelText('Blob Store'),
    getNameInput: () => screen.getByLabelText('Name'),
    getStatusCheckbox: () =>
      screen.getByRole('checkbox', {name: 'Online - Ready to connect'}),
    getContentValidationCheckbox: () =>
      screen.queryByRole('checkbox', {name: 'Enabled'}),
    getGroupSectionTitle: () => screen.queryByText('Group'),
    getMemberOption: (memberName) => screen.getByLabelText(memberName)
  };

  const renderView = (itemId = '') => {
    return render(<RepositoriesForm itemId={itemId} onDone={onDone} />);
  };

  const onDone = jest.fn();

  const RECIPES_RESPONSE = [
    {format: 'maven2', type: 'group'},
    {format: 'maven2', type: 'hosted'},
    {format: 'maven2', type: 'proxy'},
    {format: 'nuget', type: 'hosted'},
    {format: 'nuget', type: 'proxy'},
    {format: 'nuget', type: 'group'},
    {format: 'p2', type: 'proxy'}
  ];

  const BLOB_STORES_RESPONSE = [
    {name: 'default'},
    {name: 'blob-store-1'},
    {name: 'blob-store-2'}
  ];

  const BLOB_STORES_OPTIONS = [
    {name: EDITOR.SELECT_STORE_OPTION},
    ...BLOB_STORES_RESPONSE
  ];

  const FORMAT_OPTIONS = [
    {name: EDITOR.SELECT_FORMAT_OPTION},
    {name: 'maven2'},
    {name: 'nuget'},
    {name: 'p2'}
  ];

  const TYPE_OPTIONS = [
    {name: EDITOR.SELECT_TYPE_OPTION},
    {name: 'group'},
    {name: 'proxy'},
    {name: 'hosted'}
  ];

  const MAVEN_REPOS_RESPONSE = [
    {id: 'maven-central', name: 'maven-central'},
    {id: 'maven-releases', name: 'maven-releases'},
    {id: 'maven-snapshots', name: 'maven-snapshots'}
  ];

  when(axios.get)
    .calledWith(expect.stringContaining(repositoriesUrl('maven2')))
    .mockResolvedValue({data: MAVEN_REPOS_RESPONSE});

  when(axios.get)
    .calledWith(expect.stringContaining(recipesUrl))
    .mockResolvedValue({data: RECIPES_RESPONSE});

  when(axios.get)
    .calledWith(expect.stringContaining(blobStoresUrl))
    .mockResolvedValue({data: BLOB_STORES_RESPONSE});

  when(axios.post)
    .calledWith(expect.stringContaining(basePubUrl))
    .mockResolvedValue({data: {}});

  it('renders the form and populates dropdowns', async () => {
    renderView();

    await waitForElementToBeRemoved(selectors.queryLoadingMask());

    validateSelectOptions(selectors.getFormatSelect(), FORMAT_OPTIONS);

    expect(selectors.getTypeSelect()).toBeDisabled();

    await TestUtils.changeField(selectors.getFormatSelect, 'maven2');

    expect(selectors.getTypeSelect()).toBeEnabled();

    validateSelectOptions(selectors.getTypeSelect(), TYPE_OPTIONS);

    await TestUtils.changeField(selectors.getTypeSelect, 'group');

    validateSelectOptions(selectors.getBlobStoreSelect(), BLOB_STORES_OPTIONS);

    await waitFor(() =>
      expect(axios.get).toHaveBeenCalledWith(
        expect.stringContaining(repositoriesUrl('maven2'))
      )
    );

    MAVEN_REPOS_RESPONSE.forEach((repo) => {
      expect(selectors.getMemberOption(repo.name)).toBeInTheDocument();
    });
  });

  it('calls onDone when cancelled', async () => {
    renderView();

    await waitForElementToBeRemoved(selectors.queryLoadingMask());
    await TestUtils.changeField(selectors.getFormatSelect, 'maven2');
    await TestUtils.changeField(selectors.getTypeSelect, 'group');

    fireEvent.click(selectors.getCancelButton());
    await waitFor(() => expect(onDone).toHaveBeenCalled());
  });

  it('creates group repository', async () => {
    const format = 'maven2';
    const type = 'group';
    const url = repositoryUrl(format, type);
    const payload = {
      name: 'maven-group-1',
      online: false,
      storage: {
        blobStoreName: 'blob-store-1',
        strictContentTypeValidation: true
      },
      group: {
        memberNames: ['maven-releases', 'maven-snapshots']
      }
    };

    renderView();

    await waitForElementToBeRemoved(selectors.queryLoadingMask());

    await TestUtils.changeField(selectors.getFormatSelect, format);
    await TestUtils.changeField(selectors.getTypeSelect, type);

    expect(selectors.getCreateButton()).toHaveClass('disabled');

    await TestUtils.changeField(selectors.getNameInput, payload.name);
    fireEvent.click(selectors.getStatusCheckbox());
    await TestUtils.changeField(
      selectors.getBlobStoreSelect,
      payload.storage.blobStoreName
    );
    fireEvent.click(selectors.getMemberOption(payload.group.memberNames[0]));
    fireEvent.click(selectors.getMemberOption(payload.group.memberNames[1]));

    expect(selectors.getCreateButton()).not.toHaveClass('disabled');

    fireEvent.click(selectors.getCreateButton());
    await waitFor(() => expect(axios.post).toHaveBeenCalledWith(url, payload));
  });

  it('filters types by format and correct fields for group', async () => {
    when(axios.get)
      .calledWith(expect.stringContaining(repositoriesUrl('p2')))
      .mockResolvedValue({data: []});

    when(axios.get)
      .calledWith(expect.stringContaining(repositoriesUrl('nuget')))
      .mockResolvedValue({data: []});

    renderView();

    await waitForElementToBeRemoved(selectors.queryLoadingMask());

    await TestUtils.changeField(selectors.getFormatSelect, 'maven2');
    await TestUtils.changeField(selectors.getTypeSelect, 'group');
    expect(selectors.getGroupSectionTitle()).toBeInTheDocument();

    await TestUtils.changeField(selectors.getFormatSelect, 'nuget');
    expect(selectors.getTypeSelect()).toHaveValue('group');
    expect(selectors.getContentValidationCheckbox()).not.toBeInTheDocument();

    await TestUtils.changeField(selectors.getFormatSelect, 'p2');
    expect(selectors.getTypeSelect()).toHaveValue('');
    expect(selectors.getTypeSelect()).not.toContainElement(
      screen.queryByText('hosted')
    );
    expect(selectors.getTypeSelect()).not.toContainElement(
      screen.queryByText('group')
    );

    await TestUtils.changeField(selectors.getTypeSelect, 'proxy');
    expect(selectors.getContentValidationCheckbox()).toBeInTheDocument();
    expect(selectors.getGroupSectionTitle()).not.toBeInTheDocument();
  });
});

const validateSelectOptions = (selectElement, options) => {
  options.forEach((option) => {
    expect(getByRole(selectElement, 'option', option)).toBeInTheDocument();
  });
  expect(getAllByRole(selectElement, 'option')).toHaveLength(options.length);
};
