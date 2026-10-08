package com.studychain.services;

public class GoogleCalendarException extends RuntimeException {

	public GoogleCalendarException(String message) {
		super(message);
	}

	public GoogleCalendarException(String message, Throwable cause) {
		super(message, cause);
	}
}
