package de.dereingerostete.border.database;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

public class BorderDatabase extends SQLDatabase {

    public BorderDatabase(@NotNull File file) throws SQLException {
        super(file);
    }

    @Override
    public void onConnected() throws SQLException {
        createTable("Players", "`uuid` VARCHAR(36), `borderSize` DOUBLE, PRIMARY KEY (`uuid`)");
    }

    public void setPlayer(@NotNull UUID uuid, double borderSize) throws SQLException {
        update("REPLACE INTO `Players` (`uuid`, `borderSize`) VALUES (?, ?);", uuid.toString(), borderSize);
    }

    @Nullable
    public Double getPlayerBorderSize(@NotNull UUID uuid) throws SQLException {
        ResultSet resultSet = query("select * from `Players` where uuid = ?;", uuid.toString());
        if (!resultSet.next()) {
            resultSet.close();
            return null;
        }

        double size = resultSet.getDouble("borderSize");
        resultSet.close();
        return size;
    }

}