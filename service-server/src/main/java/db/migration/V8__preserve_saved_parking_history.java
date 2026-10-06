package db.migration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Locale;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

public class V8__preserve_saved_parking_history extends BaseJavaMigration {
    private static final String TABLE = "saved_parking_locations";

    @Override
    public void migrate(Context context) throws Exception {
        var connection = context.getConnection();
        scrubPlateNumbers(connection);
        var metadata = connection.getMetaData();
        var product = metadata.getDatabaseProductName();
        if (!product.equalsIgnoreCase("MySQL") && !product.equalsIgnoreCase("H2")) {
            throw new SQLException("Unsupported database for saved parking history migration: " + product);
        }
        var table = metadata.storesUpperCaseIdentifiers() ? TABLE.toUpperCase(Locale.ROOT) : TABLE;
        String foreignKey = null;
        try (var keys = metadata.getImportedKeys(connection.getCatalog(), connection.getSchema(), table)) {
            while (keys.next()) {
                if ("vehicle_id".equalsIgnoreCase(keys.getString("FKCOLUMN_NAME"))) {
                    foreignKey = keys.getString("FK_NAME");
                    break;
                }
            }
        }
        if (foreignKey == null) {
            throw new SQLException("Vehicle foreign key was not found on " + TABLE);
        }
        var quote = metadata.getIdentifierQuoteString().trim();
        var quotedForeignKey = quote + foreignKey.replace(quote, quote + quote) + quote;
        try (var statement = connection.createStatement()) {
            statement.execute("ALTER TABLE " + TABLE + (product.equalsIgnoreCase("MySQL")
                ? " DROP FOREIGN KEY " : " DROP CONSTRAINT ") + quotedForeignKey);
            statement.execute(product.equalsIgnoreCase("MySQL")
                ? "ALTER TABLE " + TABLE + " MODIFY COLUMN vehicle_id BINARY(16) NULL"
                : "ALTER TABLE " + TABLE + " ALTER COLUMN vehicle_id DROP NOT NULL");
            statement.execute("ALTER TABLE " + TABLE + " ADD CONSTRAINT fk_saved_parking_locations_vehicle " +
                "FOREIGN KEY (vehicle_id) REFERENCES vehicles(id) ON DELETE SET NULL");
        }
    }

    private void scrubPlateNumbers(Connection connection) throws Exception {
        var mapper = new ObjectMapper();
        try (var select = connection.createStatement();
             var rows = select.executeQuery("SELECT id, snapshot_json FROM " + TABLE);
             var update = connection.prepareStatement("UPDATE " + TABLE + " SET snapshot_json = ? WHERE id = ?")) {
            while (rows.next()) {
                var snapshot = mapper.readTree(rows.getString("snapshot_json"));
                if (!(snapshot instanceof ObjectNode object)) {
                    throw new SQLException("Invalid saved parking snapshot");
                }
                if (object.remove("plateNumber") != null) {
                    update.setString(1, mapper.writeValueAsString(object));
                    update.setBytes(2, rows.getBytes("id"));
                    update.executeUpdate();
                }
            }
        }
    }
}
