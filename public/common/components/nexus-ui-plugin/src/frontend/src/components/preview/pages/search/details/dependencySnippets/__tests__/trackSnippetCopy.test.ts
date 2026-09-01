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

jest.mock('axios', () => ({
  post: jest.fn(),
}));

import Axios from 'axios';
import { trackSnippetCopy } from '../trackSnippetCopy';

const mockAxios = Axios as jest.Mocked<typeof Axios>;

describe('trackSnippetCopy', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    mockAxios.post.mockResolvedValue({} as never);
  });

  it('POSTs to the dependency_snippets endpoint with the normalized format and snippet name', () => {
    trackSnippetCopy('maven', 'Apache Maven');
    expect(mockAxios.post).toHaveBeenCalledTimes(1);
    expect(mockAxios.post).toHaveBeenCalledWith(
      '/service/rest/dependency_snippets?format=maven2&snippet=Apache%20Maven',
    );
  });

  it('URL-encodes spaces in the snippet display name', () => {
    trackSnippetCopy('oci', 'OCI Pull (ORAS)');
    expect(mockAxios.post).toHaveBeenCalledWith(
      '/service/rest/dependency_snippets?format=oci&snippet=OCI%20Pull%20(ORAS)',
    );
  });

  it('does not throw when the request rejects (fire-and-forget)', () => {
    mockAxios.post.mockRejectedValue(new Error('network'));
    expect(() => trackSnippetCopy('npm', 'npm')).not.toThrow();
  });
});
