# Design QA

Target: `docs/原型设计/12、邮件工单系统-SLA策略原型.png`

Implementation: `frontend/src/App.tsx` / local Vite app `http://127.0.0.1:5173/`

Viewport checked: 1440x1024 desktop.

## Checks

- Sidebar, topbar, title, metrics, blue notice, three-column work area, and right-side SLA preview match the PG-09 structure.
- SLA strategy list uses real `GET /api/v1/sla-policies` data and shows default/enabled tags, response/resolve/warning/escalation values, and bound calendar text.
- Edit panel supports policy name, enabled/default, response hours, resolve hours, warning threshold, escalation threshold, and calendar binding.
- Delete confirmation is present and blocks default-policy deletion in the UI.
- SLA preview uses the selected work calendar to calculate work-hour deadlines; `2026-07-27 15:30 + 16 工作小时` renders as `2026-07-29 13:30` for a 09:00-18:00 weekday calendar.
- Empty-list, loading, API error, and permission states are implemented as real page states instead of persistent explanatory cards.

## Residual Notes

- The local database currently has no persistent SLA policies or work calendars after cleanup, so the default visible state is empty.
- Existing responsive behavior hides the left sidebar at narrow browser widths; desktop prototype fidelity was checked at 1440x1024.

Final result: passed
