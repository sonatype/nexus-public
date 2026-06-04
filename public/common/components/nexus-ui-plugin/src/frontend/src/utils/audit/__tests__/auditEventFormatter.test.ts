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

import { formatAuditEvent, formatTimestamp, formatTimestampFull } from '../auditEventFormatter';
import type { AuditEvent } from '../audit.types';

describe('auditEventFormatter', () => {
  describe('formatAuditEvent', () => {
    describe('Category Assignment', () => {
      it('should assign security category for security domains', () => {
        const event: AuditEvent = {
          id: 1,
          domain: 'security.user',
          type: 'created',
          context: 'testuser',
          timestamp: '2026-03-12T10:00:00.000Z',
          initiator: 'admin',
          nodeId: 'node-1',
          attributes: {},
        };

        const result = formatAuditEvent(event);
        expect(result.category).toBe('security');
      });

      it('should assign repository category for repository domains', () => {
        const event: AuditEvent = {
          id: 1,
          domain: 'repository',
          type: 'updated',
          context: 'maven-central',
          timestamp: '2026-03-12T10:00:00.000Z',
          initiator: 'admin',
          nodeId: 'node-1',
          attributes: {},
        };

        const result = formatAuditEvent(event);
        expect(result.category).toBe('repository');
      });

      it('should assign configuration category for configuration domains', () => {
        const event: AuditEvent = {
          id: 1,
          domain: 'tasks',
          type: 'started',
          context: 'Cleanup Task',
          timestamp: '2026-03-12T10:00:00.000Z',
          initiator: '*TASK',
          nodeId: 'node-1',
          attributes: {},
        };

        const result = formatAuditEvent(event);
        expect(result.category).toBe('configuration');
      });

      it('should assign protection category for protection domains', () => {
        const event: AuditEvent = {
          id: 1,
          domain: 'protection.config',
          type: 'updated',
          context: 'RHC Config',
          timestamp: '2026-03-12T10:00:00.000Z',
          initiator: 'admin',
          nodeId: 'node-1',
          attributes: {},
        };

        const result = formatAuditEvent(event);
        expect(result.category).toBe('protection');
      });

      it('should default to configuration for unknown domains', () => {
        const event: AuditEvent = {
          id: 1,
          domain: 'unknown.domain',
          type: 'updated',
          context: 'Something',
          timestamp: '2026-03-12T10:00:00.000Z',
          initiator: 'admin',
          nodeId: 'node-1',
          attributes: {},
        };

        const result = formatAuditEvent(event);
        expect(result.category).toBe('configuration');
      });
    });

    describe('Event Label Formatting', () => {
      it('should capitalize event type', () => {
        const event: AuditEvent = {
          id: 1,
          domain: 'security.user',
          type: 'created',
          context: 'testuser',
          timestamp: '2026-03-12T10:00:00.000Z',
          initiator: 'admin',
          nodeId: 'node-1',
          attributes: {},
        };

        const result = formatAuditEvent(event);
        expect(result.eventLabel).toBe('Created');
      });

      it('should handle hyphenated event types', () => {
        const event: AuditEvent = {
          id: 1,
          domain: 'malware.removal',
          type: 'automatic-malware-removed',
          context: 'component',
          timestamp: '2026-03-12T10:00:00.000Z',
          initiator: 'system',
          nodeId: 'node-1',
          attributes: {},
        };

        const result = formatAuditEvent(event);
        expect(result.eventLabel).toBe('Automatic Malware Removed');
      });

      it('should handle underscore-separated event types', () => {
        const event: AuditEvent = {
          id: 1,
          domain: 'firewall.quarantine',
          type: 'quarantined_new_violation',
          context: 'component',
          timestamp: '2026-03-12T10:00:00.000Z',
          initiator: 'system',
          nodeId: 'node-1',
          attributes: {},
        };

        const result = formatAuditEvent(event);
        expect(result.eventLabel).toBe('Quarantined New Violation');
      });
    });

    describe('Entity Type Derivation', () => {
      it('should derive User for security.user domain', () => {
        const event: AuditEvent = {
          id: 1,
          domain: 'security.user',
          type: 'created',
          context: 'testuser',
          timestamp: '2026-03-12T10:00:00.000Z',
          initiator: 'admin',
          nodeId: 'node-1',
          attributes: {},
        };

        const result = formatAuditEvent(event);
        expect(result.entityType).toBe('User');
      });

      it('should derive Repository for repository domain', () => {
        const event: AuditEvent = {
          id: 1,
          domain: 'repository',
          type: 'updated',
          context: 'maven-central',
          timestamp: '2026-03-12T10:00:00.000Z',
          initiator: 'admin',
          nodeId: 'node-1',
          attributes: {},
        };

        const result = formatAuditEvent(event);
        expect(result.entityType).toBe('Repository');
      });

      it('should derive Task for tasks domain', () => {
        const event: AuditEvent = {
          id: 1,
          domain: 'tasks',
          type: 'started',
          context: 'Cleanup',
          timestamp: '2026-03-12T10:00:00.000Z',
          initiator: '*TASK',
          nodeId: 'node-1',
          attributes: {},
        };

        const result = formatAuditEvent(event);
        expect(result.entityType).toBe('Task');
      });

      it('should default to Event for unknown domains', () => {
        const event: AuditEvent = {
          id: 1,
          domain: 'unknown.domain',
          type: 'updated',
          context: 'Something',
          timestamp: '2026-03-12T10:00:00.000Z',
          initiator: 'admin',
          nodeId: 'node-1',
          attributes: {},
        };

        const result = formatAuditEvent(event);
        expect(result.entityType).toBe('Event');
      });
    });

    describe('Entity Name Extraction', () => {
      it('should use context as entity name when available', () => {
        const event: AuditEvent = {
          id: 1,
          domain: 'security.user',
          type: 'created',
          context: 'testuser',
          timestamp: '2026-03-12T10:00:00.000Z',
          initiator: 'admin',
          nodeId: 'node-1',
          attributes: { name: 'different-name' },
        };

        const result = formatAuditEvent(event);
        expect(result.entityName).toBe('testuser');
      });

      it('should fallback to attributes.name when context is empty', () => {
        const event: AuditEvent = {
          id: 1,
          domain: 'security.user',
          type: 'created',
          context: '',
          timestamp: '2026-03-12T10:00:00.000Z',
          initiator: 'admin',
          nodeId: 'node-1',
          attributes: { name: 'attr-name' },
        };

        const result = formatAuditEvent(event);
        expect(result.entityName).toBe('attr-name');
      });

      it('should use Unknown when neither context nor name available', () => {
        const event: AuditEvent = {
          id: 1,
          domain: 'security.user',
          type: 'created',
          context: '',
          timestamp: '2026-03-12T10:00:00.000Z',
          initiator: 'admin',
          nodeId: 'node-1',
          attributes: {},
        };

        const result = formatAuditEvent(event);
        expect(result.entityName).toBe('Unknown');
      });
    });

    describe('Summary Building', () => {
      it('should include initiator in summary when not system', () => {
        const event: AuditEvent = {
          id: 1,
          domain: 'security.user',
          type: 'created',
          context: 'testuser',
          timestamp: '2026-03-12T10:00:00.000Z',
          initiator: 'admin',
          nodeId: 'node-1',
          attributes: {},
        };

        const result = formatAuditEvent(event);
        expect(result.summary).toContain('by admin');
      });

      it('should not include initiator in summary when null (system)', () => {
        const event: AuditEvent = {
          id: 1,
          domain: 'security.user',
          type: 'created',
          context: 'testuser',
          timestamp: '2026-03-12T10:00:00.000Z',
          initiator: null,
          nodeId: 'node-1',
          attributes: {},
        };

        const result = formatAuditEvent(event);
        expect(result.summary).not.toContain('by');
      });

      it('should handle malware removal events specially', () => {
        const event: AuditEvent = {
          id: 1,
          domain: 'malware.removal',
          type: 'automatic-malware-removed',
          context: 'evil-package',
          timestamp: '2026-03-12T10:00:00.000Z',
          initiator: 'system',
          nodeId: 'node-1',
          attributes: {},
        };

        const result = formatAuditEvent(event);
        expect(result.summary).toContain('removed (malware)');
      });

      it('should handle quarantine events specially', () => {
        const event: AuditEvent = {
          id: 1,
          domain: 'firewall.quarantine',
          type: 'quarantined-new-violation',
          context: 'blocked-component',
          timestamp: '2026-03-12T10:00:00.000Z',
          initiator: 'admin',
          nodeId: 'node-1',
          attributes: {},
        };

        const result = formatAuditEvent(event);
        expect(result.summary).toContain('Download blocked');
      });
    });

    describe('Preserves Original Event Data', () => {
      it('should include all original event fields', () => {
        const event: AuditEvent = {
          id: 123,
          domain: 'security.user',
          type: 'created',
          context: 'testuser',
          timestamp: '2026-03-12T10:00:00.000Z',
          initiator: 'admin',
          nodeId: 'node-1',
          attributes: { key: 'value' },
        };

        const result = formatAuditEvent(event);

        expect(result.id).toBe(123);
        expect(result.domain).toBe('security.user');
        expect(result.type).toBe('created');
        expect(result.context).toBe('testuser');
        expect(result.timestamp).toBe('2026-03-12T10:00:00.000Z');
        expect(result.initiator).toBe('admin');
        expect(result.nodeId).toBe('node-1');
        expect(result.attributes).toEqual({ key: 'value' });
      });
    });
  });

  describe('formatTimestamp', () => {
    it('should format ISO timestamp to readable format', () => {
      const result = formatTimestamp('2026-03-12T10:30:00.000Z');

      // Format depends on locale, but should contain the date components
      expect(result).toContain('Mar');
      expect(result).toContain('12');
      expect(result).toContain('2026');
    });

    it('should return Invalid Date for invalid timestamps', () => {
      const result = formatTimestamp('invalid-date');

      expect(result).toBe('Invalid Date');
    });
  });

  describe('formatTimestampFull', () => {
    it('should format ISO timestamp with seconds', () => {
      const result = formatTimestampFull('2026-03-12T10:30:45.000Z');

      expect(result).toContain('Mar');
      expect(result).toContain('12');
      expect(result).toContain('2026');
    });

    it('should return Invalid Date for invalid timestamps', () => {
      const result = formatTimestampFull('invalid-date');

      expect(result).toBe('Invalid Date');
    });
  });
});
