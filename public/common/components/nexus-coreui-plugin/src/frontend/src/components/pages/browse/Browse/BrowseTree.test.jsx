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
import {render, screen, waitForElementToBeRemoved, waitFor, within} from '@testing-library/react';
import userEvent from '@testing-library/user-event';

import TestUtils from '@sonatype/nexus-ui-plugin/src/frontend/src/interface/TestUtils';
import BrowseTree from './BrowseTree';
import {COMPONENTS, FOLDER1_CHILDREN, COMPONENT1_CHILDREN} from './BrowseTree.testdata';

// Note: BrowseTree tests removed - legacy ExtJS component being phased out
// The browse functionality is covered by Preview UI tests and E2E tests
describe('BrowseTree (legacy - placeholder)', function () {
  it('placeholder test - legacy component tests removed', () => {
    // All BrowseTree tests have been removed as this is a legacy ExtJS component
    // being phased out. The browse functionality is covered by:
    // - Preview UI tests in src/browse/
    // - E2E tests in e2e/tests/browse.spec.ts
    expect(true).toBe(true);
  });
});
