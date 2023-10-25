package net.crashcraft.crashclaim.data.providers.sqlite.versions;

import co.aikar.idb.DB;
import co.aikar.idb.DbRow;
import net.crashcraft.crashclaim.data.providers.sqlite.DataVersion;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class DataRev5 implements DataVersion {
    @Override
    public int getVersion() {
        return 5;
    }

    @Override
    public void executeUpgrade(int fromRevision) throws SQLException {
        DB.executeUpdate("PRAGMA foreign_keys = OFF"); // Turn foreign keys off
        DB.executeUpdate("CREATE TABLE players_backup (\n" +
                "\t\"id\"\tINTEGER,\n" +
                "\t\"uuid\"\tTEXT NOT NULL,\n" +
                "\t\"username\"\tTEXT,\n" +
                "\tUNIQUE(\"id\",\"uuid\"),\n" +
                "\tPRIMARY KEY(\"id\" AUTOINCREMENT)\n" +
                ")");

        DB.executeInsert("INSERT INTO players_backup SELECT * FROM players");

        DB.executeUpdate("DROP TABLE players");

        DB.executeUpdate("CREATE TABLE players (\n" +
                "\t\"id\"\tINTEGER,\n" +
                "\t\"uuid\"\tTEXT UNIQUE NOT NULL,\n" +
                "\t\"username\"\tTEXT,\n" +
                "\t\"lastLogin\"\tINTEGER,\n" +
                "\tUNIQUE(\"id\",\"uuid\"),\n" +
                "\tPRIMARY KEY(\"id\" AUTOINCREMENT)\n" +
                ")");
        List<UUID> seenUUIDs = new ArrayList<>();
        for (DbRow row : DB.getResults("SELECT id, uuid, username FROM players_backup")){
            if (row == null){
                continue;
            }
            UUID uuid = UUID.fromString(row.getString("uuid"));
            if (seenUUIDs.contains(uuid)){
                continue;
            }
            DB.executeInsert("INSERT INTO players (id, uuid, username) VALUES (?, ?, ?)", row.getInt("id"), row.getString("uuid"), row.getString("username"));
            seenUUIDs.add(uuid);
        }
        DB.executeUpdate("PRAGMA foreign_keys = ON");  // Undo
    }
}
