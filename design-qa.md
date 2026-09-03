# MODEL-P5 Design QA

- Reference: `docs/原型设计/22、邮件工单系统-企业权限模型升级原型.html`
- Viewport: 1440 × 1024
- Runtime: local frontend with real local backend API and local database
- Screens compared: enterprise management, mailbox strategy, user data grants, assignment rule groups and ticket strategy snapshot
- Interaction checks: navigation, filters, tabs, edit dialogs, strategy form, grant tree, ticket detail
- Console checks: 0 errors, 0 warnings in a clean browser session after fixes
- Build checks: frontend lint and production build passed
- Visual fixes: normalized grant-tree checkbox sizing and removed mailbox-list horizontal scrolling at 1440 × 1024
- Compatibility fix: replaced deprecated Ant Design props and removed duplicate Tiptap extensions
- Comparison artifacts: `docs/验收记录/MODEL-P5-*-compare.png`

## Prior SLA page QA baseline

- Target: `docs/原型设计/12、邮件工单系统-SLA策略原型.png`
- Implementation: `frontend/src/App.tsx`; verified at 1440 × 1024.
- Sidebar, topbar, title, metrics, notice, three-column work area, and SLA preview matched the PG-09 structure.
- SLA list used real API data and displayed default/enabled tags, response/resolve/warning/escalation values, and bound calendar text.
- The editor covered policy status, response and resolution targets, warning and escalation thresholds, calendar binding, deletion protection, and work-hour deadline preview.
- Empty, loading, API error, and permission states were implemented as actual page states.
- Existing responsive behavior hides the left sidebar at narrow widths; the original fidelity check was desktop-only.

## Assignment rule responsive layout QA (2026-08-27)

- Source visual truth: `/var/folders/cs/qt1bk1s555qg0plj2vbd3m0r0000gn/T/codex-clipboard-2a49ce25-3f1a-41d8-82d1-412d23c3160f.png`
- Source pixels: 3024 × 1722; desktop Retina capture, displayed viewport density is not available from the attachment.
- Implementation: `http://127.0.0.1:5174/`, current frontend source after the responsive layout change.
- Intended state: signed-in administrator, 分配规则 page, one selected rule group and one selected rule.
- Build evidence: frontend lint, TypeScript production build, Vite build, and `git diff --check` passed.
- Browser-rendered implementation screenshot: not captured because the controllable in-app browser session is currently at the signed-out login page rather than the authenticated state shown in the source screenshot.
- Full-view comparison: blocked until the same authenticated page state is available.
- Focused comparison target: middle rule ledger header, filters, priority/rule/action columns, and the editor placement at the reported viewport.
- Primary interactions tested: blocked by the missing authenticated browser state.
- Console errors checked: the running Vite service accepted both HMR updates without compile errors; authenticated page console inspection remains blocked.

**Findings**

- [P1 resolved in code] The three-column minimum widths compressed the middle ledger so the filter tail and action header were outside its visible width. The workspace now uses a wider center track on large screens and moves the editor below the group/list row at medium desktop widths.
- [P1 resolved in code] The assignment table allowed content-driven width expansion. It now uses fixed table layout, removes the assignment-specific 720px minimum, truncates long rule copy, and keeps a bounded action column.
- [P2 resolved in code] The four filters could not shrink safely. They now wrap while keeping usable minimum control widths.
- [P1 verification blocker] A same-state post-fix screenshot is still required before visual acceptance.

**Comparison history**

1. Before: supplied screenshot shows the final filter and the middle table's 操作 header/column clipped by the right editor.
2. Fix: responsive two-row medium-desktop layout, wrapped filters, fixed table layout, constrained rule copy and action controls.
3. After: build and HMR succeeded; same-state visual evidence is pending because the controllable browser is signed out.

**Implementation checklist**

- Refresh the already signed-in 5174 page.
- Capture the 分配规则 page at the same viewport.
- Confirm the middle 优先级、规则、操作 headers and all filter controls are simultaneously visible.
- Check the editor below the ledger at medium desktop width and beside it at wide desktop width.

