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

import React, { useState } from 'react';
import { Box, Card, Flex, Heading, Text, Code, Badge, Separator, Tabs } from '@radix-ui/themes';
import { ExternalLink } from 'lucide-react';

/**
 * Audit API Documentation Page
 *
 * User-friendly, interactive API documentation for the Protect Audit endpoints.
 * Similar to Seaworthy's Guide API documentation style.
 */
export function AuditApiPage() {
  const [selectedEndpoint, setSelectedEndpoint] = useState('list-events');

  const endpoints = [
    {
      id: 'list-events',
      method: 'GET',
      path: '/service/rest/internal/ui/audit-log',
      title: 'List Audit Events',
      description: 'Retrieve paginated audit events with filtering support',
      parameters: [
        { name: 'page', type: 'integer', required: false, default: '1', description: 'Page number (1-based)' },
        { name: 'limit', type: 'integer', required: false, default: '20', description: 'Items per page (max 100)' },
        { name: 'categories', type: 'string[]', required: false, description: 'Filter by categories: security, repository, configuration, protection' },
        { name: 'domains', type: 'string[]', required: false, description: 'Filter by specific domains (e.g., security.user, tasks)' },
        { name: 'types', type: 'string[]', required: false, description: 'Filter by event types (e.g., created, updated, deleted)' },
        { name: 'initiators', type: 'string[]', required: false, description: 'Filter by initiator usernames' },
        { name: 'startDate', type: 'string', required: false, description: 'ISO 8601 start date (inclusive)' },
        { name: 'endDate', type: 'string', required: false, description: 'ISO 8601 end date (inclusive)' },
      ],
      response: {
        items: [
          {
            id: 681,
            domain: 'tasks',
            type: 'finished',
            context: 'Check for new report availability',
            timestamp: '2026-03-12T13:21:24.415Z',
            initiator: '*TASK',
            nodeId: 'f4e5fd0c-0736d477-1db5e4cd-9a17d360-f0018344',
            attributes: {
              '.name': 'System - Repository Health Check: maven-central',
              '.id': 'ab05f582-d882-4e80-89ca-1e33a87dd1c8',
              repositoryName: 'maven-central',
            },
          },
        ],
        pagination: {
          totalItems: 664,
          totalPages: 133,
          currentPage: 1,
          itemsPerPage: 20,
        },
      },
      examples: [
        {
          title: 'Get all events (first page)',
          curl: 'curl -u admin:admin123 "http://localhost:8081/service/rest/internal/ui/audit-log?page=1&limit=20"',
        },
        {
          title: 'Filter by security category',
          curl: 'curl -u admin:admin123 "http://localhost:8081/service/rest/internal/ui/audit-log?categories=security"',
        },
        {
          title: 'Filter by date range',
          curl: 'curl -u admin:admin123 "http://localhost:8081/service/rest/internal/ui/audit-log?startDate=2026-03-01T00:00:00Z&endDate=2026-03-12T23:59:59Z"',
        },
        {
          title: 'Filter by multiple criteria',
          curl: 'curl -u admin:admin123 "http://localhost:8081/service/rest/internal/ui/audit-log?categories=security&categories=repository&types=created&types=updated&page=1&limit=50"',
        },
      ],
    },
  ];

  const selectedEndpointData = endpoints.find((e) => e.id === selectedEndpoint);

  return (
    <Box p="6" style={{ maxWidth: '1200px', margin: '0 auto' }}>
      <Flex direction="column" gap="4">
        {/* Header */}
        <Box>
          <Heading size="7" mb="2">
            Protect Audit API
          </Heading>
          <Text size="3" color="gray">
            REST API for querying audit events across all administrative changes in Nexus Repository
          </Text>
        </Box>

        <Separator size="4" />

        {/* Overview Cards */}
        <Flex gap="4" wrap="wrap">
          <Card style={{ flex: '1 1 300px' }}>
            <Flex direction="column" gap="2">
              <Text size="2" weight="bold" color="gray">
                Base URL
              </Text>
              <Code size="2">/service/rest/internal/ui</Code>
            </Flex>
          </Card>
          <Card style={{ flex: '1 1 300px' }}>
            <Flex direction="column" gap="2">
              <Text size="2" weight="bold" color="gray">
                Authentication
              </Text>
              <Text size="2">Basic Auth (admin credentials required)</Text>
            </Flex>
          </Card>
          <Card style={{ flex: '1 1 300px' }}>
            <Flex direction="column" gap="2">
              <Text size="2" weight="bold" color="gray">
                Content Type
              </Text>
              <Code size="2">application/json</Code>
            </Flex>
          </Card>
        </Flex>

        <Separator size="4" />

        {/* Endpoints */}
        <Heading size="5" mb="2">
          API Endpoints
        </Heading>

        {selectedEndpointData && (
          <Card size="3">
            <Flex direction="column" gap="4">
              {/* Method and Path */}
              <Flex align="center" gap="3">
                <Badge color="green" size="2" style={{ fontFamily: 'monospace' }}>
                  {selectedEndpointData.method}
                </Badge>
                <Code size="3">{selectedEndpointData.path}</Code>
              </Flex>

              {/* Title and Description */}
              <Box>
                <Heading size="4" mb="2">
                  {selectedEndpointData.title}
                </Heading>
                <Text size="2" color="gray">
                  {selectedEndpointData.description}
                </Text>
              </Box>

              <Separator />

              {/* Parameters */}
              <Box>
                <Heading size="3" mb="3">
                  Query Parameters
                </Heading>
                <Flex direction="column" gap="3">
                  {selectedEndpointData.parameters.map((param) => (
                    <Card key={param.name} variant="surface">
                      <Flex direction="column" gap="2">
                        <Flex align="center" gap="2">
                          <Code size="2">{param.name}</Code>
                          <Badge size="1" variant="soft" color="gray">
                            {param.type}
                          </Badge>
                          {param.required && (
                            <Badge size="1" variant="soft" color="red">
                              required
                            </Badge>
                          )}
                          {param.default && (
                            <Text size="1" color="gray">
                              default: <Code size="1">{param.default}</Code>
                            </Text>
                          )}
                        </Flex>
                        <Text size="2" color="gray">
                          {param.description}
                        </Text>
                      </Flex>
                    </Card>
                  ))}
                </Flex>
              </Box>

              <Separator />

              {/* Response */}
              <Box>
                <Heading size="3" mb="3">
                  Response Body
                </Heading>
                <Card variant="surface">
                  <Code size="2" style={{ display: 'block', whiteSpace: 'pre', overflowX: 'auto' }}>
                    {JSON.stringify(selectedEndpointData.response, null, 2)}
                  </Code>
                </Card>
              </Box>

              <Separator />

              {/* Examples */}
              <Box>
                <Heading size="3" mb="3">
                  Examples
                </Heading>
                <Tabs.Root defaultValue="0">
                  <Tabs.List>
                    {selectedEndpointData.examples.map((example, idx) => (
                      <Tabs.Trigger key={idx} value={idx.toString()}>
                        {example.title}
                      </Tabs.Trigger>
                    ))}
                  </Tabs.List>

                  {selectedEndpointData.examples.map((example, idx) => (
                    <Tabs.Content key={idx} value={idx.toString()}>
                      <Card variant="surface" mt="3">
                        <Code size="2" style={{ display: 'block', whiteSpace: 'pre-wrap', overflowX: 'auto' }}>
                          {example.curl}
                        </Code>
                      </Card>
                    </Tabs.Content>
                  ))}
                </Tabs.Root>
              </Box>
            </Flex>
          </Card>
        )}

        <Separator size="4" />

        {/* Event Categories Reference */}
        <Card>
          <Heading size="4" mb="3">
            Event Categories
          </Heading>
          <Flex direction="column" gap="3">
            <Card variant="surface">
              <Flex direction="column" gap="2">
                <Flex align="center" gap="2">
                  <Badge color="blue" size="2">
                    security
                  </Badge>
                  <Text size="2" weight="bold">
                    Security Events
                  </Text>
                </Flex>
                <Text size="2" color="gray">
                  User, Role, Privilege, LDAP, SAML, SSL Certificate, Realm, Anonymous Access, User Token
                </Text>
              </Flex>
            </Card>
            <Card variant="surface">
              <Flex direction="column" gap="2">
                <Flex align="center" gap="2">
                  <Badge color="purple" size="2">
                    repository
                  </Badge>
                  <Text size="2" weight="bold">
                    Repository Events
                  </Text>
                </Flex>
                <Text size="2" color="gray">
                  Repository, Component, Asset, Blob Store, Component Tag
                </Text>
              </Flex>
            </Card>
            <Card variant="surface">
              <Flex direction="column" gap="2">
                <Flex align="center" gap="2">
                  <Badge color="gray" size="2">
                    configuration
                  </Badge>
                  <Text size="2" weight="bold">
                    Configuration Events
                  </Text>
                </Flex>
                <Text size="2" color="gray">
                  Task, Capability, Cleanup Policy, Content Selector, Routing Rule, Email, HTTP, Logging, License, Data
                  Store
                </Text>
              </Flex>
            </Card>
            <Card variant="surface">
              <Flex direction="column" gap="2">
                <Flex align="center" gap="2">
                  <Badge color="amber" size="2">
                    protection
                  </Badge>
                  <Text size="2" weight="bold">
                    Protection Events
                  </Text>
                </Flex>
                <Text size="2" color="gray">
                  Protection Configuration, Malware Removal, Firewall Quarantine
                </Text>
              </Flex>
            </Card>
          </Flex>
        </Card>

        {/* Try It Link */}
        <Card>
          <Flex align="center" justify="between">
            <Box>
              <Heading size="4" mb="1">
                Interactive API Explorer
              </Heading>
              <Text size="2" color="gray">
                Test the API directly in your browser
              </Text>
            </Box>
            <a
              href="http://localhost:8081/?debug#preview/admin/system/api"
              target="_blank"
              rel="noopener noreferrer"
              style={{ textDecoration: 'none' }}
            >
              <Flex align="center" gap="2" style={{ color: 'var(--accent-11)' }}>
                <Text size="2" weight="medium">
                  Open Swagger UI
                </Text>
                <ExternalLink size={16} />
              </Flex>
            </a>
          </Flex>
        </Card>
      </Flex>
    </Box>
  );
}

export default AuditApiPage;
