# Deploying Talent Trail on a free Oracle Cloud VM

One Always Free ARM VM runs everything from `docker-compose.yml`: PostgreSQL, Redis, RabbitMQ, backend, frontend, Caddy (automatic HTTPS), the backup job and optionally Uptime Kuma. Nothing else is needed.

Free tier terms change. Check Oracle's current Always Free page before you start, and use a card only for identity verification.

## 1. Create the VM

1. Create an Oracle Cloud account and pick a home region close to your users (it cannot be changed later).
2. Compute, Instances, Create instance.
3. Image: Ubuntu 24.04 (aarch64). Shape: VM.Standard.A1.Flex with 2 OCPU and 12 GB memory.
4. Boot volume: 100 GB (the free allowance is 200 GB in total).
5. Add your SSH public key and create the instance. If the region says "out of capacity", retry later or try another availability domain.
6. Networking, Reserved public IPs: create one and attach it to the instance, so the address never changes.

## 2. Open ports 80 and 443 (two places)

Oracle blocks traffic twice, and both must be opened.

1. Cloud console: Networking, your VCN, Security Lists, Default, add ingress rules for source `0.0.0.0/0`, TCP, destination ports `80` and `443`. Keep port 22 only.
2. Inside the VM, the Ubuntu image ships restrictive iptables rules:

```
sudo iptables -I INPUT 6 -m state --state NEW -p tcp --dport 80 -j ACCEPT
sudo iptables -I INPUT 6 -m state --state NEW -p tcp --dport 443 -j ACCEPT
sudo apt-get install -y iptables-persistent
sudo netfilter-persistent save
```

Do not publish 3001 (Uptime Kuma), 5432, 6379 or 5672. They are bound to localhost or the internal network already.

## 3. Point a domain at the VM

Create an `A` record for your domain (for example `hr.example.com`) pointing to the reserved IP. Caddy requests the certificate automatically once the name resolves.

No domain yet? Use a free name such as `yourname.duckdns.org`, or `<ip-with-dashes>.sslip.io` (for example `152-70-1-2.sslip.io`). Both work with Let's Encrypt.

## 4. Install Docker

```
ssh ubuntu@<ip>
sudo apt-get update && sudo apt-get install -y git curl openssl
curl -fsSL https://get.docker.com | sudo sh
sudo usermod -aG docker $USER
exit
```

Sign in again so the group applies, then check `docker compose version`.

## 5. Get the code and create `.env`

```
git clone <your-repo-url> talent-trail
cd talent-trail
sh ops/init-env.sh hr.example.com you@example.com
```

The script writes `.env` with random secrets (mode 600) and prints the first admin password once. Save it, and save the whole `.env` in a password manager. If `FIELD_ENCRYPTION_KEY` is lost, stored bank and tax details cannot be read, even from a restored backup.

Then edit `.env` for mail and backups (sections 7 and 9) and run:

```
sh ops/check-env.sh
```

It fails on anything unsafe and warns about missing mail, off-server backups and alerts. The backend also refuses to start in production with unsafe settings (`ProductionGuard`).

## 6. First start

```
./ops/deploy.sh
```

The first build on the ARM VM takes several minutes (Maven and npm). The script checks `.env`, builds, starts, waits for health, runs the smoke test and records the commit in `.last-good`.

Sign in at `https://<your domain>/login` with the admin email and password from step 5. You are forced to change the password. Afterwards clear `BOOTSTRAP_ADMIN_EMAIL` and `BOOTSTRAP_ADMIN_PASSWORD` in `.env` and run `docker compose up -d`.

Smoke test with a real sign-in at any time:

```
SMOKE_EMAIL=you@example.com SMOKE_PASSWORD='...' sh ops/smoke.sh https://hr.example.com
```

## 7. Email

Reminders, invoices and offer emails need SMTP. A free relay such as Brevo (about 300 emails a day on the free plan, confirm current limits) works. Fill `MAIL_HOST`, `MAIL_PORT`, `MAIL_USERNAME`, `MAIL_PASSWORD`, `MAIL_FROM`, then `docker compose up -d backend`. Use a sender address on a domain you control and add the SPF and DKIM records your provider shows, or mail lands in spam.

