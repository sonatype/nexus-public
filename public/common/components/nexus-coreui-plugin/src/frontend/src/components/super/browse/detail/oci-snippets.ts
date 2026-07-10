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

/**
 * OCI usage snippets for the React Preview UI Detail Panel.
 *
 * Mirrors the classic ExtJS NX.oci.controller.OciDependencySnippetController so the
 * Preview UI's Usage tab gives the same copy-paste-ready commands the classic
 * Component-Info panel provides. Covers docker / podman / oras / helm / cosign /
 * skopeo / OpenTofu — the full toolchain the cloud OCI launch must support.
 *
 * Two routing modes are supported:
 *   - Path-based routing (cloud + self-hosted): host = "<connector-host>/<repo>"
 *   - Port-based routing (self-hosted only):    host = "<connector-host>:<port>"
 *
 * Snippet "host" choice is driven by the parsed component name itself rather than
 * Nexus state; the OCI component model stores its fully-qualified pull name as
 * "<host[:port]>/<repo>/<image>" or "<host[:port]>/<image>", which already encodes
 * which mode this repository runs in.
 */

export interface OciSnippet {
  readonly title: string;
  readonly code: string;
}

export interface OciCosignVerifyOptions {
  /** Identity regex from the repository's cosign keyless config; falls back to ".*". */
  readonly identityRegex?: string | null;
  /** OIDC issuer regex from the repository's cosign keyless config; falls back to ".*". */
  readonly issuerRegex?: string | null;
}

export interface OciSnippetInputs {
  /** Component name as stored in Nexus, e.g. "nexus.example.com/oci-hosted/alpine". */
  readonly name: string;
  /** Component version (tag) — when null, snippets fall back to "latest". */
  readonly version?: string | null;
  /** Asset content digest in the form "sha256:hex"; enables the digest-pinned variants. */
  readonly digest?: string | null;
  /** Cosign verify identity/issuer regexes from the repo configuration. */
  readonly cosign?: OciCosignVerifyOptions;
}

interface ParsedComponent {
  readonly host: string;
  readonly port: string | null;
  readonly authority: string;
  readonly repo: string;
  readonly imageName: string;
}

/**
 * Split "host[:port]/[repo/]image" into its parts, mirroring the ExtJS controller
 * parser. The component name is allowed to contain dots (e.g. nexus.example.com)
 * because docker hosts are FQDNs.
 */
export function parseOciComponent(name: string): ParsedComponent {
  const firstSlash = name.indexOf('/');
  const authority = firstSlash === -1 ? name : name.substring(0, firstSlash);
  const remainder = firstSlash === -1 ? '' : name.substring(firstSlash + 1);

  let host = authority;
  let port: string | null = null;
  const colonIdx = authority.lastIndexOf(':');
  if (colonIdx > -1) {
    host = authority.substring(0, colonIdx);
    port = authority.substring(colonIdx + 1);
  }

  let repo = '';
  let imageName = remainder;
  const remSlash = remainder.indexOf('/');
  if (remSlash > -1) {
    repo = remainder.substring(0, remSlash);
    imageName = remainder.substring(remSlash + 1);
  }

  return {
    host,
    port,
    authority,
    repo,
    imageName: imageName || remainder,
  };
}

const isLocalhost = (host: string): boolean =>
  host === 'localhost' || host === '127.0.0.1' || host === '0.0.0.0';

/**
 * Build the OCI usage snippet list for a given component.
 *
 * Returns an empty array when the inputs cannot make a valid command (missing
 * name, etc.) so the Usage tab falls through to its empty state.
 */
