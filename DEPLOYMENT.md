# Deployment guide (Linode VM + GitHub Actions + Docker, HTTPS via Caddy)

**Domain:** `adaptivebp.me`  |  **Trigger branches:** `deploy` or `docker-deploy` (or the manual *Run workflow* button)

```
git push → GitHub Actions: build images → ghcr.io → SSH to VM → docker compose up
Browser → Caddy (80/443, auto HTTPS) → frontend (nginx) → backend → mongo
```

Nothing is cloned on the server. The only file there is `~/formgen/docker-compose.yml`
(no secrets). Secrets live in GitHub Secrets and are piped over SSH stdin at deploy time.

---

## Part 1 - One-time server setup (MobaXterm, as root)

### 1.1 DNS
`adaptivebp.me` must have an **A record → your VM's public IP**. If the DNS provider has a
proxy option (e.g. Cloudflare orange cloud), set it to **DNS only** so Caddy can get its certificate.
Check from your PC: `nslookup adaptivebp.me`

### 1.2 Swap (you already did this) - verify and tune
```bash
swapon --show
sysctl vm.swappiness=40 && sed -i 's/^vm.swappiness=.*/vm.swappiness=40/' /etc/sysctl.conf
```

### 1.3 Install Docker
```bash
curl -fsSL https://get.docker.com | sh
docker --version && docker compose version
```

### 1.4 Firewall (only SSH + web)
```bash
apt-get install -y ufw   # skip if already installed
ufw allow 22/tcp && ufw allow 80/tcp && ufw allow 443/tcp
ufw --force enable
```
If you use a Linode **Cloud Firewall** in the Linode dashboard, also allow inbound TCP 22, 80, 443.

> Docker publishes ports itself and bypasses `ufw` rules, but only Caddy publishes ports
> (80/443); Mongo and the backend are not exposed.

### 1.5 Deploy SSH key (separate from your personal key)
On **your Windows PC** (PowerShell):
```powershell
& "C:\Windows\System32\OpenSSH\ssh-keygen.exe" -t ed25519 -C "github-deploy" -f "$HOME\.ssh\formgen_deploy" -N '""'
Get-Content "$HOME\.ssh\formgen_deploy.pub"
```
Copy the `.pub` line, then on the **server**:
```bash
mkdir -p ~/.ssh && chmod 700 ~/.ssh
echo "PASTE_THE_PUBLIC_KEY_LINE_HERE" >> ~/.ssh/authorized_keys
chmod 600 ~/.ssh/authorized_keys
```
(Optional but safer: create a non-root user, `adduser deploy && usermod -aG docker deploy`,
and put the key in `/home/deploy/.ssh/authorized_keys`; then `VM_USER=deploy`.)

Test from your PC:
```powershell
ssh -i "$HOME\.ssh\formgen_deploy" root@adaptivebp.me
```

### 1.6 Host key line for `VM_KNOWN_HOSTS`
On the **server**:
```bash
echo "adaptivebp.me $(cut -d' ' -f1,2 /etc/ssh/ssh_host_ed25519_key.pub)"
```
Copy the whole output line (starts with `adaptivebp.me ssh-ed25519 ...`).

### 1.7 Generate strong passwords
On the server (or any Linux shell):
```bash
openssl rand -hex 24   # use as MONGO_PASSWORD  (hex only - safe inside the Mongo URI)
openssl rand -hex 32   # use as JWT_SECRET      (64 chars)
```

---

## Part 2 - GitHub Secrets
Repo → **Settings → Secrets and variables → Actions → New repository secret**

| Secret | Value |
|---|---|
| `VM_HOST` | `adaptivebp.me` (must match the name used in step 1.6) |
| `VM_USER` | `root` (or `deploy`) |
| `VM_SSH_KEY` | full contents of `C:\Users\ASUS\.ssh\formgen_deploy` (the **private** key, including the BEGIN/END lines) |
| `VM_KNOWN_HOSTS` | the line from step 1.6 |
| `DOMAIN` | `adaptivebp.me` |
| `MONGO_PASSWORD` | output of `openssl rand -hex 24` |
| `JWT_SECRET` | output of `openssl rand -hex 32` |

Also check **Settings → Actions → General → Workflow permissions** allows workflows to run
(the workflow itself requests `packages: write` for pushing images).

---

## Part 3 - Deploy
1. Commit and push the new files on your branch:
   ```bat
   git add docker-compose.prod.yml .github/workflows/deploy.yml Backend/api/Dockerfile DEPLOYMENT.md
   git commit -m "Add production deployment"
   git push
   ```
2. Open the repo's **Actions** tab → **Deploy**. Or push to the `deploy` branch / click **Run workflow**.
3. First run: about 5-8 minutes (building images). Later runs: about 2-3 minutes thanks to the layer cache.
4. Open **https://adaptivebp.me** - the first request can take a minute while Caddy gets the certificate
   and the backend (Spring Boot on 1 GB) finishes starting.
5. Sign up an owner account at `https://adaptivebp.me/owner-signup`.

## Verify on the server
```bash
cd ~/formgen
docker compose ps
docker compose logs -f backend      # look for "Started AdaptiveBpApplication"
docker stats --no-stream            # per-container memory
free -h                             # swap usage appears under "Swap: used"
```

## Troubleshooting
| Symptom | Fix |
|---|---|
| `Host key verification failed` in Actions | `VM_KNOWN_HOSTS` doesn't match `VM_HOST`; redo step 1.6 |
| `Permission denied (publickey)` | Wrong key in `VM_SSH_KEY` or public key not in `authorized_keys` |
| `pull access denied` / `unauthorized` | The deploy step logs in to ghcr.io with the job's token; re-run the workflow |
| Browser shows certificate error | DNS not pointing to VM yet, port 80/443 blocked, or DNS proxy enabled; check `docker compose logs caddy` |
| Backend restarts / `Killed` | Out of memory; confirm swap with `free -h` and check `docker compose logs backend` |
| Changed `MONGO_PASSWORD` and DB auth fails | Mongo only reads the password when its volume is first created. Reset with `docker compose down -v` (deletes data) |

## Memory notes
`docker-compose.prod.yml` sets `mem_limit` (RAM) and `memswap_limit` (RAM + swap) per container, so overflow
goes to swap instead of crashing. Swap is a safety net: it keeps the app alive but is slower than RAM.

