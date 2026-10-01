package project.kjhjdh.ibid.chat.presentation.dto;

import java.util.List;

import org.springframework.data.domain.Slice;

import project.kjhjdh.ibid.chat.application.MessageListResult;
import project.kjhjdh.ibid.chat.domain.ChatMessage;

public record ChatMessageListResponse(
        List<ChatMessageResponse> messages,
        Long nextCursor,
        boolean hasNext,
        Long sellerLastReadMessageId,
        Long buyerLastReadMessageId
) {

    public static ChatMessageListResponse of(MessageListResult result) {
        Slice<ChatMessage> slice = result.messages();
        List<ChatMessage> content = slice.getContent();
        Long nextCursor = slice.hasNext() && !content.isEmpty()
                ? content.get(content.size() - 1).getId()
                : null;
        return new ChatMessageListResponse(
                content.stream().map(ChatMessageResponse::from).toList(),
                nextCursor,
                slice.hasNext(),
                result.sellerLastReadMessageId(),
                result.buyerLastReadMessageId()
        );
    }
}
