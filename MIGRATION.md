# Native migration

This checklist tracks user-visible behavior from the `main` Capacitor branch. Features move as complete vertical slices: shared models and Ktor API, native Android UI, native iOS UI, then tests.

## Foundation

- [x] Secure token storage and PAT sign-in
- [x] Modrinth OAuth callback flow
- [x] Dashboard, projects, teams, analytics, and profile shell
- [x] Native dark/light theme and floating glass navigation
- [ ] Localization (English, Russian, German, French, Italian, Polish)
- [ ] Persisted theme and accent
- [ ] Persisted project sorting and favorites
- [ ] Release update checks and Android update download flow

## Projects

- [ ] Project details: overview, links, environment, categories, gallery
- [x] Project search, sorting, and favorites
- [ ] Create and delete projects
- [ ] Edit metadata, status, license, links, icon, and gallery
- [ ] Versions: list, inspect, create, edit, delete, and upload files
- [ ] Resolve and manage dependencies
- [ ] Team members, permissions, payout split, invitations, and ownership transfer

## Teams and organizations

- [ ] Organization details and projects
- [ ] Create, edit, and delete organizations
- [ ] Transfer projects into and out of organizations
- [ ] Organization member management

## Activity

- [ ] Unread notifications and grouping
- [ ] Mark one or all notifications as read
- [ ] Notification actions and deep links

## Analytics and payouts

- [x] Project totals and top-project overview
- [ ] Modrinth v3 analytics metrics and time ranges
- [ ] Local analytics snapshots and weekly comparisons
- [ ] Payout balance and payout history

## Account

- [x] Profile summary and sign out
- [ ] Edit username and bio
- [ ] Upload and remove avatar
- [ ] Onboarding and welcome setup
