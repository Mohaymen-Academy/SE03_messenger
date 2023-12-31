package ir.mohaymen.iris.message;

import ir.mohaymen.iris.chat.Chat;
import ir.mohaymen.iris.media.Media;
import ir.mohaymen.iris.user.User;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.Instant;

@Entity
@Table(name = "messages")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Message {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long messageId;

    @JoinColumn(name = "reply_id")
    @ManyToOne
    @OnDelete(action = OnDeleteAction.SET_NULL)
    private Message repliedMessage;

    @Column(columnDefinition = "TEXT")
    private String text;

    @JoinColumn(name = "chat_id")
    @ManyToOne
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Chat chat;

    @JoinColumn(name = "origin_message_id")
    @ManyToOne
    private Message originMessage;

    @JoinColumn(name = "user_id")
    @ManyToOne
    private User sender;

    @JoinColumn(name = "media_id")
    @OneToOne
    private Media media;

    @NotNull
    private Instant sendAt;

    private Instant editedAt;
}
