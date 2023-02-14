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
import {render, screen} from '@testing-library/react';

import NodeCard from './NodeCard';
import NodeCardTestData from './NodeCard.testdata';

describe('NodeCard', function () {
    const testNodes = NodeCardTestData;

    const NODE_ACTIVE_INDICATOR = 'Node Active';

    const DOWNLOAD_ZIP_STATUS = 'Download Zip';
    const CREATE_ZIP_STATUS = 'Create Support zip';
    const CREATING_ZIP_STATUS = 'Creating Zip...';

    const NO_ZIP_CREATED = 'No Zip created';

    const ACTIVE_NODE_INDEX = 0;
    const ZIP_CREATED_NODE_INDEX = 0;
    const ZIP_NOT_CREATED_NODE_INDEX = 2;
    const ZIP_CREATING_NODE_INDEX = 4;

    const selectors = {
        nodeHostName: (hostname) => screen.getByText(hostname),
        downloadZipStatus: () => screen.getByText(DOWNLOAD_ZIP_STATUS),
        noZipCreated: () => screen.getByText(NO_ZIP_CREATED),
        zipCreate: () => screen.getByText(CREATE_ZIP_STATUS),
        zipCreating: () => screen.getByText(CREATING_ZIP_STATUS)
    }

    const renderView = (nxrmNode) => {
        return render(
            <NodeCard initial={nxrmNode} />
        );
    };

    it('renders node card', async () => {
        const activeNode = testNodes[ACTIVE_NODE_INDEX];
        renderView(activeNode);

        expect(selectors.nodeHostName(activeNode.hostname)).toBeInTheDocument();
    });


    it('renders zip is created', async () => {
        const node = testNodes[ZIP_CREATED_NODE_INDEX];
        renderView(node);

        expect(selectors.nodeHostName(node.hostname)).toBeInTheDocument();
        expect(selectors.downloadZipStatus()).toBeInTheDocument();
    });

    it('renders zip is not created', async () => {
        const node = testNodes[ZIP_NOT_CREATED_NODE_INDEX];
        renderView(node);

        expect(selectors.nodeHostName(node.hostname)).toBeInTheDocument();
        expect(selectors.noZipCreated()).toBeInTheDocument();
        expect(selectors.zipCreate()).toBeInTheDocument();
    });

    it('renders zip creation in progress', async () => {
        const node = testNodes[ZIP_CREATING_NODE_INDEX];
        renderView(node);

        expect(selectors.nodeHostName(node.hostname)).toBeInTheDocument();
        expect(selectors.zipCreating()).toBeInTheDocument();
    });
});
