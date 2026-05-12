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

import React, { useState, useEffect, useMemo, useCallback } from 'react';
import { Box, Flex, Text } from '@radix-ui/themes';
import { 
  Search, 
  ChevronUp, 
  ChevronDown, 
  ChevronsUpDown,
  Loader2,
  AlertCircle,
  Info,
  ExternalLink,
  Pencil,
} from 'lucide-react';

import { useSslCertificatesApi } from './useSslCertificatesApi';
import { 
  SslCertificate, 
  SortDirection, 
  CertificateSortField, 
  SslCertificatesListProps,
  isCertificateExpiring,
  isCertificateExpired,
} from './types';

import './SslCertificatesList.scss';

/**
 * SslCertificatesList - Displays SSL certificates in a searchable, sortable table
 */
export function SslCertificatesList({ onSelect, onCreate }: SslCertificatesListProps) {
  const [certificates, setCertificates] = useState<SslCertificate[]>([]);
  const [filter, setFilter] = useState('');
  const [sortField, setSortField] = useState<CertificateSortField>('subjectCommonName');
  const [sortDirection, setSortDirection] = useState<SortDirection>('asc');
  const [loading, setLoading] = useState(true);

  const { error, setError, fetchCertificates } = useSslCertificatesApi();

  // Load certificates on mount
  useEffect(() => {
    const loadCertificates = async () => {
      setLoading(true);
      try {
        const data = await fetchCertificates();
        setCertificates(data);
      } catch (err: any) {
        setError(err.message);
      } finally {
        setLoading(false);
      }
    };

    loadCertificates();
  }, [fetchCertificates, setError]);

  // Filter certificates
  const filteredCertificates = useMemo(() => {
    if (!filter) return certificates;

    const lowerFilter = filter.toLowerCase();
    return certificates.filter((cert) =>
      cert.subjectCommonName?.toLowerCase().includes(lowerFilter) ||
      cert.subjectOrganization?.toLowerCase().includes(lowerFilter) ||
      cert.issuerOrganization?.toLowerCase().includes(lowerFilter) ||
      cert.fingerprint?.toLowerCase().includes(lowerFilter)
    );
  }, [certificates, filter]);

  // Sort certificates
  const sortedCertificates = useMemo(() => {
    if (!sortDirection) return filteredCertificates;

    return [...filteredCertificates].sort((a, b) => {
      let aVal: string = '';
      let bVal: string = '';

      switch (sortField) {
        case 'subjectCommonName':
          aVal = a.subjectCommonName || '';
          bVal = b.subjectCommonName || '';
          break;
        case 'subjectOrganization':
          aVal = a.subjectOrganization || '';
          bVal = b.subjectOrganization || '';
          break;
        case 'issuerOrganization':
          aVal = a.issuerOrganization || '';
          bVal = b.issuerOrganization || '';
          break;
        case 'fingerprint':
          aVal = a.fingerprint || '';
          bVal = b.fingerprint || '';
          break;
      }

      const comparison = aVal.toLowerCase().localeCompare(bVal.toLowerCase());
      return sortDirection === 'asc' ? comparison : -comparison;
    });
  }, [filteredCertificates, sortField, sortDirection]);

  const handleSort = useCallback((field: CertificateSortField) => {
    if (sortField === field) {
      // Cycle: asc -> desc -> null -> asc
      if (sortDirection === 'asc') {
        setSortDirection('desc');
      } else if (sortDirection === 'desc') {
        setSortDirection(null);
      } else {
        setSortDirection('asc');
      }
    } else {
      setSortField(field);
      setSortDirection('asc');
    }
  }, [sortField, sortDirection]);

  const handleRowClick = useCallback((certificate: SslCertificate) => {
    onSelect(certificate.id);
  }, [onSelect]);

  const renderSortIcon = (field: CertificateSortField) => {
    if (sortField !== field || !sortDirection) {
      return <ChevronsUpDown size={14} className="ssl-certificates-list__sort-icon ssl-certificates-list__sort-icon--inactive" />;
    }
    return sortDirection === 'asc' 
      ? <ChevronUp size={14} className="ssl-certificates-list__sort-icon" />
      : <ChevronDown size={14} className="ssl-certificates-list__sort-icon" />;
  };

  return (
    <Box className="ssl-certificates-list">
      {/* Filters */}
      <Flex gap="4" className="ssl-certificates-list__filters">
        <Box className="ssl-certificates-list__search">
          <Search size={16} className="ssl-certificates-list__search-icon" />
          <input
            type="text"
            placeholder="Filter by name, organization, or fingerprint"
            value={filter}
            onChange={(e) => setFilter(e.target.value)}
            className="ssl-certificates-list__search-input"
          />
        </Box>
      </Flex>

      {/* Error State */}
      {error && (
        <Flex align="center" gap="2" className="ssl-certificates-list__error">
          <AlertCircle size={16} />
          <Text size="2">{error}</Text>
        </Flex>
      )}

      {/* Loading State */}
      {loading && (
        <Flex align="center" justify="center" className="ssl-certificates-list__loading">
          <Loader2 size={24} className="ssl-certificates-list__spinner" />
          <Text size="2">Loading certificates...</Text>
        </Flex>
      )}

      {/* Empty State */}
      {!loading && !error && sortedCertificates.length === 0 && (
        <Box className="ssl-certificates-list__empty">
          <Text size="2">There are no SSL certificates available</Text>
        </Box>
      )}

      {/* Table */}
      {!loading && !error && sortedCertificates.length > 0 && (
        <Box className="ssl-certificates-list__table-wrapper">
          <table className="ssl-certificates-list__table">
            <thead>
              <tr>
                <th onClick={() => handleSort('subjectCommonName')} className="ssl-certificates-list__th ssl-certificates-list__th--sortable">
                  <Flex align="center" gap="1">
                    Name
                    {renderSortIcon('subjectCommonName')}
                  </Flex>
                </th>
                <th onClick={() => handleSort('subjectOrganization')} className="ssl-certificates-list__th ssl-certificates-list__th--sortable">
                  <Flex align="center" gap="1">
                    Issued To
                    {renderSortIcon('subjectOrganization')}
                  </Flex>
                </th>
                <th onClick={() => handleSort('issuerOrganization')} className="ssl-certificates-list__th ssl-certificates-list__th--sortable">
                  <Flex align="center" gap="1">
                    Issued By
                    {renderSortIcon('issuerOrganization')}
                  </Flex>
                </th>
                <th onClick={() => handleSort('fingerprint')} className="ssl-certificates-list__th ssl-certificates-list__th--sortable">
                  <Flex align="center" gap="1">
                    Fingerprint
                    {renderSortIcon('fingerprint')}
                  </Flex>
                </th>
                <th className="ssl-certificates-list__th ssl-certificates-list__th--action"></th>
              </tr>
            </thead>
            <tbody>
              {sortedCertificates.map((certificate) => {
                const isExpired = isCertificateExpired(certificate);
                const isExpiring = !isExpired && isCertificateExpiring(certificate);
                
                return (
                  <tr
                    key={certificate.id}
                    onClick={() => handleRowClick(certificate)}
                    className={`ssl-certificates-list__row ${isExpired ? 'ssl-certificates-list__row--expired' : ''} ${isExpiring ? 'ssl-certificates-list__row--expiring' : ''}`}
                  >
                    <td className="ssl-certificates-list__td">
                      {certificate.subjectCommonName}
                    </td>
                    <td className="ssl-certificates-list__td">
                      {certificate.subjectOrganization}
                    </td>
                    <td className="ssl-certificates-list__td">
                      {certificate.issuerOrganization}
                    </td>
                    <td className="ssl-certificates-list__td">
                      <Text size="1" className="ssl-certificates-list__fingerprint">
                        {certificate.fingerprint}
                      </Text>
                    </td>
                    <td className="ssl-certificates-list__td ssl-certificates-list__td--action">
                      <Pencil size={16} className="ssl-certificates-list__row-edit-icon" />
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        </Box>
      )}

      {/* Help Section */}
      <Box className="ssl-certificates-list__help">
        <Flex align="center" gap="2" className="ssl-certificates-list__help-header">
          <Info size={16} />
          <Text size="2" weight="medium">What is SSL?</Text>
        </Flex>
        <Text size="2" className="ssl-certificates-list__help-text">
          Using Secure Socket Layer (SSL) communication with the repository manager is an important security feature
          and a recommended best practice. Secure communication can be inbound or outbound. Outbound client
          communication may include integration with: proxy repository, email servers, LDAPS servers. Inbound client
          communication includes: web browser HTTPS access, tool access to repository content, usage of REST APIs.
          See our{' '}
          <a 
            href="http://links.sonatype.com/products/nxrm3/docs/ssl-certificate" 
            target="_blank" 
            rel="noopener noreferrer"
            className="ssl-certificates-list__help-link"
          >
            documentation
            <ExternalLink size={12} />
          </a>
          {' '}for more information.
        </Text>
      </Box>
    </Box>
  );
}

export default SslCertificatesList;


