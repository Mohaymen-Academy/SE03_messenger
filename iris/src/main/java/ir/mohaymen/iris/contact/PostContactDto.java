package ir.mohaymen.iris.contact;

import ir.mohaymen.iris.user.User;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
public class PostContactDto {
    private Long id;
    private Long secondUserId;
    private String firstName;
    private String lastName;
    private Instant lastSeen;
}
