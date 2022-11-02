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
import {render, screen, waitForElementToBeRemoved} from '@testing-library/react';
import BundleDetail from './BundleDetail';
import {interpret} from 'xstate';
import BundlesListMachine from './BundlesListMachine';
import mockData from './bundles.testdata';
import TestUtils from '@sonatype/nexus-ui-plugin/src/frontend/src/interface/TestUtils';

const DEFAULT_RESPONSE = () => Promise.resolve({data: mockData});

const selectors = {
  ...TestUtils.selectors,
  getTerm: (index) => screen.getAllByRole('term')[index],
  getDefinition: (index) => screen.getAllByRole('definition')[index]
}

describe('BundleDetail', function() {
  it('renders the resolved data', async function() {
    const service = interpret(BundlesListMachine.withConfig({
      services: {
        fetchData: DEFAULT_RESPONSE
      }
    })).start();

    render(<BundleDetail service={service} itemId="1" />);

    await waitForElementToBeRemoved(selectors.queryLoadingMask());

    expect(selectors.getTerm(0)).toHaveTextContent("ID");
    expect(selectors.getDefinition(0)).toHaveTextContent(mockData[0].id);
    expect(selectors.getTerm(1)).toHaveTextContent("Name");
    expect(selectors.getDefinition(1)).toHaveTextContent(mockData[0].name);
    expect(selectors.getTerm(2)).toHaveTextContent("Symbolic Name");
    expect(selectors.getDefinition(2)).toHaveTextContent(mockData[0].symbolicName);
    expect(selectors.getTerm(3)).toHaveTextContent("Version");
    expect(selectors.getDefinition(3)).toHaveTextContent(mockData[0].version);
    expect(selectors.getTerm(4)).toHaveTextContent("State");
    expect(selectors.getDefinition(4)).toHaveTextContent(mockData[0].state);
    expect(selectors.getTerm(5)).toHaveTextContent("Location");
    expect(selectors.getDefinition(5)).toHaveTextContent(mockData[0].location);
    expect(selectors.getTerm(6)).toHaveTextContent("Start Level");
    expect(selectors.getDefinition(6)).toHaveTextContent(mockData[0].startLevel);
    expect(selectors.getTerm(7)).toHaveTextContent("Last Modified");
    expect(selectors.getDefinition(7)).toHaveTextContent(mockData[0].lastModified);
    expect(selectors.getTerm(8)).toHaveTextContent("Fragment");
    expect(selectors.getDefinition(8)).toHaveTextContent(mockData[0].fragment);
    expect(selectors.getTerm(9)).toHaveTextContent("Bnd-LastModified");
    expect(selectors.getDefinition(9)).toHaveTextContent(mockData[0].headers['Bnd-LastModified']);
    expect(selectors.getTerm(10)).toHaveTextContent("Build-Jdk-Spec");
    expect(selectors.getDefinition(10)).toHaveTextContent(mockData[0].headers['Build-Jdk-Spec']);
    expect(selectors.getTerm(11)).toHaveTextContent("Bundle-ManifestVersion");
    expect(selectors.getDefinition(11)).toHaveTextContent(mockData[0].headers['Bundle-ManifestVersion']);
    expect(selectors.getTerm(12)).toHaveTextContent("Bundle-Name");
    expect(selectors.getDefinition(12)).toHaveTextContent(mockData[0].headers['Bundle-Name']);
    expect(selectors.getTerm(13)).toHaveTextContent("Bundle-SymbolicName");
    expect(selectors.getDefinition(13)).toHaveTextContent(mockData[0].headers['Bundle-SymbolicName']);
    expect(selectors.getTerm(14)).toHaveTextContent("Bundle-Version");
    expect(selectors.getDefinition(14)).toHaveTextContent(mockData[0].headers['Bundle-Version']);
    expect(selectors.getTerm(15)).toHaveTextContent("Created-By");
    expect(selectors.getDefinition(15)).toHaveTextContent(mockData[0].headers['Created-By']);
    expect(selectors.getTerm(16)).toHaveTextContent("Export-Package");
    expect(selectors.getDefinition(16)).toHaveTextContent(mockData[0].headers['Export-Package']);
    expect(selectors.getTerm(17)).toHaveTextContent("Generated-By-Ops4j-Pax-From");
    expect(selectors.getDefinition(17)).toHaveTextContent(mockData[0].headers['Generated-By-Ops4j-Pax-From']);
    expect(selectors.getTerm(18)).toHaveTextContent("Import-Package");
    expect(selectors.getDefinition(18)).toHaveTextContent(mockData[0].headers['Import-Package']);
    expect(selectors.getTerm(19)).toHaveTextContent("Manifest-Version");
    expect(selectors.getDefinition(19)).toHaveTextContent(mockData[0].headers['Manifest-Version']);
    expect(selectors.getTerm(20)).toHaveTextContent("Originally-Created-By");
    expect(selectors.getDefinition(20)).toHaveTextContent(mockData[0].headers['Originally-Created-By']);
    expect(selectors.getTerm(21)).toHaveTextContent("Require-Capability");
    expect(selectors.getDefinition(21)).toHaveTextContent(mockData[0].headers['Require-Capability']);
    expect(selectors.getTerm(22)).toHaveTextContent("Tool");
    expect(selectors.getDefinition(22)).toHaveTextContent(mockData[0].headers.Tool);
  });
});
