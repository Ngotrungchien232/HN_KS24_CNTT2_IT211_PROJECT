package com.project_cuoimon.repository;

import com.project_cuoimon.entity.KycProfile;
import com.project_cuoimon.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface KycProfileRepository extends JpaRepository<KycProfile, Long> {
    Optional<KycProfile> findByUser(User user);
    boolean existsByIdNumber(String idNumber);
}
