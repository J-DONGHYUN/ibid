package project.kjhjdh.ibid.chat.application;

import org.springframework.data.domain.Slice;

import project.kjhjdh.ibid.chat.domain.ChatMessage;

public record MessageListResult(
        Slice<ChatMessage> messages,
        Long sellerLastReadMessageId,
        Long buyerLastReadMessageId
) {
}
