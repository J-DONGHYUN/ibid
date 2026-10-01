package project.kjhjdh.ibid.chat.presentation.dto;

import java.util.List;

import org.springframework.data.domain.Slice;

import project.kjhjdh.ibid.chat.application.MyChatRoomResult;

public record MyChatRoomListResponse(
        List<MyChatRoomResponse> rooms,
        Long nextCursor,
        boolean hasNext
) {

    public static MyChatRoomListResponse of(Slice<MyChatRoomResult> slice) {
        List<MyChatRoomResult> content = slice.getContent();
        Long nextCursor = slice.hasNext() && !content.isEmpty()
                ? content.get(content.size() - 1).lastMessage().getId()
                : null;
        return new MyChatRoomListResponse(
                content.stream().map(MyChatRoomResponse::from).toList(),
                nextCursor,
                slice.hasNext());
    }
}