export function getOciUsageSnippets(input: OciSnippetInputs): OciSnippet[] {
  if (!input || !input.name) return [];

  const name = input.name;
  const version = input.version || 'latest';
  const digest = input.digest || null;
  const cosign = input.cosign || {};
  const identityRegex = (cosign.identityRegex && cosign.identityRegex.trim()) || '.*';
  const issuerRegex = (cosign.issuerRegex && cosign.issuerRegex.trim()) || '.*';

  const parts = parseOciComponent(name);
  const plainHttpFlag = isLocalhost(parts.host) ? ' --plain-http' : '';
  const hostPort = parts.authority;
  const repoPath = parts.repo ? '/' + parts.repo : '';
  const imageRef = `${name}:${version}`;
  const digestRef = digest ? `${name}@${digest}` : null;
  const indent = '  ';
  const shortName = name.substring(name.lastIndexOf('/') + 1);
  const buildxTarget = `${hostPort}${repoPath}/${parts.imageName}:${version}`;

  const snippets: OciSnippet[] = [
    {
      title: 'Docker Pull (tag)',
      code: `docker pull ${imageRef}`,
    },
    ...(digestRef
      ? [{ title: 'Docker Pull (digest)', code: `docker pull ${digestRef}` }]
      : []),
    {
      title: 'Podman Pull',
      code: `podman pull ${imageRef}`,
    },
    {
      title: 'Docker Buildx Push',
      code: `docker buildx build --push --tag ${buildxTarget} .`,
    },
    {
      title: 'Docker Buildx Push (multi-platform)',
      code: `docker buildx build --push \\\n` +
        `${indent}--platform linux/amd64,linux/arm64 \\\n` +
        `${indent}--tag ${buildxTarget} .`,
    },
    {
      title: 'Dockerfile',
      code: digestRef
        ? `FROM ${digestRef}`
        : `FROM ${imageRef}`,
    },
    {
      title: 'ORAS Push',
      code: `oras push ${hostPort}${repoPath}/${parts.imageName}:${version}` +
        ` ./file.tar${plainHttpFlag}`,
    },
    {
      title: 'ORAS Pull',
      code: `oras pull ${imageRef}${plainHttpFlag}`,
    },
    {
      title: 'ORAS Attach',
      code: `oras attach --artifact-type application/vnd.example.sbom.v1+json ` +
        `${imageRef} ./sbom.json${plainHttpFlag}`,
    },
    {
      title: 'Helm Push (OCI)',
      code: `helm push mychart-1.0.0.tgz oci://${hostPort}${repoPath}`,
    },
    {
      title: 'Helm Install (OCI)',
      code: `helm install myrel oci://${hostPort}${repoPath}/${parts.imageName}` +
        ` --version ${version}`,
    },
    {
      title: 'Helm Install (Bitnami)',
      code: `helm install myrelease oci://${hostPort}${repoPath}/bitnamicharts/postgresql` +
        ` --version ${version}`,
    },
    {
      title: 'Cosign Sign',
      code: `cosign sign ${digest
          ? `${hostPort}${repoPath}/${parts.imageName}@${digest}`
          : `${hostPort}${repoPath}/${parts.imageName}@<digest>`}`,
    },
    {
      title: 'Cosign Verify (keyless)',
      code: `cosign verify ${imageRef} \\\n` +
        `${indent}--certificate-identity-regexp '${identityRegex}' \\\n` +
        `${indent}--certificate-oidc-issuer-regexp '${issuerRegex}'`,
    },
    {
      title: 'OpenTofu (tofu init)',
      code: `tofu init\n` +
        `# configure in your .tofurc / CLI config:\n` +
        `oci_credentials "${hostPort}" {\n` +
        `${indent}username = "<nexus-user>"\n` +
        `${indent}password = "<nexus-token>"\n` +
        `}`,
    },
    {
      title: 'Skopeo Copy',
      code: `skopeo copy docker://${imageRef} oci:./${parts.imageName}:${version}`,
    },
    {
      title: 'Kubernetes',
      code: `apiVersion: apps/v1\n` +
        `kind: Deployment\n` +
        `metadata:\n` +
        `${indent}name: ${shortName}\n` +
        `spec:\n` +
        `${indent}replicas: 1\n` +
        `${indent}selector:\n` +
        `${indent}${indent}matchLabels:\n` +
        `${indent}${indent}${indent}app: ${shortName}\n` +
        `${indent}template:\n` +
        `${indent}${indent}metadata:\n` +
        `${indent}${indent}${indent}labels:\n` +
        `${indent}${indent}${indent}${indent}app: ${shortName}\n` +
        `${indent}${indent}spec:\n` +
        `${indent}${indent}${indent}containers:\n` +
        `${indent}${indent}${indent}- name: ${shortName}\n` +
        `${indent}${indent}${indent}${indent}image: ${digestRef || imageRef}`,
    },
  ];

  return snippets;
}
