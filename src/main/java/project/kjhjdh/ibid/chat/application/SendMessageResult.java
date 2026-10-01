package project.kjhjdh.ibid.chat.application;

import project.kjhjdh.ibid.chat.domain.ChatMessage;

public record SendMessageResult(ChatMessage message, boolean created) {
}
