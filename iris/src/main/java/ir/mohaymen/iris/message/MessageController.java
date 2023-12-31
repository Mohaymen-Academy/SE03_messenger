package ir.mohaymen.iris.message;

import ir.mohaymen.iris.chat.Chat;
import ir.mohaymen.iris.chat.ChatService;
import ir.mohaymen.iris.chat.ChatType;
import ir.mohaymen.iris.contact.ContactService;
import ir.mohaymen.iris.file.FileService;
import ir.mohaymen.iris.media.Media;
import ir.mohaymen.iris.media.MediaService;
import ir.mohaymen.iris.permission.Permission;
import ir.mohaymen.iris.permission.PermissionService;
import ir.mohaymen.iris.subscription.SubDto;
import ir.mohaymen.iris.subscription.SubscriptionService;
import ir.mohaymen.iris.user.User;
import ir.mohaymen.iris.utility.BaseController;
import ir.mohaymen.iris.utility.EncryptionUtils;
import ir.mohaymen.iris.utility.Nameable;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.text.MessageFormat;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static org.springframework.web.bind.annotation.RequestMethod.POST;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/messages")
public class MessageController extends BaseController {

    private final MessageService messageService;
    private final ModelMapper modelMapper;
    private final ChatService chatService;
    private final MediaService mediaService;
    private final SubscriptionService subscriptionService;
    private final ContactService contactService;
    private final FileService fileService;
    private final PermissionService permissionService;
    private final EncryptionUtils encryptionUtils;

    private final Logger logger = LoggerFactory.getLogger(MessageController.class);

    @GetMapping("/get-messages/{chatId}/{floor}/{ceil}")
    public ResponseEntity<List<GetMessageDto>> getMessages(@PathVariable("chatId") Long chatId,
                                                           @PathVariable("floor") Integer floor, @PathVariable("ceil") Integer ceil) {
        if (ceil - floor > 50)
            throw new HttpClientErrorException(HttpStatus.BAD_REQUEST);
        Chat chat = new Chat(); chat.setChatId(chatId);
        List<Message> messages = messageService.getByChat(chat);
        if (messages.size() < ceil)
            ceil = (messages.size());
        if (floor < 0)
            throw new HttpClientErrorException(HttpStatus.BAD_REQUEST);
        List<GetMessageDto> getMessageDtoList = new ArrayList<>();
        for (Message message : messages.subList(messages.size() - ceil, messages.size() - floor)) {
            getMessageDtoList.add(mapMessageToGetMessageDto(message));
        }
        List<GetMessageDto> sorted = getMessageDtoList.stream()
                .sorted(Comparator.comparing(GetMessageDto::getMessageId))
                .collect(Collectors.toList());
        return new ResponseEntity<>(sorted, HttpStatus.OK);
    }

    @GetMapping("/seen-users/{messageId}")
    public ResponseEntity<List<SubDto>> usersSeen(@PathVariable Long messageId) {
        Chat chat = messageService.getById(messageId).getChat();
        if (chat.getChatType() == ChatType.CHANNEL)
            return new ResponseEntity<>(null, HttpStatus.FORBIDDEN);
        List<SubDto> users = new ArrayList<>();
        messageService.getSubSeen(messageId, chat.getChatId()).forEach(s -> {
            SubDto subDto = new SubDto();
            Nameable nameable = subscriptionService.setName(contactService.getContactByFirstUser(getUserByToken()),
                    s.getUser());
            subDto.setFirstName(nameable.getFirstName());
            subDto.setLastName(nameable.getLastName());
            subDto.setUserId(s.getUser().getUserId());
            users.add(subDto);
        });
        return new ResponseEntity<>(users, HttpStatus.OK);
    }

    @GetMapping("/seen-user-count/{messageId}")
    public ResponseEntity<Integer> userSeenCount (@PathVariable Long messageId) {
        Message message = messageService.getById(messageId);
        return new ResponseEntity<>(messageService.usersSeen(messageId, message.getChat().getChatId()).size(), HttpStatus.OK);
    }

