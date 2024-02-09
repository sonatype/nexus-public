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
import {act} from 'react-dom/test-utils';
import {render} from '@testing-library/react';
import userEvent from '@testing-library/user-event';

import SupportZipResponse from './SupportZipResponse';

describe('SupportZipResponse', function() {
  const renderView = async (response, download) => {
    var selectors;

    await act(async () => {
      selectors = render(<SupportZipResponse response={response} download={download}/>);
    });

    return selectors;
  };

  it('renders support zip response', async function() {
    const response = {
      "name": "support-20200527-095617-6.zip",
      "size": "281152",
      "file": "/Users/ataylor/dev/sonatype/nexus-internal/target/sonatype-work/nexus3/downloads/support-20200527-095617-6.zip",
      "truncated": "false"
    };

    const {container} = await renderView(response);

    expect(container.querySelector('input[id="name"]')).toHaveValue("support-20200527-095617-6.zip");
    expect(container.querySelector('input[id="size"]')).toHaveValue("281152");
    expect(container.querySelector('input[id="file"]')).toHaveValue("/Users/ataylor/dev/sonatype/nexus-internal/target/sonatype-work/nexus3/downloads/support-20200527-095617-6.zip");
  });

  it('downloads', async function() {
    const response = {
      "name": "support-20200527-095617-6.zip",
      "size": "281152",
      "file": "/Users/ataylor/dev/sonatype/nexus-internal/target/sonatype-work/nexus3/downloads/support-20200527-095617-6.zip",
      "truncated": "false"
    };

    const download = jest.fn();
    const {container} = await renderView(response, download);
    const downloadButton = container.querySelector('button[type=submit]');

    userEvent.click(downloadButton);

    expect(download).toBeCalledWith(expect.any(Object), 'support-20200527-095617-6.zip');
  });
});
