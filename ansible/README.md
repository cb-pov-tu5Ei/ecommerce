# Ansible Deployment for Ecommerce Spring Boot Application

This Ansible playbook automates the deployment of the Spring Boot e-commerce application to Ubuntu VMs on GCP. It handles Java installation, artifact download from Artifactory, systemd service configuration, and health verification.

## Overview

The playbook is designed for CD (Continuous Deployment) pipeline integration and is fully parameterized. All configuration (VM IPs, SSH credentials, Artifactory credentials) is passed via command-line parameters, making it environment-agnostic and suitable for automated deployments.

**What it does:**
- Installs OpenJDK 17 JRE on Ubuntu 24.04 LTS
- Downloads JAR artifact from Artifactory
- Configures systemd service for automatic startup and restart
- Validates deployment with health checks
- Supports idempotent re-deployments

## Prerequisites

### Local Machine
- Ansible 2.9+ installed (`pip install ansible`)
- SSH access to target VMs
- SSH private key for authentication

### Target VMs
- Ubuntu 24.04 LTS (tested)
- SSH access enabled on port 22
- User with sudo privileges (default: `ansible`)
- Port 8080 open in firewall
- Directories pre-created: `/opt/app/{bin,config,logs}`

### Artifactory
- JAR artifact uploaded to Artifactory
- Valid credentials for download

## Directory Structure

```
ansible/
├── ansible.cfg                          # Ansible configuration
├── deploy.yml                           # Main deployment playbook
├── inventory/
│   └── host_vars/.gitkeep               # Placeholder (not used in CD)
├── roles/
│   ├── java/                            # Install Java 17 JRE
│   │   ├── tasks/main.yml
│   │   └── defaults/main.yml
│   ├── springboot-app/                  # Deploy application
│   │   ├── tasks/
│   │   │   ├── main.yml
│   │   │   ├── deploy.yml
│   │   │   └── service.yml
│   │   ├── templates/
│   │   │   └── ecommerce.service.j2
│   │   ├── handlers/main.yml
│   │   └── defaults/main.yml
│   └── health-check/                    # Verify deployment
│       ├── tasks/main.yml
│       └── defaults/main.yml
└── README.md                            # This file
```

## Quick Start

### 1. Get VM IP addresses

```bash
# From terraform directory
cd /Users/ldonley/workspace/pov/tjx/terraform
terraform output vm_external_ips

# Example output:
# staging = "34.73.123.45"
# production = "35.185.45.67"
```

### 2. Test SSH connectivity

```bash
ansible all -i "34.73.123.45," -m ping \
  -e "ansible_user=ansible" \
  -e "ansible_ssh_private_key_file=/Users/ldonley/workspace/pov/tjx/terraform/tjx_pov_key"
```

### 3. Deploy to staging

```bash
cd /Users/ldonley/workspace/pov/ecommerce/ansible

ansible-playbook -i "34.73.123.45," deploy.yml \
  -e "ansible_user=ansible" \
  -e "ansible_ssh_private_key_file=/Users/ldonley/workspace/pov/tjx/terraform/tjx_pov_key" \
  -e "artifactory_user=<YOUR_USERNAME>" \
  -e "artifactory_password=<YOUR_PASSWORD>" \
  -e "environment=staging"
```

## Usage Examples

### Deploy to Staging (with staging JVM settings)

```bash
ansible-playbook -i "34.73.123.45," deploy.yml \
  -e "ansible_user=ansible" \
  -e "ansible_ssh_private_key_file=/Users/ldonley/workspace/pov/tjx/terraform/tjx_pov_key" \
  -e "artifactory_user=admin" \
  -e "artifactory_password=password123" \
  -e "environment=staging" \
  -e "jvm_options='-Xms256m -Xmx512m -XX:+UseG1GC'"
```

### Deploy to Production (with production JVM settings)

