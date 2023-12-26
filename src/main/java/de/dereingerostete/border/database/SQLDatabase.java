package de.dereingerostete.border.database;

import org.jetbrains.annotations.NotNull;
import org.sqlite.JDBC;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Properties;

public abstract class SQLDatabase {
    protected final @NotNull Connection connection;

    public SQLDatabase(@NotNull File file) throws SQLException {
        JDBC jdbc = new JDBC();
        String url = "jdbc:sqlite:" + file.getPath();
        connection = jdbc.connect(url, new Properties());
        onConnected();
    }

    public abstract void onConnected() throws SQLException;

    public boolean isConnected() throws SQLException {
        return !connection.isClosed();
    }

    public void disconnect() throws SQLException {
        if (isConnected()) connection.close();
    }

    @NotNull
    protected ResultSet query(@NotNull String sql, @NotNull Object @NotNull... objects) throws SQLException {
        return prepare(sql, objects).executeQuery();
    }

    protected void update(@NotNull String sql, @NotNull Object @NotNull... objects) throws SQLException {
        try (PreparedStatement statement = prepare(sql, objects)) {
            statement.executeUpdate();
        }
    }

    @NotNull
    protected PreparedStatement prepare(@NotNull String sql, @NotNull Object @NotNull ... objects) throws SQLException {
        PreparedStatement statement = connection.prepareStatement(sql);
        for (int i = 0; i < objects.length; i++) statement.setObject(i + 1, objects[i]);
        statement.closeOnCompletion();
        return statement;
    }

    protected void createTable(@NotNull String name, @NotNull String arguments) throws SQLException {
        update("create table if not exists `" + name + "` (" + arguments + ");");
    }

}
