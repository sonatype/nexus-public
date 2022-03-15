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
import {render, screen, waitForElementToBeRemoved, waitFor, fireEvent} from '@testing-library/react';
import '@testing-library/jest-dom/extend-expect';
import {act} from "react-dom/test-utils";
import {when} from 'jest-when';
import TestUtils from '@sonatype/nexus-ui-plugin/src/frontend/src/interface/TestUtils';
import {ExtJS} from '@sonatype/nexus-ui-plugin';

import Axios from 'axios';
import ProprietaryRepositories from './ProprietaryRepositories';
import UIStrings from "../../../../constants/UIStrings";

const {PROPRIETARY_REPOSITORIES: LABELS, SETTINGS} = UIStrings;

jest.mock('@sonatype/nexus-ui-plugin', () => {
  return {
    ...jest.requireActual('@sonatype/nexus-ui-plugin'),
    ExtJS: {
      showSuccessMessage: jest.fn(),
      showErrorMessage: jest.fn(),
      checkPermission: jest.fn().mockReturnValue(true),
    }
  }
});

jest.mock('axios', () => {  // Mock out parts of axios, has to be done in same scope as import statements
  return {
    ...jest.requireActual('axios'), // Use most functions from actual axios
    post: jest.fn()
  };
});

global.NX = {
  Permissions: {
    check: jest.fn(() => true)
  },
  Messages: { success: () => {} }
}

const selectors = {
  ...TestUtils.selectors,
  discardButton: () => screen.getByText(SETTINGS.DISCARD_BUTTON_LABEL),
  saveButton: () => screen.getByText(SETTINGS.SAVE_BUTTON_LABEL),
  availableList: () => screen.getByRole('group', {name: LABELS.CONFIGURATION.AVAILABLE_TITLE}),
  selectedList: () => screen.getByRole('group', {name: LABELS.CONFIGURATION.SELECTED_TITLE}),
};

const ALL_REPOS_REQUEST = {
  action: 'coreui_ProprietaryRepositories',
  data: null,
  method: 'readPossibleRepos',
  tid: 1,
  type: 'rpc',
};

const ALL_REPOS_RESPONSE = {
  action: "coreui_ProprietaryRepositories",
  method: "readPossibleRepos",
  tid: 1,
  type: "rpc",
  result: {
    data: [
      {
        id: "maven-releases",
        name: "maven-releases",
      }, {
        id: "maven-snapshots",
        name: "maven-snapshots",
      }, {
        id: "nuget-hosted",
        name: "nuget-hosted",
      },
    ],
    success: true,
  }
};

const ENABLED_REPOS_REQUEST = {
  action: 'coreui_ProprietaryRepositories',
  data: null,
  method: 'read',
  tid: 1,
  type: 'rpc',
};

const ENABLED_REPOS_RESPONSE = {
  action: "coreui_ProprietaryRepositories",
  method: "read",
  tid: 1,
  type: "rpc",
  result: {
    data: {
      enabledRepositories: ['nuget-hosted'],
    },
    success: true,
  }
};

