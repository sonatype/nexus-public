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


import { ExtJS } from '../../../../interface/ExtJS';

/**
 * Path of the server-rendered repository browse view (the "HTML View").
 *
 * Same endpoint Classic UI links to from the Browse toolbar
 * (NX.coreui.controller.ComponentAssetTree.updateHtmlLink); it lists repository
 * content as plain HTML directories and inherits the browser session, so no
 * separate authentication is involved.
 */
const HTML_VIEW_PATH = '/service/rest/repository/browse';

/**
 * Absolute URL of the HTML View for a repository, or undefined when no
 * repository is selected.
 *
 * Resolved through {@link ExtJS.urlOf}, which prefixes the deployed context path
 * — the URL must never be assembled from a hardcoded host. The repository name is
 * URI-encoded as a single path segment: a name is never a path, so a slash inside
 * one is escaped rather than emitted as a separator.
 *
 * @param repositoryName Repository to link to, or undefined
 */
export function htmlViewUrl(repositoryName: string | undefined): string | undefined {
  if (!repositoryName) {
    return undefined;
  }
  return ExtJS.urlOf(`${HTML_VIEW_PATH}/${encodeURIComponent(repositoryName)}`);
}
