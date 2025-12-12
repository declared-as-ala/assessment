package com.chess.repository;

import com.chess.model.Invitation;
import com.chess.model.Invitation.InvitationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InvitationRepository extends JpaRepository<Invitation, Long> {
    List<Invitation> findByToUserIdAndStatus(Long toUserId, InvitationStatus status);
    List<Invitation> findByFromUserIdAndStatus(Long fromUserId, InvitationStatus status);
    Optional<Invitation> findByIdAndToUserId(Long id, Long toUserId);
}



