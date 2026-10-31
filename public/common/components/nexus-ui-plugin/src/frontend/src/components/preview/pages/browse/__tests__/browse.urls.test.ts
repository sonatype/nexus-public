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


jest.mock('../../../../../interface/ExtJS', () => ({
  ExtJS: { urlOf: jest.fn() },
}));

import { htmlViewUrl } from '../browse.urls';
import { ExtJS } from '../../../../../interface/ExtJS';

const mockUrlOf = ExtJS.urlOf as jest.MockedFunction<typeof ExtJS.urlOf>;

describe('htmlViewUrl (HTML View link)', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    // Stand in for NX.util.Url.urlOf, which prefixes the deployed context path.
    mockUrlOf.mockImplementation((path: string) => `https://nexus.example.com/nexus-ctx${path}`);
  });

  it('targets the Classic REST browse endpoint for the repository', () => {
    expect(htmlViewUrl('maven-releases')).toBe(
      'https://nexus.example.com/nexus-ctx/service/rest/repository/browse/maven-releases'
    );
  });

  it('resolves through ExtJS.urlOf so a non-root context path is preserved and no hostname is hardcoded', () => {
    htmlViewUrl('maven-releases');

    expect(mockUrlOf).toHaveBeenCalledWith('/service/rest/repository/browse/maven-releases');
  });

  it('encodes repository names containing URL-significant characters', () => {
    htmlViewUrl('repo with spaces&more');

    expect(mockUrlOf).toHaveBeenCalledWith(
      '/service/rest/repository/browse/repo%20with%20spaces%26more'
    );
  });

  it('encodes a slash in the repository name rather than creating a path segment', () => {
    htmlViewUrl('a/b');

    expect(mockUrlOf).toHaveBeenCalledWith('/service/rest/repository/browse/a%2Fb');
  });

  it('returns undefined when no repository is selected', () => {
    expect(htmlViewUrl(undefined)).toBeUndefined();
    expect(mockUrlOf).not.toHaveBeenCalled();
  });
});