```bash
ansible-playbook -i "35.185.45.67," deploy.yml \
  -e "ansible_user=ansible" \
  -e "ansible_ssh_private_key_file=/Users/ldonley/workspace/pov/tjx/terraform/tjx_pov_key" \
  -e "artifactory_user=admin" \
  -e "artifactory_password=password123" \
  -e "environment=production" \
  -e "jvm_options='-Xms512m -Xmx1024m -XX:+UseG1GC -XX:MaxGCPauseMillis=200'"
```

### Deploy with Custom JAR Version

```bash
ansible-playbook -i "34.73.123.45," deploy.yml \
  -e "ansible_user=ansible" \
  -e "ansible_ssh_private_key_file=/Users/ldonley/workspace/pov/tjx/terraform/tjx_pov_key" \
  -e "artifactory_user=admin" \
  -e "artifactory_password=password123" \
  -e "jar_filename=ecommerce-0.0.2-SNAPSHOT.jar" \
  -e "artifactory_jar_path=io/cb-demos/ecommerce/0.0.2-SNAPSHOT/ecommerce-0.0.2-SNAPSHOT.jar"
```

### Dry Run (Check Mode)

Test what would change without making actual changes:

```bash
ansible-playbook -i "34.73.123.45," deploy.yml \
  -e "ansible_user=ansible" \
  -e "ansible_ssh_private_key_file=/Users/ldonley/workspace/pov/tjx/terraform/tjx_pov_key" \
  -e "artifactory_user=admin" \
  -e "artifactory_password=password123" \
  --check --diff
```

### Skip Health Check (Faster Testing)

Useful when testing deployment steps without waiting for application startup:

```bash
ansible-playbook -i "34.73.123.45," deploy.yml \
  -e "ansible_user=ansible" \
  -e "ansible_ssh_private_key_file=/Users/ldonley/workspace/pov/tjx/terraform/tjx_pov_key" \
  -e "artifactory_user=admin" \
  -e "artifactory_password=password123" \
  --skip-tags health
```

### Only Install Java (VM Initialization)

Install Java without deploying the application:

```bash
ansible-playbook -i "34.73.123.45," deploy.yml \
  -e "ansible_user=ansible" \
  -e "ansible_ssh_private_key_file=/Users/ldonley/workspace/pov/tjx/terraform/tjx_pov_key" \
  --tags java
```

### Verbose Output (Debugging)

```bash
ansible-playbook -i "34.73.123.45," deploy.yml \
  -e "ansible_user=ansible" \
  -e "ansible_ssh_private_key_file=/Users/ldonley/workspace/pov/tjx/terraform/tjx_pov_key" \
  -e "artifactory_user=admin" \
  -e "artifactory_password=password123" \
  -vvv
```

## Configuration Variables

### Required Variables (Must be provided via command-line)

| Variable | Description | Example |
|----------|-------------|---------|
| `ansible_user` | SSH user on target VM | `ansible` |
| `ansible_ssh_private_key_file` | Path to SSH private key | `/path/to/key` |
| `artifactory_user` | Artifactory username | `admin` |
| `artifactory_password` | Artifactory password | `password123` |

### Optional Variables (Have defaults)

| Variable | Default | Description |
|----------|---------|-------------|
| `environment` | (not set) | Environment name (staging/production) |
| `jar_filename` | `ecommerce-0.0.1-SNAPSHOT.jar` | JAR file name |
| `artifactory_jar_path` | `io/cb-demos/ecommerce/0.0.1-SNAPSHOT/ecommerce-0.0.1-SNAPSHOT.jar` | Path in Artifactory |
| `jvm_options` | `-Xms512m -Xmx1024m -XX:+UseG1GC` | JVM heap and GC settings |
| `app_port` | `8080` | Application port |
| `service_name` | `ecommerce` | Systemd service name |

### Environment-Specific JVM Options

**Staging (low memory):**
```bash
-e "jvm_options='-Xms256m -Xmx512m -XX:+UseG1GC'"
```

**Production (optimized):**
```bash
-e "jvm_options='-Xms512m -Xmx1024m -XX:+UseG1GC -XX:MaxGCPauseMillis=200'"
```

## Verification

### After Deployment

