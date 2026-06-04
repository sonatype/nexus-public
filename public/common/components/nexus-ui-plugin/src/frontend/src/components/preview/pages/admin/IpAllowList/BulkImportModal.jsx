/*
 * Copyright (c) 2008-present Sonatype, Inc.
 *
 * All rights reserved. Includes the third-party code listed at http://links.sonatype.com/products/nexus/pro/attributions
 * Sonatype and Sonatype Nexus are trademarks of Sonatype, Inc. Apache Maven is a trademark of the Apache Foundation.
 * M2Eclipse is a trademark of the Eclipse Foundation. All other trademarks are the property of their respective owners.
 */
import React, { useState, useEffect, useRef } from 'react';
import { Dialog, Button, Flex, Box, Text, Spinner, IconButton } from '@radix-ui/themes';
import { Upload, X, AlertCircle } from 'lucide-react';

export function BulkImportModal({ isOpen, onClose, onImport, isLoading = false }) {
  const [selectedFile, setSelectedFile] = useState(null);
  const [isDragging, setIsDragging] = useState(false);
  const [fileSizeError, setFileSizeError] = useState('');
  const fileInputRef = useRef(null);

  useEffect(() => {
    if (!isOpen) {
      setSelectedFile(null);
      setFileSizeError('');
      setIsDragging(false);
      if (fileInputRef.current) {
        fileInputRef.current.value = '';
      }
    }
  }, [isOpen]);

  const handleFileSelect = (e) => {
    const file = e.target.files?.[0];

    // Clear previous error
    setFileSizeError('');

    if (!file) {
      setSelectedFile(null);
      return;
    }

    // Check file type
    if (!(file.type === 'text/csv' || file.type === 'text/plain')) {
      setFileSizeError('Invalid file type. Please upload a CSV or TXT file.');
      setSelectedFile(null);
      return;
    }

    // Check file size (1 MB limit)
    const MAX_FILE_SIZE = 1048576; // 1 MB in bytes
    if (file.size > MAX_FILE_SIZE) {
      const fileSizeMB = (file.size / 1048576).toFixed(2);
      setFileSizeError(`File is too large (${fileSizeMB} MB). Maximum size is 1 MB.`);
      setSelectedFile(null);
      return;
    }

    setSelectedFile(file);
  };

  const handleDrop = (e) => {
    e.preventDefault();
    setIsDragging(false);
    const file = e.dataTransfer.files?.[0];
    if (file) {
      handleFileSelect({ target: { files: [file] } });
    }
  };

  const handleImport = () => {
    if (selectedFile) {
      onImport(selectedFile);
    }
  };

  const handleDownloadSample = () => {
    const sampleContent = `ip_address,description\n192.168.1.1,Office network\n10.0.0.0/24,Internal subnet\n"192.168.1.5","Office, Building A"`;

    const blob = new Blob([sampleContent], { type: 'text/csv' });
    const url = window.URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = 'ip-allowlist-sample.csv';
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
    window.URL.revokeObjectURL(url);
  };

  const requirements = [
    'File extension must be .csv or .txt (e.g. file.csv)',
    'Must be less than 1 MB',
    'The first row must contain the column names (e.g. ip_address, description)',
    'Column names must be formatted with lowercase characters and no spaces (replace spaces with underscores)',
    'Columns must be separated by commas',
    'IP addresses must be valid IPv4, IPv6, or CIDR notation (e.g. 192.168.1.0/24)',
  ];

  return (
    <Dialog.Root open={isOpen} onOpenChange={onClose}>
      <Dialog.Content maxWidth="520px" className="nxrm-ip-allowlist__modal-content" data-testid="bulk-import-modal">
        <Flex align="start" justify="between" mb="4">
          <Dialog.Title size="5" weight="bold">Import IP Addresses</Dialog.Title>
          <Dialog.Close>
            <IconButton variant="ghost" size="1">
              <X size={18} />
            </IconButton>
          </Dialog.Close>
        </Flex>

        <Box style={{ display: 'flex', flexDirection: 'column', gap: '20px' }}>
          <Dialog.Description style={{ display: 'none' }}>
            Upload a CSV or TXT file with IP addresses and optional descriptions
          </Dialog.Description>

          {/* Drag-and-drop zone */}
          <Box
            className={`nxrm-ip-allowlist__dropzone ${isDragging ? 'dragging' : ''}`}
            onDragOver={(e) => { e.preventDefault(); setIsDragging(true); }}
            onDragLeave={() => setIsDragging(false)}
            onDrop={handleDrop}
            style={{
              border: '2px dashed var(--gray-6)',
              borderRadius: '8px',
              padding: '32px 24px',
              textAlign: 'center',
              backgroundColor: isDragging ? 'var(--blue-2)' : 'var(--gray-1)',
              cursor: 'pointer',
              transition: 'background-color 0.15s ease, border-color 0.15s ease',
              borderColor: isDragging ? 'var(--blue-7)' : selectedFile ? 'var(--green-7)' : 'var(--gray-6)',
            }}
          >
            <input
              type="file"
              accept=".csv,.txt"
              onChange={handleFileSelect}
              style={{ display: 'none' }}
              id="bulk-import-file"
              ref={fileInputRef}
              data-testid="file-input"
            />
            <label htmlFor="bulk-import-file" style={{ cursor: 'pointer', display: 'block' }}>
              <Upload size={28} style={{ margin: '0 auto 12px', color: 'var(--gray-9)', display: 'block' }} />
              {selectedFile ? (
                <Text size="3" weight="medium" style={{ color: 'var(--green-11)' }}>
                  {selectedFile.name}
                </Text>
              ) : (
                <Text size="3" weight="medium">
                  Drag a file here or{' '}
                  <Text style={{ color: 'var(--blue-9)', textDecoration: 'underline', cursor: 'pointer' }}>
                    browse
                  </Text>{' '}
                  for a file...
                </Text>
              )}
              <Text size="1" color="gray" style={{ display: 'block', marginTop: '4px' }}>
                (max 1 MB)
              </Text>
            </label>
          </Box>

          {/* File error */}
          {fileSizeError && (
            <Box p="2" style={{ backgroundColor: 'var(--red-2)', borderRadius: '6px' }}>
              <Flex gap="2" align="start">
                <AlertCircle size={16} style={{ color: 'var(--red-9)', marginTop: '2px', flexShrink: 0 }} />
                <Text size="2" style={{ color: 'var(--red-11)' }}>{fileSizeError}</Text>
              </Flex>
            </Box>
          )}

          {/* Requirements checklist */}
          <Box>
            <Text size="3" weight="bold" style={{ display: 'block', marginBottom: '10px' }}>
              Please check your file meets these requirements:
            </Text>
            <Box style={{ display: 'flex', flexDirection: 'column', gap: '7px' }}>
              {requirements.map((req, i) => (
                <Flex key={i} gap="2" align="start">
                  <Text size="2" style={{ color: 'var(--green-9)', flexShrink: 0, marginTop: '1px' }}>✓</Text>
                  <Text size="2">{req}</Text>
                </Flex>
              ))}
            </Box>
          </Box>

          {/* Sample file download */}
          <Text
            as="span"
            size="2"
            style={{ color: 'var(--blue-9)', textDecoration: 'underline', cursor: 'pointer' }}
            onClick={handleDownloadSample}
            data-testid="download-sample-button"
          >
            Download a sample file
          </Text>

          {/* Actions */}
          <Flex gap="3" justify="end">
            <Button variant="surface" onClick={onClose} disabled={isLoading} data-testid="import-cancel-button">
              Cancel
            </Button>
            <Button
              variant="solid"
              color="blue"
              onClick={handleImport}
              disabled={!selectedFile || isLoading}
              data-testid="import-submit-button"
            >
              {isLoading && <Spinner size="1" style={{ marginRight: '8px' }} />}
              {isLoading ? 'Importing...' : 'Import'}
            </Button>
          </Flex>
        </Box>
      </Dialog.Content>
    </Dialog.Root>
  );
}