describe('ProprietaryRepositories', () => {
  beforeEach(() => {
    when(Axios.post).calledWith('/service/extdirect', ALL_REPOS_REQUEST).mockResolvedValue({
      data: ALL_REPOS_RESPONSE
    });
    when(Axios.post).calledWith('/service/extdirect', ENABLED_REPOS_REQUEST).mockResolvedValue({
      data: ENABLED_REPOS_RESPONSE
    });
  });

  it('renders the resolved data', async () => {
    render(<ProprietaryRepositories/>);

    await waitForElementToBeRemoved(selectors.queryLoadingMask());

    expect(selectors.availableList()).toHaveTextContent('maven-releases');
    expect(selectors.selectedList()).not.toHaveTextContent('maven-releases');
    expect(selectors.availableList()).toHaveTextContent('maven-snapshots');
    expect(selectors.selectedList()).toHaveTextContent('nuget-hosted');
  });

  it('discards changes', async () => {
    render(<ProprietaryRepositories/>);
    await waitFor(() => expect(Axios.post).toHaveBeenCalledTimes(2));

    expect(selectors.availableList()).toHaveTextContent('maven-snapshots');
    expect(selectors.availableList()).toHaveTextContent('maven-releases');
    expect(selectors.selectedList()).toHaveTextContent('nuget-hosted');

    expect(selectors.discardButton()).toHaveClass('disabled');

    fireEvent.click(screen.getByText('maven-snapshots'));
    fireEvent.click(screen.getByText('nuget-hosted'));

    expect(selectors.selectedList()).toHaveTextContent('maven-snapshots');
    expect(selectors.availableList()).toHaveTextContent('maven-releases');
    expect(selectors.availableList()).toHaveTextContent('nuget-hosted');

    expect(selectors.discardButton()).not.toHaveClass('disabled');
    fireEvent.click(selectors.discardButton());

    expect(selectors.availableList()).toHaveTextContent('maven-snapshots');
    expect(selectors.availableList()).toHaveTextContent('maven-releases');
    expect(selectors.selectedList()).toHaveTextContent('nuget-hosted');
  });

  it('edits the Proprietary Repositories Form', async () => {
    when(Axios.post).calledWith('/service/extdirect', expect.objectContaining({method: 'update'}))
      .mockResolvedValue({
        data: {
          action: "coreui_ProprietaryRepositories",
          method: "update",
          tid: 1,
          type: "rpc",
          result: {
            data: {
              enabledRepositories: ['nuget-hosted'],
            },
            success: true,
          }
        }
      });

    await act(async () => {
      render(<ProprietaryRepositories/>);
    });

    await waitFor(() => expect(Axios.post).toHaveBeenCalledTimes(2));

    expect(selectors.saveButton()).toHaveClass('disabled');

    fireEvent.click(screen.getByText('maven-releases'));
    fireEvent.click(screen.getByText('nuget-hosted'));

    expect(selectors.availableList()).toHaveTextContent('nuget-hosted');
    expect(selectors.availableList()).toHaveTextContent('maven-snapshots');
    expect(selectors.selectedList()).toHaveTextContent('maven-releases');

    expect(selectors.saveButton()).not.toHaveClass('disabled');

    await act(async () => {fireEvent.click(selectors.saveButton())});

    expect(Axios.post).toHaveBeenCalledWith('/service/extdirect', {
      action: 'coreui_ProprietaryRepositories',
      data: [{enabledRepositories: ['maven-releases']}],
      method: 'update',
      tid: 1,
      type: 'rpc',
    });
  });

  describe('Read Only Mode', () => {
    const listItemClass = 'nx-list__text';

    it('shows Proprietary Repositories configuration in Read Only mode', async () => {
      ExtJS.checkPermission.mockReturnValueOnce(false);

      await act(async () => {
        render(<ProprietaryRepositories/>);
      });

      await waitFor(() => expect(Axios.post).toHaveBeenCalledTimes(2));

      expect(screen.getByText(LABELS.CONFIGURATION.SELECTED_TITLE)).toBeInTheDocument();

      expect(screen.getByText('nuget-hosted')).toHaveClass(listItemClass);
    });

    it('Shows empty Proprietary Repositories page in Read Only mode', async () => {
      ExtJS.checkPermission.mockReturnValueOnce(false);

      when(Axios.post).calledWith('/service/extdirect', ENABLED_REPOS_REQUEST).mockResolvedValue({
        data: {
          ...ENABLED_REPOS_RESPONSE,
          result: {
            ...ENABLED_REPOS_RESPONSE.result,
            data: {enabledRepositories: []},
          }
        }
      });

      await act(async () => {
        render(<ProprietaryRepositories/>);
      });
      await waitFor(() => expect(Axios.post).toHaveBeenCalledTimes(2));

      expect(screen.getByText(LABELS.CONFIGURATION.SELECTED_TITLE)).toBeInTheDocument();
      expect(screen.getByText(LABELS.CONFIGURATION.EMPTY_LIST)).toBeInTheDocument();
    });
  })
});
