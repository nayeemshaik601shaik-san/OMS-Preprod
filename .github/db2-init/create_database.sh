#!/bin/bash
# Licensed Materials - Property of IBM
# IBM Sterling Order Management (5725-D10), IBM Order Management (5737-D18)
# (C) Copyright IBM Corp. 2020 All Rights Reserved.
# US Government Users Restricted Rights - Use, duplication or disclosure restricted by GSA ADP Schedule Contract with IBM Corp.

echo " =============== Starting instance/db initialization ==============="
date

# Set ownership for fencedid BEFORE any db2set commands
chown root:db2iadm1 /database/config/db2inst1/sqllib/adm/fencedid 2>/dev/null || true

su - db2inst1 -c "db2set DB2_CAPTURE_LOCKTIMEOUT=OFF"
su - db2inst1 -c "db2set DB2_SKIPINSERTED=ON"
su - db2inst1 -c "db2set DB2_USE_ALTERNATE_PAGE_CLEANING=ON"
su - db2inst1 -c "db2set DB2_NUM_CKPW_DAEMONS=0"
su - db2inst1 -c "db2set DB2_EVALUNCOMMITTED=ON"
su - db2inst1 -c "db2set DB2_SELECTIVITY=ON"
su - db2inst1 -c "db2set DB2_SKIPDELETED=ON"
su - db2inst1 -c "db2set DB2LOCK_TO_RB=STATEMENT"
su - db2inst1 -c "db2set DB2COMM=tcpip"
su - db2inst1 -c "db2set DB2_PARALLEL_IO=ON"
su - db2inst1 -c "db2set DB2_NUM_CKPW_DAEMONS=0"
su - db2inst1 -c "db2set DB2_COMPATIBILITY_VECTOR=ORA"
su - db2inst1 -c "db2set DB2_DEFERRED_PREPARE_SEMANTICS=NO"

update_db()
{
        su - db2inst1 -c "db2 update db cfg for $DB_DATA using SELF_TUNING_MEM ON"
        su - db2inst1 -c "db2 update db cfg for $DB_DATA using LOGFILSIZ 102400"
        su - db2inst1 -c "db2 update db cfg for $DB_DATA using LOGPRIMARY 10"
        su - db2inst1 -c "db2 update db cfg for $DB_DATA using LOGSECOND 100"
}

# Check if database needs upgrade (SQL5035N error) or has SQL1701N error
su - db2inst1 -c "db2 connect to $DB_DATA 2>&1" | tee /tmp/db_connect.log
DB_CONNECT_RC=${PIPESTATUS[0]}

# Check for SQL1701N error (database terminated incorrectly)
HAS_SQL1701N=false
HAS_SQL5035N=false

if grep -q "SQL1701N" /tmp/db_connect.log; then
    HAS_SQL1701N=true
fi

if grep -q "SQL5035N" /tmp/db_connect.log; then
    HAS_SQL5035N=true
fi

# Handle SQL1701N - database crash recovery
if [ "$HAS_SQL1701N" = true ]; then
    echo "=========================================="
    echo "Database recovery required (SQL1701N detected)"
    echo "Database was not properly shut down"
    echo "=========================================="
    date

    # Check if this is a downlevel database that also needs upgrade
    if [ "$HAS_SQL5035N" = true ]; then
        echo "Database also needs upgrade (SQL5035N detected)"
        echo "Using db2ckrst to clear crash flag before upgrade..."

        # For downlevel databases, use db2ckrst to reset crash state
        su - db2inst1 -c "db2ckrst -d $DB_DATA"
        CKRST_RC=$?

        if [ $CKRST_RC -eq 0 ]; then
            echo "✓ Database crash state cleared successfully"
            echo "Proceeding with database upgrade..."
        else
            echo "WARNING: db2ckrst returned code $CKRST_RC"
            echo "Attempting to continue with upgrade anyway..."
        fi
    else
        # Same-level database, use restart for crash recovery
        echo "Attempting crash recovery with db2 restart..."
        su - db2inst1 -c "db2 restart database $DB_DATA"
        RESTART_RC=$?

        if [ $RESTART_RC -eq 0 ]; then
            echo "✓ Database crash recovery completed successfully"
            su - db2inst1 -c "db2 connect to $DB_DATA"
            if [ $? -eq 0 ]; then
                echo "✓ Database is now accessible"
                su - db2inst1 -c "db2 disconnect $DB_DATA"
            fi
        else
            echo "ERROR: Database restart failed with return code $RESTART_RC"
            echo "Check DB2 diagnostic logs at: ~db2inst1/sqllib/db2dump/"
            exit 1
        fi
    fi
fi

