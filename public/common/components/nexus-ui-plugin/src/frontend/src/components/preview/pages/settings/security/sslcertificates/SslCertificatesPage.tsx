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
import React, { useState, useEffect, useCallback } from 'react';
import { Box, } from '@radix-ui/themes';
import { Plus, } from 'lucide-react';
import { ExtJS } from '../../../../../../interface/ExtJS';

import { useToast, PageHeader } from '../../../../shared';
import { SettingsButton, SettingsAlert, ConfirmDialog } from '../../../../shared/form';
import { SslCertificatesList } from './SslCertificatesList';
import { SslCertificatesDetail } from './SslCertificatesDetail';
import { SslCertificatesAddForm } from './SslCertificatesAddForm';
import { useSslCertificatesApi } from './useSslCertificatesApi';
import { SslCertificate, AddCertificateFormData } from './types';

import './SslCertificatesPage.scss';

const navigateTo = (path: string) => {
  window.location.hash = path;
};

type ViewMode = 'list' | 'create' | 'detail';

/**
 * SslCertificatesPage - Main SSL Certificates management page for Preview UI
 * 
 * Displays certificate list with search/filter, and allows adding and deleting certificates.
 */
export function SslCertificatesPage() {
  const [viewMode, setViewMode] = useState<ViewMode>('list');
  const [selectedCertificateId, setSelectedCertificateId] = useState<string | null>(null);
  const [certificate, setCertificate] = useState<SslCertificate | null>(null);
  const [refreshKey, setRefreshKey] = useState(0);
  const [deleteDialogOpen, setDeleteDialogOpen] = useState(false);

  // Toast notifications (app-level provider)
  const toast = useToast();

  const {
    loading,
    error,
    setError,
    fetchCertificates,
    addCertificate,
    deleteCertificate,
  } = useSslCertificatesApi();

  // Permissions load asynchronously (NEXUS-53199), so checkPermission() can legitimately report
  // false on the first paint. Nothing else re-renders this page afterwards, so without
  // re-evaluating once they arrive the create/delete affordances would stay hidden for the whole
  // session even for an admin (NEXUS-54265).
  const [{ canCreate, canDelete }, setPermissions] = useState(() => ({
    canCreate: ExtJS.checkPermission('nexus:ssl-truststore:create'),
    canDelete: ExtJS.checkPermission('nexus:ssl-truststore:delete'),
  }));

  useEffect(() => {
    let cancelled = false;

    ExtJS.waitForPermissions()
      .then(() => {
        if (cancelled) return;
        setPermissions({
          canCreate: ExtJS.checkPermission('nexus:ssl-truststore:create'),
          canDelete: ExtJS.checkPermission('nexus:ssl-truststore:delete'),
        });
      })
      // On timeout keep the initial evaluation rather than blanking the page.
      .catch(() => {});

    return () => {
      cancelled = true;
    };
  }, []);

  // Load certificate details when selected
  useEffect(() => {
    if (selectedCertificateId && viewMode === 'detail') {
      fetchCertificates()
        .then((certificates) => {
          const found = certificates.find((c) => c.id === selectedCertificateId);
          if (found) {
            setCertificate(found);
          } else {
            setError('Certificate not found');
            setViewMode('list');
          }
        })
        .catch((err) => {
          setError(err.message);
          setViewMode('list');
        });
    }
  }, [selectedCertificateId, viewMode, fetchCertificates, setError]);

  const handleSelectCertificate = useCallback((certificateId: string) => {
    setSelectedCertificateId(certificateId);
    setCertificate(null);
    setError(null);
    setViewMode('detail');
  }, [setError]);

  const handleCreate = useCallback(() => {
    setSelectedCertificateId(null);
    setCertificate(null);
    setError(null);
    setViewMode('create');
  }, [setError]);

  const handleBack = useCallback(() => {
    setSelectedCertificateId(null);
    setCertificate(null);
    setError(null);
    setViewMode('list');
  }, [setError]);

  const handleSave = useCallback(async (data: AddCertificateFormData) => {
    try {
      const result = await addCertificate(data.pemContent);
      toast.success(`SSL certificate "${result.subjectCommonName}" added to trust store`);
      setRefreshKey((k) => k + 1);
      handleBack();
    } catch (err: any) {
      // Check if it's a conflict (certificate already exists)
      if (err?.response?.status === 409 || err?.message?.includes('already exists')) {
        setError('This certificate already exists in the trust store and cannot be added again.');
      }
      // Other errors are set by the API hook
    }
  }, [addCertificate, handleBack, setError, toast]);

  const handleDelete = useCallback(() => {
    if (!certificate) return;
    setDeleteDialogOpen(true);
  }, [certificate]);

  const handleDeleteConfirm = useCallback(async () => {
    if (!certificate) return;
    
    try {
      await deleteCertificate(certificate.id);
      toast.success(`SSL certificate "${certificate.subjectCommonName}" removed from trust store`);
      setRefreshKey((k) => k + 1);
      setDeleteDialogOpen(false);
      handleBack();
    } catch (err) {
      const message = err instanceof Error ? err.message : "Operation failed";
      toast.error(message);
      setDeleteDialogOpen(false);
    }
  }, [certificate, deleteCertificate, handleBack, toast]);

  // Render header based on view mode
  const renderHeader = () => {
    if (viewMode === 'list') {
      return (
        <PageHeader
          title="SSL Certificates"
          description="Manage trusted SSL certificates for use with the Nexus truststore"
          breadcrumbs={[
            { label: 'Settings', onClick: () => navigateTo('#preview/admin/settings') },
            { label: 'SSL Certificates' }
          ]}
          actions={canCreate && (
            <SettingsButton variant="primary" onClick={handleCreate} icon={Plus}>
              Add Certificate
            </SettingsButton>
          )}
        />
      );
    }

    const title = viewMode === 'create'
      ? 'Add SSL Certificate'
      : certificate
        ? `Certificate ${certificate.subjectCommonName}`
        : 'Certificate Details';

    const lastBreadcrumb = viewMode === 'create'
      ? 'Add'
      : certificate?.subjectCommonName || 'Loading...';

    return (
      <PageHeader
        title={title}
        breadcrumbs={[
          { label: 'Settings', onClick: () => navigateTo('#preview/admin/settings') },
          { label: 'SSL Certificates', onClick: handleBack },
          { label: lastBreadcrumb }
        ]}
      />
    );
  };

  return (
    <Box
      className="ssl-certificates-page"
      data-testid="ssl-certificates-page"
      data-view={viewMode}
      data-loading={loading ? 'true' : 'false'}
      px={{ initial: '4', md: '6', lg: '6' }}
      py={{ initial: '4', md: '5', lg: '6' }}
    >
      <Box mb="4">
        {renderHeader()}
      </Box>

      {/* Alerts */}
      {error && (
        <Box className="ssl-certificates-page__alerts">
          <SettingsAlert type="error" onClose={() => setError(null)}>
            {error}
          </SettingsAlert>
        </Box>
      )}

      {/* Content */}
      <Box className="ssl-certificates-page__content">
        {viewMode === 'list' && (
          <SslCertificatesList
            key={refreshKey}
            onSelect={handleSelectCertificate}
            onCreate={handleCreate}
          />
        )}

        {viewMode === 'create' && (
          <SslCertificatesAddForm
            onSave={handleSave}
            onCancel={handleBack}
            loading={loading}
            onViewExisting={handleSelectCertificate}
          />
        )}

        {viewMode === 'detail' && (
          <SslCertificatesDetail
            certificate={certificate}
            loading={loading && !certificate}
            canDelete={canDelete}
            onDelete={handleDelete}
            onCancel={handleBack}
            
          />
        )}
      </Box>

      {/* Delete Confirmation Dialog */}
      <ConfirmDialog
        open={deleteDialogOpen}
        testId="delete-certificate-dialog"
        onOpenChange={setDeleteDialogOpen}
        title="Delete Certificate?"
        message={`Are you sure you want to delete the certificate "${certificate?.subjectCommonName}"? This action cannot be undone. Any services relying on this certificate may fail to connect.`}
        confirmLabel={loading ? 'Deleting...' : 'Delete Certificate'}
        variant="danger"
        onConfirm={handleDeleteConfirm}
        loading={loading}
      />
    </Box>
  );
}

export default SslCertificatesPage;

