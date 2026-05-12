# Search Results - Agent 1 Workspace

**Owner**: Agent 1  
**Status**: 🟢 READY TO START

## ⚠️ FIRST: Verify You're in the Right Place

**This file should be at:**
```
/Users/mitchellsjohnson/.cursor/worktrees/nexus-internal__Workspace_/sgi/plugins/nexus-coreui-plugin/src/frontend/src/search/results/README.md
```

**Your working directory:**
```
/Users/mitchellsjohnson/.cursor/worktrees/nexus-internal__Workspace_/sgi/plugins/nexus-coreui-plugin/src/frontend/src/search/results/
```

**Run this to navigate here:**
```bash
cd /Users/mitchellsjohnson/.cursor/worktrees/nexus-internal__Workspace_/sgi/plugins/nexus-coreui-plugin/src/frontend/src/search/results/
```

---

## Your Mission

Build the GA Search Results UI for Preview UI (`#preview/browse/search/maven`).

**Key requirement**: Show ONE row per GA (groupId:artifactId), NOT per version.

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
/search/results/
  index.ts                 ← Export all public components
  GASearchPage.tsx         ← Main page (search input + results)
  GASearchInput.tsx        ← Search box with typeahead
  GASearchFilters.tsx      ← Filters: groupId, artifactId, repository
  GASearchResults.tsx      ← Results table
  GAResultRow.tsx          ← Single result row
  GASearchPagination.tsx   ← Load more button
  useGASearch.ts           ← React hook for search state
  mockData.ts              ← Mock GAResult[] for development
  __tests__/               ← Jest tests
```

---

## Key Imports

```typescript
// Types
import type {
  GAResult,
  GASearchRequest,
  GASearchResponse,
  GASuggestion,
} from '../core';

// Utilities
import {
  GA_SEARCH_PARAMS,
  buildSearchRoute,
  buildDetailRoute,
} from '../core';
```

---

## Component Specifications

### GASearchPage.tsx

Main container that:
- Renders search input at top
- Renders filters below input
- Renders results table
- Handles URL query params (source of truth)

### GASearchResults.tsx

Table showing:
| Column | Source |
|--------|--------|
| Name | `result.displayName` |
| Namespace | `result.namespace` |
| Latest Version | `result.latestVersion` |
| Versions | `result.versionsCount` |
| Repositories | `result.repositoriesCount` |

Click row → navigate to `buildDetailRoute(result.gaId)`

### GAResultRow.tsx

Single row component. Props:
```typescript
interface GAResultRowProps {
  result: GAResult;
  onSelect: (gaId: string) => void;
}
```

---

## Mock Data Example

```typescript
// mockData.ts
import type { GAResult } from '../core';

export const mockResults: GAResult[] = [
  {
    gaId: 'maven:org.apache.commons:commons-lang3',
    format: 'maven',
    displayName: 'commons-lang3',
    namespace: 'org.apache.commons',
    latestVersion: '3.14.0',
    versionsCount: 47,
    repositoriesCount: 2,
    lastUpdated: '2024-01-15T10:30:00Z',
  },
  {
    gaId: 'maven:com.google.guava:guava',
    format: 'maven',
    displayName: 'guava',
    namespace: 'com.google.guava',
    latestVersion: '33.0.0-jre',
    versionsCount: 156,
    repositoriesCount: 3,
    lastUpdated: '2024-02-01T14:22:00Z',
  },
  // ... more
];
```

---

## DO NOT

- ❌ Modify files in `/search/core/`
- ❌ Modify files in `/search/details/`
- ❌ Modify route files (Agent 0 owns those)
- ❌ Create duplicate type definitions

---

## When Done

1. Update `../TASK_PLAN.md`:
   - Mark your tasks as `[x]`
   - Add timestamp

2. Ensure `index.ts` exports:
   ```typescript
   export { GASearchPage } from './GASearchPage';
   // ... other public exports
   ```

3. Agent 0 will wire it into routes
