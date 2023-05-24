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
  render,
  screen,
  waitForElementToBeRemoved,
  within,
} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {ExtJS, APIConstants} from '@sonatype/nexus-ui-plugin';
import TestUtils from '@sonatype/nexus-ui-plugin/src/frontend/src/interface/TestUtils';
import {when} from 'jest-when';
import UIStrings from '../../../../constants/UIStrings';
import Axios from 'axios';
import Realms from './Realms';

jest.mock('axios', () => ({
  ...jest.requireActual('axios'),
  get: jest.fn(),
  put: jest.fn(),
}));

jest.mock('@sonatype/nexus-ui-plugin', () => {
  return {
    ...jest.requireActual('@sonatype/nexus-ui-plugin'),
    ExtJS: {
      showSuccessMessage: jest.fn(),
      showErrorMessage: jest.fn(),
      checkPermission: jest.fn().mockReturnValue(true),
    },
  };
});

const {REALMS: LABELS} = UIStrings;

const selectors = {
  ...TestUtils.selectors,
  ...TestUtils.formSelectors,
  availableList: () => screen.getByRole('group', {name: LABELS.CONFIGURATION.AVAILABLE_TITLE}),
  selectedList: () => screen.getByRole('group', {name: LABELS.CONFIGURATION.SELECTED_TITLE}),
  title: () => screen.getByText(LABELS.CONFIGURATION.SELECTED_TITLE),
  emptyList: () => screen.getByText(LABELS.CONFIGURATION.EMPTY_LIST),
  allActiveItems: () => selectors.selectedList().querySelectorAll('.nx-transfer-list__item'),

  localRealmRemovalModal: {
    modal: () => screen.getByRole('dialog', {name: LABELS.LOCAL_REALM_REMOVAL_MODAL.HEADER}),
    input: () => within(selectors.localRealmRemovalModal.modal()).getByRole('textbox'),
    confirmBtn: () => within(selectors.localRealmRemovalModal.modal()).getByRole('button', {name: 'Confirm'}),
    validationError: () => within(selectors.localRealmRemovalModal.modal()).queryByRole('alert')
  }
};