```bash
# SSH to VM
ssh -i /Users/ldonley/workspace/pov/tjx/terraform/tjx_pov_key ansible@<VM_IP>

# Check Java version
java -version  # Should show OpenJDK 17

# Check service status
sudo systemctl status ecommerce

# Check service logs
sudo journalctl -u ecommerce -n 50
tail -f /opt/app/logs/application.log

# Check if port is listening
sudo ss -tlnp | grep 8080

# Test application locally
curl http://localhost:8080/

# Exit VM
exit

# Test from local machine
curl http://<VM_IP>:8080/

# Open in browser
open http://<VM_IP>:8080/
```

### Verification Checklist

- [ ] Java 17 JRE installed (`java -version`)
- [ ] JAR file exists at `/opt/app/bin/ecommerce-0.0.1-SNAPSHOT.jar`
- [ ] Systemd service file at `/etc/systemd/system/ecommerce.service`
- [ ] Service is enabled (`systemctl is-enabled ecommerce`)
- [ ] Service is running (`systemctl is-active ecommerce`)
- [ ] Port 8080 listening (`ss -tlnp | grep 8080`)
- [ ] Application responds (`curl http://localhost:8080/`)
- [ ] External access works (`curl http://<vm-ip>:8080/`)
- [ ] Logs written to `/opt/app/logs/application.log`
- [ ] Service survives reboot (test with `sudo reboot`)

## Troubleshooting

### SSH Connection Failures

```bash
# Check SSH key permissions
chmod 400 /Users/ldonley/workspace/pov/tjx/terraform/tjx_pov_key

# Test SSH manually
ssh -i /Users/ldonley/workspace/pov/tjx/terraform/tjx_pov_key ansible@<VM_IP>

# Verify VM firewall allows SSH
gcloud compute firewall-rules list --filter="name:tjx-allow-ssh"
```

### Java Installation Failures

```bash
# SSH to VM and check
ssh -i /Users/ldonley/workspace/pov/tjx/terraform/tjx_pov_key ansible@<VM_IP>

# Update package cache manually
sudo apt-get update

# Try installing Java manually
sudo apt-get install openjdk-17-jre-headless
```

### Artifactory Download Failures

**Common causes:**
- Invalid credentials
- Incorrect URL or path
- Network connectivity issues
- Artifact doesn't exist

**Debug:**
```bash
# Test Artifactory access from VM
ssh -i /Users/ldonley/workspace/pov/tjx/terraform/tjx_pov_key ansible@<VM_IP>

# Test download manually
curl -u admin:password https://artifactory.tjx-poc.cb-demos.io/artifactory/libs-snapshot/io/cb-demos/ecommerce/0.0.1-SNAPSHOT/ecommerce-0.0.1-SNAPSHOT.jar -o test.jar

# Check artifact exists in Artifactory
curl -u admin:password https://artifactory.tjx-poc.cb-demos.io/artifactory/api/storage/libs-snapshot/io/cb-demos/ecommerce/0.0.1-SNAPSHOT/
```

### Service Start Failures

```bash
# SSH to VM
ssh -i /Users/ldonley/workspace/pov/tjx/terraform/tjx_pov_key ansible@<VM_IP>

# Check service status
sudo systemctl status ecommerce

# View logs
sudo journalctl -u ecommerce -n 100 --no-pager

# Check application logs
tail -100 /opt/app/logs/application.log

# Try starting manually
sudo systemctl start ecommerce

# Check if port is already in use
sudo ss -tlnp | grep 8080
```

### Health Check Failures

**Common causes:**
- Application takes too long to start (increase retries)
- Port not accessible (firewall issue)
- Application crashes on startup (check logs)

**Solutions:**
```bash
# Increase health check retries
-e "health_check_retries=60"

# Skip health check temporarily
--skip-tags health

# Check logs on VM
sudo journalctl -u ecommerce -f
```

### Memory Issues (e2-small VMs)

VMs have only 2GB RAM. If application fails with OOM:

