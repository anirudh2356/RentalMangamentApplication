package com.realestate.messaging.repository;

import com.realestate.messaging.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {
}
