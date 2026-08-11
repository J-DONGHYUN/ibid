package project.kjhjdh.ibid.product.infra;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import project.kjhjdh.ibid.product.domain.Tag;

public interface TagRepository extends JpaRepository<Tag, Long> {

    Optional<Tag> findByName(String name);
}
