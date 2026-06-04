# Agent 1: npm Search Implementation

## Your Assignment
Build the npm search UI for Preview UI (`#preview/browse/search/npm`).

## Worktree Path
```
/Users/mitchellsjohnson/.cursor/worktrees/nexus-internal__Workspace_/sgi
```

## Your Directory
```
/Users/mitchellsjohnson/.cursor/worktrees/nexus-internal__Workspace_/sgi/plugins/nexus-coreui-plugin/src/frontend/src/search/npm/
```

## Reference Implementation
Copy the pattern from Maven search:
```
/Users/mitchellsjohnson/.cursor/worktrees/nexus-internal__Workspace_/sgi/plugins/nexus-coreui-plugin/src/frontend/src/search/results/
```

## Files to Create

| File | Purpose |
|------|---------|
| `index.ts` | Public exports |
| `NpmSearchPage.tsx` | Main search page (copy GASearchPage pattern) |
| `NpmSearchFilters.tsx` | npm-specific filters |
| `NpmSearchResults.tsx` | Results table |
| `NpmDetailPage.tsx` | Package detail view |
| `useNpmSearch.ts` | Search state hook |
| `npm.types.ts` | npm-specific types |

## npm-Specific Filters

```typescript
interface NpmSearchFilters {
  scope?: string;      // @scope (e.g., @angular, @types)
  name?: string;       // Package name
  version?: string;    // Semver version  
  tag?: string;        // dist-tag (latest, next, beta)
}
```

## npm Result Fields

```typescript
interface NpmResult {
  id: string;           // Unique ID
  scope: string;        // @scope or empty
  name: string;         // Package name
  displayName: string;  // @scope/name or name
  latestVersion: string;
  versionsCount: number;
  description?: string;
  author?: string;
  lastUpdated: string;
}
```

## Route to Register

After completing, tell Agent 0 to add this route to `previewBrowseRoutes.js`:
```javascript
{
  name: 'preview.browse.search.npm',
  url: '/npm/:keyword',
  component: NpmSearchPage,
  params: {keyword: {value: null, raw: true, dynamic: true}},
  data: {title: 'npm Search'},
}
```

## Acceptance Criteria

- [ ] Search by scope, name, version, tag
- [ ] Results show: displayName, latestVersion, versionsCount, description
- [ ] Click row navigates to detail page
- [ ] URL reflects search state (bookmarkable)
- [ ] Use mock data initially (Agent 0 will connect API later)

## When Done

1. Update TASK_PLAN.md with your completion status
2. Notify Agent 0 that npm search is ready for route registration