if [ "$HAS_SQL5035N" = true ]; then
    echo "=========================================="
    echo "Database upgrade required (SQL5035N detected)"
    echo "Performing automatic database upgrade from 11.5.9 to 12.1.3.0"
    echo "=========================================="
    date

    # Ensure clean state before upgrade to prevent SQL1701N
    echo "Step 1: Preparing database for upgrade..."
    echo "  - Forcing all applications to disconnect..."
    su - db2inst1 -c "db2 force applications all" || true
    sleep 3

    echo "  - Deactivating database..."
    su - db2inst1 -c "db2 deactivate database $DB_DATA" || true
    sleep 2

    echo "  - Performing clean DB2 restart before upgrade..."
    su - db2inst1 -c "db2stop"
    sleep 2
    su - db2inst1 -c "db2start"
    sleep 2

    # Perform upgrade
    echo "Step 2: Executing database upgrade..."
    echo "  Command: db2 upgrade database $DB_DATA"
    su - db2inst1 -c "db2 upgrade database $DB_DATA"
    UPGRADE_RC=$?

    if [ $UPGRADE_RC -eq 0 ]; then
        echo "✓ Database upgrade completed successfully"

        # Rebind packages
        echo "Step 3: Rebinding database packages..."
        su - db2inst1 -c "db2 connect to $DB_DATA && db2 bind @db2ubind.lst blocking all grant public && db2 bind @db2cli.lst blocking all grant public && db2 terminate"

        # Ensure clean shutdown after upgrade to prevent SQL1701N
        echo "Step 4: Ensuring clean database state after upgrade..."
        su - db2inst1 -c "db2 connect to $DB_DATA"
        su - db2inst1 -c "db2 quiesce database immediate force connections"
        su - db2inst1 -c "db2 unquiesce database"
        su - db2inst1 -c "db2 deactivate database $DB_DATA" || true
        su - db2inst1 -c "db2 disconnect all" || true
        su - db2inst1 -c "db2stop"
        sleep 2
        su - db2inst1 -c "db2start"
        sleep 2

        # Verify clean state
        echo "Step 5: Verifying database is in clean state..."
        su - db2inst1 -c "db2 connect to $DB_DATA"
        if [ $? -eq 0 ]; then
            echo "✓ Database verified and ready for use"
            su - db2inst1 -c "db2 disconnect $DB_DATA"
        fi

        echo "✓ Database upgrade and configuration completed"
        date
    else
        echo "ERROR: Database upgrade failed with return code $UPGRADE_RC"
        echo "Check DB2 diagnostic logs at: ~db2inst1/sqllib/db2dump/"
        exit 1
    fi

elif [ $DB_CONNECT_RC -ne 0 ]; then
    date
    if [ "$DB_BACKUP_RESTORE" = "true" ] && [ -f "/var/oms/$DB_BACKUP_FILE" ] && [ "$DB_IMPORTDATA" != "true" ]; then
                DB_BACKUP_NAME=$(basename "/var/oms/$DB_BACKUP_FILE" ".tar.gz")
                cd /tmp
                rm -rf $DB_BACKUP_NAME
                tar xzf /var/oms/$DB_BACKUP_FILE
                echo "Restoring database $DB_DATA from /tmp/$DB_BACKUP_NAME"
                date
                chmod -R 777 /tmp/$DB_BACKUP_NAME
                su - db2inst1 -c "db2 -x 'RESTORE DATABASE $DB_DATA FROM /tmp/$DB_BACKUP_NAME REPLACE EXISTING'"
                rm -rf /tmp/$DB_BACKUP_NAME
                echo "$DB_DATA restored...."

                # Check if restored database needs upgrade
                if su - db2inst1 -c "db2 connect to $DB_DATA 2>&1" | grep -q "SQL5035N"; then
                    echo "Restored database needs upgrade, performing upgrade..."
                    su - db2inst1 -c "db2 force applications all" || true
                    sleep 2
                    su - db2inst1 -c "db2 upgrade database $DB_DATA"
                    if [ $? -eq 0 ]; then
                        echo "✓ Database upgrade after restore completed"
                        su - db2inst1 -c "db2 connect to $DB_DATA && db2 bind @db2ubind.lst blocking all grant public && db2 bind @db2cli.lst blocking all grant public && db2 terminate"
                    fi
                fi
    else
                echo "Creating new database $DB_DATA"
                date
            su - db2inst1 -c "db2 -x 'CREATE DATABASE $DB_DATA'"
                echo "Configuring database $DB_DATA"
        su - db2inst1 -c "db2 -x 'connect to $DB_DATA' && db2 -x 'CREATE BUFFERPOOL OMS32K_BP IMMEDIATE SIZE AUTOMATIC PAGESIZE 32k' && db2 -x 'CREATE BUFFERPOOL OMS_TMP_32K_BP IMMEDIATE SIZE AUTOMATIC PAGESIZE 32k' && db2 -x 'CREATE TABLESPACE OMS_32K_TS PAGESIZE 32k MANAGED BY AUTOMATIC STORAGE BUFFERPOOL OMS32K_BP' && db2 -x 'CREATE TEMPORARY TABLESPACE OMS_TMP_32K_TS PAGESIZE 32k MANAGED BY AUTOMATIC STORAGE BUFFERPOOL OMS_TMP_32K_BP' && db2 -x 'GRANT USE OF TABLESPACE OMS_32K_TS to public'"
                update_db
        echo "$DB_DATA configured...."
        if [ "$DB_IMPORTDATA" = "true" ]; then
                cd /tmp
                rm -rf db2move
                cp -a /tmp/oms/db2move .
                chmod -R 777 db2move
                su - db2inst1 -c "db2 -x 'connect to $DB_DATA' && cd /tmp/db2move && db2 -tvf db2look.sql && db2move $DB_DATA import"
        fi
    fi
else
    echo "✓ Database $DB_DATA is already accessible, no upgrade needed"
fi
update_db
su - db2inst1 -c "db2 disconnect ALL"
su - db2inst1 -c "db2stop force"
su - db2inst1 -c "db2start"
touch /var/oms/db.ready
echo " =============== Instance/db initialization done ==============="
date
