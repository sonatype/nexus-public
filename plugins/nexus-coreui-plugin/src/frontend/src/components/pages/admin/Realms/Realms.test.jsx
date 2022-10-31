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
} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {TestUtils, ExtJS, APIConstants} from '@sonatype/nexus-ui-plugin';
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

const { REALMS: LABELS, SETTINGS } = UIStrings;

const selectors = {
  ...TestUtils.selectors,
  discardButton: () => screen.getByText(SETTINGS.DISCARD_BUTTON_LABEL),
  saveButton: () => screen.getByText(SETTINGS.SAVE_BUTTON_LABEL),
  availableList: () =>
    screen.getByRole('group', { name: LABELS.CONFIGURATION.AVAILABLE_TITLE }),
  selectedList: () =>
    screen.getByRole('group', { name: LABELS.CONFIGURATION.SELECTED_TITLE }),
  title: () => screen.getByText(LABELS.CONFIGURATION.SELECTED_TITLE),
  emptyList: () => screen.getByText(LABELS.CONFIGURATION.EMPTY_LIST),
  allActiveItems: () =>
    selectors.selectedList().querySelectorAll('.nx-transfer-list__item'),
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
    const { queryLoadingMask, availableList, selectedList } = selectors;

    render(<Realms />);
    await waitForElementToBeRemoved(queryLoadingMask());

    expect(availableList()).toHaveTextContent(data[0].name);
    expect(availableList()).toHaveTextContent(data[1].name);
    expect(selectedList()).toHaveTextContent(data[2].name);
    expect(selectedList()).toHaveTextContent(data[3].name);
  });

  it('discards changes', async () => {
    const { queryLoadingMask, availableList, selectedList, discardButton } =
      selectors;

    render(<Realms />);

    await waitForElementToBeRemoved(queryLoadingMask());

    expect(availableList()).toHaveTextContent(data[0].name);
    expect(availableList()).toHaveTextContent(data[1].name);
    expect(selectedList()).toHaveTextContent(data[2].name);
    expect(selectedList()).toHaveTextContent(data[3].name);

    expect(discardButton()).toHaveClass('disabled');

    userEvent.click(screen.getByText(data[0].name));
    expect(selectedList()).toHaveTextContent(data[0].name);

    userEvent.click(screen.getByText(data[2].name));
    expect(availableList()).toHaveTextContent(data[2].name);

    expect(discardButton()).not.toHaveClass('disabled');

    userEvent.click(discardButton());

    expect(availableList()).toHaveTextContent(data[0].name);
    expect(selectedList()).toHaveTextContent(data[2].name);
  });

  it('edits the Realms Form', async () => {
    const { queryLoadingMask, availableList, selectedList, saveButton } =
      selectors;

    when(Axios.put)
      .calledWith(
        APIConstants.REST.PUBLIC.ACTIVE_REALMS,
        expect.objectContaining({ method: 'update' })
      )
      .mockResolvedValue({
        data: [{ active: [data[0].id, data[3].id] }],
      });

    render(<Realms />);

    await waitForElementToBeRemoved(queryLoadingMask());

    expect(saveButton()).toHaveClass('disabled');

    userEvent.click(screen.getByText(data[0].name));
    userEvent.click(screen.getByText(data[2].name));

    expect(selectedList()).toHaveTextContent(data[0].name);
    expect(availableList()).toHaveTextContent(data[2].name);

    expect(saveButton()).not.toHaveClass('disabled');

    await userEvent.click(saveButton());

    expect(Axios.put).toHaveBeenCalledWith(
      APIConstants.REST.PUBLIC.ACTIVE_REALMS,
      [data[3].id, data[0].id]
    );
  });

  it('allows reordering the active column', async () => {
    const { queryLoadingMask, allActiveItems } = selectors;

    const expected = 'nx-transfer-list__item--with-reordering';

    render(<Realms />);

    await waitForElementToBeRemoved(queryLoadingMask());

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
      queryLoadingMask,
      selectedList,
      availableList,
      saveButton,
      allActiveItems,
    } = selectors;

    render(<Realms />);

    await waitForElementToBeRemoved(queryLoadingMask());

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

    expect(saveButton()).not.toHaveClass('disabled');

    await userEvent.click(saveButton());

    expect(Axios.put).toHaveBeenCalledWith(
      APIConstants.REST.PUBLIC.ACTIVE_REALMS,
      data.map(({ id }) => id)
    );
  });

  it('do not allow save if there is not at least 1 realm is marked as active', async () => {
    const {
      queryLoadingMask,
      selectedList,
      allActiveItems,
      availableList,
      saveButton,
    } = selectors;

    render(<Realms />);

    await waitForElementToBeRemoved(queryLoadingMask());
    expect(selectedList()).toHaveTextContent(data[2].name);
    expect(selectedList()).toHaveTextContent(data[3].name);

    userEvent.click(screen.getByText(data[2].name));
    userEvent.click(screen.getByText(data[3].name));

    expect(availableList()).toHaveTextContent(data[2].name);
    expect(availableList()).toHaveTextContent(data[3].name);

    const activesRealms = allActiveItems();

    expect(activesRealms).toHaveLength(0);

    expect(saveButton()).toHaveClass('disabled');
    expect(saveButton()).toHaveAttribute(
      'title',
      LABELS.MESSAGES.NO_REALMS_CONFIGURED
    );
  });

  describe('Read Only Mode', () => {
    const listItemClass = 'nx-list__text';

    it('shows Realms configuration in Read Only mode', async () => {
      ExtJS.checkPermission.mockReturnValueOnce(false);

      const { queryLoadingMask, title } = selectors;

      render(<Realms />);

      await waitForElementToBeRemoved(queryLoadingMask());

      expect(title()).toBeInTheDocument();

      expect(screen.getByText(data[2].id)).toHaveClass(listItemClass);
    });

    it('Shows empty Realms page in Read Only mode', async () => {
      ExtJS.checkPermission.mockReturnValueOnce(false);

      const { queryLoadingMask, emptyList, title } = selectors;

      when(Axios.get)
        .calledWith(APIConstants.REST.PUBLIC.ACTIVE_REALMS)
        .mockResolvedValue({
          data: [],
        });

      render(<Realms />);

      await waitForElementToBeRemoved(queryLoadingMask());

      expect(title()).toBeInTheDocument();
      expect(emptyList()).toBeInTheDocument();
    });
  });
});