## Notification template layout QA (2026-08-27)

- Source visual truth: `/var/folders/cs/qt1bk1s555qg0plj2vbd3m0r0000gn/T/codex-clipboard-815789bf-d8c9-4100-9a12-02de4a9cfb3b.png`
- Source pixels: 3840 × 1984; authenticated desktop state.
- Implementation: `http://127.0.0.1:5174/`, current frontend after the template layout correction.
- Intended state: signed-in administrator, 通知模板 page, an existing template selected.
- Browser-rendered implementation screenshot: not captured because the controllable in-app browser remains at the signed-out login page.
- Build evidence: frontend lint, TypeScript/Vite production build, `git diff --check`, and Vite HMR all passed.
- Full-view and focused comparison: blocked until the authenticated 通知模板 state is available.

**Findings**

- [P1 resolved in code] `.template-list-panel` had three explicit grid rows for four sections. The enterprise/type filters consumed the flexible list track and caused the template cards to overlap the filter area. It now declares header, search, filters, and scrollable list as four rows.
- [P1 resolved in code] `.template-form-grid` styled inputs and textareas but omitted native selects. Enterprise and template-type fields therefore collapsed to intrinsic content width. Selects now share the same full-width, 36px control treatment and disabled state.
- [P1 verification blocker] A same-state post-fix screenshot is required before visual acceptance.

**Required fidelity surfaces**

- Typography, colors, icons/assets and copy were not changed.
- Layout rhythm was corrected only for the template filter/list tracks and native form controls.
- No new raster or custom icon assets were introduced.

**Comparison history**

1. Before: supplied screenshot shows filters overlapped by the selected template card and the enterprise/type controls collapsed inside otherwise full-width grid cells.
2. Fix: corrected the list panel row model and applied the established form-control sizing to selects.
3. After: build and HMR succeeded; same-state browser evidence is pending because the controllable browser session is signed out.

## Mailbox table layout QA (2026-08-27)

- Source visual truth: `/var/folders/cs/qt1bk1s555qg0plj2vbd3m0r0000gn/T/codex-clipboard-3bb8178b-6fcd-448a-8604-3f7a1e79dbd9.png`
- Source pixels: 2352 × 1362; authenticated desktop state.
- Implementation: `http://127.0.0.1:5174/`, current frontend after the mailbox row correction.
- Intended state: signed-in administrator, 邮箱配置 page, one mailbox selected.
- Browser-rendered implementation screenshot: not captured because the controllable in-app browser remains at the signed-out login page.
- Build evidence: frontend lint, TypeScript/Vite production build, and Vite HMR passed.

**Findings**

- [P1 resolved in code] The mailbox table had been converted from semantic table rows into block/grid containers, while mailbox name, enterprise and both service endpoints were still nested in one cell. The result stacked all mailbox data vertically and allowed it to overflow the compact row.
- [P1 resolved in code] The table now keeps native table layout and separates 邮箱、所属企业、收发服务器、状态/规则、操作 into independent columns. IMAP and SMTP endpoints remain distinct within the service column.
- [P2 resolved in code] The compact split pane now uses an 860px table minimum and scrolls inside the list panel when necessary, rather than collapsing the columns or changing table display semantics.
- [P1 verification blocker] A same-state post-fix screenshot is still required before visual acceptance.

**Comparison history**

1. Before: supplied screenshot shows mailbox name, email, enterprise and both servers stacked below one avatar while the right side of the row remains largely empty.
2. Fix: restored semantic table rendering and assigned each information group to a bounded column with ellipsis handling.
3. After: build and HMR succeeded; same-state browser evidence is pending because the controllable browser session is signed out.

## Assignment and notification workflow redesign QA (2026-08-27)

