/*
 * Copyright © 2013-2016 The Nxt Core Developers.
 * Copyright © 2016-2019 Jelurida IP B.V.
 *
 * See the LICENSE.txt file at the top-level directory of this distribution
 * for licensing information.
 *
 * Unless otherwise agreed in a custom licensing agreement with Jelurida B.V.,
 * no part of this software, including this file, may be copied, modified,
 * propagated, or distributed except according to the terms contained in the
 * LICENSE.txt file.
 *
 * Removal or modification of this copyright notice is prohibited.
 *
 */

package nxt.db;

import nxt.Constants;
import nxt.Nxt;

import java.sql.*;

public abstract class DerivedDbTable extends Table {

    protected DerivedDbTable(String schemaTable) {
        super(schemaTable);
        Nxt.getBlockchainProcessor().registerDerivedTable(this);
    }

    public void popOffTo(int height) {
        if (!db.isInTransaction()) {
            throw new IllegalStateException("Not in transaction");
        }
        try (Connection con = getConnection();
             PreparedStatement pstmtDelete = con.prepareStatement("DELETE FROM " + schemaTable + " WHERE height > ? LIMIT " + Constants.BATCH_COMMIT_SIZE)) {
            pstmtDelete.setInt(1, height);
            int deleted;
            do {
                deleted = pstmtDelete.executeUpdate();
                db.commitTransaction();
            } while (deleted >= Constants.BATCH_COMMIT_SIZE);
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

    public void rollback(int height) {
        popOffTo(height);
    }

    @Override
    public void truncate() {
        if (!db.isInTransaction()) {
            throw new IllegalStateException("Not in transaction");
        }
        boolean hasPermanentRecords;
        try (Connection con = getConnection();
             Statement stmt = con.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM " + schemaTable + " WHERE height < 0 LIMIT 1")) {
            hasPermanentRecords = rs.next();
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
        if (hasPermanentRecords) {
            popOffTo(-1);
        } else {
            super.truncate();
        }
    }

    public void trim(int height) {
        //nothing to trim
    }

    public void createSearchIndex(Connection con) throws SQLException {
        //implemented in EntityDbTable only
    }

    public boolean isPersistent() {
        return false;
    }

}
