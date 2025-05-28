package ncpl.bms.reports.service;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.jdbc.core.JdbcTemplate;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import ncpl.bms.reports.model.dto.UserDTO;
import org.springframework.jdbc.core.RowMapper;
import java.sql.ResultSet;
import java.sql.SQLException;

//---------------------COMPLETE FILE BY VISHAL---------------------

@Component
@Slf4j
public class UserService {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    public static String hashPassword(String password) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hashedPassword = md.digest(password.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte b : hashedPassword) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Error hashing password", e);
        }
    }

    public UserDTO loginUser(String username, String password) {
        String hashedPassword = hashPassword(password);
//        log.info(hashedPassword);
        String sql = "SELECT * FROM users WHERE username = ? AND password = ?";

        List<UserDTO> users = jdbcTemplate.query(sql, new Object[]{username, hashedPassword}, new UserRowMapper());

        if (!users.isEmpty()) {
            return users.get(0);  // Return the found user
        } else {
            return null;  // Return null if no user is found
        }
    }

    // RowMapper to map SQL result set to User object
    private static class UserRowMapper implements RowMapper<UserDTO> {
        @Override
        public UserDTO mapRow(ResultSet rs, int rowNum) throws SQLException {
            UserDTO user = new UserDTO();
            user.setId(rs.getLong("id"));
            user.setUsername(rs.getString("username"));
            user.setPassword(rs.getString("password"));
            user.setRole(rs.getString("role"));
            return user;
        }
    }

    public List<UserDTO> getAllUsers() {
        String sql = "SELECT * FROM users";
        return jdbcTemplate.query(sql, new UserRowMapper());
    }
}
