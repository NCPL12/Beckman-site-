package ncpl.bms.reports.controller;
import lombok.extern.slf4j.Slf4j;
import ncpl.bms.reports.model.dto.UserDTO;
import ncpl.bms.reports.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

//--------------------COMPLETE FILE IS WRITTEN BY VISHAL----------------------//

@RestController
@RequestMapping("v1")
@CrossOrigin(origins = "http://localhost:4200")
@Slf4j
public class UserController {


    @Autowired
    private UserService userService;

    @PostMapping("/login")
    public ResponseEntity<UserDTO> loginUser(@RequestBody UserDTO loginRequest) {
        UserDTO userDTO = userService.loginUser(loginRequest.getUsername(), loginRequest.getPassword());
        if (userDTO != null) {
            return ResponseEntity.ok(userDTO);  // Return user details if login is successful
        } else {
            return ResponseEntity.status(401).build();  // Return unauthorized if login fails
        }
    }

    @GetMapping("/users")
    public ResponseEntity<List<UserDTO>> getAllUsers() {
        List<UserDTO> users = userService.getAllUsers();
        return ResponseEntity.ok(users);
    }
}