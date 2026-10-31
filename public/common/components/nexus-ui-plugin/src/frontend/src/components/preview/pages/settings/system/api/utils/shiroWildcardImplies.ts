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

const WILDCARD = '*';

/**
 * True if {@code grantedWildcard} implies {@code requiredPermission} (Apache Shiro {@link WildcardPermission#implies}).
 */
export function wildcardImplies(grantedWildcard: string, requiredPermission: string): boolean {
  const grantedParts = parseParts(grantedWildcard);
  const requiredParts = parseParts(requiredPermission);
  let i = 0;
  for (const otherPart of requiredParts) {
    if (grantedParts.length - 1 < i) {
      return true;
    }
    const part = grantedParts[i];
    if (!(setContainsWildcard(part) || isSubset(otherPart, part))) {
      if (!isSubset(part, otherPart)) {
        return false;
      }
    }
    i++;
  }
  for (; i < grantedParts.length; i++) {
    if (!setContainsWildcard(grantedParts[i])) {
      return false;
    }
  }
  return true;
}

/**
 * True if any granted string implies {@code required}.
 */
export function shiroWildcardImplies(required: string, grantedPermissionStrings: Iterable<string>): boolean {
  if (!required) {
    return false;
  }
  for (const g of grantedPermissionStrings) {
    if (g && wildcardImplies(g, required)) {
      return true;
    }
  }
  return false;
}

export function userImpliesPermission(required: string, userPermissions: Record<string, boolean>): boolean {
  return shiroWildcardImplies(required, Object.keys(userPermissions).filter((k) => userPermissions[k]));
}

function parseParts(wildcardString: string): Set<string>[] {
  return wildcardString.split(':').map((segment) => {
    const tokens = segment.split(',').map((t) => t.trim()).filter(Boolean);
    return new Set(tokens);
  });
}

function setContainsWildcard(part: Set<string>): boolean {
  return part.has(WILDCARD);
}

function isSubset(small: Set<string>, large: Set<string>): boolean {
  for (const x of small) {
    if (!large.has(x)) {
      return false;
    }
  }
  return true;
}
