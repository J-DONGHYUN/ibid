package project.kjhjdh.ibid.chat.presentation.dto;

import java.util.List;

import org.springframework.data.domain.Slice;

import project.kjhjdh.ibid.chat.domain.ChatMessage;

public record ChatMessageListResponse(
        List<ChatMessageResponse> messages,
        Long nextCursor,
        boolean hasNext
) {

    public static ChatMessageListResponse of(Slice<ChatMessage> slice) {
        List<ChatMessage> content = slice.getContent();
        Long nextCursor = slice.hasNext() && !content.isEmpty()
                ? content.get(content.size() - 1).getId()
                : null;
        return new ChatMessageListResponse(
                content.stream().map(ChatMessageResponse::from).toList(),
                nextCursor,
                slice.hasNext()
        );
    }
}
