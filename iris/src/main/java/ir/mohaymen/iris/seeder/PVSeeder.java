package ir.mohaymen.iris.seeder;

import ir.mohaymen.iris.chat.Chat;
import ir.mohaymen.iris.chat.ChatRepository;
import ir.mohaymen.iris.chat.ChatType;
import ir.mohaymen.iris.subscription.Subscription;
import ir.mohaymen.iris.subscription.SubscriptionRepository;
import ir.mohaymen.iris.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
@RequiredArgsConstructor
public class PVSeeder implements Seeder {

    private final ChatRepository chatRepository;
    private final SubscriptionRepository subscriptionRepository;

    static final int NUMBER_OF_INSTANCES = 30;
    private final List<Subscription> subscriptionList = new ArrayList<>();
    private final List<Chat> pvs = new ArrayList<>();
    private List<Chat> savedChats;

    @Override
    public void load() {
        if (chatRepository.count() >= ChatSeeder.NUMBER_OF_INSTANCES + NUMBER_OF_INSTANCES) return;

        for (int i = 0; i < NUMBER_OF_INSTANCES; i++)
            generateRandomPV();
        savedChats = chatRepository.saveAll(pvs);

        for (int i = 0; i < NUMBER_OF_INSTANCES; i++)
            generateRandomSubscription(i);
        subscriptionRepository.saveAll(subscriptionList);
    }

    private void generateRandomPV() {
        long id = Long.parseLong(faker.regexify("\\d{1,5}"));
        String title = null;
        String link = null;
        ChatType chatType = ChatType.PV;
        String bio = id % 3 == 0 ? faker.regexify("(\\w|\\d| |_|,|\\.){5,50}") : null;
        boolean isPublic = false;

        Chat chat = new Chat();
        chat.setTitle(title);
        chat.setLink(link);
        chat.setPublic(isPublic);
        chat.setChatType(chatType);
        chat.setBio(bio);

        pvs.add(chat);
    }

    private void generateRandomSubscription(int chatId) {
        long userId1 = faker.random().nextInt(1, UserSeeder.NUMBER_OF_INSTANCES);
        long userId2;
        do {
            userId2 = faker.random().nextInt(1, UserSeeder.NUMBER_OF_INSTANCES);
        } while (userId1 == userId2);

        User user1 = new User();
        user1.setUserId(userId1);
        User user2 = new User();
        user2.setUserId(userId2);


        Chat chat = savedChats.get(chatId);

        Subscription subscription1 = new Subscription();
        subscription1.setUser(user1);
        subscription1.setChat(chat);
        Subscription subscription2 = new Subscription();
        subscription2.setUser(user2);
        subscription2.setChat(chat);

        subscriptionList.add(subscription1);
        subscriptionList.add(subscription2);
    }
}
