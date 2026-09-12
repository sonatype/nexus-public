# Detail Views - Agent 2 Workspace

**Owner**: Agent 2  
**Status**: :green_circle: READY TO START

## :warning: FIRST: Verify You're in the Right Place

**This file should be at:**
```
/Users/mitchellsjohnson/.cursor/worktrees/nexus-internal__Workspace_/sgi/plugins/nexus-coreui-plugin/src/frontend/src/search/details/README.md
```

**Your working directory:**
```
/Users/mitchellsjohnson/.cursor/worktrees/nexus-internal__Workspace_/sgi/plugins/nexus-coreui-plugin/src/frontend/src/search/details/
```

**Run this to navigate here:**
```bash
cd /Users/mitchellsjohnson/.cursor/worktrees/nexus-internal__Workspace_/sgi/plugins/nexus-coreui-plugin/src/frontend/src/search/details/
```

---

## Your Mission

Build the GA Detail Views UI for Preview UI.

When user clicks a search result, they see this detail page with 5 tabs:
- Overview
- Versions
- Repositories
- Files
- Security

---

## Quick Start

1. **Read the contracts**:
   ```bash
   cat ../core/search.types.ts   # Domain types
   cat ../core/search.api.ts     # API contracts  
   cat ../core/search.routes.ts  # Route definitions
   ```

2. **Create your components** (see file list below)

3. **Update task status** in `../TASK_PLAN.md` when done

---

## Files to Create

```
/search/details/
  index.ts                  <- Export all public components
  GADetailPage.tsx          <- Main container with tab navigation
  GAOverviewTab.tsx         <- Overview: description, license, URLs
  GAVersionsTab.tsx         <- Version list table
  GARepositoriesTab.tsx     <- Repositories containing this GA
  GAFilesTab.tsx            <- Files for selected version
  GASecurityTab.tsx         <- Vulnerabilities for selected version
  useGADetail.ts            <- React hook for detail state
  mockData.ts               <- Mock GADetail for development
  __tests__/                <- Jest tests
```

---

## Key Imports

```typescript
// Types
import type {
  GADetail,
  GAVersion,
  GARepository,
  GAAsset,
  GADetailTab,
} from '../core';

// Utilities
import {
  GA_SEARCH_ROUTE_NAMES,
  TAB_ROUTE_MAP,
  getTabFromRoute,
  buildDetailRoute,
  buildSearchRoute,
} from '../core';
```

---

## Component Specifications

### GADetailPage.tsx

Main container that:
- Shows GA identity header (displayName, namespace)
- Renders tab bar (Overview, Versions, Repos, Files, Security)
- Routes to child tab based on URL
- Handles gaId from route params

```typescript
interface GADetailPageProps {
  gaId: string;        // From route: preview.browse.search.maven.detail
  activeTab: GADetailTab;
}
```

### GAVersionsTab.tsx

Table of versions:
| Column | Source |
|--------|--------|
| Version | `version.version` |
| Repositories | `version.repositories` |
| Last Updated | `version.lastUpdated` |

Click version -> updates selected version for Files/Security tabs

### GAFilesTab.tsx & GASecurityTab.tsx

These require a **version to be selected**.

If no version selected, show:
> "Select a version from the Versions tab to view files/security info"

---

## Mock Data Example

```typescript
// mockData.ts
import type { GADetail, GAVersion } from '../core';

export const mockDetail: GADetail = {
  gaId: 'maven:org.apache.commons:commons-lang3',
  format: 'maven',
  displayName: 'commons-lang3',
  namespace: 'org.apache.commons',
  description: 'Apache Commons Lang provides helper utilities for the java.lang API',
  latestVersion: '3.14.0',
  versionsCount: 47,
  repositoriesCount: 2,
  lastUpdated: '2024-01-15T10:30:00Z',
  projectUrl: 'https://commons.apache.org/proper/commons-lang/',
  issuesUrl: 'https://issues.apache.org/jira/browse/LANG',
  license: 'Apache-2.0',
  versions: [
    {
      version: '3.14.0',
      lastUpdated: '2024-01-15T10:30:00Z',
      repositories: ['maven-central'],
    },
    {
      version: '3.13.0',
      lastUpdated: '2023-07-01T08:00:00Z',
      repositories: ['maven-central'],
    },
    // ... more
  ],
  repositories: [
    { name: 'maven-central', type: 'proxy', url: 'https://repo1.maven.org/maven2/' },
  ],
};
```

---

## Tab Navigation

Tabs are URL-driven. Use router to change tabs:

```typescript
import { useRouter } from '@uirouter/react';

const router = useRouter();

// Navigate to versions tab
router.stateService.go(GA_SEARCH_ROUTE_NAMES.MAVEN_DETAIL_VERSIONS, { gaId });
```

---

## DO NOT

- :x: Modify files in `/search/core/`
- :x: Modify files in `/search/results/`
- :x: Modify route files (Agent 0 owns those)
- :x: Create duplicate type definitions

---

## When Done

1. Update `../TASK_PLAN.md`:
   - Mark your tasks as `[x]`
   - Add timestamp

2. Ensure `index.ts` exports:
   ```typescript
   export { GADetailPage } from './GADetailPage';
   // ... other public exports
   ```

3. Agent 0 will wire it into routes
