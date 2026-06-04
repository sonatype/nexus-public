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

export type FindingStatus = 'pending' | 'deleted' | 'acknowledged';
export type ViewMode = 'findings' | 'by-component' | 'by-repository';

export interface MaliciousFinding {
  id: number;
  repositoryName: string;
  assetId: string;
  path: string;
  format: string;
  recordedTime: string | null;
  deletedTime: string | null;
  deletedBy: string | null;
  deletionMethod: string | null;
  acknowledgedAt: string | null;
  acknowledgedBy: string | null;
  acknowledgedReason: string | null;
  firstDetectedAt: string | null;
  hash: string | null;
  createdBy: string | null;
  createdByIp: string | null;
  componentName: string | null;
  componentVersion: string | null;
  componentFormat: string | null;
  threatLevel: number | null;
  threatSummary: string | null;
  threatReference: string | null;
  policyName: string | null;
}

export interface MaliciousPackagesState {
  activeFindings: MaliciousFinding[];
  historyFindings: MaliciousFinding[];
  malwareCount: number;
  countsByRepo: Record<string, number>;
  hasFirewall: boolean;
  hcEnabledRepos: string[];
  totalProxyRepoCount: number;
  loading: boolean;
  error: string | null;
}

export interface RepoGroup {
  repositoryName: string;
  findings: MaliciousFinding[];
  pendingCount: number;
  deletedCount: number;
  acknowledgedCount: number;
}

export interface ComponentGroup {
  componentName: string;
  componentVersion: string | null;
  format: string;
  findings: MaliciousFinding[];
  repositories: string[];
}

export function getFindingStatus(finding: MaliciousFinding): FindingStatus {
  if (finding.acknowledgedAt) return 'acknowledged';
  if (finding.deletedTime) return 'deleted';
  return 'pending';
}

export function groupByRepository(findings: MaliciousFinding[]): RepoGroup[] {
  const map = new Map<string, MaliciousFinding[]>();
  for (const f of findings) {
    const list = map.get(f.repositoryName) ?? [];
    list.push(f);
    map.set(f.repositoryName, list);
  }
  return Array.from(map.entries()).map(([repositoryName, repoFindings]) => {
    let pendingCount = 0;
    let deletedCount = 0;
    let acknowledgedCount = 0;
    for (const f of repoFindings) {
      const status = getFindingStatus(f);
      if (status === 'pending') pendingCount++;
      else if (status === 'deleted') deletedCount++;
      else acknowledgedCount++;
    }
    return { repositoryName, findings: repoFindings, pendingCount, deletedCount, acknowledgedCount };
  });
}

/**
 * Extract component name and version from the asset path when the backend
 * hasn't resolved componentName/componentVersion (common for pypi, npm, etc.).
 *
 * Known patterns:
 *   pypi:  /packages/{name}/{version}/{filename}
 *   npm:   /{scope}/{name}/-/{tarball}  or  /{name}/-/{tarball}
 *   maven: /{group/...}/{artifact}/{version}/{filename}
 */
export function deriveComponentIdentity(f: MaliciousFinding): { name: string; version: string | null } {
  if (f.componentName) return { name: f.componentName, version: f.componentVersion };

  const parts = f.path.replace(/^\//, '').split('/');

  if (f.format === 'pypi' && parts[0] === 'packages' && parts.length >= 3) {
    return { name: parts[1], version: parts[2] };
  }

  if (f.format === 'npm' && parts.length >= 3) {
    const nameIdx = parts.indexOf('-') - 1;
    if (nameIdx >= 0) {
      const name = parts.slice(0, nameIdx + 1).join('/');
      const filename = parts[parts.indexOf('-') + 1] ?? '';
      const versionMatch = filename.match(/-(\d+\.\d+[^.]*)\./);
      return { name, version: versionMatch?.[1] ?? null };
    }
  }

  if (parts.length >= 3) {
    return { name: parts[parts.length - 3], version: parts[parts.length - 2] };
  }

  const filename = parts[parts.length - 1] ?? f.path;
  return { name: filename, version: null };
}

export function groupByComponent(findings: MaliciousFinding[]): ComponentGroup[] {
  const map = new Map<string, MaliciousFinding[]>();
  for (const f of findings) {
    const identity = deriveComponentIdentity(f);
    const key = `${identity.name}@${identity.version ?? ''}`;
    const list = map.get(key) ?? [];
    list.push(f);
    map.set(key, list);
  }
  return Array.from(map.entries()).map(([, compFindings]) => {
    const first = compFindings[0];
    const identity = deriveComponentIdentity(first);
    const repos = [...new Set(compFindings.map((f) => f.repositoryName))];
    return {
      componentName: identity.name,
      componentVersion: identity.version,
      format: first.format,
      findings: compFindings,
      repositories: repos,
    };
  });
}

export type TabId = 'overview' | 'detect' | 'remediate' | 'harden' | 'report';

export interface TabCounts {
  overview: number;
  detect: number;
  remediate: number;
  harden: number;
  report: number;
}