```bash
# Reduce JVM heap size
-e "jvm_options='-Xms256m -Xmx512m -XX:+UseG1GC'"

# Check memory usage on VM
ssh -i /Users/ldonley/workspace/pov/tjx/terraform/tjx_pov_key ansible@<VM_IP>
free -h
ps aux --sort=-%mem | head
```

## CD Pipeline Integration

This playbook is designed to be called from a CD workflow. Example usage in a CD pipeline:

```bash
# Variables provided by CD workflow:
# - VM_IP: Target VM IP address
# - SSH_KEY_PATH: Path to SSH private key
# - ARTIFACTORY_USER: Artifactory username
# - ARTIFACTORY_PASS: Artifactory password
# - ENVIRONMENT: staging or production

ansible-playbook -i "${VM_IP}," deploy.yml \
  -e "ansible_user=ansible" \
  -e "ansible_ssh_private_key_file=${SSH_KEY_PATH}" \
  -e "artifactory_user=${ARTIFACTORY_USER}" \
  -e "artifactory_password=${ARTIFACTORY_PASS}" \
  -e "environment=${ENVIRONMENT}"
```

## Idempotency

The playbook is idempotent - safe to run multiple times:

- Java installation: Skips if already installed
- JAR download: Only downloads if changed
- Service configuration: Only updates if changed
- Service restart: Only if configuration changed

**Test idempotency:**
```bash
# Run twice, second run should show minimal changes
ansible-playbook -i "34.73.123.45," deploy.yml \
  -e "ansible_user=ansible" \
  -e "ansible_ssh_private_key_file=/Users/ldonley/workspace/pov/tjx/terraform/tjx_pov_key" \
  -e "artifactory_user=admin" \
  -e "artifactory_password=password123"

# Run again - should be mostly "ok", few "changed"
ansible-playbook -i "34.73.123.45," deploy.yml \
  -e "ansible_user=ansible" \
  -e "ansible_ssh_private_key_file=/Users/ldonley/workspace/pov/tjx/terraform/tjx_pov_key" \
  -e "artifactory_user=admin" \
  -e "artifactory_password=password123"
```

## Rollback

If deployment fails or issues are found:

```bash
# SSH to VM
ssh -i /Users/ldonley/workspace/pov/tjx/terraform/tjx_pov_key ansible@<VM_IP>

# List backup JARs
ls -lh /opt/app/bin/*.backup.*

# Stop service
sudo systemctl stop ecommerce

# Restore previous JAR (replace timestamp)
sudo cp /opt/app/bin/ecommerce-0.0.1-SNAPSHOT.jar.backup.1234567890 /opt/app/bin/ecommerce-0.0.1-SNAPSHOT.jar

# Start service
sudo systemctl start ecommerce

# Verify
sudo systemctl status ecommerce
curl http://localhost:8080/
```

## Maintenance

### View Service Logs

```bash
# Real-time logs
ssh -i /Users/ldonley/workspace/pov/tjx/terraform/tjx_pov_key ansible@<VM_IP>
sudo journalctl -u ecommerce -f

# Last 100 lines
sudo journalctl -u ecommerce -n 100

# Application logs
tail -f /opt/app/logs/application.log
```

### Restart Service

```bash
ssh -i /Users/ldonley/workspace/pov/tjx/terraform/tjx_pov_key ansible@<VM_IP>
sudo systemctl restart ecommerce
sudo systemctl status ecommerce
```

### Stop Service

```bash
ssh -i /Users/ldonley/workspace/pov/tjx/terraform/tjx_pov_key ansible@<VM_IP>
sudo systemctl stop ecommerce
```

### Start Service

```bash
ssh -i /Users/ldonley/workspace/pov/tjx/terraform/tjx_pov_key ansible@<VM_IP>
sudo systemctl start ecommerce
```

## Support

For issues or questions:
1. Check the troubleshooting section above
2. Review Ansible logs: `ansible/ansible.log`
3. Check application logs on VM: `/opt/app/logs/application.log`
4. Review systemd logs: `sudo journalctl -u ecommerce -n 100`

## License

This playbook is part of the ecommerce demo application.