- Existing assignment source: `/var/folders/cs/qt1bk1s555qg0plj2vbd3m0r0000gn/T/codex-clipboard-2a49ce25-3f1a-41d8-82d1-412d23c3160f.png` (3024 × 1722).
- Existing template source: `/var/folders/cs/qt1bk1s555qg0plj2vbd3m0r0000gn/T/codex-clipboard-815789bf-d8c9-4100-9a12-02de4a9cfb3b.png` (3840 × 1984).
- Visual reference: `/Users/tanzhixing/workSpace/knowlab/docs/test/screenshots/p1-13-page-audit/final-desktop-iam-orgs.png` and `final-desktop-iam-users.png` (both 1280 × 720).
- Implementation: `http://127.0.0.1:5174/`, current frontend source after the workflow redesign.
- Intended state: authenticated administrator with a selected rule/template and real local API data.
- Browser-rendered implementation screenshot: unavailable because the controllable in-app browser is at the signed-out login page.
- Build evidence: oxlint, TypeScript build, Vite production build, `git diff --check`, and Vite HMR passed.

**User workflow findings and changes**

- [P1 resolved in code] Assignment rules previously exposed rule-group maintenance, the rule ledger, editing and testing at the same visual level. The redesign keeps rule groups as the context rail, rules as the primary task, and switches the inspector between 规则配置 and 测试验证.
- [P1 resolved in code] The rule form was a flat field matrix. It now follows the user's decision order: 基础信息 → 匹配条件 → 执行动作 → 规则预览.
- [P1 resolved in code] Notification templates previously required horizontal attention shifts across list, form, variable rail and preview rail. The redesign keeps a two-column master/detail layout and switches the detail between 模板内容 and 发送预览.
- [P1 resolved in code] Template variables are now available beside the composition task as compact insert controls rather than a competing page-level panel.
- [P2 resolved in code] Both pages now reuse the KnowLab organization/identity hierarchy: 16px medium page titles, 11–13px working text, restrained weights, 12–14px panel radii, blue-violet selected states, compact 32–34px controls and quiet blue-gray surfaces.
- [P2 resolved in code] Both workflows retain responsive reflow, visible selected states, keyboard-focus outlines on new tabs/variable controls, and existing permission/loading/empty/save-confirmation behavior.

**Required fidelity surfaces**

- Fonts and typography: implemented from the KnowLab UI font stack and optical weight scale; rendered comparison is pending.
- Spacing and layout rhythm: master/detail tracks, 12px panel rhythm and compact control heights are implemented; overflow and fold position need authenticated browser evidence.
- Colors and tokens: KnowLab blue-violet, blue-gray, semantic success/warning/danger and panel tokens are mapped into the two MailTrace pages.
- Image quality and assets: no raster content is required by either management screen; existing Lucide and Ant Design icon assets were retained.
- Copy and content: labels now follow task order and explain whether actions affect live data, historical tickets or actual sending.

**Comparison history**

1. Before: assignment showed four competing work surfaces; notification templates showed three simultaneous columns with the variable panel separated from composing content.
2. Reference: KnowLab organization and identity screens establish compact master/detail workspaces, lower font weight, restrained card hierarchy and a single dominant task surface.
3. Fix: rebuilt both page structures around selection-first task flow, inspector tabs, grouped forms and shared workflow tokens.
4. After: code/build/HMR checks pass; full-view and focused same-state comparisons remain blocked until the authenticated pages can be captured.

**Remaining blocker**

- The implementation cannot pass visual QA without authenticated screenshots of 分配规则 and 通知模板 at the target desktop viewport, including the new secondary tabs.

final result: blocked

## Global notification template workflow QA (2026-08-27)

- Product decision: notification templates are a global shared library and no longer belong to an enterprise.
- Relationship model: each mailbox selects templates for automatic receipt, assignment notice, agent reply, SLA warning, and SLA breach; SLA policies and assignment rule groups remain enterprise-scoped.
- Notification template layout: left-side scenario navigation, middle global template list, and right-side editor/preview workspace.
- Mailbox strategy layout: template choices remain in the existing two-column form grid, with required assignment/agent-reply choices and conditional SLA warning/breach choices.
- Runtime evidence: local backend started on port 8080 with Nacos and storage disabled; Flyway validated 20 migrations and applied V20 to the local `127.0.0.1` database.
- Build evidence: backend full unit suite passed; frontend oxlint and production build passed.
- Visual status: source and runtime checks pass, but authenticated screenshots of 通知模板 and 邮箱配置 remain blocked because the controllable browser is currently at the signed-out login page.

