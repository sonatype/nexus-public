# Agent 2: NuGet Search Implementation

## Your Assignment
Build the NuGet search UI for Preview UI (`#preview/browse/search/nuget`).

## Worktree Path
```
/Users/mitchellsjohnson/.cursor/worktrees/nexus-internal__Workspace_/sgi
```

## Your Directory
```
/Users/mitchellsjohnson/.cursor/worktrees/nexus-internal__Workspace_/sgi/plugins/nexus-coreui-plugin/src/frontend/src/search/nuget/
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
| `NuGetSearchPage.tsx` | Main search page (copy GASearchPage pattern) |
| `NuGetSearchFilters.tsx` | NuGet-specific filters |
| `NuGetSearchResults.tsx` | Results table |
| `NuGetDetailPage.tsx` | Package detail view |
| `useNuGetSearch.ts` | Search state hook |
| `nuget.types.ts` | NuGet-specific types |

## NuGet-Specific Filters

```typescript
interface NuGetSearchFilters {
  packageId?: string;       // Package ID
  version?: string;         // Package version
  prerelease?: boolean;     // Include prerelease versions
  targetFramework?: string; // .NET framework (net6.0, net8.0, etc.)
}
```

## NuGet Result Fields

```typescript
interface NuGetResult {
  id: string;              // Unique ID
  packageId: string;       // NuGet Package ID
  displayName: string;     // Display name
  latestVersion: string;
  versionsCount: number;
  description?: string;
  authors?: string[];
  projectUrl?: string;
  iconUrl?: string;
  lastUpdated: string;
}
```

## Route to Register

After completing, tell Agent 0 to add this route to `previewBrowseRoutes.js`:
```javascript
{
  name: 'preview.browse.search.nuget',
  url: '/nuget/:keyword',
  component: NuGetSearchPage,
  params: {keyword: {value: null, raw: true, dynamic: true}},
  data: {title: 'NuGet Search'},
}
```

## Acceptance Criteria

- [ ] Search by packageId, version, prerelease, targetFramework
- [ ] Results show: displayName, latestVersion, versionsCount, description
- [ ] Click row navigates to detail page
- [ ] URL reflects search state (bookmarkable)
- [ ] Use mock data initially (Agent 0 will connect API later)

## When Done

1. Update TASK_PLAN.md with your completion status
2. Notify Agent 0 that NuGet search is ready for route registration


