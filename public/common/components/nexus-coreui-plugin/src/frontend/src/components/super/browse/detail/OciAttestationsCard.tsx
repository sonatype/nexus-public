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

import React, { useEffect, useState } from 'react';
import {
  Badge,
  Box,
  Button,
  Card,
  Code,
  Flex,
  Heading,
  Spinner,
  Table,
  Text,
  Tooltip,
} from '@radix-ui/themes';
import { Copy, Download, FileCheck2, FileText, KeyRound, Shield } from 'lucide-react';

import { restClient } from '@/utils/api';

interface AttestationRow {
  digest: string;
  artifactType: string;
  format:
    | 'COSIGN'
    | 'SIGSTORE_BUNDLE'
    | 'NOTARY'
    | 'CYCLONEDX'
    | 'SPDX'
    | 'IN_TOTO'
    | 'SLSA'
    | 'OPENVEX'
    | 'VULN_REPORT'
    | 'OTHER'
    | string;
  downloadUrl?: string;
}

interface AttestationsResponse {
  attestations?: AttestationRow[];
}

interface RawAsset {
  path?: string;
  contentType?: string;
  oci?: { content_digest?: string };
}

interface RawComponentResponse {
  assets?: RawAsset[];
}

const MANIFEST_DIGEST_PATTERN = /manifests\/(sha256:[a-f0-9]{64})/i;
const OCI_MANIFEST_CONTENT_TYPES = new Set([
  'application/vnd.oci.image.manifest.v1+json',
  'application/vnd.oci.image.index.v1+json',
  'application/vnd.docker.distribution.manifest.v2+json',
  'application/vnd.docker.distribution.manifest.list.v2+json',
]);

/**
 * Extract the manifest digest from a list of assets.
 *
 * Strategy (in priority order):
 * 1. {@code asset.oci.content_digest} — set by the server on every OCI manifest asset;
 *    works for both tag assets ({@code /v2/name/manifests/v1.0}) and digest assets.
 * 2. Path pattern {@code manifests/sha256:<hex>} — the canonical OCI manifest URL shape.
 * 3. Fallback: asset whose contentType is a known manifest media type and whose path
 *    contains a {@code sha256:<hex>} segment.
 *
 * Returns null if no manifest asset is found, e.g. for proxy components that have not
 * yet cached the manifest.
 */
function findManifestDigest(assets: RawAsset[] | undefined): string | null {
  if (!assets) return null;
  // Priority 1: server-provided oci.content_digest (present on both tag and digest assets)
  for (const asset of assets) {
    if (asset.oci?.content_digest) return asset.oci.content_digest;
  }
  // Priority 2: sha256 digest embedded in the path
  for (const asset of assets) {
    const match = asset.path?.match(MANIFEST_DIGEST_PATTERN);
    if (match) return match[1];
  }
  // Priority 3: manifest content-type + sha256 anywhere in path
  for (const asset of assets) {
    if (asset.contentType && OCI_MANIFEST_CONTENT_TYPES.has(asset.contentType)) {
      const inner = asset.path?.match(/(sha256:[a-f0-9]{64})/i);
      if (inner) return inner[1];
    }
  }
  return null;
}

/**
 * Truncate a sha256 digest for display: "sha256:abcdef12…f09a1234".
 * Shows 8 hex chars at the start and 8 at the end of the hex portion.
 */
function truncateDigest(digest: string): string {
  const match = digest.match(/^(sha256:)([a-f0-9]{64})$/i);
  if (!match) return digest;
  return `${match[1]}${match[2].slice(0, 8)}…${match[2].slice(-8)}`;
}

/**
 * Minimal shape used to fetch attestations. Both {@code ComponentData} (from DetailPanel) and
 * {@code ComponentXO} (from ComponentDetailPanel) satisfy this — id + repositoryName are the
 * only fields the attestations lookup actually needs.
 */
interface AttestationsComponent {
  id: string;
  repositoryName: string;
}

interface OciAttestationsCardProps {
  componentData: AttestationsComponent;
}

/**
 * Inner content for OCI Attestations — shared between the card variant (search/component detail)
 * and the tab variant (browse tree asset detail). Renders the heading, description, and the
 * loading/error/table states. Does NOT add a Card wrapper — callers decide the container.
 */
