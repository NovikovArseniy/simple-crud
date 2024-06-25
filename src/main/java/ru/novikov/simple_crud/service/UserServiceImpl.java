package ru.novikov.simple_crud.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.novikov.simple_crud.exceptions.UserIsAlreadyExistsException;
import ru.novikov.simple_crud.exceptions.UserNotFoundException;
import ru.novikov.simple_crud.model.User;
import ru.novikov.simple_crud.repository.UserRepository;

import java.util.Optional;

@Service
@Transactional
public class UserServiceImpl implements UserService{

    @Autowired
    private UserRepository userRepository;

    @Override
    public User createUser(User user) throws UserIsAlreadyExistsException{
        Optional<User> userFromDb = userRepository.findById(user.getId());
        if (userFromDb.isPresent()){
            throw new UserIsAlreadyExistsException("User with id = " + user.getId() + "is already exists");
        } else {
            return userRepository.save(user);
        }
    }

    @Override
    public User updateUser(User user) throws UserNotFoundException{
        Optional<User> userFromDb = userRepository.findById(user.getId());
        if (userFromDb.isPresent()){
            User userUpdate = userFromDb.get();
            userUpdate.setId(user.getId());
            userUpdate.setName(user.getName());
            userUpdate.setSurname(user.getSurname());
            userUpdate.setAge(user.getAge());
            userRepository.save(userUpdate);
            return userUpdate;
        } else {
            throw new UserNotFoundException("User with id = " + user.getId() + "not found");
        }
    }

    @Override
    public void deleteUser(long id) throws UserNotFoundException{
        Optional<User> userFromDB = userRepository.findById(id);
        if (userFromDB.isPresent()){
            userRepository.delete(userFromDB.get());
        } else {
            throw new UserNotFoundException("User with id = " + id + "not found");
        }
    }

    @Override
    public User getUserById(long id) throws UserNotFoundException{
        Optional<User> userFromDB = userRepository.findById(id);
        if (userFromDB.isPresent()){
            return userFromDB.get();
        } else {
            throw new UserNotFoundException("User with id = " + id + "not found");
        }
    }

    @Override
    public long countRows() {
        return userRepository.count();
    }
}
