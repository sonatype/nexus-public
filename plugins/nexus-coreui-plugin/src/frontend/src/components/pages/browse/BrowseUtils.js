/*
 * Sonatype Nexus (TM) Open Source Version
 * Copyright (c) 2008-present Sonatype, Inc.
 * All rights reserved. Includes the third-party code listed at http://links.sonatype.com/products/nexus/oss/attributions.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse Public License Version 1.0,
 * which accompanies this distribution and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Sonatype Nexus (TM) Open Source Version is distributed with Sencha Ext JS pursuant to a FLOSS Exception agreed upon
 * between Sonatype, Inc. and Sencha Inc. Sencha Ext JS is licensed under GPL v3 and cannot be redistributed as part of a
 * closed source work.
 *
 * Sonatype Nexus (TM) Professional Version is available from Sonatype, Inc. "Sonatype" and "Sonatype Nexus" are trademarks
 * of Sonatype, Inc. Apache Maven is a trademark of the Apache Software Foundation. M2eclipse is a trademark of the
 * Eclipse Foundation. All other trademarks are the property of their respective owners.
 */
import { __, allPass, complement, filter, includes, isEmpty, map, path, pathEq, pipe, prop, propEq } from 'ramda';

const isHosted = propEq('type', 'hosted'),
    isOnline = complement(pathEq(['status', 'online'], false)),
    isNotMavenSnapshotRepo = complement(propEq('versionPolicy', 'SNAPSHOT')),
    isKnownUiUploadableFormat = formats => repo => formats.has(repo.format);

/**
 * Given a list of repos and a list of uploadDefinitions, return a list of repos from the input which
 * support upload through the UI.
 * @param repos a list of repository data structures as returned by the coreui_Repository readReferences ExtDirect
 * API
 */
export function filterReposByUiUpload(uploadDefinitions, repos) {
  const uiUploadableFormats = new Set(map(prop('format'), filter(propEq('uiUpload', true), uploadDefinitions))),
      repoSupportsUpload = allPass([
        isHosted,
        isOnline,
        isNotMavenSnapshotRepo,
        isKnownUiUploadableFormat(uiUploadableFormats)
      ]);

  return filter(repoSupportsUpload, repos);
}

export function repoSupportsUiUpload(uploadDefinitions, repo) {
  return !isEmpty(filterReposByUiUpload(uploadDefinitions, [repo]));
}
