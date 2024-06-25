package ru.novikov.simple_crud.service;

import ru.novikov.simple_crud.exceptions.UserIsAlreadyExistsException;
import ru.novikov.simple_crud.exceptions.UserNotFoundException;
import ru.novikov.simple_crud.model.User;

public interface UserService {
    User createUser(User user) throws UserIsAlreadyExistsException;
    User updateUser(User user) throws UserNotFoundException;
    void deleteUser(long id) throws UserNotFoundException;
    User getUserById(long id) throws UserNotFoundException;
    long countRows();
}