    @DeleteMapping("/delete-message/{id}")
    public ResponseEntity<?> deleteMessage(@PathVariable Long id) {
        Message message = messageService.getById(id);
        Chat chat = message.getChat();
        User user = getUserByToken();
        if (!chatService.isInChat(chat, user)
                || !permissionService.hasAccessToDeleteMessage(message, user.getUserId(), chat)) {
            logger.info(MessageFormat.format("user with phoneNumber:{0} does not have access to delete message in chat{1}!",
                    user.getPhoneNumber(), chat.getChatId()));
            throw new HttpClientErrorException(HttpStatus.FORBIDDEN);
        }
        messageService.deleteById(id);
        return ResponseEntity.ok("If you only could delete feelings the same way you delete a text message");
    }

    @RequestMapping(path = "/send-message", method = POST, consumes = {MediaType.MULTIPART_FORM_DATA_VALUE})
    public ResponseEntity<GetMessageDto> sendMessage(@ModelAttribute @Valid MessageDto messageDto) throws IOException {
        User user = getUserByToken();
        Chat chat = new Chat(){{
            setChatId(messageDto.getChatId());
        }};

        logger.info(MessageFormat.format("user with phoneNumber:{0} attempts to send message in chat:{1}!",
                user.getPhoneNumber(), chat.getChatId()));

        Message repliedMessage = (messageDto.getRepliedMessageId() != null)
                ? Message.builder().messageId(messageDto.getRepliedMessageId()).build()
                : null;

        if (!chatService.isInChat(chat, user)
                || !permissionService.hasAccess(user.getUserId(), messageDto.getChatId(), Permission.SEND_MESSAGE)) {
            logger.info(
                    MessageFormat.format("user with phoneNumber:{0} does not have access to send message in chat{1}!",
                            user.getPhoneNumber(), chat.getChatId()));
            throw new HttpClientErrorException(HttpStatus.FORBIDDEN);
        } else if (repliedMessage != null && !messageService.getChatIdByMessageId(repliedMessage.getMessageId()).equals(chat.getChatId())) {
            logger.info(MessageFormat.format(
                    "user with phoneNumber:{0} attempts to reply a message which is in another chat!",
                    user.getPhoneNumber()));
            throw new HttpClientErrorException(HttpStatus.BAD_REQUEST);
        }

        MultipartFile file = messageDto.getFile();
        Media media = null;
        if (file == null || file.isEmpty()) {
            if (messageDto.getText().isBlank()) throw new HttpClientErrorException(HttpStatus.BAD_REQUEST);
        } else media = fileService.saveFile(file.getOriginalFilename(), file);

        Message message = new Message();
        message.setRepliedMessage(repliedMessage);
        message.setText(encryptionUtils.encrypt(messageDto.getText()));
        message.setChat(chat);
        message.setSender(user);
        message.setMedia(media);
        message.setSendAt(Instant.now());
        message.setRepliedMessage(repliedMessage);

        GetMessageDto getMessageDto = mapMessageToGetMessageDto(message);
         subscriptionService.updateLastSeenMessage(chat.getChatId() , user.getUserId() , getMessageDto.getMessageId());
        return new ResponseEntity<>(getMessageDto, HttpStatus.OK);
    }

    @PatchMapping("/edit-message")
    public ResponseEntity<?> editMessage(@RequestBody @Valid EditMessageDto editMessageDto) {
        var user = getUserByToken();
        Message message = messageService.getById(editMessageDto.getMessageId());
        if (!Objects.equals(message.getSender().getUserId(), user.getUserId())) {
            logger.info(MessageFormat.format("user with phoneNumber:{0} wants to edit message with id:{1}!",
                    user.getPhoneNumber(), message.getMessageId()));
            return new ResponseEntity<>("Access violation", HttpStatus.FORBIDDEN);
        }
        message.setText(editMessageDto.getText());
        message.setEditedAt(Instant.now());
        return new ResponseEntity<>(mapMessageToGetMessageDto(message), HttpStatus.OK);
    }

