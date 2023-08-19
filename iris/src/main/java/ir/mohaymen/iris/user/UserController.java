package ir.mohaymen.iris.user;

import ir.mohaymen.iris.utility.BaseController;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@AllArgsConstructor
@RequestMapping("/api/users")
public class UserController extends BaseController {

    private final UserService userService;

    @GetMapping("/{id}")
    public ResponseEntity<UserDto> getUserById(@PathVariable Long id) {
        UserDto userDto = UserMapper.mapToUserDto(userService.getById(id));
        return new ResponseEntity<>(userDto, HttpStatus.OK);
    }
    @GetMapping("/get-current-user")
    public ResponseEntity<UserDto> getCurrentUser() {
        UserDto userDto = UserMapper.mapToUserDto(getUserByToken());
        return new ResponseEntity<>(userDto, HttpStatus.OK);
    }

    @PutMapping("")
    public ResponseEntity<?> updateUser(@RequestBody @Valid EditUserDto editUserDto) {
        User user = getUserByToken();
        user.setFirstName(editUserDto.getFirstName());
        user.setLastName(editUserDto.getLastName());
        user.setUserName(editUserDto.getUserName());
        user.setBio(editUserDto.getBio());
        userService.createOrUpdate(user);
        return ResponseEntity.ok("success");
    }

}
