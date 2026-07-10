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

export const HA_PATH_WARNING = {
  TITLE: 'High Availability Path Warning',
  MESSAGE: 'Using a relative path or a path under the working directory in HA mode can cause ' +
    'severe performance issues and data inconsistency. The path may not be shared between HA pods, leading to ' +
    'random request failures. Additionally, initContainer recursive operations can cause severe I/O bottlenecks ' +
    'and startup delays (10+ minutes). It is strongly recommended to use an absolute shared path outside the ' +
    'working directory for file blob stores in HA deployments.'
};

export function isRelativePath(path: string | undefined | null): boolean {
  if (!path) {
    return false;
  }
  return !path.startsWith('/') && !/^[A-Za-z]:\\/.test(path);
}

export function isUnderWorkDirectory(path: string | undefined | null, workDirectory: string | undefined | null): boolean {
  if (!path || !workDirectory) {
    return false;
  }
  const normalizedPath = path.replace(/\\/g, '/');
  let normalizedWorkDir = workDirectory.replace(/\\/g, '/');

  if (!normalizedWorkDir.endsWith('/')) {
    normalizedWorkDir += '/';
  }

  return normalizedPath.startsWith(normalizedWorkDir);
}

export function shouldShowHaPathWarning(path: string | undefined | null, workDirectory: string | undefined | null): boolean {
  return isRelativePath(path) || isUnderWorkDirectory(path, workDirectory);
}
