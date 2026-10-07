package com.realestate.messaging.repository;

import com.realestate.messaging.model.User;
import com.realestate.messaging.model.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmailIgnoreCase(String email);

    @Query("""
            select u from User u
            where u.broker.id = :brokerId
              and u.role <> com.realestate.messaging.model.Role.ADMIN
              and (lower(u.name) like lower(concat('%', :search, '%'))
                   or lower(coalesce(u.email, '')) like lower(concat('%', :search, '%')))
            order by u.createdAt desc, u.name asc
            """)
    List<User> searchTeamMembersForWorkspace(
            @Param("brokerId") Long brokerId,
            @Param("search") String search,
            Pageable pageable
    );

    @Query("""
            select u from User u
            where u.broker.id = :brokerId
              and u.role <> com.realestate.messaging.model.Role.ADMIN
              and (:role is null or u.role = :role)
              and (:enabled is null or u.enabled = :enabled)
              and (:search is null or
                   lower(u.name) like lower(concat('%', :search, '%')) or
                   lower(coalesce(u.email, '')) like lower(concat('%', :search, '%')))
            order by u.createdAt desc, u.name asc
            """)
    List<User> findTeamMembers(
            @Param("brokerId") Long brokerId,
            @Param("role") Role role,
            @Param("enabled") Boolean enabled,
            @Param("search") String search
    );

}
