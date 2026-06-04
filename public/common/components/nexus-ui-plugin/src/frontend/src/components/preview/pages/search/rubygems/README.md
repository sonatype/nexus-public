# Agent 3: RubyGems Search - Sprint 2

## Your Mission
Build the RubyGems package search UI with real API integration.

## API Endpoint
```
GET /service/rest/v1/search?format=rubygems
```

## Required Files

Create these files in `/search/rubygems/`:

| File | Purpose |
|------|---------|
| `rubygems.types.ts` | TypeScript types |
| `RubyGemsSearchPage.tsx` | Main page component |
| `RubyGemsSearchFilters.tsx` | Filter controls |
| `RubyGemsSearchResults.tsx` | Results list |
| `RubyGemsResultRow.tsx` | Single result row |
| `RubyGemsDetailPage.tsx` | Package detail view |
| `useRubyGemsSearch.ts` | Search hook with real API |
| `mockData.ts` | Mock data for dev/testing |
| `RubyGemsSearchPage.scss` | Styles |
| `index.ts` | Exports |
| `__tests__/RubyGemsSearchPage.test.tsx` | Unit tests |

## RubyGems-Specific Filters

```typescript
interface RubyGemsSearchFilters {
  name?: string;        // Gem name
  version?: string;     // Version
  platform?: string;    // ruby, java, etc.
  repository?: string;  // Repository name
}
```

## API Parameters

| Filter | API Param |
|--------|-----------|
| name | `q` or `name` |
| version | `version` |
| platform | `rubygems.platform` |
| repository | `repository` |

## Reference Implementation
Copy patterns from `/search/pypi/usePyPISearch.ts`:
- `USE_REAL_API = true`
- Axios calls to `/service/rest/v1/search?format=rubygems`
- Result aggregation by gem name

## RubyGems Result Fields
```typescript
interface RubyGemsResult {
  id: string;
  name: string;
  version: string;
  platform: string;
  repository: string;
  summary?: string;
  description?: string;
  authors?: string;
  licenses?: string[];
  homepage?: string;
  downloadUrl?: string;
}
```

## Test Requirements
- Test page renders
- Test filters work
- Test search execution
- Test result display
- Mock Axios in tests

## When Done
1. Ensure all tests pass: `yarn jest -- src/search/rubygems`
2. Ensure build passes: `yarn build`
3. Report completion to Agent 0


