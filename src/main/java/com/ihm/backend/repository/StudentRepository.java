package cm.enspy.xccm.repository;

import cm.enspy.xccm.domain.entity.Student;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface StudentRepository extends R2dbcRepository<Student, UUID> {
    Mono<Student> findByEmail(String email);
}