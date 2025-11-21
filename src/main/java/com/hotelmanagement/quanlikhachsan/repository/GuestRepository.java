package com.hotelmanagement.quanlikhachsan.repository;

import com.hotelmanagement.quanlikhachsan.model.guest.Guest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface GuestRepository extends JpaRepository<Guest, UUID> {

    Optional<Guest> findByEmail(String email);

    Optional<Guest> findByKeycloakUserId(UUID keycloakUserId);

    boolean existsByEmail(String email);

    boolean existsByKeycloakUserId(UUID keycloakUserId);
    Optional<Guest> findByUserId(UUID UserId);

}
