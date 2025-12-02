package cm.enspy.xccm.repository;

import cm.enspy.xccm.domain.entity.Teacher;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface TeacherRepository extends R2dbcRepository<Teacher, UUID> {
    Mono<Teacher> findByEmail(String email);
}