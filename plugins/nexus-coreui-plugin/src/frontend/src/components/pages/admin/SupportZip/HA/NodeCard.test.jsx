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
import {spawn} from 'xstate';
import {render, screen} from '@testing-library/react';
import {when} from 'jest-when';
import {DateUtils, ExtJS} from '@sonatype/nexus-ui-plugin';
import userEvent from '@testing-library/user-event';
import NodeCard from './NodeCard';
import NodeCardTestData from './NodeCard.testdata';
import UIStrings from '../../../../../constants/UIStrings';
import NodeCardMachine from './NodeCardMachine';
const {SUPPORT_ZIP: LABELS} = UIStrings;

jest.mock('@sonatype/nexus-ui-plugin', () => ({
  ...jest.requireActual('@sonatype/nexus-ui-plugin'),
  ExtJS: {
    state: jest.fn().mockReturnValue({
      getValue: jest.fn(),
    }),
    urlOf: jest.fn().mockImplementation((path) => '/' + path)
  },
  DateUtils: {
    prettyDateTime: jest.fn()
  }
}));

describe('NodeCard', function () {
  const testNodes = NodeCardTestData;
  const ACTIVE_NODE_INDEX = 0;
  const ZIP_CREATED_NODE_INDEX = 0;
  const ZIP_NOT_CREATED_NODE_INDEX = 2;
  const ZIP_CREATING_NODE_INDEX = 4;
  const ZIP_FAILED_NODE_INDEX = 9;

  const selectors = {
    nodeHostName: (hostname) =>
      screen.getByRole('heading', {level: 3, name: hostname}),
    generateZipStatus: () =>
      screen.getByRole('button', {name: LABELS.GENERATE_NEW_ZIP_FILE}),
    noZipCreated: () => screen.getByText(LABELS.NO_ZIP_CREATED),
    zipCreate: () => screen.getByText(LABELS.GENERATE_NEW_ZIP_FILE),
    zipCreating: () => screen.getByText(LABELS.CREATING_ZIP),
    zipLink: () => screen.getByRole('link'),
    errorMessage: () => screen.getByText(LABELS.GENERATE_ERROR),
    retryButton: () => screen.getByRole('button', {name: LABELS.RETRY}),
  };

  const renderView = (nxrmNode, createZip = jest.fn()) => {
    const id = 'test-machine';
    const context = {
      data: nxrmNode,
      pristineData: nxrmNode,
    };
    const machineRef = spawn(NodeCardMachine.withContext(context), id);
    return render(
      <NodeCard
        actor={machineRef}
        isBlobStoreConfigured
        createZip={createZip}
      />
    );
  };

  it('renders node card', () => {
    const activeNode = testNodes[ACTIVE_NODE_INDEX];
    renderView(activeNode);

    expect(selectors.nodeHostName(activeNode.hostname)).toBeInTheDocument();
  });

  it('renders zip is created', () => {
    when(DateUtils.prettyDateTime).calledWith(expect.any(Date)).mockReturnValue('5-7-2022 00:00:00 (GMT-0500)');

    const node = testNodes[ZIP_CREATED_NODE_INDEX];
    renderView(node);

    expect(selectors.nodeHostName(node.hostname)).toBeInTheDocument();
    expect(selectors.generateZipStatus()).toBeInTheDocument();
    expect(selectors.zipLink()).toHaveTextContent(
      'Download Zip Generated5-7-2022 00:00:00 (GMT-0500)'
    );
    expect(selectors.zipLink()).toHaveAttribute(
        'href', '/service/rest/wonderland/download/http://download.com?support-zip.zip'
    );
  });

  it('renders zip link with context path when provided', () => {
    when(DateUtils.prettyDateTime).calledWith(expect.any(Date)).mockReturnValue('5-7-2022 00:00:00 (GMT-0500)');

    ExtJS.urlOf.mockImplementation((path) => '/test/' + path);

    const node = testNodes[ZIP_CREATED_NODE_INDEX];
    renderView(node);

    expect(selectors.zipLink()).toHaveTextContent(
        'Download Zip Generated5-7-2022 00:00:00 (GMT-0500)'
    );
    expect(selectors.zipLink()).toHaveAttribute(
        'href', '/test/service/rest/wonderland/download/http://download.com?support-zip.zip'
    );
  });

  it('renders zip is not created', () => {
    const node = testNodes[ZIP_NOT_CREATED_NODE_INDEX];
    renderView(node);

    expect(selectors.nodeHostName(node.hostname)).toBeInTheDocument();
    expect(selectors.noZipCreated()).toBeInTheDocument();
    expect(selectors.zipCreate()).toBeInTheDocument();
  });

  it('renders zip creation in progress', () => {
    const node = testNodes[ZIP_CREATING_NODE_INDEX];
    renderView(node);

    expect(selectors.nodeHostName(node.hostname)).toBeInTheDocument();
    expect(selectors.zipCreating()).toBeInTheDocument();
    expect(selectors.zipCreate()).toHaveAttribute('aria-disabled', 'true');
  });

  it('renders an error message', () => {
    const handlerMock = jest.fn();
    const node = testNodes[ZIP_FAILED_NODE_INDEX];
    renderView(node, handlerMock);

    expect(selectors.nodeHostName(node.hostname)).toBeInTheDocument();
    expect(selectors.retryButton()).toBeInTheDocument();
    expect(selectors.errorMessage()).toBeInTheDocument();

    userEvent.click(selectors.retryButton());

    expect(handlerMock).toHaveBeenCalled();
  });
});
