package dev.langchain4j.memory.chat;

import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.memory.ChatMemory;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Lightweight local implementation that keeps only the latest messages
 * in a fixed-size window while exposing the LangChain4j ChatMemory contract.
 */
public final class MessageWindowChatMemory implements ChatMemory {

	private static final AtomicLong SEQUENCE = new AtomicLong(1L);

	private final Object id;
	private final int maxMessages;
	private final List<ChatMessage> messages = new ArrayList<>();

	private MessageWindowChatMemory(int maxMessages) {
		if (maxMessages < 2) {
			throw new IllegalArgumentException("maxMessages must be greater than or equal to 2.");
		}
		this.id = SEQUENCE.getAndIncrement();
		this.maxMessages = maxMessages;
	}

	public static MessageWindowChatMemory withMaxMessages(int maxMessages) {
		return new MessageWindowChatMemory(maxMessages);
	}

	@Override
	public Object id() {
		return id;
	}

	@Override
	public synchronized void add(ChatMessage message) {
		messages.add(message);
		if (messages.size() <= maxMessages) {
			return;
		}
		int overflow = messages.size() - maxMessages;
		messages.subList(0, overflow).clear();
	}

	@Override
	public synchronized List<ChatMessage> messages() {
		return List.copyOf(messages);
	}

	@Override
	public synchronized void clear() {
		messages.clear();
	}
}
