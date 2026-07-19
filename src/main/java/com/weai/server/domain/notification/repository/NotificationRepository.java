package com.weai.server.domain.notification.repository;

import com.weai.server.domain.notification.domain.Notification;
import com.weai.server.domain.notification.domain.NotificationType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

	@Query("""
		select n
		from Notification n
		join n.project p
		where p.id = :projectId
		  and n.receiver.id = :receiverUserId
		  and n.deletedAt is null
		  and (:type is null or n.type = :type)
		  and (:isRead is null or n.isRead = :isRead)
		""")
	Page<Notification> findByProjectIdAndReceiverUserIdWithFilters(
		@Param("projectId") Long projectId,
		@Param("receiverUserId") Long receiverUserId,
		@Param("type") NotificationType type,
		@Param("isRead") Boolean isRead,
		Pageable pageable
	);

	Optional<Notification> findByIdAndProject_IdAndReceiver_IdAndDeletedAtIsNull(
		Long notificationId,
		Long projectId,
		Long receiverUserId
	);

	long countByProject_IdAndReceiver_IdAndIsReadFalseAndDeletedAtIsNull(Long projectId, Long receiverUserId);

	List<Notification> findByProject_IdAndReceiver_IdAndIsReadFalseAndDeletedAtIsNull(Long projectId, Long receiverUserId);
}
