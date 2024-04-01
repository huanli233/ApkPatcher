package com.huanli233.apkpatcher.event;

import java.util.logging.Level;

public class MessageEvent {
	Level level;
	
	String message;

	/**
	 * @return level
	 */
	public Level getLevel() {
		return level;
	}

	/**
	 * @param level 要设置的 level
	 */
	public void setLevel(Level level) {
		this.level = level;
	}

	/**
	 * @return message
	 */
	public String getMessage() {
		return message;
	}

	/**
	 * @param message 要设置的 message
	 */
	public void setMessage(String message) {
		this.message = message;
	}

	public MessageEvent(Level level, String message) {
		this.level = level;
		this.message = message;
	}
	
}
