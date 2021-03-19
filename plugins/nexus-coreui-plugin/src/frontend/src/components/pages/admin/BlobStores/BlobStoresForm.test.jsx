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
import axios from 'axios';
import {when} from 'jest-when';
import {fireEvent, waitFor, waitForElementToBeRemoved} from '@testing-library/react'
import userEvent from '@testing-library/user-event';
import '@testing-library/jest-dom/extend-expect';
import TestUtils from '@sonatype/nexus-ui-plugin/src/frontend/src/interface/TestUtils';

import BlobStoresForm from './BlobStoresForm';

jest.mock('axios', () => ({
  ...jest.requireActual('axios'), // Use most functions from actual axios
  get: jest.fn(),
  post: jest.fn(),
  put: jest.fn()
}));

const blobstoreTypes = {
  data: [{
    "id": "File",
    "name": "File",
    "fields": [
      {
        "helpText": "An absolute path or a path relative to <data-directory>/blobs",
        "id": "path",
        "regexValidation": null,
        "required": true,
        "disabled": false,
        "readOnly": false,
        "label": "Path",
        "attributes": {
          tokenReplacement: "/<data-directory>/blobs/${name}",
          "long": true
        },
        "type": "string",
        "allowAutocomplete": false
      }
    ]
  }, {
    "id": "Group",
    "name": "Group",
    "fields": [
      {
        "id": "members",
        "required": true,
        "label": "Members",
        "initialValue": null,
        "attributes": {
          "toTitle": "Selected",
          "fromTitle": "Available",
          "buttons": ["up", "add", "remove", "down"],
          "options": ["default", "test", "test-promoted"]
        },
        "type": "itemselect",
        "allowAutocomplete": false
      }, {
        "id": "fillPolicy",
        "required": true,
        "label": "Fill Policy",
        "initialValue": null,
        "attributes": {
          "options": ["Round Robin", "Write to First"]
        },
        "type": "combobox"
      }
    ]
  }]
};

const quotaTypes = {
  data: [
    {
      "id": "spaceRemainingQuota",
      "name": "Space Remaining"
    }, {
      "id": "spaceUsedQuota",
      "name": "Space Used"
    }
  ]
};

