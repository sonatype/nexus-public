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
import {render, screen, waitForElementToBeRemoved, act} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import axios from 'axios';

import TagStrings from '../../../../constants/pages/browse/tags/TagsStrings';
import TestUtils from '@sonatype/nexus-ui-plugin/src/frontend/src/interface/TestUtils';
import TagsDetails from './TagsDetails';

jest.mock('axios', () => ({
  get: jest.fn()
}));

const testTag = {
  name: 'test-tag',
  firstCreated: '1/1/2023, 7:00:00 AM',
  lastUpdated: '1/2/2023, 8:00:00 AM',
  attributes: {"additionalProp1": {}},
};

const selectors = {
  ...TestUtils.selectors,
  ...TestUtils.formSelectors,
  getLoadError: () => screen.queryByRole('alert'),
  getBacklink: () => screen.queryByRole('link', {name: TAGS.DETAILS.BACK_TO_TAGS_TABLE}),
  getTileHeader: () => screen.queryByRole('heading', {level: 2}),
  getSearchTaggedComponentsLink: () => screen.queryByRole('link', {name: TAGS.DETAILS.FIND_TAGGED}),
  getClipboardContent: () => screen.queryByRole('textbox'),
  getFirstCreatedLabel: () => screen.queryByText(TAGS.DETAILS.FIRST_CREATED),
  getLastUpdatedLabel: () => screen.queryByText(TAGS.DETAILS.LAST_UPDATED),
  getTagAttributesLabel: () => screen.queryByText(TAGS.DETAILS.ATTRIBUTES),
  getCopyToClipboardButton: () => screen.queryByRole('button', {name:'Copy to Clipboard'}),
  getRetryButton: () => screen.queryByRole('button', {name:'Retry'}),
}

const {TAGS} = TagStrings;

async function renderView() {
  axios.get.mockResolvedValue({data: testTag});
  render(<TagsDetails itemId="test-tag"/>);
  await waitForElementToBeRemoved(selectors.queryLoadingMask());
};

describe('TagsDetails', function() {

  describe('renders an error message when ', function() {

    it('a promise is rejected', async function() {
      axios.get.mockRejectedValue({message: 'Error'});
      render(<TagsDetails itemId="test-tag"/>);
      await waitForElementToBeRemoved(selectors.queryLoadingMask());
  
      expect(selectors.getLoadError()).toBeInTheDocument();
      expect(selectors.getLoadError()).toHaveTextContent('Error');
    });
  
    it('a tag is not found', async function() {
      axios.get.mockRejectedValue({message: 'Request failed with status code 404'});
      render(<TagsDetails itemId="tag-does-not-exist"/>);
      await waitForElementToBeRemoved(selectors.queryLoadingMask());

      const error = selectors.queryLoadError('Tag was not found');
      expect(error).toBeInTheDocument();
    });

  });

  describe('correctly renders the following based on the test tag: ', function() {

    it('the header', async function() {
      await renderView();

      expect(selectors.queryTitle()).toHaveTextContent(TAGS.DETAILS.HEADER.text);
      expect(selectors.getBacklink()).toBeInTheDocument();
      expect(selectors.getBacklink()).toHaveAttribute('href','#browse/tags');
    });

    it('the tile', async function() {
      await renderView();

      expect(selectors.getTileHeader()).toHaveTextContent(`test-tag ${TAGS.DETAILS.TILE_HEADER}`);
      expect(selectors.getSearchTaggedComponentsLink()).toBeInTheDocument();
      expect(selectors.getSearchTaggedComponentsLink()).toHaveAttribute('href','#browse/search/custom=tags%3D%22test-tag%22');
      expect(selectors.getFirstCreatedLabel()).toBeInTheDocument();
      expect(screen.queryByText('1/1/2023, 7:00:00 AM')).toBeInTheDocument();
      expect(selectors.getLastUpdatedLabel()).toBeInTheDocument();
      expect(screen.queryByText('1/2/2023, 8:00:00 AM')).toBeInTheDocument();
      expect(selectors.getTagAttributesLabel()).toBeInTheDocument();
      expect(selectors.getCopyToClipboardButton()).toBeInTheDocument();
      expect(selectors.getClipboardContent()).toBeInTheDocument();
      expect(selectors.getClipboardContent()).toHaveTextContent('{ "additionalProp1": {} }');
    });
    
  });

  it('copies the clipboard content when the copy button is clicked', async function() {
    await renderView();
    Object.defineProperty(window.navigator, 'clipboard', {
      value: {
        writeText: jest.fn(() => {
          return new Promise(() => {
          });
        })
      },
      configurable: true
    });
    const clipboardContentWithExpectedPrettyPrint = 
`{
  "additionalProp1": {}
}`

    userEvent.click(selectors.getCopyToClipboardButton());
    expect(window.navigator.clipboard.writeText).toHaveBeenCalledWith(clipboardContentWithExpectedPrettyPrint);
  });

  it('has its error retry button load tag data upon cleared error', async function() {
    axios.get.mockRejectedValue({message: 'Error'});
    render(<TagsDetails itemId="test-tag"/>);
    await waitForElementToBeRemoved(selectors.queryLoadingMask());

    axios.get.mockResolvedValue({data: testTag});
    await act(async () => userEvent.click(selectors.getRetryButton()));
    expect(selectors.getTileHeader()).toHaveTextContent(`test-tag ${TAGS.DETAILS.TILE_HEADER}`);
  });

});
