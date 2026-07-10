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

import {
  getOciUsageSnippets,
  parseOciComponent,
} from '../oci-snippets';

describe('parseOciComponent', () => {
  it('parses host:port/repo/image', () => {
    const p = parseOciComponent('nexus.example.com:8082/oci-hosted/alpine');
    expect(p).toEqual({
      host: 'nexus.example.com',
      port: '8082',
      authority: 'nexus.example.com:8082',
      repo: 'oci-hosted',
      imageName: 'alpine',
    });
  });

  it('parses path-routed host/repo/image', () => {
    const p = parseOciComponent('nexus.example.com/oci-hosted/alpine');
    expect(p.host).toBe('nexus.example.com');
    expect(p.port).toBeNull();
    expect(p.repo).toBe('oci-hosted');
    expect(p.imageName).toBe('alpine');
  });

  it('parses host:port/image (no repo segment)', () => {
    const p = parseOciComponent('localhost:5000/alpine');
    expect(p.host).toBe('localhost');
    expect(p.port).toBe('5000');
    expect(p.repo).toBe('');
    expect(p.imageName).toBe('alpine');
  });
});

describe('getOciUsageSnippets', () => {
  const PATH_HOSTED = {
    name: 'nexus.example.com/oci-hosted/alpine',
    version: '3.20',
  };
  const PORT_HOSTED = {
    name: 'nexus.example.com:8082/oci-hosted/alpine',
    version: '3.20',
  };
  const DIGEST = 'sha256:abc123def456';

  function titles(input: Parameters<typeof getOciUsageSnippets>[0]) {
    return getOciUsageSnippets(input).map((s) => s.title);
  }

  it('returns no snippets when name is missing', () => {
    expect(getOciUsageSnippets({ name: '' })).toEqual([]);
  });

  it('emits the full toolchain on path-routed hosted', () => {
    const t = titles(PATH_HOSTED);
    expect(t).toEqual([
      'Docker Pull (tag)',
      'Podman Pull',
      'Docker Buildx Push',
      'Docker Buildx Push (multi-platform)',
      'Dockerfile',
      'ORAS Push',
      'ORAS Pull',
      'ORAS Attach',
      'Helm Push (OCI)',
      'Helm Install (OCI)',
      'Helm Install (Bitnami)',
      'Cosign Sign',
      'Cosign Verify (keyless)',
      'OpenTofu (tofu init)',
      'Skopeo Copy',
      'Kubernetes',
    ]);
  });

  it('adds the digest-pinned docker pull when digest is supplied', () => {
    const t = titles({ ...PATH_HOSTED, digest: DIGEST });
    expect(t).toContain('Docker Pull (digest)');
    const digestSnippet = getOciUsageSnippets({ ...PATH_HOSTED, digest: DIGEST }).find(
      (s) => s.title === 'Docker Pull (digest)'
    );
    expect(digestSnippet?.code).toBe(
      `docker pull nexus.example.com/oci-hosted/alpine@${DIGEST}`
    );
  });

  it('uses port authority on port-routed components', () => {
    const snippets = getOciUsageSnippets(PORT_HOSTED);
    const dockerPull = snippets.find((s) => s.title === 'Docker Pull (tag)');
    expect(dockerPull?.code).toBe('docker pull nexus.example.com:8082/oci-hosted/alpine:3.20');
    const helmPush = snippets.find((s) => s.title === 'Helm Push (OCI)');
    expect(helmPush?.code).toBe('helm push mychart-1.0.0.tgz oci://nexus.example.com:8082/oci-hosted');
  });

  it('builds path-style helm install with version', () => {
    const helmInstall = getOciUsageSnippets(PATH_HOSTED).find(
      (s) => s.title === 'Helm Install (OCI)'
    );
    expect(helmInstall?.code).toBe(
      'helm install myrel oci://nexus.example.com/oci-hosted/alpine --version 3.20'
    );
  });

  it('cosign sign uses the supplied digest when present', () => {
    const signed = getOciUsageSnippets({ ...PATH_HOSTED, digest: DIGEST }).find(
      (s) => s.title === 'Cosign Sign'
    );
    expect(signed?.code).toBe(
      `cosign sign nexus.example.com/oci-hosted/alpine@${DIGEST}`
    );
  });

  it('cosign sign falls back to a placeholder when no digest is known', () => {
    const signed = getOciUsageSnippets(PATH_HOSTED).find((s) => s.title === 'Cosign Sign');
    expect(signed?.code).toBe(
      'cosign sign nexus.example.com/oci-hosted/alpine@<digest>'
    );
  });

  it('cosign verify pre-fills identity and issuer regexes from cosign config', () => {
    const verify = getOciUsageSnippets({
      ...PATH_HOSTED,
      cosign: {
        identityRegex: '^https://github\\.com/acme/.*$',
        issuerRegex: '^https://token\\.actions\\.githubusercontent\\.com$',
      },
    }).find((s) => s.title === 'Cosign Verify (keyless)');
    expect(verify?.code).toContain(
      "--certificate-identity-regexp '^https://github\\.com/acme/.*$'"
    );
    expect(verify?.code).toContain(
      "--certificate-oidc-issuer-regexp '^https://token\\.actions\\.githubusercontent\\.com$'"
    );
  });

  it('cosign verify falls back to ".*" regexes when cosign config is absent', () => {
    const verify = getOciUsageSnippets(PATH_HOSTED).find(
      (s) => s.title === 'Cosign Verify (keyless)'
    );
    expect(verify?.code).toContain("--certificate-identity-regexp '.*'");
    expect(verify?.code).toContain("--certificate-oidc-issuer-regexp '.*'");
  });

  it('appends --plain-http to oras commands when host is localhost', () => {
    const localhost = getOciUsageSnippets({ name: 'localhost:5000/oci-hosted/alpine', version: 'latest' });
    const orasPull = localhost.find((s) => s.title === 'ORAS Pull');
    const orasPush = localhost.find((s) => s.title === 'ORAS Push');
    expect(orasPull?.code).toMatch(/--plain-http$/);
    expect(orasPush?.code).toMatch(/--plain-http$/);
  });

  it('omits --plain-http for non-localhost hosts', () => {
    const orasPull = getOciUsageSnippets(PATH_HOSTED).find((s) => s.title === 'ORAS Pull');
    expect(orasPull?.code).not.toContain('--plain-http');
  });

  it('falls back to "latest" when version is null/undefined', () => {
    const snippets = getOciUsageSnippets({ name: 'nexus.example.com/r/img' });
    const dockerPull = snippets.find((s) => s.title === 'Docker Pull (tag)');
    expect(dockerPull?.code).toBe('docker pull nexus.example.com/r/img:latest');
  });

  it('renders skopeo copy with image name and version only', () => {
    const skopeo = getOciUsageSnippets(PATH_HOSTED).find((s) => s.title === 'Skopeo Copy');
    expect(skopeo?.code).toBe(
      'skopeo copy docker://nexus.example.com/oci-hosted/alpine:3.20 oci:./alpine:3.20'
    );
  });

  it('renders an OpenTofu init block including the host:port authority', () => {
    const tofu = getOciUsageSnippets(PORT_HOSTED).find(
      (s) => s.title === 'OpenTofu (tofu init)'
    );
    expect(tofu?.code).toContain('oci_credentials "nexus.example.com:8082" {');
  });

  describe('Docker Buildx Push', () => {
    it('emits a single-platform buildx push for path-routed hosted', () => {
      const buildx = getOciUsageSnippets(PATH_HOSTED).find(
        (s) => s.title === 'Docker Buildx Push'
      );
      expect(buildx?.code).toBe(
        'docker buildx build --push --tag nexus.example.com/oci-hosted/alpine:3.20 .'
      );
    });

    it('emits a single-platform buildx push for port-routed hosted', () => {
      const buildx = getOciUsageSnippets(PORT_HOSTED).find(
        (s) => s.title === 'Docker Buildx Push'
      );
      expect(buildx?.code).toBe(
        'docker buildx build --push --tag nexus.example.com:8082/oci-hosted/alpine:3.20 .'
      );
    });

    it('emits a multi-platform buildx push variant with linux/amd64,linux/arm64', () => {
      const buildx = getOciUsageSnippets(PATH_HOSTED).find(
        (s) => s.title === 'Docker Buildx Push (multi-platform)'
      );
      expect(buildx?.code).toContain('--platform linux/amd64,linux/arm64');
      expect(buildx?.code).toContain(
        '--tag nexus.example.com/oci-hosted/alpine:3.20'
      );
    });
  });

  describe('Dockerfile', () => {
    it('uses tag form when no digest is supplied', () => {
      const dockerfile = getOciUsageSnippets(PATH_HOSTED).find(
        (s) => s.title === 'Dockerfile'
      );
      expect(dockerfile?.code).toBe('FROM nexus.example.com/oci-hosted/alpine:3.20');
    });

    it('pins to digest when digest is supplied', () => {
      const dockerfile = getOciUsageSnippets({
        ...PATH_HOSTED,
        digest: DIGEST,
      }).find((s) => s.title === 'Dockerfile');
      expect(dockerfile?.code).toBe(
        `FROM nexus.example.com/oci-hosted/alpine@${DIGEST}`
      );
    });

    it('renders correctly for proxy-style host:port authority', () => {
      const dockerfile = getOciUsageSnippets(PORT_HOSTED).find(
        (s) => s.title === 'Dockerfile'
      );
      expect(dockerfile?.code).toBe(
        'FROM nexus.example.com:8082/oci-hosted/alpine:3.20'
      );
    });
  });

  describe('Helm Install (Bitnami)', () => {
    it('renders bitnamicharts/postgresql install for path-routed hosted', () => {
      const helmBitnami = getOciUsageSnippets(PATH_HOSTED).find(
        (s) => s.title === 'Helm Install (Bitnami)'
      );
      // Path-routed installs surface <host>/<repo>/bitnamicharts/postgresql so the
      // proxy repository segment is preserved for path-based registries.
      expect(helmBitnami?.code).toBe(
        'helm install myrelease oci://nexus.example.com/oci-hosted/bitnamicharts/postgresql --version 3.20'
      );
    });

    it('renders bitnamicharts/postgresql install for port-routed proxy', () => {
      const helmBitnami = getOciUsageSnippets(PORT_HOSTED).find(
        (s) => s.title === 'Helm Install (Bitnami)'
      );
      expect(helmBitnami?.code).toBe(
        'helm install myrelease oci://nexus.example.com:8082/oci-hosted/bitnamicharts/postgresql --version 3.20'
      );
    });

    it('omits the repository segment when the component has no repo path', () => {
      // localhost:5000/alpine has no repo segment between authority and image.
      const helmBitnami = getOciUsageSnippets({
        name: 'localhost:5000/alpine',
        version: 'latest',
      }).find((s) => s.title === 'Helm Install (Bitnami)');
      expect(helmBitnami?.code).toBe(
        'helm install myrelease oci://localhost:5000/bitnamicharts/postgresql --version latest'
      );
    });
  });

  describe('Kubernetes', () => {
    it('emits a Deployment manifest referencing the image:tag', () => {
      const k8s = getOciUsageSnippets(PATH_HOSTED).find((s) => s.title === 'Kubernetes');
      expect(k8s?.code).toContain('apiVersion: apps/v1');
      expect(k8s?.code).toContain('kind: Deployment');
      expect(k8s?.code).toContain('image: nexus.example.com/oci-hosted/alpine:3.20');
      expect(k8s?.code).toContain('name: alpine');
    });

    it('pins the image to digest when digest is supplied', () => {
      const k8s = getOciUsageSnippets({ ...PATH_HOSTED, digest: DIGEST }).find(
        (s) => s.title === 'Kubernetes'
      );
      expect(k8s?.code).toContain(
        `image: nexus.example.com/oci-hosted/alpine@${DIGEST}`
      );
    });
  });
});
