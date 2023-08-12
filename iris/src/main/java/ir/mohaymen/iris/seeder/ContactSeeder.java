package ir.mohaymen.iris.seeder;

import com.github.javafaker.Name;
import ir.mohaymen.iris.contact.Contact;
import ir.mohaymen.iris.contact.ContactRepository;
import ir.mohaymen.iris.user.User;
import ir.mohaymen.iris.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ContactSeeder implements Seeder {

    private final ContactRepository contactRepository;
    private final UserRepository userRepository;

    @Override
    public void load() {
        final int NUMBER_OF_INSTANCES = 200;

        for (int i = 0; i < NUMBER_OF_INSTANCES; i++) {
            Contact contact = generateRandomUser();
            if (contact != null) contactRepository.save(contact);
        }
    }

    private Contact generateRandomUser() {
        long id = Long.parseLong(fakeValuesService.regexify("\\d{1-5}"));

        long firstUserId = Long.parseLong(fakeValuesService.regexify("\\d{2}"));
        User firstUser = userRepository.findById(firstUserId).orElse(null);

        long SecondUserId = Long.parseLong(fakeValuesService.regexify("\\d{2}"));
        User secondUser = userRepository.findById(SecondUserId).orElse(null);

        if (firstUser == null || secondUser == null || firstUser == secondUser) return null;

        Name name = faker.name();
        String firstName = name.firstName();
        String lastName = id % 2 == 1 ? name.lastName() : null;

        Contact contact = new Contact();
        contact.setId(id);
        contact.setFirstUser(firstUser);
        contact.setSecondUser(secondUser);
        contact.setFirstName(firstName);
        contact.setLastName(lastName);
        return contact;
    }
}