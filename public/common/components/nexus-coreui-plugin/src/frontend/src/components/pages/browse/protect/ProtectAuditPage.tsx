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

import React from 'react';
import { Box, Card, Flex, Heading, Text, Button } from '@radix-ui/themes';
import { ScrollText, Code, ArrowRight } from 'lucide-react';

/**
 * Protect Audit Overview Page
 *
 * Landing page for Protect auditing features with navigation to API docs and audit log viewer.
 */
export function ProtectAuditPage() {
  return (
    <Box p="6" style={{ maxWidth: '1200px', margin: '0 auto' }}>
      <Flex direction="column" gap="6">
        {/* Header */}
        <Box>
          <Heading size="7" mb="2">
            Protect Audit
          </Heading>
          <Text size="3" color="gray">
            Track and query all administrative changes related to repository protection, security, and configuration
          </Text>
        </Box>

        {/* Quick Actions */}
        <Flex gap="4" wrap="wrap">
          <Card style={{ flex: '1 1 400px' }}>
            <Flex direction="column" gap="3" height="100%">
              <Flex align="center" gap="2">
                <ScrollText size={24} style={{ color: 'var(--accent-11)' }} />
                <Heading size="4">Audit Log Viewer</Heading>
              </Flex>
              <Text size="2" color="gray" style={{ flex: 1 }}>
                Browse and filter all audit events across Security, Repository, Configuration, and Protection domains.
                View detailed event information including timestamps, initiators, and full attribute data.
              </Text>
              <Box>
                <a href="http://localhost:8081/?debug#preview/admin/audit" style={{ textDecoration: 'none' }}>
                  <Button size="2" variant="soft">
                    <Flex align="center" gap="2">
                      <span>Open Audit Log</span>
                      <ArrowRight size={16} />
                    </Flex>
                  </Button>
                </a>
              </Box>
            </Flex>
          </Card>

          <Card style={{ flex: '1 1 400px' }}>
            <Flex direction="column" gap="3" height="100%">
              <Flex align="center" gap="2">
                <Code size={24} style={{ color: 'var(--accent-11)' }} />
                <Heading size="4">API Documentation</Heading>
              </Flex>
              <Text size="2" color="gray" style={{ flex: 1 }}>
                Interactive REST API documentation for programmatic access to audit events. Includes request/response
                examples, parameter reference, and curl commands for easy testing.
              </Text>
              <Box>
                <a href="http://localhost:8081/?debug#preview/browse/protect/audit/api" style={{ textDecoration: 'none' }}>
                  <Button size="2" variant="soft">
                    <Flex align="center" gap="2">
                      <span>View API Docs</span>
                      <ArrowRight size={16} />
                    </Flex>
                  </Button>
                </a>
              </Box>
            </Flex>
          </Card>
        </Flex>

        {/* Features Overview */}
        <Card>
          <Heading size="4" mb="3">
            What Gets Audited?
          </Heading>
          <Flex direction="column" gap="3">
            <Box>
              <Text size="2" weight="bold" mb="1">
                🔒 Security Events
              </Text>
              <Text size="2" color="gray">
                User creation/updates, role changes, privilege modifications, LDAP/SAML configuration, SSL certificates,
                realm updates, user tokens
              </Text>
            </Box>
            <Box>
              <Text size="2" weight="bold" mb="1">
                📦 Repository Events
              </Text>
              <Text size="2" color="gray">
                Repository CRUD, component operations, asset management, blob store changes, component tagging
              </Text>
            </Box>
            <Box>
              <Text size="2" weight="bold" mb="1">
                ⚙️ Configuration Events
              </Text>
              <Text size="2" color="gray">
                Task execution, capability changes, cleanup policies, content selectors, routing rules, email/HTTP config,
                logging, licensing, data store operations
              </Text>
            </Box>
            <Box>
              <Text size="2" weight="bold" mb="1">
                🛡️ Protection Events
              </Text>
              <Text size="2" color="gray">
                Repository Health Check configuration, Firewall settings, malware removal actions, quarantine events
              </Text>
            </Box>
          </Flex>
        </Card>

        {/* Event Attributes */}
        <Card>
          <Heading size="4" mb="3">
            Event Information
          </Heading>
          <Text size="2" color="gray" mb="3">
            Each audit event includes:
          </Text>
          <Flex direction="column" gap="2">
            <Text size="2">
              • <strong>Domain</strong> — The functional area (e.g., security.user, tasks, repository.component)
            </Text>
            <Text size="2">
              • <strong>Type</strong> — The action performed (created, updated, deleted, started, finished, etc.)
            </Text>
            <Text size="2">
              • <strong>Context</strong> — The specific entity affected (e.g., user name, repository name, task name)
            </Text>
            <Text size="2">
              • <strong>Timestamp</strong> — ISO 8601 timestamp of when the event occurred
            </Text>
            <Text size="2">
              • <strong>Initiator</strong> — Username of the person/system who triggered the event
            </Text>
            <Text size="2">
              • <strong>Node ID</strong> — Cluster node identifier (for high-availability deployments)
            </Text>
            <Text size="2">
              • <strong>Attributes</strong> — Full JSON object containing all relevant metadata for the event
            </Text>
          </Flex>
        </Card>
      </Flex>
    </Box>
  );
}

export default ProtectAuditPage;
