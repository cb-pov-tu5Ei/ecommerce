# Quick Testing Guide

This guide shows how to test the Ansible playbook with the terraform-provisioned VMs.

## Prerequisites

1. Terraform infrastructure is deployed
2. Artifactory has the JAR artifact
3. You have Artifactory credentials

## Step 1: Get VM IPs

```bash
cd /Users/ldonley/workspace/pov/tjx/terraform
terraform output vm_external_ips
```

Save the IPs:
- Staging: `<STAGING_IP>`
- Production: `<PRODUCTION_IP>`

## Step 2: Test SSH Connectivity

```bash
# Test SSH to staging
ssh -i /Users/ldonley/workspace/pov/tjx/terraform/tjx_pov_key ansible@<STAGING_IP>
# Should connect successfully, then exit

# Test with Ansible ping
ansible all -i "<STAGING_IP>," -m ping \
  -e "ansible_user=ansible" \
  -e "ansible_ssh_private_key_file=/Users/ldonley/workspace/pov/tjx/terraform/tjx_pov_key"
```

## Step 3: Verify Artifactory Access

```bash
# Test from local machine
curl -u admin:password \
  https://artifactory.tjx-poc.cb-demos.io/artifactory/api/storage/libs-snapshot/io/cb-demos/ecommerce/0.0.1-SNAPSHOT/

# Should return JSON with file listing
```

## Step 4: Deploy to Staging (Dry Run)

```bash
cd /Users/ldonley/workspace/pov/ecommerce/ansible

ansible-playbook -i "<STAGING_IP>," deploy.yml \
  -e "ansible_user=ansible" \
  -e "ansible_ssh_private_key_file=/Users/ldonley/workspace/pov/tjx/terraform/tjx_pov_key" \
  -e "artifactory_user=<YOUR_USERNAME>" \
  -e "artifactory_password=<YOUR_PASSWORD>" \
  -e "environment=staging" \
  --check --diff
```

## Step 5: Deploy to Staging (Actual)

```bash
ansible-playbook -i "<STAGING_IP>," deploy.yml \
  -e "ansible_user=ansible" \
  -e "ansible_ssh_private_key_file=/Users/ldonley/workspace/pov/tjx/terraform/tjx_pov_key" \
  -e "artifactory_user=<YOUR_USERNAME>" \
  -e "artifactory_password=<YOUR_PASSWORD>" \
  -e "environment=staging"
```

## Step 6: Verify Deployment

```bash
# Test from local machine
curl http://<STAGING_IP>:8080/

# Should return HTML of home page

# Open in browser
open http://<STAGING_IP>:8080/
```

## Step 7: SSH to VM and Check

```bash
ssh -i /Users/ldonley/workspace/pov/tjx/terraform/tjx_pov_key ansible@<STAGING_IP>

# Check Java
java -version

# Check service
sudo systemctl status ecommerce

# Check logs
sudo journalctl -u ecommerce -n 50
tail -20 /opt/app/logs/application.log

# Check port
sudo ss -tlnp | grep 8080

# Test locally
curl http://localhost:8080/

# Exit
exit
```

## Step 8: Test Idempotency

Run the deployment again - should show minimal changes:

```bash
ansible-playbook -i "<STAGING_IP>," deploy.yml \
  -e "ansible_user=ansible" \
  -e "ansible_ssh_private_key_file=/Users/ldonley/workspace/pov/tjx/terraform/tjx_pov_key" \
  -e "artifactory_user=<YOUR_USERNAME>" \
  -e "artifactory_password=<YOUR_PASSWORD>" \
  -e "environment=staging"
```

Expected output: Mostly "ok" tasks, few "changed" tasks.

## Step 9: Deploy to Production

After staging is validated:

```bash
ansible-playbook -i "<PRODUCTION_IP>," deploy.yml \
  -e "ansible_user=ansible" \
  -e "ansible_ssh_private_key_file=/Users/ldonley/workspace/pov/tjx/terraform/tjx_pov_key" \
  -e "artifactory_user=<YOUR_USERNAME>" \
  -e "artifactory_password=<YOUR_PASSWORD>" \
  -e "environment=production" \
  -e "jvm_options='-Xms512m -Xmx1024m -XX:+UseG1GC -XX:MaxGCPauseMillis=200'"
```

## Common Issues

### SSH Permission Denied
```bash
chmod 400 /Users/ldonley/workspace/pov/tjx/terraform/tjx_pov_key
```

### Artifactory 401 Unauthorized
- Check username/password
- Verify artifact exists at specified path

### Service Won't Start
```bash
# SSH to VM
ssh -i /Users/ldonley/workspace/pov/tjx/terraform/tjx_pov_key ansible@<STAGING_IP>

# Check logs
sudo journalctl -u ecommerce -n 100
tail -100 /opt/app/logs/application.log

# Try starting manually
sudo systemctl start ecommerce
sudo systemctl status ecommerce
```

### Health Check Timeout
- Application takes 30-60 seconds to start
- Increase retries: `-e "health_check_retries=60"`
- Or skip: `--skip-tags health`

## Success Criteria

✅ Java 17 installed  
✅ JAR downloaded to `/opt/app/bin/`  
✅ Service running (`systemctl status ecommerce`)  
✅ Port 8080 listening  
✅ Application responds to HTTP requests  
✅ External access works from browser  
✅ Logs being written  
✅ Re-deployment is idempotent
