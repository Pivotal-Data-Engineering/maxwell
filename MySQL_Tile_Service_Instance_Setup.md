# MySQL service instance configuration for _Maxwell's Daemon_

Refer to [this document](https://docs.pivotal.io/pivotalcf/2-2/customizing/trouble-advanced.html)

1. Get the GUID for the service instance:
   ```bash
   $ cf service mysql --guid
   4cb22f7f-0551-4ba8-b3d0-eb98aa60e56f
   ```
1. SSH into the Ops Manager VM:
   ```bash
   $ ssh ubuntu@<ops_manager_host_name>
   ```
1. Get the BOSH Director IP from the Ops Manager UI
   - https://opsman.pcf-retail.com/infrastructure/director/status
   - Take the value from the `IPS` column
1. Use this value to create an alias
   ```bash
   $ bosh alias-env gcp -e 10.0.0.21 --ca-cert /var/tempest/workspaces/default/root_ca_certificate
   ```
1. Using the web browser (logged into Ops Manager), fetch the BOSH Director credentials:
   - https://opsman.pcf-retail.com/api/v0/deployed/director/credentials/director_credentials
1. Use these to log in
   ```bash
   $ bosh -e gcp log-in
   ```
1. Use the GUID obtained earlier to find the matching BOSH deployment:
   ```bash
   $ bosh -e gcp deployments | grep 4cb22f7f-0551-4ba8-b3d0-eb98aa60e56f
   service-instance_4cb22f7f-0551-4ba8-b3d0-eb98aa60e56f ...
   ```
1. Use this deployment value to SSH into the VM:
   ```bash
   $ bosh -e gcp -d service-instance_4cb22f7f-0551-4ba8-b3d0-eb98aa60e56f ssh mysql/0
   ```
1. Sudo to the `root` user:
   ```bash
   $ sudo su -
   ```
1. Edit the my.cnf file, changing the `binlog-row-image` setting to `full`:
   ```bash
   # vim /var/vcap/jobs/mysql/config/my.cnf
   binlog-row-image = full
   ```
1. Stop and then start MySQL:
   ```bash
   # monit stop mysql
   # sleep 10
   # monit start mysql
   ```
1. Get the `admin` user's password:
   ```bash
   # grep "ALTER USER 'admin'@'localhost'" /var/vcap/jobs/mysql/scripts/init.sql
   ```
1. Log into MySQL:
   ```bash
   # mysql -uadmin -p
   ```
1. Setup the user and do the grants (NOTE: this user will create and use a DB for tracking):
   ```bash
   SET SESSION SQL_LOG_BIN = 0;
   GRANT ALL on maxwell.* to 'maxwell'@'%' identified by 'SOME_PASSWORD';
   GRANT SELECT, REPLICATION CLIENT, REPLICATION SLAVE on *.* to 'maxwell'@'%';
   SET SESSION SQL_LOG_BIN = 1;
   ```
1. That should have done it.

