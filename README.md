# AdaptiveBP — Modular Monolith Platform

A full-stack platform for dynamic form generation, domain management, and application provisioning. Built with **Spring Boot 3.3** + **Angular 17** + **MongoDB Atlas**.

---

## Prerequisites

| Tool | Version | Download |
|---|---|---|
| **JDK** | 17+ | [Adoptium](https://adoptium.net/) |
| **Node.js** | 18+ | [nodejs.org](https://nodejs.org/) |
| **npm** | 9+ | Bundled with Node.js |
| **MongoDB Atlas** | Free tier works | [mongodb.com/atlas](https://www.mongodb.com/atlas) |

---

## Quick Start

### 1. Clone & Setup Environment

```bash
git clone <repo-url>
cd formGenarator-MrChand
```

Copy the environment file and add your MongoDB Atlas connection string:

```bash
cp .env.example .env
```

Edit `.env` with your MongoDB Atlas URI:

```env
SPRING_DATA_MONGODB_URI=mongodb+srv://<username>:<password>@<cluster>.mongodb.net/<database>?retryWrites=true&w=majority
```

> **Tip:** Get your connection string from the [MongoDB Atlas Dashboard](https://cloud.mongodb.com/) → Database → Connect → Drivers.

### 2. Start the Backend

```bash
cd Backend/api
```

**Windows (PowerShell):**
```powershell
$env:JAVA_HOME = "C:\Program Files\Java\jdk-17"
.\mvnw.cmd spring-boot:run -DskipTests
```

**macOS / Linux:**
```bash
export JAVA_HOME=/path/to/jdk-17
./mvnw spring-boot:run -DskipTests
```

Wait for the startup log:
```
Started AdaptiveBpApplication in X.XX seconds
```

The backend runs on **http://localhost:8005**

### 3. Start the Frontend

Open a **new terminal** and run:

```bash
cd Frontend
npm install       # Only needed on first run
npx ng serve
```

Wait for:
```
✔ Compiled successfully.
```

The frontend runs on **http://localhost:4200**

### 4. Open the App

Navigate to **http://localhost:4200** in your browser.

---

## Project Structure

```
formGenarator-MrChand/
├── Backend/api/                    # Spring Boot API
│   └── src/main/java/com/adaptivebp/
│       ├── AdaptiveBpApplication.java    # Entry point
│       ├── shared/                       # Shared Kernel
│       │   ├── config/                   # CORS, Security, Mongo configs
│       │   ├── security/                 # JWT auth, filters, user details
│       │   ├── exception/                # Global error handler
│       │   └── audit/                    # Auditable base entity
│       └── modules/                      # Business Modules
│           ├── identity/                 # Auth, users, roles
│           ├── organisation/             # Domains, groups, members
│           ├── appmanagement/            # Applications, app groups
│           └── formbuilder/              # Form models, fields, custom forms
│
├── Frontend/                       # Angular 17 SPA
│   └── src/app/
│       ├── core/                         # Singleton services, guards, interceptors
│       ├── shared/                       # Reusable components (navbar, etc.)
│       └── features/                     # Lazy-loaded feature modules
│           ├── auth/                     # Login, signup flows
│           ├── organisation/             # Domain management
│           ├── application/              # App management
│           ├── form-builder/             # Visual form designer
│           └── dashboard/                # Home page
│
├── .env.example                    # Environment template
└── .gitignore
```

---

## Tech Stack

| Layer | Technology |
|---|---|
| **Backend** | Spring Boot 3.3, Spring Security, Spring Data MongoDB |
| **Frontend** | Angular 17, Bootstrap, D3.js, IDS Enterprise |
| **Database** | MongoDB Atlas (cloud) |
| **Auth** | JWT (jjwt 0.12.6) with cookie-based transport |
| **Build** | Maven (backend), Angular CLI (frontend) |

---

## API Configuration

All backend config lives in `Backend/api/src/main/resources/application.properties`:

| Property | Default | Description |
|---|---|---|
| `server.port` | `8005` | Backend HTTP port |
| `spring.data.mongodb.uri` | _from env_ | MongoDB connection string |
| `uigenerator.app.jwtSecret` | _(set in file)_ | JWT signing key (≥32 chars) |
| `uigenerator.app.jwtExpirationMs` | `86400000` | Token expiry (24 hours) |
| `application.fronend.path` | `http://localhost:4200` | CORS allowed origin |

---

## Common Issues

### `WeakKeyException` on startup
The JWT secret in `application.properties` must be **at least 32 characters** (256 bits). Update `uigenerator.app.jwtSecret` to a longer value.

### `SPRING_DATA_MONGODB_URI` not found
Make sure you've created a `.env` file from `.env.example` and your terminal loads it. On Windows, you may need to set it manually:
```powershell
$env:SPRING_DATA_MONGODB_URI = "mongodb+srv://..."
```

On WSL / bash, note that MongoDB Atlas URIs often contain `&` (e.g. `...retryWrites=true&w=majority`).
If you `source .env` without quoting, bash will treat `&` as a background operator and the variable will not be set.
Use quotes in `.env` or export it explicitly:
```bash
export SPRING_DATA_MONGODB_URI='mongodb+srv://...'
```

### Frontend `ng serve` fails
Run `npm install` in the `Frontend/` directory first to install dependencies.

### Port already in use
Kill existing processes on ports 8005 (backend) or 4200 (frontend):
```powershell
# Windows
netstat -ano | findstr :8005
taskkill /PID <PID> /F
```

---

## Contributing

See [CONTRIBUTION.md](CONTRIBUTION.md) for guidelines.

## License

See [LICENSE](LICENSE) for details.