final result: functional checks passed; authenticated visual comparison pending

## Enterprise management KnowLab-style redesign QA (2026-08-27)

- Source visual truth: `/Users/tanzhixing/workSpace/knowlab/docs/test/screenshots/p1-13-page-audit/final-desktop-iam-orgs.png` and `/Users/tanzhixing/workSpace/knowlab/docs/test/screenshots/p1-13-page-audit/final-desktop-iam-users.png`.
- Source dimensions: both references are 1280 × 720 pixels; desktop application captures at the visible source density.
- Previous MailTrace implementation evidence: `/Users/tanzhixing/workSpace/sfd-mailtrace/docs/验收记录/MODEL-P5-企业管理-actual.png` at 1440 × 1024 pixels.
- Implementation target: `http://127.0.0.1:5174/`, 企业管理 page, authenticated administrator, enterprise list with one selected enterprise.
- Implementation screenshot path: unavailable; the controllable in-app browser opens the signed-out login page, so the authenticated 企业管理 state cannot be captured without user sign-in.
- Intended comparison viewport: 1280 × 720 CSS pixels at device scale factor 1. A density-normalized same-state implementation capture is still required.
- State: enterprise list, search/status controls, selected row, right-side enterprise detail, and edit dialog.
- Build evidence: frontend oxlint, TypeScript build, Vite production build, and `git diff --check` passed.
- Browser evidence: local app opened successfully, but the only visible route is the login page; primary enterprise interactions and authenticated console state could not be checked.

**Full-view comparison evidence**

- Reference captures establish a compact header, four low-height metric cards, a white ledger panel, blue-violet selection, restrained 11–15px typography, and a persistent detail inspector.
- The rendered authenticated implementation is unavailable, so composition, actual fold position, panel height, text wrapping, and overflow cannot be judged from browser pixels.

**Focused region comparison evidence**

- Required focused regions are the enterprise table header/selected row, the detail summary/contact blocks, and the create/edit dialog.
- Focused comparison is blocked because those regions are behind authentication in the controllable browser.

**Findings**

- [P1 resolved in code] The former page was a wide flat table with actions repeated on every row and no persistent enterprise context. It is now a master/detail workspace: filter and select on the left, inspect status, scale, contacts and remarks on the right, then edit or enable/disable from one stable action area.
- [P2 resolved in code] The former large page title, 24px metrics and broad card spacing did not match the KnowLab identity screens. The redesign uses the KnowLab blue-violet token family, compact 32–34px controls, 11–18px typography, 10–12px rhythm, quieter borders and selected-row emphasis.
- [P2 resolved in code] Summary content now presents real operational totals: enterprises, enabled enterprises, associated mailboxes and associated tickets. No placeholder enterprise code or invented business field is shown.
- [P1 verification blocker] A same-state browser capture is required to verify that the master/detail tracks, table columns, modal and responsive fallback do not overlap or clip at the target viewport.

**Required fidelity surfaces**

- Fonts and typography: KnowLab-style system/PingFang stack and compact weight scale are implemented; pixel comparison remains blocked.
- Spacing and layout rhythm: 12px workspace rhythm, compact summary cards and master/detail tracks are implemented; actual browser overflow remains blocked.
- Colors and visual tokens: blue-violet primary, cool blue-gray surfaces and semantic success/disabled colors are mapped from the reference direction.
- Image quality and assets: the management screen has no raster content; existing Lucide icon assets are used consistently and no custom SVG/CSS illustrations were introduced.
- Copy and content: terminology follows the current enterprise model and makes the enterprise business boundary explicit without exposing technical identifiers.

