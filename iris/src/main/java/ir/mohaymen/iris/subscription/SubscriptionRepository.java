package ir.mohaymen.iris.subscription;

import ir.mohaymen.iris.chat.Chat;
import ir.mohaymen.iris.user.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {

    Iterable<Subscription> findSubscriptionByUser(User user);

    Iterable<Subscription> findSubscriptionByChat(Chat chat);
}