    @PostMapping("/forward-message/{chatId}/{messageId}")
    public ResponseEntity<SendForwardMessageDto> forwardMessage(@PathVariable Long chatId, @PathVariable Long messageId) {
        Message newMessage = generateForwardMessage(chatId, messageId);

        Message savedMessage = messageService.createOrUpdate(newMessage);
        return new ResponseEntity<>(mapMessageToForwardMessageDto(savedMessage), HttpStatus.OK);
    }

    @PostMapping("/forward-message/{chatId}")
    public ResponseEntity<List<SendForwardMessageDto>> forwardMessage(@PathVariable Long chatId,
                                                                      @RequestBody @Valid List<Long> messageIds) {
        logger.info(MessageFormat.format("user with phone number:{0} attempts to forward {1} message(s) to chat:{2}",
                getUserByToken().getPhoneNumber(), messageIds.size(), chatId));

        List<Message> messages = messageIds.stream().map(messageId -> generateForwardMessage(chatId, messageId)).toList();
        messageService.createOrUpdate(messages);
        List<SendForwardMessageDto> forwardMessageDtos = messages.stream().map(this::mapMessageToForwardMessageDto).toList();

        return new ResponseEntity<>(forwardMessageDtos, HttpStatus.OK);
    }

    private Message generateForwardMessage(long chatId, long messageId) {
        User user = getUserByToken();
        Chat newChat =new Chat(){{
            setChatId(chatId);
        }};

        GetForwardMessageDto originMessage = messageService.getForwardMessageDto(messageId);

        logger.info(MessageFormat.format("user with phone number:{0} attempts to forward message:{1} to chat:{2}!",
                user.getPhoneNumber(), messageId, chatId));

        if (!chatService.isInChat(newChat, user)
                || !permissionService.hasAccess(user.getUserId(), newChat.getChatId(), Permission.SEND_MESSAGE)) {
            logger.info(MessageFormat.format(
                    "user with phoneNumber:{0} does not have access to forward message in chat{1}!",
                    user.getPhoneNumber(), newChat.getChatId()));
            throw new HttpClientErrorException(HttpStatus.FORBIDDEN);
        } else if (!chatService.isInChat(new Chat(){{setChatId(originMessage.getChatId());}}, user)) {
            logger.info(MessageFormat.format(
                    "user with phone number:{0} does not have access to message:{1} to forward it!",
                    user.getPhoneNumber(), messageId));
            throw new HttpClientErrorException(HttpStatus.BAD_REQUEST);
        }

        Message newMessage = new Message();
        newMessage.setChat(newChat);
        newMessage.setOriginMessage(Message.builder().messageId(messageId).build());
        newMessage.setSender(user);
        newMessage.setText(originMessage.getText());
        newMessage.setSendAt(Instant.now());

        Media media = messageService.getMediaByMessageId(messageId);
        if (media != null) {
            Media newMedia = fileService.duplicateMediaById(media);
            newMessage.setMedia(newMedia);
        }

        return newMessage;
    }

    private GetMessageDto mapMessageToGetMessageDto(Message message) {
        GetMessageDto getMessageDto = modelMapper.map(messageService.createOrUpdate(message), GetMessageDto.class);
        getMessageDto.setUserId(message.getSender().getUserId());
        getMessageDto.setText(encryptionUtils.decrypt(message.getText()));
        getMessageDto.setSeen(
                messageService.usersSeen(message.getMessageId(), message.getChat().getChatId()).size() > 1);
        if (message.getRepliedMessage() != null)
            getMessageDto.setRepliedMessagePlacement(
                    messageService.messagePlacementInChat(message.getRepliedMessage().getMessageId() , message.getChat().getChatId())
            );
        return getMessageDto;
    }

    private SendForwardMessageDto mapMessageToForwardMessageDto(Message message) {
        SendForwardMessageDto forwardMessageDto = modelMapper.map(message, SendForwardMessageDto.class);
        forwardMessageDto.setUserId(message.getSender().getUserId());
        return forwardMessageDto;
    }
}
