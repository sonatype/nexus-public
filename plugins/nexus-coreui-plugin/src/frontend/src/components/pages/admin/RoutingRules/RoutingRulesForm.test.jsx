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
import {act} from 'react-dom/test-utils';
import {waitFor, waitForElementToBeRemoved, within, screen} from '@testing-library/react';
import {when} from 'jest-when';
import userEvent from '@testing-library/user-event';

import {ExtJS} from '@sonatype/nexus-ui-plugin';
import TestUtils from '@sonatype/nexus-ui-plugin/src/frontend/src/interface/TestUtils';

import UIStrings from '../../../../constants/UIStrings';
import RoutingRulesForm from './RoutingRulesForm';

jest.mock('axios', () => ({
  ...jest.requireActual('axios'), // Use most functions from actual axios
  get: jest.fn(),
  put: jest.fn(),
  post: jest.fn(),
  delete: jest.fn()
}));

jest.mock('@sonatype/nexus-ui-plugin', () => ({
  ...jest.requireActual('@sonatype/nexus-ui-plugin'),
  ExtJS: {
    requestConfirmation: jest.fn(),
    showErrorMessage: jest.fn()
  },
  Utils: {
    buildFormMachine: function(args) {
      const machine = jest.requireActual('@sonatype/nexus-ui-plugin').Utils.buildFormMachine(args);
      return machine.withConfig({
        actions: {
          logSaveSuccess: jest.fn(),
          logSaveError: jest.fn(),
          logLoadError: jest.fn()
        }
      })
    },
    isInvalid: jest.requireActual('@sonatype/nexus-ui-plugin').Utils.isInvalid,
    isBlank: jest.requireActual('@sonatype/nexus-ui-plugin').Utils.isBlank,
    notBlank: jest.requireActual('@sonatype/nexus-ui-plugin').Utils.notBlank,
    fieldProps: jest.requireActual('@sonatype/nexus-ui-plugin').Utils.fieldProps,
    saveTooltip: jest.requireActual('@sonatype/nexus-ui-plugin').Utils.saveTooltip
  }
}));

const selectors = {
  ...TestUtils.selectors,
  ...TestUtils.formSelectors,
};

