# Design QA - P1-W0-01 登录页原型

final result: passed

## Source

- Reference image: `docs/原型设计/5、邮件工单系统-登录页原型.png`
- Prototype HTML: `docs/原型设计/5、邮件工单系统-登录页原型.html`
- Captured screenshot: `docs/原型设计/5、邮件工单系统-登录页原型-渲染截图.png`
- Wide-screen ratio check: `docs/原型设计/5、邮件工单系统-登录页原型-宽屏比例校验.png`
- Viewports: 1309x1201, 2048x1280

## Checks

| Area | Result | Notes |
|------|--------|-------|
| Layout | passed | Two-column desktop layout matches the reference structure. |
| Brand area | passed | Version badge, product name, subtitle, headline, and capability copy are present. |
| Illustration | passed | Left workflow illustration approximates the reference using existing system UI language. |
| Login card | passed | Card position, scale, title, inputs, remember row, forgot action, and primary button align with the reference. |
| Interaction states | passed | Required, error, loading, success, password toggle, forgot password, and no-account states are implemented. |
| Wide-screen scaling | passed | Prototype now uses a fixed 1309x1201 design canvas with viewport-fit scaling, preventing oversized display and footer overlap. |
| Column balance | passed | Left content column is widened and the right login panel/form is narrowed so the two sides read with more balanced visual weight. |

## Follow-Up Notes

- `PG-01` remains pending because only 谭总 can mark a prototype gate as ✅.
- The rendered screenshot is a QA artifact; the original numbered PNG remains the provided visual reference.
