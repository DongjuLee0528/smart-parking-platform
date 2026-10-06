package com.smartparking.savedparkinglocation;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.SQLException;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;

class SavedParkingLocationMigrationTest {
    @Test
    void upgradesExistingV7RowsWithoutLosingHistoryOrPlatePrivacy() throws Exception {
        var url = "jdbc:h2:mem:saved_location_v7_upgrade;MODE=MySQL;DB_CLOSE_DELAY=-1";
        var dataSource = new SingleConnectionDataSource(url, "sa", "", true);
        try {
            var v7 = Flyway.configure().dataSource(dataSource)
                .locations("classpath:db/migration").target("7").load();
            v7.migrate();

            var userId = id(1);
            var lotId = id(2);
            var floorId = id(3);
            var zoneId = id(4);
            var spaceId = id(5);
            var vehicleId = id(6);
            var locationId = id(7);
            try (var connection = dataSource.getConnection()) {
                insert(connection, "INSERT INTO users VALUES (?, 'migration-user', 'migration@example.com', " +
                    "'Test', 'USER', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)", userId);
                insert(connection, "INSERT INTO parking_lots VALUES (?, 'Original Lot', 'Address', 37.5, 127.0, " +
                    "'', '', 'ACTIVE', 'ACTIVE', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)", lotId);
                insert(connection, "INSERT INTO parking_floors VALUES (?, ?, 'B1', -1, 0)", floorId, lotId);
                insert(connection, "INSERT INTO parking_zones VALUES (?, ?, 'A')", zoneId, floorId);
                insert(connection, "INSERT INTO parking_spaces VALUES (?, ?, 'A-01', 'GENERAL', '[]', TRUE)",
                    spaceId, zoneId);
                insert(connection, "INSERT INTO vehicles VALUES (?, ?, '12가3456', 'Mine', TRUE, 0, " +
                    "CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)", vehicleId, userId);
                try (var statement = connection.prepareStatement("INSERT INTO saved_parking_locations " +
                    "VALUES (?, ?, ?, ?, ?, CURRENT_TIMESTAMP, NULL)")) {
                    statement.setBytes(1, locationId);
                    statement.setBytes(2, userId);
                    statement.setBytes(3, vehicleId);
                    statement.setBytes(4, spaceId);
                    statement.setString(5, "{\"parkingLotName\":\"Original Lot\",\"plateNumber\":\"12가3456\"}");
                    statement.executeUpdate();
                }
            }

            var current = Flyway.configure().dataSource(dataSource)
                .locations("classpath:db/migration").load();
            current.migrate();
            current.validate();

            try (var connection = dataSource.getConnection()) {
                try (var statement = connection.prepareStatement("SELECT snapshot_json FROM saved_parking_locations " +
                    "WHERE id = ?")) {
                    statement.setBytes(1, locationId);
                    try (var rows = statement.executeQuery()) {
                        assertThat(rows.next()).isTrue();
                        assertThat(rows.getString(1)).contains("Original Lot")
                            .doesNotContain("plateNumber", "12가3456");
                    }
                }
                try (var statement = connection.prepareStatement("DELETE FROM vehicles WHERE id = ?")) {
                    statement.setBytes(1, vehicleId);
                    assertThat(statement.executeUpdate()).isEqualTo(1);
                }
                try (var statement = connection.prepareStatement("SELECT vehicle_id FROM saved_parking_locations " +
                    "WHERE id = ?")) {
                    statement.setBytes(1, locationId);
                    try (var rows = statement.executeQuery()) {
                        assertThat(rows.next()).isTrue();
                        assertThat(rows.getBytes(1)).isNull();
                    }
                }
            }
        } finally {
            dataSource.destroy();
        }
    }

    private static byte[] id(int value) {
        var id = new byte[16];
        id[15] = (byte) value;
        return id;
    }

    private static void insert(java.sql.Connection connection, String sql, byte[]... ids) throws SQLException {
        try (var statement = connection.prepareStatement(sql)) {
            for (var index = 0; index < ids.length; index++) {
                statement.setBytes(index + 1, ids[index]);
            }
            statement.executeUpdate();
        }
    }
}
