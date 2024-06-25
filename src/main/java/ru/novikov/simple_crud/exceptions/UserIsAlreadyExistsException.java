package ru.novikov.simple_crud.exceptions;

public class UserIsAlreadyExistsException extends Exception{
    public UserIsAlreadyExistsException(String message){
        super(message);
    }
}
