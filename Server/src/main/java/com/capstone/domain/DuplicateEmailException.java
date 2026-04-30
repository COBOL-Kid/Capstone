package com.capstone.domain;

public class DuplicateEmailException extends RuntimeException {

	public DuplicateEmailException() {
		super("Email is already registered");
	}
}
