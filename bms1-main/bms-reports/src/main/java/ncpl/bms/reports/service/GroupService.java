package ncpl.bms.reports.service;

import ncpl.bms.reports.model.dto.GroupDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class GroupService {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    public void saveGroup(GroupDTO groupDTO) {
        String sql = "INSERT INTO dbo.group_names (name) VALUES (?)";
        jdbcTemplate.update(sql, groupDTO.getName());
    }

}
