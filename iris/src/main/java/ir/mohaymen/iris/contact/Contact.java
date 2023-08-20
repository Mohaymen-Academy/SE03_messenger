package ir.mohaymen.iris.contact;

import ir.mohaymen.iris.user.User;
import ir.mohaymen.iris.utility.Nameable;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "contacts")
@Getter
@Setter
public class Contact implements Nameable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "userId")
    private User firstUser;

    @ManyToOne
    private User secondUser;

    @NotBlank
    private String firstName;

    private String lastName;
    @Override
    public String fullName() {
        String name = firstName;
        if(lastName!=null)
            name =name + " " + lastName;
        return name;
    }
}
