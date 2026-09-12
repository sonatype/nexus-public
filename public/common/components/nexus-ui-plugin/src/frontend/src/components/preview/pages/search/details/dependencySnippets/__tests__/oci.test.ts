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

import { generate } from '../oci';
import type { SnippetAssetModel, SnippetComponentModel } from '../types';

function byName(component: SnippetComponentModel, asset?: SnippetAssetModel) {
  const map: Record<string, string> = {};
  for (const s of generate(component, asset)) map[s.displayName] = s.snippetText;
  return map;
}

describe('oci dependency snippet generator', () => {
  const localhost: SnippetComponentModel = {
    format: 'oci',
    group: '',
    name: 'localhost:8081/myrepo/myimage',
    version: '1.0',
  };

  it('emits the Classic tool set in order', () => {
    expect(generate(localhost).map((s) => s.displayName)).toEqual([
      'OCI Pull (ORAS)',
      'ORAS Push',
      'Docker Pull',
      'Podman Pull',
      'Docker Buildx Push',
      'Dockerfile',
      'Helm Push (OCI)',
      'Helm Install (OCI)',
      'Helm Install (Bitnami charts)',
      'Skopeo Copy',
      'Cosign Sign',
      'Cosign Verify (keyless)',
      'OpenTofu (tofu init)',
      'Kubernetes',
    ]);
  });

  it('parses host:port/repo/name and appends --plain-http for localhost', () => {
    const s = byName(localhost);
    expect(s['OCI Pull (ORAS)']).toBe('oras pull localhost:8081/myrepo/myimage:1.0 --plain-http');
    expect(s['ORAS Push']).toBe(
      'oras push localhost:8081/myrepo/myimage:1.0 ./file.tar --plain-http',
    );
    expect(s['Docker Pull']).toBe('docker pull localhost:8081/myrepo/myimage:1.0');
    expect(s['Podman Pull']).toBe('podman pull localhost:8081/myrepo/myimage:1.0');
    expect(s['Docker Buildx Push']).toBe(
      'docker buildx build --push --tag localhost:8081/myrepo/myimage:1.0 .',
    );
    expect(s['Dockerfile']).toBe('FROM localhost:8081/myrepo/myimage:1.0');
    expect(s['Helm Push (OCI)']).toBe('helm push mychart-1.0.0.tgz oci://localhost:8081/myrepo');
    expect(s['Helm Install (OCI)']).toBe(
      'helm install myrel oci://localhost:8081/myrepo/myimage --version 1.0',
    );
    expect(s['Helm Install (Bitnami charts)']).toBe(
      'helm install myrel oci://localhost:8081/bitnamicharts/postgresql',
    );
    expect(s['Skopeo Copy']).toBe(
      'skopeo copy docker://localhost:8081/myrepo/myimage:1.0 oci:./myimage:1.0',
    );
    expect(s['Cosign Verify (keyless)']).toBe(
      "cosign verify localhost:8081/myrepo/myimage:1.0 \\\n" +
        "  --certificate-identity-regexp '.*' \\\n" +
        "  --certificate-oidc-issuer-regexp '.*'",
    );
    expect(s['OpenTofu (tofu init)']).toBe(
      'tofu init\n' +
        '# configure in your .tofurc / CLI config:\n' +
        'oci_credentials "localhost:8081" {\n' +
        '  username = "<nexus-user>"\n' +
        '  password = "<nexus-token>"\n' +
        '}',
    );
    expect(s['Kubernetes']).toBe(
      'spec:\n  containers:\n  - name: myimage\n    image: localhost:8081/myrepo/myimage:1.0',
    );
  });

  it('uses the <digest> placeholder for Cosign Sign when no digest is present', () => {
    expect(byName(localhost)['Cosign Sign']).toBe('cosign sign localhost:8081/myrepo/myimage@<digest>');
  });

  it('omits --plain-http for a non-localhost host and uses the asset digest', () => {
    const remote: SnippetComponentModel = {
      format: 'oci',
      group: '',
      name: 'nexus.example.com/myrepo/myimage',
      version: '2.0',
    };
    const s = byName(remote, { digest: 'sha256:abc' });
    expect(s['OCI Pull (ORAS)']).toBe('oras pull nexus.example.com/myrepo/myimage:2.0');
    expect(s['Cosign Sign']).toBe('cosign sign nexus.example.com/myrepo/myimage@sha256:abc');
  });

  it('returns no snippets when name or version is missing', () => {
    expect(generate({ ...localhost, version: '' })).toEqual([]);
    expect(generate({ ...localhost, name: '' })).toEqual([]);
  });
});
