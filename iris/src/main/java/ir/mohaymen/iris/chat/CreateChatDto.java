package ir.mohaymen.iris.chat;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;

@Getter
@AllArgsConstructor
@Setter
@NoArgsConstructor
public class CreateChatDto {
    private String title;
    private String bio;
    private String link;
    private boolean isPublic;
    private ChatType chatType;
    private ArrayList<Long> userIds;
}
