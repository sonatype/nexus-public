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
import {fireEvent, wait, waitForElement, waitForElementToBeRemoved} from '@testing-library/react';
import '@testing-library/jest-dom/extend-expect';
import TestUtils from '@sonatype/nexus-ui-plugin/src/frontend/src/interface/TestUtils';
import axios from 'axios';
import {ExtJS} from '@sonatype/nexus-ui-plugin';

import RoutingRulesForm from './RoutingRulesForm';

import UIStrings from '../../../../constants/UIStrings';

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

describe('RoutingRulesForm', function() {
  const CONFIRM = Promise.resolve();
  const onDone = jest.fn();

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
      createButton: () => queryByText(UIStrings.ROUTING_RULES.FORM.CREATE_BUTTON, {selector: '[type="submit"]'}),
      saveButton: () => queryByText(UIStrings.SETTINGS.SAVE_BUTTON_LABEL),
      cancelButton: () => queryByText(UIStrings.SETTINGS.CANCEL_BUTTON_LABEL),
      deleteButton: () => queryByText(UIStrings.SETTINGS.DELETE_BUTTON_LABEL)
    }));
  }

  it('renders a loading spinner', async function() {
    axios.get.mockImplementation(() => new Promise(() => {}));

    const {container, loadingMask} = renderEditView('test');

    await waitForElement(loadingMask);

    expect(container).toMatchSnapshot();
  });

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

    const {container, loadingMask, name, description, mode, matcher, saveButton} = renderEditView(itemId);

    await waitForElementToBeRemoved(loadingMask);

    expect(name()).toHaveValue('allow-all');
    expect(description()).toHaveValue('Allow all requests');
    expect(mode()).toHaveValue('ALLOW');
    expect(matcher(0)).toHaveValue('.*');
    expect(saveButton()).toHaveClass('disabled');
    expect(container).toMatchSnapshot();
  });

  it('renders an error message', async function() {
    axios.get.mockRejectedValue({message: 'Error'});

    const {container, loadingMask} = renderEditView('itemId');

    await waitForElementToBeRemoved(loadingMask);

    expect(container.querySelector('.nx-alert--error')).toHaveTextContent('Error');
  });

  it('renders an error message when saving an invalid field', async function() {
    const {loadingMask, name, matcher, createButton, getByText, savingMask} = renderCreateView();

    await waitForElementToBeRemoved(loadingMask);

    await TestUtils.changeField(name, 'newValue');
    await TestUtils.changeField(() => matcher(0), '.*');

    axios.post.mockRejectedValue({
      response: {
        data: [
          {
            "id": "name",
            "message": "A rule with the same name already exists. Name must be unique."
          }
        ]
      }
    });
    fireEvent.click(createButton());

    await waitForElementToBeRemoved(savingMask);

    expect(getByText('A rule with the same name already exists. Name must be unique.')).toBeInTheDocument();
  });

  it('requires the name, description, and at least one matcher', async function() {
    axios.get.mockResolvedValue({data: []});

    const {loadingMask, name, description, matcher, createButton} = renderCreateView();

    await waitForElementToBeRemoved(loadingMask);

    await TestUtils.changeField(name, '');
    await TestUtils.changeField(description, '');
    await TestUtils.changeField(() => matcher(0), '.*');
    expect(createButton()).toHaveClass('disabled');

    await TestUtils.changeField(name, 'name');
    await TestUtils.changeField(description, '')
    await TestUtils.changeField(() => matcher(0), '');
    expect(createButton()).toHaveClass('disabled');

    await TestUtils.changeField(name, '');
    await TestUtils.changeField(description, 'description');
    await TestUtils.changeField(() => matcher(0), '');
    expect(createButton()).toHaveClass('disabled');

    await TestUtils.changeField(name, 'name');
    await TestUtils.changeField(description, 'description');
    await TestUtils.changeField(() => matcher(0), '.*');
    expect(createButton()).not.toHaveClass('disabled');
  });

  it('fires onDone when cancelled', async function() {
    const {loadingMask, cancelButton} = renderCreateView();

    await waitForElementToBeRemoved(loadingMask);

    fireEvent.click(cancelButton());

    await wait(() => expect(onDone).toBeCalled());
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
    fireEvent.click(deleteButton());

    await wait(() => expect(axios.delete).toBeCalledWith(`/service/rest/internal/ui/routing-rules/${itemId}`));
    expect(onDone).toBeCalled();
  });

  it('creates a new routing rule', async function() {
    axios.get.mockResolvedValue({data: []});
    axios.post.mockResolvedValue(null);

    const {loadingMask, name, description, mode, matcher, createButton} = renderCreateView();

    await waitForElementToBeRemoved(loadingMask);

    await wait(() => expect(window.dirty).toEqual([]));

    await TestUtils.changeField(name, 'block-all');
    await TestUtils.changeField(description, 'Block all requests');
    await TestUtils.changeField(mode, 'BLOCK');
    await TestUtils.changeField(() => matcher(0), '.*');

    await wait(() => expect(window.dirty).toEqual(['RoutingRulesFormMachine']));

    expect(createButton()).not.toBeDisabled();
    fireEvent.click(createButton());

    await wait(() => expect(axios.post).toHaveBeenCalledWith(
        '/service/rest/internal/ui/routing-rules/',
        {name: 'block-all', description: 'Block all requests', mode: 'BLOCK', matchers: ['.*']}
    ));
    expect(window.dirty).toEqual([]);
  });
});
