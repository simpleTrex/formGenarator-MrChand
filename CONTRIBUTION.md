# 🧩 Contributing to formGenarator-MrChand

Welcome! 👋  
We’re excited that you’re interested in contributing to **formGenarator-MrChand** — a modular form generation system built for adaptive business workflows.

This guide will help you set up your environment, understand our branching workflow, follow our coding standards, and make your first successful pull request 🚀

---

## 🛠️ 1. Project Setup

### Requirements
- Node.js (v18 or above)
- npm or yarn
- Git
- (Optional) Docker, if contributing to deployment or CI/CD

### Setup Instructions
```bash
# 1. Fork the repository
https://github.com/UGSNBandara/formGenarator-MrChand.git

# 2. Clone your fork
git clone https://github.com/<your-username>/formGenarator-MrChand.git

# 3. Navigate into project
cd formGenarator-MrChand

# 4. Install dependencies
npm install

# 5. Start local development server
npm start
```

---

## 🌿 2. Branching Strategy

We use a simplified **Git Flow** model for clear collaboration:

| **Branch** | **Purpose** |
|-------------|-------------|
| `main` | Stable production-ready branch. Only merges from `release/*` or approved hotfixes. |
| `dev` | Active development branch. All new features and fixes branch from here. |
| `feature/<name>` | New feature branches (e.g., `feature/login-ui`). |
| `bugfix/<name>` | Fixes for non-critical issues. |
| `hotfix/<name>` | Urgent fixes directly for production. |
| `release/<version>` | Used for staging and release preparation. |

---

## 🧱 3. Commit Message Convention

We follow **Conventional Commits** format for clarity and automation.

```
<type>(<scope>): <short summary>
```

**Example:**
```
feat(ui): add form preview modal
fix(api): correct POST route error for form submission
chore(ci): add test workflow for pull requests
```

**Allowed Types:**
| Type | Description |
|------|-------------|
| feat | New feature |
| fix | Bug fix |
| docs | Documentation updates |
| style | Formatting / UI changes (no logic) |
| refactor | Code restructuring without behavior change |
| test | Adding or modifying tests |
| chore | Build, CI/CD, or maintenance tasks |

---

## 🧪 4. Testing & Code Quality

Before pushing:
```bash
# Run linting and tests
npm run lint
npm run test
```

Pull requests **must pass all CI checks** before merging.

If you add new features, please include relevant unit tests.

---

## 🧩 5. Pull Request (PR) Guidelines

1. Create your branch from `dev`.
2. Ensure code follows ESLint and Prettier standards.
3. Include descriptive commit messages.
4. Push changes and open a PR to `dev` branch.
5. Request **peer review** from at least one team member.
6. PR titles should match this format:
   ```
   [<type>] <summary>
   ```
   Example: `[feat] Implement dynamic field generator`

### ✅ PR Checklist
- [ ] Code builds and runs locally
- [ ] All CI checks pass
- [ ] Code reviewed and approved
- [ ] Documentation updated if needed

---

## ⚙️ 6. CI/CD & Deployment Notes

Our CI/CD pipeline (GitHub Actions) automatically:
- Runs linting, build, and tests on every PR.
- Deploys from `dev` → staging.
- Deploys from `main` → production after release approval.

Environment variables are managed securely via **GitHub Secrets**.

---

## 📅 7. Scrum & Collaboration

We follow a **Scrum-based workflow**:
- **Daily Standup:** 5–10 min updates on progress/blockers.
- **Sprint Duration:** 2 weeks.
- **Sprint Review:** Showcase completed features.
- **Retrospective:** Discuss improvements.

Use the **Jira board / Confluence space** for:
- Sprint tasks
- Meeting notes
- Action item tracking

---

## 💬 8. Need Help?

If you’re stuck or have questions:
- Open a **GitHub Discussion** or issue.
- Mention **@UGSNBandara** or **@PrageethBanuka** for DevOps-related help.

---

### 💡 Tip
Keep your PRs **small and focused**. A PR that does one thing well is easier to review and merge!

Thank you for contributing to `formGenarator-MrChand` 🎉  
Together, we build adaptive systems that empower smarter workflows.