function OciAttestationsContent({ componentData }: OciAttestationsCardProps): JSX.Element {
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [digest, setDigest] = useState<string | null>(null);
  const [rows, setRows] = useState<AttestationRow[]>([]);

  useEffect(() => {
    let cancelled = false;
    async function load() {
      setLoading(true);
      setError(null);
      setRows([]);
      setDigest(null);
      try {
        // componentData.id is already the REST-encoded base64 ID (e.g. from ComponentXO.id
        // or the REST response's "id" field). Do NOT re-encode it.
        const component = await restClient.get<RawComponentResponse>(
          `/service/rest/v1/components/${componentData.id}`
        );
        const subjectDigest = findManifestDigest(component.assets);
        if (!subjectDigest) {
          if (!cancelled) setLoading(false);
          return;
        }
        if (!cancelled) setDigest(subjectDigest);
        const params = new URLSearchParams({
          repo: componentData.repositoryName,
          digest: subjectDigest,
        });
        const data = await restClient.get<AttestationsResponse>(
          `/service/rest/internal/oci/attestations?${params.toString()}`
        );
        if (!cancelled) {
          setRows(data.attestations ?? []);
          setLoading(false);
        }
      }
      catch (err) {
        if (!cancelled) {
          setError(err instanceof Error ? err.message : 'Failed to load attestations');
          setLoading(false);
        }
      }
    }
    load();
    return () => {
      cancelled = true;
    };
  }, [componentData.id, componentData.repositoryName]);

  return (
    <>
      <Heading size="3" mb="2">Attestations</Heading>
      <Text size="1" color="gray" mb="3" as="p">
        OCI 1.1 referrers attached to this image — SBOMs, cosign signatures, in-toto attestations.
      </Text>
      {loading && (
        <Flex align="center" gap="2">
          <Spinner size="1" />
          <Text size="2">Loading attestations…</Text>
        </Flex>
      )}
      {!loading && error && (
        <Text size="2" color="red">
          Could not load attestations: {error}
        </Text>
      )}
      {!loading && !error && !digest && (
        <Text size="2" color="gray">
          No manifest asset found for this component, so no attestations to display.
        </Text>
      )}
      {!loading && !error && digest && (
        <>
          <Flex align="center" gap="2" mb="3">
            <Text size="1" color="gray" weight="medium">Subject digest:</Text>
            <Tooltip content={digest}>
              <Code size="1">{truncateDigest(digest)}</Code>
            </Tooltip>
            <CopyButton value={digest} label="Copy subject digest" />
          </Flex>
          {rows.length === 0 ? (
            <Text size="2" color="gray">
              No attestations attached to this image.
            </Text>
          ) : (
            <AttestationsTable rows={rows} />
          )}
        </>
      )}
    </>
  );
}

/**
 * OCI Attestations card for the React preview component-detail panel.
 *
 * Wraps {@code OciAttestationsContent} in a {@code Card} so it sits as a peer of the Summary
 * card in {@code ComponentDetailPanel} (search result detail page). Uses the same heading size
 * and spacing as the Summary card — {@code <Heading size="3" mb="2">} directly inside the card
 * with no inner Box wrapper or Separator — so the header background and left alignment match.
 *
 * For the browse-tree tab context (asset leaf node), {@code DetailPanel} renders
 * {@code OciAttestationsContent} directly inside a {@code Tabs.Content} box so no extra card
 * nesting occurs.
 *
 * Mirrors the classic ExtJS AttestationsTab (NX.oci.view.component.AttestationsTab): resolves
 * the manifest digest from the component's assets, then GETs
 * {@code /service/rest/internal/oci/attestations?repo=&digest=} and renders a row per referrer
 * (SBOM, cosign signature, in-toto attestation). Closes the cloud-side gap reported in
 * NEXUS-52732 — the classic ExtJS panel was wired earlier but the Cloud distribution runs the
 * React preview, where no equivalent existed.
 */
export function OciAttestationsCard({ componentData }: OciAttestationsCardProps): JSX.Element {
  return (
    <Card>
      <OciAttestationsContent componentData={componentData} />
    </Card>
  );
}