describe('Realms', () => {
  const data = [
    {
      id: 'ConanToken',
      name: 'Conan Bearer Token Realm',
    },
    {
      id: 'Crowd',
      name: 'Crowd Realm',
    },
    {
      id: 'NexusAuthenticatingRealm',
      name: 'Local Authenticating Realm',
    },
    {
      id: 'NexusAuthorizingRealm',
      name: 'Local Authorizing Realm',
    },
  ];

  const renderAndWaitForLoad = async () => {
    render(<Realms />);
    await waitForElementToBeRemoved(selectors.queryLoadingMask());
  }

  beforeEach(() => {
    when(Axios.get)
      .calledWith(APIConstants.REST.PUBLIC.ACTIVE_REALMS)
      .mockResolvedValue({
        data: [data[2].id, data[3].id],
      });
    when(Axios.get)
      .calledWith(APIConstants.REST.PUBLIC.AVAILABLE_REALMS)
      .mockResolvedValue({
        data,
      });
  });

  it('renders the resolved data', async () => {
    const {availableList, selectedList} = selectors;

    await renderAndWaitForLoad();

    expect(availableList()).toHaveTextContent(data[0].name);
    expect(availableList()).toHaveTextContent(data[1].name);
    expect(selectedList()).toHaveTextContent(data[2].name);
    expect(selectedList()).toHaveTextContent(data[3].name);
  });

  it('discards changes', async () => {
    const {availableList, selectedList, queryDiscardButton} = selectors;

    await renderAndWaitForLoad();

    expect(availableList()).toHaveTextContent(data[0].name);
    expect(availableList()).toHaveTextContent(data[1].name);
    expect(selectedList()).toHaveTextContent(data[2].name);
    expect(selectedList()).toHaveTextContent(data[3].name);

    expect(queryDiscardButton()).toHaveClass('disabled');

    userEvent.click(screen.getByText(data[0].name));
    expect(selectedList()).toHaveTextContent(data[0].name);

    userEvent.click(screen.getByText(data[2].name));
    expect(availableList()).toHaveTextContent(data[2].name);

    expect(queryDiscardButton()).not.toHaveClass('disabled');

    userEvent.click(queryDiscardButton());

    expect(availableList()).toHaveTextContent(data[0].name);
    expect(selectedList()).toHaveTextContent(data[2].name);
  });

  it('edits the Realms Form', async () => {
    const {selectedList, querySubmitButton} = selectors;

    const ldapRealm = {
      id: 'Ldap',
      name: 'LDAP Realm',
    };

    when(Axios.get)
      .calledWith(APIConstants.REST.PUBLIC.AVAILABLE_REALMS)
      .mockResolvedValueOnce({
        data: [...data, ldapRealm]
    });

    when(Axios.get)
      .calledWith(APIConstants.REST.PUBLIC.ACTIVE_REALMS)
      .mockResolvedValueOnce({
        data: [data[2].id, data[3].id, ldapRealm.id],
    });

    when(Axios.put)
      .calledWith(
        APIConstants.REST.PUBLIC.ACTIVE_REALMS,
        expect.objectContaining({ method: 'update' })
      )
      .mockResolvedValue({
        data: [{active: [data[0].id, data[3].id]}],
      });

    await renderAndWaitForLoad();

    userEvent.click(screen.getByText(data[0].name));
    userEvent.click(screen.getByText(ldapRealm.name));

    expect(selectedList()).toHaveTextContent(data[0].name);

    expect(querySubmitButton()).not.toHaveClass('disabled');

    await userEvent.click(querySubmitButton());

    expect(Axios.put).toHaveBeenCalledWith(
      APIConstants.REST.PUBLIC.ACTIVE_REALMS,
      [data[2].id, data[3].id, data[0].id]
    );
  });

  it('allows reordering the active column', async () => {
    const {allActiveItems} = selectors;
    const expected = 'nx-transfer-list__item--with-reordering';

    await renderAndWaitForLoad();

    const activesRealms = allActiveItems();

    expect(activesRealms.length).toBeGreaterThan(0);

    activesRealms.forEach((item) => {
      expect(item).toHaveClass(expected);

      const actionButtons = item.querySelectorAll('.nx-btn');
      expect(actionButtons).toHaveLength(2);
    });
  });

  it('saves the active realms in the corresponding order', async () => {
    const {
      selectedList,
      availableList,
      querySubmitButton,
      allActiveItems,
    } = selectors;

    await renderAndWaitForLoad();

    expect(availableList()).toHaveTextContent(data[0].name);
    expect(availableList()).toHaveTextContent(data[1].name);
    expect(selectedList()).toHaveTextContent(data[2].name);
    expect(selectedList()).toHaveTextContent(data[3].name);

    // Moves elements to the selected list.
    userEvent.click(screen.getByText(data[0].name));
    userEvent.click(screen.getByText(data[1].name));

    expect(selectedList()).toHaveTextContent(data[0].name);
    expect(selectedList()).toHaveTextContent(data[1].name);

    const previousActivesRealms = allActiveItems();

    expect(previousActivesRealms).toHaveLength(4);

    const initialOrder = [data[2], data[3], data[0], data[1]];

    // Checks initial orders.
    previousActivesRealms.forEach((item, index) => {
      expect(item).toHaveTextContent(initialOrder[index].name);
    });

    // Reorders the elements.
    const secondElement = previousActivesRealms[1];
    const secondElementButtons = secondElement.querySelectorAll('.nx-btn');
    const secondElementMoveDownButton = secondElementButtons[1];

    await userEvent.dblClick(secondElementMoveDownButton);

    const firstElement = previousActivesRealms[0];
    const firstElementButtons = firstElement.querySelectorAll('.nx-btn');
    const firstElementMoveDownButton = firstElementButtons[1];

    await userEvent.dblClick(firstElementMoveDownButton);

    const currentActivesRealms = allActiveItems();

    expect(currentActivesRealms).toHaveLength(4);

    // Checks new order which should be the same as the data object.
    currentActivesRealms.forEach((item, index) => {
      expect(item).toHaveTextContent(data[index].name);
    });

    expect(querySubmitButton()).not.toHaveClass('disabled');

    await userEvent.click(querySubmitButton());

    expect(Axios.put).toHaveBeenCalledWith(
      APIConstants.REST.PUBLIC.ACTIVE_REALMS,
      data.map(({ id }) => id)
    );
  });

  it('do not allow save if there is not at least 1 realm is marked as active', async () => {
    const {
      selectedList,
      allActiveItems,
      availableList,
      queryFormError,
    } = selectors;

    await renderAndWaitForLoad();

    expect(selectedList()).toHaveTextContent(data[2].name);
    expect(selectedList()).toHaveTextContent(data[3].name);

    userEvent.click(screen.getByText(data[2].name));
    userEvent.click(screen.getByText(data[3].name));

    expect(availableList()).toHaveTextContent(data[2].name);
    expect(availableList()).toHaveTextContent(data[3].name);

    const activesRealms = allActiveItems();
    expect(activesRealms).toHaveLength(0);
    expect(queryFormError(LABELS.MESSAGES.NO_REALMS_CONFIGURED)).toBeInTheDocument();
  });

  it('shows confirmation modal on attempt to remove active local realms', async () => {
    const {
      querySubmitButton,
      localRealmRemovalModal: {
        modal,
        input,
        confirmBtn,
        validationError
      }
    } = selectors;

    const {
      LOCAL_REALM_REMOVAL_MODAL: {
        ACKNOWLEDGEMENT: {
          STRING,
          VALIDATION_ERROR
        }
      }
    } = LABELS

    await renderAndWaitForLoad();

    userEvent.click(screen.getByText(data[2].name));

    userEvent.click(querySubmitButton());

    expect(modal()).toBeVisible();
    expect(confirmBtn()).toBeDisabled();

    await TestUtils.changeField(input, 'wrongstring');
    expect(validationError()).toHaveTextContent(VALIDATION_ERROR);
    expect(confirmBtn()).toBeDisabled();

    await TestUtils.changeField(input, '');
    expect(validationError()).toHaveTextContent(VALIDATION_ERROR);
    expect(confirmBtn()).toBeDisabled();

    await TestUtils.changeField(input, STRING);
    expect(validationError()).not.toBeInTheDocument();
    expect(confirmBtn()).toBeEnabled();
    
    userEvent.click(confirmBtn());

    expect(Axios.put).toHaveBeenCalledWith(
      APIConstants.REST.PUBLIC.ACTIVE_REALMS,
      [data[3].id]
    );
  });

  describe('Read Only Mode', () => {
    const listItemClass = 'nx-list__text';

    it('shows Realms configuration in Read Only mode', async () => {
      const {title} = selectors;
      ExtJS.checkPermission.mockReturnValueOnce(false);

      await renderAndWaitForLoad();

      expect(title()).toBeInTheDocument();

      expect(screen.getByText(data[2].id)).toHaveClass(listItemClass);
    });

    it('Shows empty Realms page in Read Only mode', async () => {
      ExtJS.checkPermission.mockReturnValueOnce(false);

      const {emptyList, title} = selectors;

      when(Axios.get)
        .calledWith(APIConstants.REST.PUBLIC.ACTIVE_REALMS)
        .mockResolvedValue({
          data: [],
        });

      await renderAndWaitForLoad();

      expect(title()).toBeInTheDocument();
      expect(emptyList()).toBeInTheDocument();
    });
  });
});