## 8. Monitoring

```
docker compose --profile monitoring up -d uptime
```

Uptime Kuma listens on `127.0.0.1:3001`. Reach it with `ssh -L 3001:127.0.0.1:3001 ubuntu@<ip>` and open `http://localhost:3001`. Add an HTTPS monitor for `https://<domain>/api/v1/system/status`, and a push monitor whose URL you paste into `BACKUP_HEARTBEAT_URL`, so a missed backup alerts you. Set `ALERT_EMAILS` for in-app health alerts by email.

## 9. Backups off the server

The VM disk is not a backup. Daily dumps and file archives are made at 20:00 UTC and kept `BACKUP_RETENTION_DAYS` days locally. Turn on the off-server copy:

- Oracle Object Storage has a free allowance and an S3-compatible endpoint. Cloudflare R2 and Backblaze B2 also have free tiers. Create a bucket and an access key.
- In `.env`: `OFFSITE_ENABLED=true`, `OFFSITE_TYPE=s3`, `OFFSITE_ENDPOINT`, `OFFSITE_REGION`, `OFFSITE_ACCESS_KEY`, `OFFSITE_SECRET_KEY`, `OFFSITE_PATH=<bucket>/talent-trail`, and a long random `OFFSITE_ENCRYPTION_PASSPHRASE` (store it with your other secrets).
- `docker compose up -d backup`, then `docker compose exec backup sh /ops/backup.sh` to run one now and confirm the file appears in the bucket.

The README section "Backups" lists the exact variables for SFTP too.

### Restore drill

Do this once before you rely on the system, on a spare machine or after a deploy to a fresh VM:

```
docker compose run --rm restore list
docker compose run --rm restore pull
docker compose run --rm restore db
docker compose run --rm restore files
```

Check that you can sign in and open an old candidate. A backup you have never restored is a guess.

## 10. Updates and rollback

Deploy the latest `main`:

```
./ops/deploy.sh main
```

Or from GitHub: Actions, Deploy, Run workflow. Add these repository secrets under an environment called `production`: `DEPLOY_HOST`, `DEPLOY_USER`, `DEPLOY_PATH`, `DEPLOY_SSH_KEY` (a key authorised only for this VM) and `DEPLOY_KNOWN_HOSTS` (output of `ssh-keyscan <ip>`).

Roll back application code:

```
./ops/rollback.sh
```

It returns to the commit in `.last-good`, or pass a tag or commit. Flyway migrations are not reversed. If a bad migration ran, restore the backup taken before the deploy (the daily one, or run `docker compose exec backup sh /ops/backup.sh` just before risky deploys).

## 11. Hardening

- SSH keys only: set `PasswordAuthentication no` in `/etc/ssh/sshd_config`, then `sudo systemctl restart ssh`.
- `sudo apt-get install -y fail2ban unattended-upgrades`.
- Restrict port 22 in the Oracle security list to your own IP if it is stable.
- Rotate secrets by editing `.env` and running `docker compose up -d`. Changing `JWT_SECRET` signs everyone out. Never change `FIELD_ENCRYPTION_KEY` without a re-encryption plan.
- Run `sudo apt-get update && sudo apt-get upgrade` monthly and reboot when the kernel changes.

## 12. Oracle idle reclaim

Oracle may reclaim Always Free instances that stay very idle for a week (low CPU, network and memory use). A running app with RabbitMQ, the monitor and daily jobs normally stays above the thresholds, but check the current policy. Upgrading the account to Pay As You Go keeps the free resources free and removes this risk; set a budget alert if you do.

## Alternative: a spare computer at home

Run the same compose on any always-on Linux machine and use a Cloudflare Tunnel instead of opening ports. Set `DOMAIN` to your hostname, point the tunnel at `http://localhost:80`, and set `HTTP_PORT`/`HTTPS_PORT` if needed. Caddy will not obtain a certificate behind the tunnel, so use `DOMAIN=http://hr.example.com` only if the tunnel terminates HTTPS, and keep `AUTH_COOKIE_SECURE=true`. This path depends on your power and internet staying up; the off-server backups matter even more here.
