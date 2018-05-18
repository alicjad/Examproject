package motorhomes.com.examproject.repositories;

import motorhomes.com.examproject.model.User;
import motorhomes.com.examproject.util.DbConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * @ Pawel Pohl
 * todo comments
 */
public class UsersDBRepository {

    private Connection connection;
    private PreparedStatement statement;
    private ResultSet result;
    private long connectionTime;

    public UsersDBRepository() throws SQLException {
        connection = DbConnection.getConnection();
        connectionTime = System.currentTimeMillis();
    }

    public User read(String username) throws SQLException {
        User savedUser = null;
        for (int i = 0; i < 2; i++) {
            try {
                statement = connection.prepareStatement("SELECT user_id, password FROM users WHERE username=?;");
                statement.setString(1, username);
                result = statement.executeQuery();
                result.first();
                savedUser = new User(result.getInt("user_id"), username, result.getString("password"));
                break;
            } catch (SQLException e) {
                this.tryReconnect();
            }
        }
        statement = null;
        result = null;
        return savedUser;
    }

    public void create(User user) throws SQLException{
        for (int i = 0; i < 2; i++) {
            try {
                statement = connection.prepareStatement("INSERT INTO users(username, password) VALUE (?,?);");
                statement.setString(1, user.getUsername());
                statement.setString(2, user.getPassword());
                statement.execute();
                break;
            } catch (SQLException e) {
                this.tryReconnect();
            }
        }
        statement = null;
        result = null;
    }

    private void tryReconnect() throws SQLException {
        if (System.currentTimeMillis() - this.connectionTime > 600000){
            this.connection = DbConnection.getConnection();
        }
    }


}