/**
 * Bare content variant for use inside an existing card container (e.g. the browse-tree
 * asset-detail tab panel). Renders heading, description, and attestation rows without adding
 * a Card wrapper, preventing double-card nesting.
 */
export function OciAttestationsTabContent({ componentData }: OciAttestationsCardProps): JSX.Element {
  return <OciAttestationsContent componentData={componentData} />;
}

function AttestationsTable({ rows }: { rows: AttestationRow[] }): JSX.Element {
  return (
    <Table.Root size="1">
      <Table.Header>
        <Table.Row>
          <Table.ColumnHeaderCell>Format</Table.ColumnHeaderCell>
          <Table.ColumnHeaderCell>Artifact Type</Table.ColumnHeaderCell>
          <Table.ColumnHeaderCell>Digest</Table.ColumnHeaderCell>
          <Table.ColumnHeaderCell>Type</Table.ColumnHeaderCell>
          <Table.ColumnHeaderCell aria-label="Download" />
        </Table.Row>
      </Table.Header>
      <Table.Body>
        {rows.map((row) => (
          <Table.Row key={row.digest}>
            <Table.Cell>
              <FormatBadge format={row.format} artifactType={row.artifactType} />
            </Table.Cell>
            <Table.Cell>
              <ArtifactTypeCell artifactType={row.artifactType} />
            </Table.Cell>
            <Table.Cell>
              <Flex align="center" gap="1">
                <Tooltip content={row.digest}>
                  <Code size="1">{truncateDigest(row.digest)}</Code>
                </Tooltip>
                <CopyButton value={row.digest} label="Copy digest" />
              </Flex>
            </Table.Cell>
            <Table.Cell>
              <AttestationType format={row.format} />
            </Table.Cell>
            <Table.Cell>
              {row.downloadUrl && (
                <Button
                  variant="ghost"
                  size="1"
                  onClick={() => window.open(row.downloadUrl, '_blank', 'noopener,noreferrer')}
                  aria-label="Download attestation"
                >
                  <Download size={14} />
                </Button>
              )}
            </Table.Cell>
          </Table.Row>
        ))}
      </Table.Body>
    </Table.Root>
  );
}

/**
 * Color-coded badge for the classified attestation format.
 *
 * <p>
 * For unrecognised formats (backend returns {@code OTHER} or an empty value), display the raw
 * artifactType — shortened by stripping the {@code application/vnd.} / {@code application/}
 * prefix — instead of the unhelpful literal string "Unknown". The full type stays available in
 * the Artifact Type column with its tooltip.
 */
function FormatBadge({ format, artifactType }: { format: string; artifactType?: string | null }): JSX.Element {
  switch (format) {
    case 'COSIGN':
      return <Badge color="blue" size="1">cosign (legacy)</Badge>;
    case 'SIGSTORE_BUNDLE':
      return <Badge color="blue" size="1">Sigstore Bundle</Badge>;
    case 'NOTARY':
      return <Badge color="indigo" size="1">Notary</Badge>;
    case 'CYCLONEDX':
      return <Badge color="green" size="1">CycloneDX</Badge>;
    case 'SPDX':
      return <Badge color="teal" size="1">SPDX</Badge>;
    case 'IN_TOTO':
      return <Badge color="purple" size="1">in-toto</Badge>;
    case 'SLSA':
      return <Badge color="orange" size="1">SLSA</Badge>;
    case 'OPENVEX':
      return <Badge color="ruby" size="1">OpenVEX</Badge>;
    case 'VULN_REPORT':
      return <Badge color="red" size="1">Vulnerability Report</Badge>;
    default: {
      // Show the raw artifactType (lightly shortened) so operators can still recognise the
      // payload kind even when Nexus didn't classify it. Falls back to "—" if no artifactType
      // is present, never to the misleading literal "Unknown".
      const display = artifactType
        ? artifactType.replace('application/vnd.', '').replace('application/', '')
        : '—';
      return <Badge color="gray" size="1">{display}</Badge>;
    }
  }
}

