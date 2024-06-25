package ru.novikov.simple_crud.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.novikov.simple_crud.model.User;

public interface UserRepository extends JpaRepository<User,Long> {
}