describe('RoutingRulesForm', function() {
  const CONFIRM = Promise.resolve();
  const onDone = jest.fn();
  const ROUTING_RULES_URL = (name) => `/service/rest/internal/ui/routing-rules/${name}`;

  function renderEditView(itemId) {
    return renderView(<RoutingRulesForm itemId={itemId} onDone={onDone}/>);
  }

  function renderCreateView() {
    return renderView(<RoutingRulesForm onDone={onDone} />);
  }

  function renderView(view) {
    return TestUtils.render(view, ({queryByLabelText, queryByText}) => ({
      name: () => queryByLabelText(UIStrings.ROUTING_RULES.FORM.NAME_LABEL),
      description: () => queryByLabelText(UIStrings.ROUTING_RULES.FORM.DESCRIPTION_LABEL),
      mode: () => queryByLabelText(UIStrings.ROUTING_RULES.FORM.MODE_LABEL),
      matcher: (index) => queryByLabelText(UIStrings.ROUTING_RULES.FORM.MATCHER_LABEL(index)),
      matcherButton: (index) => within(screen.getByText('Matchers').closest('.nx-form-group')).getAllByRole('button')[index],
      createButton: () => queryByText(UIStrings.ROUTING_RULES.FORM.CREATE_BUTTON, {selector: '.nx-btn'}),
      saveButton: () => queryByText(UIStrings.SETTINGS.SAVE_BUTTON_LABEL),
      cancelButton: () => queryByText(UIStrings.SETTINGS.CANCEL_BUTTON_LABEL),
      deleteButton: () => queryByText(UIStrings.SETTINGS.DELETE_BUTTON_LABEL)
    }));
  }

  it('renders the resolved data', async function() {
    const itemId = 'allow-all';

    axios.get.mockResolvedValue({
      data: {
        name: 'allow-all',
        description: 'Allow all requests',
        mode: 'ALLOW',
        matchers: ['.*']
      }
    });

    const {loadingMask, name, description, mode, matcher} = renderEditView(itemId);

    await waitForElementToBeRemoved(loadingMask);

    expect(name()).toHaveValue('allow-all');
    expect(description()).toHaveValue('Allow all requests');
    expect(mode()).toHaveValue('ALLOW');
    expect(matcher(0)).toHaveValue('.*');
    expect(selectors.queryFormError(TestUtils.NO_CHANGES_MESSAGE)).toBeInTheDocument();
  });

  it('renders an error message', async function() {
    axios.get.mockRejectedValue({message: 'Error'});

    const {container, loadingMask} = renderEditView('itemId');

    await waitForElementToBeRemoved(loadingMask);

    expect(container.querySelector('.nx-alert--error')).toHaveTextContent('Error');
  });

  it('renders an error message when saving an invalid field', async function() {
    const {name, matcher, createButton, getByText} = renderCreateView();

    await waitForElementToBeRemoved(selectors.queryLoadingMask());

    await TestUtils.changeField(name, 'newValue');
    await TestUtils.changeField(() => matcher(0), '.*');
    expect(screen.queryByRole('alert')).not.toBeInTheDocument();

    when(axios.post).calledWith('/service/rest/internal/ui/routing-rules/', expect.any(Object)).mockRejectedValue({
      response: {
        data: [
          {
            "id": "name",
            "message": "A rule with the same name already exists. Name must be unique."
          }
        ]
      }
    });

    userEvent.click(createButton());
    await waitForElementToBeRemoved(selectors.querySavingMask());
    expect(getByText('A rule with the same name already exists. Name must be unique.')).toBeInTheDocument();
  });

  it('requires the name, description, and at least one matcher', async function() {
    axios.get.mockResolvedValue({data: []});

    const {name, description, matcher} = renderCreateView();

    await waitForElementToBeRemoved(selectors.queryLoadingMask());

    expect(selectors.queryFormError(TestUtils.NO_CHANGES_MESSAGE)).toBeInTheDocument();
    await TestUtils.changeField(name, '');
    await TestUtils.changeField(description, '');
    await TestUtils.changeField(() => matcher(0), '.*');
    expect(selectors.queryFormError(TestUtils.VALIDATION_ERRORS_MESSAGE)).toBeInTheDocument();

    await TestUtils.changeField(name, 'name');
    await TestUtils.changeField(description, '')
    await TestUtils.changeField(() => matcher(0), '');
    expect(selectors.queryFormError(TestUtils.VALIDATION_ERRORS_MESSAGE)).toBeInTheDocument();

    await TestUtils.changeField(name, '');
    await TestUtils.changeField(description, 'description');
    await TestUtils.changeField(() => matcher(0), '');
    expect(selectors.queryFormError(TestUtils.VALIDATION_ERRORS_MESSAGE)).toBeInTheDocument();

    await TestUtils.changeField(name, 'name');
    await TestUtils.changeField(description, 'description');
    await TestUtils.changeField(() => matcher(0), '.*');
    expect(screen.queryByRole('alert')).not.toBeInTheDocument();
  });

  it('fires onDone when cancelled', async function() {
    const {loadingMask, cancelButton} = renderCreateView();

    await waitForElementToBeRemoved(loadingMask);

    userEvent.click(cancelButton());

    await waitFor(() => expect(onDone).toBeCalled());
  });

  it('requests confirmation when delete is requested', async function() {
    const itemId = 'allow-all';
    axios.get.mockResolvedValue({
      data: {
        name: 'allow-all',
        description: 'Allow all requests',
        mode: 'ALLOW',
        matchers: ['.*']
      }
    });

    axios.delete.mockResolvedValue(null);

    const {loadingMask, deleteButton} = renderEditView(itemId);

    await waitForElementToBeRemoved(loadingMask);

    axios.put.mockResolvedValue(null);

    ExtJS.requestConfirmation.mockReturnValue(CONFIRM);
    userEvent.click(deleteButton());

    await waitFor(() => expect(axios.delete).toBeCalledWith(ROUTING_RULES_URL(itemId)));
    expect(onDone).toBeCalled();
  });

  it('creates a new routing rule', async function() {
    axios.get.mockResolvedValue({data: []});
    axios.post.mockResolvedValue(null);

    const {loadingMask, name, description, mode, matcher, createButton} = renderCreateView();

    await waitForElementToBeRemoved(loadingMask);

    await waitFor(() => expect(window.dirty).toEqual([]));

    await TestUtils.changeField(name, 'block-all');
    await TestUtils.changeField(description, 'Block all requests');
    await TestUtils.changeField(mode, 'BLOCK');
    await TestUtils.changeField(() => matcher(0), '.*');

    await waitFor(() => expect(window.dirty).toEqual(['RoutingRulesFormMachine']));

    expect(createButton()).not.toBeDisabled();
    userEvent.click(createButton());

    await waitFor(() => expect(axios.post).toHaveBeenCalledWith(
        ROUTING_RULES_URL(''),
        {name: 'block-all', description: 'Block all requests', mode: 'BLOCK', matchers: ['.*']}
    ));
    expect(window.dirty).toEqual([]);
  });

  it('removes existing matchers', async function() {
    const itemId = 'allow-all';

    axios.get.mockResolvedValue({
      data: {
        name: itemId,
        description: 'Allow all requests',
        mode: 'ALLOW',
        matchers: ['a', 'b', 'c']
      }
    });

    const {loadingMask, matcher, saveButton, matcherButton} = renderEditView(itemId);

    await waitForElementToBeRemoved(loadingMask);

    expect(matcher(0)).toHaveValue('a');
    expect(matcher(1)).toHaveValue('b');
    expect(matcher(2)).toHaveValue('c');

    expect(matcherButton(0)).toBeInTheDocument();
    expect(matcherButton(1)).toBeInTheDocument();
    expect(matcherButton(2)).toBeInTheDocument();

    userEvent.click(matcherButton(2));
    expect(matcher(2)).not.toBeInTheDocument();

    userEvent.click(matcherButton(1));
    expect(matcher(1)).not.toBeInTheDocument();

    expect(saveButton()).not.toHaveClass('disabled');

    await act(async () => userEvent.click(saveButton()));

    expect(axios.put).toHaveBeenLastCalledWith(ROUTING_RULES_URL(itemId), {
      description: 'Allow all requests',
      matchers: ['a'],
      mode: 'ALLOW',
      name: 'allow-all',
    });
  });
});