**Comparison history**

1. Before: the previous MailTrace capture shows a large flat statistic-and-table page with excessive empty space and row-level actions competing with the main record data.
2. Reference: KnowLab organization and user screens use compact metrics, a primary ledger, persistent selected state and a dedicated detail surface.
3. Fix: rebuilt 企业管理 into a responsive master/detail workspace and retained all existing search, status filter, create, edit and enable/disable behavior.
4. After: lint/build/diff checks pass; post-fix visual evidence is blocked at the login page.

**Implementation checklist**

- Sign in to the local MailTrace page and open 企业管理.
- Capture 1280 × 720 and a wide desktop viewport with one enterprise selected.
- Check table header alignment, selected-row emphasis, detail-panel scroll, long contact text truncation and modal focus state.
- Re-run full-view and focused comparisons, then resolve any remaining P0/P1/P2 issue.

final result: blocked

## Enterprise edit modal persistent actions QA (2026-08-28)

- Source visual truth: `/var/folders/cs/qt1bk1s555qg0plj2vbd3m0r0000gn/T/codex-clipboard-4d29850d-512b-4e17-9714-c65a1e4ee12f.png`.
- Source pixels: 2140 × 1958 at Retina density; normalized to a 1070 × 979 CSS-pixel comparison.
- Browser-rendered implementation: `/private/tmp/mailtrace-enterprise-modal-save-fixed-1070x979.png`.
- Normalized side-by-side comparison: `/private/tmp/mailtrace-enterprise-modal-save-comparison-normalized.jpg`.
- Implementation viewport: 1070 × 979 CSS pixels with the temporary viewport override reset after testing.
- State: authenticated administrator, 企业管理, 默认企业 selected, 编辑企业 dialog open.
- Primary interactions tested: open enterprise management, open the edit dialog, scroll the form body to its maximum position, and verify the persistent action footer.
- Console errors checked: none.
- Build checks: frontend oxlint, TypeScript build and Vite production build passed.

**Full-view comparison evidence**

- Before: the form content expanded the dialog beyond the viewport and pushed the footer, including 保存企业, outside the visible area.
- After: the dialog is a bounded column layout. Header and footer remain fixed inside the modal while only the form body scrolls.
- At 1070 × 979, the dialog is 820 × 931 CSS pixels and remains within the viewport. The save button occupies y=902–938 and is fully visible.

**Focused region comparison evidence**

- The modal footer remains at y=885–954 while the form body scrolls from 0 to 112.5 pixels.
- After scrolling the form body to the bottom, 保存企业 remains fully visible at y=902–938.
- The footer uses the existing white surface, divider, button sizes, typography and violet primary action token; no unrelated visual style changed.

**Findings**

- [P1 resolved] The long enterprise form had no constrained flex layout, so `overflow-y: auto` on the body had no usable height and the footer was pushed below the viewport.
- Fix: make `.enterprise-modal` a bounded vertical flex container with hidden outer overflow; give `.p5-form-grid` `min-height: 0`, flexible remaining height and vertical scrolling; keep direct header/footer children non-shrinking.
- No remaining P0/P1/P2 issue was found in the affected modal action flow.

**Required fidelity surfaces**

- Fonts and typography: unchanged from the existing enterprise modal.
- Spacing and layout rhythm: header, scrollable body and 69px action footer now remain structurally separated.
- Colors and visual tokens: unchanged; the established white surface, subtle dividers and violet primary action remain intact.
- Image quality and assets: no image assets are used or changed in this fix.
- Copy and content: unchanged; 取消 and 保存企业 are visible and retain their existing actions.

**Comparison history**

1. Initial evidence: supplied screenshot shows the modal continuing below the viewport with no visible save action.
2. Fix: introduced the constrained three-part flex layout and body-only scrolling.
3. Post-fix evidence: same-density browser capture shows both footer actions; scroll testing confirms they remain visible, with zero console errors.

final result: passed