describe('BlobStoresForm', function() {
  const onDone = jest.fn();

  function render(itemId) {
    return TestUtils.render(<BlobStoresForm itemId={itemId || ''} onDone={onDone}/>,
        ({getByRole, getByLabelText, queryByLabelText, getByText, queryByText}) => ({
          title: () => getByRole('heading', {level: 1}),
          typeSelect: () => queryByLabelText('Type'),
          name: () => queryByLabelText('Name'),
          path: () => getByLabelText('Path'),
          availableMembers: () => queryByLabelText('Available'),
          selectedMembers: () => queryByLabelText('Selected'),
          softQuota: () => getByLabelText('Soft Quota'),
          softQuotaType: () => queryByLabelText('Constraint Type'),
          softQuotaLimit: () => queryByLabelText('Constraint Limit (in MB)'),
          saveButton: () => getByText('Save'),
          cancelButton: () => getByText('Cancel'),
          promoteToGroup: () => queryByText('Promote to Group')
        }));
  }

  it('renders the loading spinner', async function() {
    axios.get.mockReturnValue(new Promise(() => {}));

    const {loadingMask} = render();

    expect(loadingMask()).toBeInTheDocument();
  });

  it('renders the type selection for create', async function() {
    when(axios.get).calledWith('/service/rest/internal/ui/blobstores/types').mockResolvedValue(blobstoreTypes);
    when(axios.get).calledWith('/service/rest/internal/ui/blobstores/quotaTypes').mockResolvedValue(quotaTypes);

    const {loadingMask, typeSelect} = render();

    await waitForElementToBeRemoved(loadingMask);

    expect(typeSelect().options.length).toBe(3);
    expect(Array.from(typeSelect().options).map(option => option.textContent)).toEqual(expect.arrayContaining([
        '',
        'File',
        'Group'
    ]));
    expect(typeSelect()).toHaveValue('');
  });

  it ('renders the form and buttons when the File type is selected', async function() {
    when(axios.get).calledWith('/service/rest/internal/ui/blobstores/types').mockResolvedValue(blobstoreTypes);
    when(axios.get).calledWith('/service/rest/internal/ui/blobstores/quotaTypes').mockResolvedValue(quotaTypes);

    const {container, loadingMask, typeSelect} = render();

    await waitForElementToBeRemoved(loadingMask);

    userEvent.selectOptions(typeSelect(), 'File');
    expect(typeSelect()).toHaveValue('File');

    expect(container).toMatchSnapshot();
  });

  it ('renders the name field and dynamic path field when the File type is selected', async function() {
    when(axios.get).calledWith('/service/rest/internal/ui/blobstores/types').mockResolvedValue(blobstoreTypes);
    when(axios.get).calledWith('/service/rest/internal/ui/blobstores/quotaTypes').mockResolvedValue(quotaTypes);

    const {loadingMask, typeSelect, name, path} = render();

    await waitForElementToBeRemoved(loadingMask);

    userEvent.selectOptions(typeSelect(), 'File');
    expect(typeSelect()).toHaveValue('File');

    expect(name()).toBeInTheDocument();

    expect(path()).toBeInTheDocument();
    expect(path()).toHaveValue('/<data-directory>/blobs/');
  });

  it ('renders the soft quota fields when the blobstore type is selected', async function() {
    when(axios.get).calledWith('/service/rest/internal/ui/blobstores/types').mockResolvedValue(blobstoreTypes);
    when(axios.get).calledWith('/service/rest/internal/ui/blobstores/quotaTypes').mockResolvedValue(quotaTypes);

    const {loadingMask, typeSelect, softQuota, softQuotaType, softQuotaLimit, getByText} = render();

    await waitForElementToBeRemoved(loadingMask);

    userEvent.selectOptions(typeSelect(), 'File');
    expect(typeSelect()).toHaveValue('File');

    expect(softQuota()).toBeInTheDocument();
    expect(softQuotaType()).not.toBeInTheDocument();
    expect(softQuotaLimit()).not.toBeInTheDocument();

    fireEvent.click(softQuota());

    expect(softQuotaType()).toBeInTheDocument();
    expect(softQuotaLimit()).toBeInTheDocument();
  });

  it('enables the save button when there are changes', async function() {
    when(axios.get).calledWith('/service/rest/internal/ui/blobstores/types').mockResolvedValue(blobstoreTypes);
    when(axios.get).calledWith('/service/rest/internal/ui/blobstores/quotaTypes').mockResolvedValue(quotaTypes);

    const {loadingMask, typeSelect, name, softQuota, softQuotaType, softQuotaLimit, saveButton} = render();

    await waitForElementToBeRemoved(loadingMask);

    userEvent.selectOptions(typeSelect(), 'File');
    expect(typeSelect()).toHaveValue('File');

    expect(saveButton()).toHaveClass('disabled');

    userEvent.type(name(), 'test');
    expect(name()).toHaveValue('test');

    expect(saveButton()).not.toHaveClass('disabled');

    fireEvent.click(softQuota());

    expect(saveButton()).toHaveClass('disabled');

    userEvent.selectOptions(softQuotaType(), 'spaceRemainingQuota');
    expect(softQuotaType()).toHaveValue('spaceRemainingQuota');
    userEvent.type(softQuotaLimit(), '100');
    expect(softQuotaLimit()).toHaveValue('100');

    expect(saveButton()).not.toHaveClass('disabled');

    userEvent.clear(softQuotaLimit());
    expect(softQuotaLimit()).toHaveValue('');

    expect(saveButton()).toHaveClass('disabled');

    fireEvent.click(softQuota());

    expect(saveButton()).not.toHaveClass('disabled');
  });

  it('creates a new file blob store', async function() {
    when(axios.get).calledWith('/service/rest/internal/ui/blobstores/types').mockResolvedValue(blobstoreTypes);
    when(axios.get).calledWith('/service/rest/internal/ui/blobstores/quotaTypes').mockResolvedValue(quotaTypes);

    const {loadingMask, typeSelect, name, path, softQuota, softQuotaType, softQuotaLimit, saveButton} = render();

    await waitForElementToBeRemoved(loadingMask);

    userEvent.selectOptions(typeSelect(), 'File');
    expect(typeSelect()).toHaveValue('File');
    userEvent.type(name(), 'test');
    expect(name()).toHaveValue('test');
    expect(path()).toHaveValue('/<data-directory>/blobs/test');
    userEvent.clear(path());
    userEvent.type(path(), 'testPath');
    expect(path()).toHaveValue('testPath');
    userEvent.click(softQuota());
    userEvent.selectOptions(softQuotaType(), 'spaceRemainingQuota');
    expect(softQuotaType()).toHaveValue('spaceRemainingQuota');
    userEvent.type(softQuotaLimit(), '100');
    expect(softQuotaLimit()).toHaveValue('100');
    userEvent.click(saveButton());

    expect(axios.post).toHaveBeenCalledWith(
        '/service/rest/v1/blobstores/file',
        {
          name: 'test',
          path: 'testPath',
          softQuota: {
            enabled: true,
            type: 'spaceRemainingQuota',
            limit: '100'
          }
        }
    );
  });

  it('edits a file blob store', async function() {
    when(axios.get).calledWith('/service/rest/internal/ui/blobstores/types').mockResolvedValue(blobstoreTypes);
    when(axios.get).calledWith('/service/rest/internal/ui/blobstores/quotaTypes').mockResolvedValue(quotaTypes);
    when(axios.get).calledWith('/service/rest/v1/blobstores/file/test').mockResolvedValue({
      data: {
        path: 'testPath',
        softQuota: {
          type: 'spaceRemainingQuota',
          limit: '100'
        }
      }
    });

    const {
      loadingMask,
      promoteToGroup
    } = render('file/test');

    await waitForElementToBeRemoved(loadingMask);

    expect(promoteToGroup()).toBeInTheDocument();
  });

  it('edits a file blob store', async function() {
    when(axios.get).calledWith('/service/rest/internal/ui/blobstores/types').mockResolvedValue(blobstoreTypes);
    when(axios.get).calledWith('/service/rest/internal/ui/blobstores/quotaTypes').mockResolvedValue(quotaTypes);
    when(axios.get).calledWith('/service/rest/v1/blobstores/file/test').mockResolvedValue({
      data: {
        path: 'testPath',
        softQuota: {
          type: 'spaceRemainingQuota',
          limit: '100'
        }
      }
    });

    const {
      getByText,
      loadingMask,
      name,
      path,
      title,
      softQuota,
      softQuotaType,
      softQuotaLimit,
      typeSelect
    } = render('file/test');

    await waitForElementToBeRemoved(loadingMask);

    expect(title()).toHaveTextContent('Edit test');
    expect(getByText('File Blob Store')).toBeInTheDocument();

    // The type and name fields cannot be changed during edit
    expect(typeSelect()).not.toBeInTheDocument();
    expect(name()).not.toBeInTheDocument();

    expect(path()).toHaveValue('testPath');
    expect(softQuota()).toBeChecked();
    expect(softQuotaType()).toHaveValue('spaceRemainingQuota');
    expect(softQuotaLimit()).toHaveValue('100');
  });

  it('edits a group blob store', async function() {
    when(axios.get).calledWith('/service/rest/internal/ui/blobstores/types').mockResolvedValue(blobstoreTypes);
    when(axios.get).calledWith('/service/rest/internal/ui/blobstores/quotaTypes').mockResolvedValue(quotaTypes);
    when(axios.get).calledWith('/service/rest/v1/blobstores/group/test').mockResolvedValue({
      data: {
        "softQuota" : null,
        "members" : [ "test-promoted" ],
        "fillPolicy" : "writeToFirst"
      }
    });

    const {
      availableMembers,
      getByText,
      loadingMask,
      name,
      selectedMembers,
      title,
      typeSelect
    } = render('group/test');

    await waitForElementToBeRemoved(loadingMask);

    expect(title()).toHaveTextContent('Edit test');
    expect(getByText('Group Blob Store')).toBeInTheDocument();

    expect(typeSelect()).not.toBeInTheDocument();
    expect(name()).not.toBeInTheDocument();

    expect(availableMembers()).toContainElement(getByText('default'));
    expect(selectedMembers()).toContainElement(getByText('test-promoted'));
  });

  it('promote to group is not shown when editing a group', async function() {
    when(axios.get).calledWith('/service/rest/internal/ui/blobstores/types').mockResolvedValue(blobstoreTypes);
    when(axios.get).calledWith('/service/rest/internal/ui/blobstores/quotaTypes').mockResolvedValue(quotaTypes);
    when(axios.get).calledWith('/service/rest/v1/blobstores/group/test').mockResolvedValue({
      data: {
        "softQuota" : null,
        "members" : [ "test-promoted" ],
        "fillPolicy" : "writeToFirst"
      }
    });

    const {
      loadingMask,
      promoteToGroup
    } = render('group/test');

    await waitForElementToBeRemoved(loadingMask);

    expect(promoteToGroup()).not.toBeInTheDocument();
  });
});