/**
 * Human-readable attestation type label.
 *
 * <p>
 * Taxonomy follows the OCI 1.1 referrer ecosystem: SBOM | Signature | Attestation | Unknown.
 *
 * <ul>
 *   <li>Signature — anything that asserts authorship/integrity (cosign legacy + Sigstore bundle +
 *       Notary v2).</li>
 *   <li>SBOM — software bill of materials (CycloneDX, SPDX).</li>
 *   <li>Attestation — an in-toto statement. Reserved for in-toto specifically because the
 *       in-toto spec defines "attestation" as a DSSE-signed Statement; calling everything an
 *       attestation is incorrect. Future refinement (reading the inner predicateType) will split
 *       this into Provenance (SLSA), VEX (OpenVEX), etc.</li>
 *   <li>Unknown — default. The Format column shows the raw artifactType, so the row is still
 *       interpretable.</li>
 * </ul>
 *
 * <p>
 * NOTE: The backend's AttestationEntry DTO does not include a {@code verified} field — the API
 * response never carries signature verification status. The column therefore shows a neutral
 * descriptive label (e.g. "Signature", "SBOM") rather than a verified/unverified indicator
 * which would be permanently misleading.
 */
function AttestationType({ format }: { format: string }): JSX.Element {
  switch (format) {
    case 'COSIGN':
    case 'SIGSTORE_BUNDLE':
    case 'NOTARY':
      return (
        <Flex align="center" gap="1">
          <KeyRound size={13} color="var(--blue-9)" />
          <Text size="1" color="blue">Signature</Text>
        </Flex>
      );
    case 'CYCLONEDX':
    case 'SPDX':
      return (
        <Flex align="center" gap="1">
          <FileText size={13} color="var(--green-9)" />
          <Text size="1" color="green">SBOM</Text>
        </Flex>
      );
    case 'IN_TOTO':
      return (
        <Flex align="center" gap="1">
          <FileCheck2 size={13} color="var(--purple-9)" />
          <Text size="1" color="purple">Attestation</Text>
        </Flex>
      );
    case 'SLSA':
      return (
        <Flex align="center" gap="1">
          <FileCheck2 size={13} color="var(--orange-9)" />
          <Text size="1" color="orange">Provenance</Text>
        </Flex>
      );
    case 'OPENVEX':
      return (
        <Flex align="center" gap="1">
          <Shield size={13} color="var(--ruby-9)" />
          <Text size="1" color="ruby">VEX</Text>
        </Flex>
      );
    case 'VULN_REPORT':
      return (
        <Flex align="center" gap="1">
          <Shield size={13} color="var(--red-9)" />
          <Text size="1" color="red">Vulnerability Report</Text>
        </Flex>
      );
    default:
      return (
        <Flex align="center" gap="1">
          <Shield size={13} color="var(--gray-9)" />
          <Text size="1" color="gray">Unknown</Text>
        </Flex>
      );
  }
}

/** Truncate a long MIME-type artifactType string with the full value in a tooltip. */
function ArtifactTypeCell({ artifactType }: { artifactType: string | null | undefined }): JSX.Element {
  if (!artifactType) {
    return <Text size="1" color="gray">—</Text>;
  }
  // Shorten known verbose prefixes for display while keeping the full string accessible
  const display = artifactType
    .replace('application/vnd.', '')
    .replace('application/', '');
  const needsTooltip = display !== artifactType;
  if (needsTooltip) {
    return (
      <Tooltip content={artifactType}>
        <Text size="1" style={{ cursor: 'default' }}>{display}</Text>
      </Tooltip>
    );
  }
  return <Text size="1">{artifactType}</Text>;
}

/** Small clipboard copy button. */
function CopyButton({ value, label }: { value: string; label: string }): JSX.Element {
  const [copied, setCopied] = useState(false);

  const handleCopy = () => {
    navigator.clipboard.writeText(value).then(() => {
      setCopied(true);
      setTimeout(() => setCopied(false), 1500);
    });
  };

  return (
    <Tooltip content={copied ? 'Copied!' : label}>
      <Button
        variant="ghost"
        size="1"
        onClick={handleCopy}
        aria-label={label}
        style={{ padding: '0 2px' }}
      >
        <Copy size={12} color={copied ? 'var(--green-9)' : 'var(--gray-9)'} />
      </Button>
    </Tooltip>
  );
}
