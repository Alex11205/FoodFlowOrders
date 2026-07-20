package com.alex.foodfloworders.exceptions;

public class NoSuchOrderException extends RuntimeException {
  public NoSuchOrderException(String message) {
    super(message);
  }
}
